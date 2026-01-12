package com.tgo.rtc.manager

import com.tgo.rtc.TgoRTC
import com.tgo.rtc.entity.TgoConnectStatus
import com.tgo.rtc.entity.TgoRoomInfo
import com.tgo.rtc.entity.TgoVideoInfo
import com.tgo.rtc.utils.TgoLogger
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Manager for handling room connection and events.
 */
class TgoRoomManager private constructor() {

    companion object {
        val instance: TgoRoomManager by lazy { TgoRoomManager() }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timeoutJob: Job? = null

    var room: Room? = null
        private set

    var currentRoomInfo: TgoRoomInfo? = null
        private set

    private val connectListeners = mutableListOf<(String, TgoConnectStatus, String) -> Unit>()
    private val videoInfoListeners = mutableListOf<(TgoVideoInfo) -> Unit>()
    private var currentVideoInfo = TgoVideoInfo.EMPTY

    fun addConnectListener(listener: (String, TgoConnectStatus, String) -> Unit) {
        connectListeners.add(listener)
    }

    fun removeConnectListener(listener: (String, TgoConnectStatus, String) -> Unit) {
        connectListeners.remove(listener)
    }

    private fun setConnectStatus(roomName: String, status: TgoConnectStatus, reason: String) {
        // Use a copy to avoid ConcurrentModificationException
        connectListeners.toList().forEach { it(roomName, status, reason) }
    }

    fun addVideoInfoListener(listener: (TgoVideoInfo) -> Unit) {
        videoInfoListeners.add(listener)
        if (currentVideoInfo.isValid) listener(currentVideoInfo)
    }

    fun removeVideoInfoListener(listener: (TgoVideoInfo) -> Unit) {
        videoInfoListeners.remove(listener)
    }

    private fun notifyVideoInfoChanged(info: TgoVideoInfo) {
        if (currentVideoInfo == info) return
        currentVideoInfo = info
        TgoLogger.info("[Video] Stats updated: $info")
        videoInfoListeners.toList().forEach { it(info) }
    }

     suspend fun joinRoom(
        roomInfo: TgoRoomInfo,
        micEnabled: Boolean = false,
        cameraEnabled: Boolean = false
    ) {
        if (currentRoomInfo != null) {
            TgoLogger.error("already in room")
            return
        }
        currentRoomInfo = roomInfo
        setConnectStatus(roomInfo.roomName, TgoConnectStatus.CONNECTING, "connecting")

        val newRoom = LiveKit.create(TgoRTC.instance.getContext())
        room = newRoom

        scope.launch {
            newRoom.events.collect { event ->
                when (event) {
                    is RoomEvent.Disconnected -> {
                        setConnectStatus(
                            roomInfo.roomName,
                            TgoConnectStatus.DISCONNECTED,
                            "disconnected"
                        )
                        TgoRTC.instance.participantManager.getLocalParticipantOrNull()?.notifyLeave()
                    }

                    is RoomEvent.Reconnecting -> {
                        setConnectStatus(
                            roomInfo.roomName,
                            TgoConnectStatus.CONNECTING,
                            "reconnecting"
                        )
                    }

                    is RoomEvent.Connected -> {
                        setConnectStatus(roomInfo.roomName, TgoConnectStatus.CONNECTED, "connected")
                        newRoom.localParticipant.let {
                            TgoRTC.instance.participantManager.getLocalParticipant()
                                .setLocalParticipant(it)
                            TgoRTC.instance.participantManager.getLocalParticipant().notifyJoined()
                        }
                    }

                    is RoomEvent.ParticipantConnected -> {
                        TgoRTC.instance.participantManager.setParticipantJoin(event.participant)
                    }

                    is RoomEvent.ParticipantDisconnected -> {
                        TgoRTC.instance.participantManager.setParticipantLeave(event.participant)
                    }

                    else -> {}
                }
            }
        }

        try {
            newRoom.connect(
                url = roomInfo.url,
                token = roomInfo.token,
                options = ConnectOptions()
            )

            newRoom.localParticipant.setMicrophoneEnabled(micEnabled)
            newRoom.localParticipant.setCameraEnabled(cameraEnabled)

            startTimeoutChecker(roomInfo.timeout)
        } catch (e: Exception) {
            TgoLogger.error("Failed to connect to room", e)
            setConnectStatus(
                roomInfo.roomName,
                TgoConnectStatus.DISCONNECTED,
                e.message ?: "unknown error"
            )
            leaveRoom()
        }
    }

    private fun startTimeoutChecker(timeoutSeconds: Int) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            while (isActive) {
                checkParticipantsTimeout(timeoutSeconds)
                delay(1000)
            }
        }
    }

    private fun checkParticipantsTimeout(timeoutSeconds: Int) {
        val now = Date()
        val participants =
            TgoRTC.instance.participantManager.getRemoteParticipants(includeTimeout = true)

        for (participant in participants) {
            if (participant.isLocal) continue

            if (participant.isJoined()) {
                if (participant.isTimeout()) participant.setTimeout(false)
                continue
            }

            val elapsed = (now.time - participant.getCreatedAt().time) / 1000
            if (elapsed >= timeoutSeconds && !participant.isTimeout()) {
                participant.setTimeout(true)
                TgoLogger.info("参与者 ${participant.uid} 超时未加入")
            }
        }
    }

    private fun stopTimeoutChecker() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

     fun leaveRoom() {
        stopTimeoutChecker()
        try {
            room?.disconnect()
        } catch (e: Exception) {
            // ignore
        }
        room = null
        currentRoomInfo = null
        TgoRTC.instance.participantManager.clear()
    }
}
