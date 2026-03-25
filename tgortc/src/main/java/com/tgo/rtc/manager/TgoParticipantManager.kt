package com.tgo.rtc.manager

import com.tgo.rtc.TgoRTC
import com.tgo.rtc.participant.TgoParticipant
import com.tgo.rtc.utils.TgoLogger
import io.livekit.android.room.participant.RemoteParticipant

/**
 * Manager for handling local and remote participants in a room.
 */
class TgoParticipantManager private constructor() {

    companion object {
        val instance: TgoParticipantManager by lazy { TgoParticipantManager() }
    }

    private var localParticipant: TgoParticipant? = null
    private val remoteParticipants = mutableMapOf<String, TgoParticipant>()
    private val newParticipantListeners = mutableListOf<(TgoParticipant) -> Unit>()

    /**
     * Get local participant. Returns null if room info is not available.
     */
    fun getLocalParticipantOrNull(): TgoParticipant? {
        val roomInfo = TgoRTC.instance.roomManager.currentRoomInfo ?: return localParticipant

        if (localParticipant == null) {
            localParticipant = TgoParticipant(roomInfo.loginUID)
        }

        TgoRTC.instance.roomManager.room?.localParticipant?.let {
            localParticipant?.setLocalParticipant(it)
        }

        return localParticipant
    }

    /**
     * Get local participant. Throws if room info is not available.
     */
    fun getLocalParticipant(): TgoParticipant {
        return getLocalParticipantOrNull()
            ?: throw IllegalStateException("Cannot get local participant: room info is null and no cached participant")
    }

    fun clear() {
        localParticipant?.dispose()
        localParticipant = null
        remoteParticipants.values.forEach { it.dispose() }
        remoteParticipants.clear()
    }

    fun getAllParticipants(): List<TgoParticipant> {
        val local = getLocalParticipant()
        val remote = getRemoteParticipants()
        return listOf(local) + remote.filter { it.uid != local.uid }
    }

    fun getRemoteParticipants(): List<TgoParticipant> {
        val room = TgoRTC.instance.roomManager.room
        val roomInfo = TgoRTC.instance.roomManager.currentRoomInfo
        val uidList = roomInfo?.uidList ?: emptyList<String>()
        val loginUID = roomInfo?.loginUID

        val result = mutableListOf<TgoParticipant>()
        val addedUids = mutableSetOf<String>()

        // 1. Process uidList
        for (uid in uidList) {
            if (uid == loginUID) continue
            val remoteP = room?.remoteParticipants?.values?.find { it.identity?.value == uid }

            val tgoP = remoteParticipants.getOrPut(uid) {
                TgoParticipant(
                    uid,
                    remoteParticipant = remoteP
                )
            }

            // 如果有远程参与者对象，更新绑定
            if (remoteP != null && !tgoP.isJoined()) {
                tgoP.setRemoteParticipant(remoteP)
            }

            result.add(tgoP)
            addedUids.add(uid)
        }

        // 2. Process other participants not in uidList
        room?.remoteParticipants?.values?.forEach { p ->
            val identity = p.identity?.value ?: return@forEach
            if (!addedUids.contains(identity)) {
                val tgoP = remoteParticipants.getOrPut(identity) {
                    TgoParticipant(
                        identity,
                        remoteParticipant = p
                    )
                }
                result.add(tgoP)
            }
        }

        return result
    }

    fun inviteParticipant(roomName: String?,uids: List<String>) {
        val roomInfo = TgoRTC.instance.roomManager.currentRoomInfo ?: return
        if (roomName != null && roomName != TgoRTC.instance.roomManager.currentRoomInfo?.roomName) {
            TgoLogger.warn("非当前通话，忽略,roomName:$roomName")
            return
        }

        val existingUids = remoteParticipants.keys
        val newUids = uids.filter { it !in existingUids }

        if (newUids.isEmpty()) return

        val currentCount = roomInfo.uidList.size
        val availableSlots = roomInfo.maxParticipants - currentCount

        if (availableSlots <= 0) {
            TgoLogger.error("已达到最大参与人数限制: ${roomInfo.maxParticipants}")
            return
        }

        val toAdd = if (newUids.size > availableSlots) {
            TgoLogger.error("邀请人数超出限制，最多还能添加 $availableSlots 人，实际邀请 ${newUids.size} 人")
            newUids.take(availableSlots)
        } else {
            newUids
        }

        for (uid in toAdd) {
            val tgoP = TgoParticipant(uid)
            remoteParticipants[uid] = tgoP
            notifyNewParticipant(tgoP)
            roomInfo.uidList.add(uid)
            TgoLogger.error("通知新的参与者加入")
        }
    }

    fun removePendingParticipants(
        roomName: String? = null,
        uids: List<String>
    ) {
        if (roomName != null && roomName != TgoRTC.instance.roomManager.currentRoomInfo?.roomName) {
            TgoLogger.warn("非当前通话，忽略,roomName:$roomName")
            return
        }

        for (uid in uids) {
            val removedLog = if (roomName == null) {
                "参与者 $uid 未加入房间已删除"
            } else {
                "参与者 $uid 未接听已删除"
            }

            removePendingParticipantInternal(
                uid = uid,
                skipLog = "参与者 $uid 已加入，跳过删除",
                removedLog = removedLog
            )
        }
    }


    fun setParticipantJoin(participant: RemoteParticipant) {
        val identity = participant.identity?.value ?: return
        val tgoP = remoteParticipants[identity]
        if (tgoP != null) {
            tgoP.setRemoteParticipant(participant)
            return
        }

        val roomInfo = TgoRTC.instance.roomManager.currentRoomInfo
        if (roomInfo != null && identity !in roomInfo.uidList) {
            roomInfo.uidList.add(identity)
        }

        val newTgoP = TgoParticipant(identity, remoteParticipant = participant)
        remoteParticipants[identity] = newTgoP
        notifyNewParticipant(newTgoP)
    }

    fun setParticipantLeave(participant: RemoteParticipant) {
        val identity = participant.identity?.value ?: return
        remoteParticipants[identity]?.let {
            it.notifyLeave()
            remoteParticipants.remove(identity)
        }
        TgoRTC.instance.roomManager.currentRoomInfo?.uidList?.remove(identity)
    }

    private fun removePendingParticipantInternal(
        uid: String,
        skipLog: String,
        removedLog: String
    ) {
        val participant = remoteParticipants[uid] ?: return

        if (participant.isJoined()) {
            TgoLogger.info(skipLog)
            return
        }

        participant.notifyLeave()
        remoteParticipants.remove(uid)
        TgoRTC.instance.roomManager.currentRoomInfo?.uidList?.remove(uid)
        TgoLogger.info(removedLog)
    }

    private fun notifyNewParticipant(participant: TgoParticipant) {
        newParticipantListeners.toList().forEach { it(participant) }
    }

    fun addNewParticipantListener(listener: (TgoParticipant) -> Unit) {
        newParticipantListeners.add(listener)
    }

    fun removeNewParticipantListener(listener: (TgoParticipant) -> Unit) {
        newParticipantListeners.remove(listener)
    }
}
