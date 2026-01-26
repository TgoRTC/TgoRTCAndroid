package com.tgo.rtc.participant

import com.tgo.rtc.TgoRTC
import com.tgo.rtc.entity.TgoCameraPosition
import com.tgo.rtc.entity.TgoConnectionQuality
import com.tgo.rtc.entity.TgoVideoInfo
import com.tgo.rtc.utils.TgoLogger
import io.livekit.android.events.ParticipantEvent
import io.livekit.android.events.collect
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.*
import kotlinx.coroutines.*
import java.util.Date

/**
 * Speaking listener callback type
 * @param isSpeaking Whether the participant is currently speaking
 * @param audioLevel Audio level (0.0 to 1.0), higher values indicate louder audio
 */
typealias SpeakingListener = (isSpeaking: Boolean, audioLevel: Float) -> Unit

/**
 * Represents a participant in a room (local or remote).
 */
class TgoParticipant(
    val uid: String,
    private var localParticipant: LocalParticipant? = null,
    private var remoteParticipant: RemoteParticipant? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var videoTrackJob: Job? = null
    private var audioLevelJob: Job? = null

    private val createdAt = Date()

    private var currentVideoInfo = TgoVideoInfo.EMPTY
    private val videoInfoListeners = mutableListOf<(TgoVideoInfo) -> Unit>()

    private val microphoneListeners = mutableListOf<(Boolean) -> Unit>()
    private val cameraListeners = mutableListOf<(Boolean) -> Unit>()
    private val speakerListeners = mutableListOf<(Boolean) -> Unit>()
    private val screenShareListeners = mutableListOf<(Boolean) -> Unit>()
    private val speakingListeners = mutableListOf<SpeakingListener>()
    private val cameraPositionListeners = mutableListOf<(TgoCameraPosition) -> Unit>()
    private val connectionQualityListeners = mutableListOf<(TgoConnectionQuality) -> Unit>()
    private val joinedListeners = mutableListOf<() -> Unit>()
    private val leaveListeners = mutableListOf<() -> Unit>()
    private val trackPublishedListeners = mutableListOf<() -> Unit>()
    private val trackUnpublishedListeners = mutableListOf<() -> Unit>()

    companion object {
        // Audio level monitoring interval in milliseconds
        private const val AUDIO_LEVEL_UPDATE_INTERVAL = 100L
    }

    init {
        setupListeners()
    }

    fun getCreatedAt(): Date = createdAt

    fun getVideoTrack(source: Track.Source = Track.Source.CAMERA): VideoTrack? {
        val p = localParticipant ?: remoteParticipant ?: return null
        return p.videoTrackPublications.find { it.first.source == source }?.second as? VideoTrack
    }

    val isLocal: Boolean
        get() = uid == TgoRTC.instance.roomManager.currentRoomInfo?.loginUID

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
        TgoLogger.info("[Video] $uid stats updated: $info")
        videoInfoListeners.toList().forEach { it(info) }
    }

    // Listener methods
    fun addMicrophoneStatusListener(listener: (Boolean) -> Unit) {
        microphoneListeners.add(listener)
    }

    fun removeMicrophoneStatusListener(listener: (Boolean) -> Unit) {
        microphoneListeners.remove(listener)
    }

    fun addCameraStatusListener(listener: (Boolean) -> Unit) {
        cameraListeners.add(listener)
    }

    fun removeCameraStatusListener(listener: (Boolean) -> Unit) {
        cameraListeners.remove(listener)
    }

    fun addSpeakerStatusListener(listener: (Boolean) -> Unit) {
        speakerListeners.add(listener)
    }

    fun removeSpeakerStatusListener(listener: (Boolean) -> Unit) {
        speakerListeners.remove(listener)
    }

    fun addScreenShareStatusListener(listener: (Boolean) -> Unit) {
        screenShareListeners.add(listener)
    }

    fun removeScreenShareStatusListener(listener: (Boolean) -> Unit) {
        screenShareListeners.remove(listener)
    }

    /**
     * Add speaking listener with audio level
     * @param listener Callback with (isSpeaking, audioLevel) where audioLevel is 0.0 to 1.0
     */
    fun addSpeakingListener(listener: SpeakingListener) {
        val wasEmpty = speakingListeners.isEmpty()
        speakingListeners.add(listener)
        // Start audio level monitoring when first listener is added
        if (wasEmpty) {
            startAudioLevelMonitoring()
        }
    }

    /**
     * Remove speaking listener
     */
    fun removeSpeakingListener(listener: SpeakingListener) {
        speakingListeners.remove(listener)
        // Stop monitoring if no more listeners
        if (speakingListeners.isEmpty()) {
            stopAudioLevelMonitoring()
        }
    }

    /**
     * Get current audio level (0.0 to 1.0)
     * Higher values indicate louder audio
     */
    fun getAudioLevel(): Float {
        return localParticipant?.audioLevel ?: remoteParticipant?.audioLevel ?: 0f
    }

    /**
     * Check if participant is currently speaking
     */
    fun getIsSpeaking(): Boolean {
        return localParticipant?.isSpeaking ?: remoteParticipant?.isSpeaking ?: false
    }

    /**
     * Start audio level monitoring
     * Periodically polls audio level and notifies listeners
     */
    private fun startAudioLevelMonitoring() {
        stopAudioLevelMonitoring()
        
        audioLevelJob = scope.launch {
            while (isActive) {
                val isSpeaking = getIsSpeaking()
                val audioLevel = getAudioLevel()
                speakingListeners.toList().forEach { it(isSpeaking, audioLevel) }
                delay(AUDIO_LEVEL_UPDATE_INTERVAL)
            }
        }
    }

    /**
     * Stop audio level monitoring
     */
    private fun stopAudioLevelMonitoring() {
        audioLevelJob?.cancel()
        audioLevelJob = null
    }

    fun addCameraPositionListener(listener: (TgoCameraPosition) -> Unit) {
        cameraPositionListeners.add(listener)
    }

    fun removeCameraPositionListener(listener: (TgoCameraPosition) -> Unit) {
        cameraPositionListeners.remove(listener)
    }

    fun addConnQualityListener(listener: (TgoConnectionQuality) -> Unit) {
        connectionQualityListeners.add(listener)
    }

    fun removeConnQualityListener(listener: (TgoConnectionQuality) -> Unit) {
        connectionQualityListeners.remove(listener)
    }

    fun addJoinedListener(listener: () -> Unit) {
        joinedListeners.add(listener)
    }

    fun removeJoinedListener(listener: () -> Unit) {
        joinedListeners.remove(listener)
    }

    fun addLeaveListener(listener: () -> Unit) {
        leaveListeners.add(listener)
    }

    fun removeLeaveListener(listener: () -> Unit) {
        leaveListeners.remove(listener)
    }

    fun addTrackPublishedListener(listener: () -> Unit) {
        trackPublishedListeners.add(listener)
    }

    fun removeTrackPublishedListener(listener: () -> Unit) {
        trackPublishedListeners.remove(listener)
    }

    fun addTrackUnpublishedListener(listener: () -> Unit) {
        trackUnpublishedListeners.add(listener)
    }

    fun removeTrackUnpublishedListener(listener: () -> Unit) {
        trackUnpublishedListeners.remove(listener)
    }

    private fun setupListeners() {
        val p = localParticipant ?: remoteParticipant ?: return
        scope.launch {
            p.events.collect { event ->
                when (event) {
                    is ParticipantEvent.TrackMuted -> {
                        if (event.publication.source == Track.Source.MICROPHONE) microphoneListeners.forEach {
                            it(
                                false
                            )
                        }
                        else if (event.publication.source == Track.Source.CAMERA) cameraListeners.forEach {
                            it(
                                false
                            )
                        }
                    }

                    is ParticipantEvent.TrackUnmuted -> {
                        if (event.publication.source == Track.Source.MICROPHONE) microphoneListeners.forEach {
                            it(
                                true
                            )
                        }
                        else if (event.publication.source == Track.Source.CAMERA) cameraListeners.forEach {
                            it(
                                true
                            )
                        }
                    }

                    is ParticipantEvent.SpeakingChanged -> {
                        val audioLevel = getAudioLevel()
                        speakingListeners.toList().forEach { it(event.isSpeaking, audioLevel) }
                    }

                    is ParticipantEvent.TrackSubscribed -> {
                        if (event.publication.source == Track.Source.CAMERA) {
                            cameraListeners.forEach { it(true) }
                            (event.track as? RemoteVideoTrack)?.let { subscribeToVideoStats(it) }
                        } else if (event.publication.source == Track.Source.MICROPHONE) {
                            microphoneListeners.forEach { it(true) }
                        }
                    }

                    is ParticipantEvent.LocalTrackPublished -> {
                        if (event.publication.source == Track.Source.CAMERA) {
                            cameraListeners.forEach { it(true) }
                            (event.publication.track as? LocalVideoTrack)?.let {
                                subscribeToLocalVideoStats(
                                    it
                                )
                            }
                        } else if (event.publication.source == Track.Source.MICROPHONE) {
                            microphoneListeners.forEach { it(true) }
                        }
                        trackPublishedListeners.forEach { it() }
                    }

                    is ParticipantEvent.LocalTrackUnpublished -> {
                        if (event.publication.source == Track.Source.CAMERA) {
                            cameraListeners.forEach { it(false) }
                            unsubscribeFromVideoStats()
                        } else if (event.publication.source == Track.Source.MICROPHONE) {
                            microphoneListeners.forEach { it(false) }
                        }
                        trackUnpublishedListeners.forEach { it() }
                    }

                    is ParticipantEvent.TrackPublished -> {
                        if (event.publication.source == Track.Source.CAMERA) cameraListeners.forEach {
                            it(
                                true
                            )
                        }
                        else if (event.publication.source == Track.Source.MICROPHONE) microphoneListeners.forEach {
                            it(
                                true
                            )
                        }
                        trackPublishedListeners.forEach { it() }
                    }

                    is ParticipantEvent.TrackUnpublished -> {
                        if (event.publication.source == Track.Source.CAMERA) cameraListeners.forEach {
                            it(
                                false
                            )
                        }
                        else if (event.publication.source == Track.Source.MICROPHONE) microphoneListeners.forEach {
                            it(
                                false
                            )
                        }
                        trackUnpublishedListeners.forEach { it() }
                    }

                    is ParticipantEvent.TrackUnsubscribed -> {
                        if (event.publication.source == Track.Source.CAMERA) {
                            cameraListeners.forEach { it(false) }
                            unsubscribeFromVideoStats()
                        } else if (event.publication.source == Track.Source.MICROPHONE) {
                            microphoneListeners.forEach { it(false) }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun subscribeToLocalVideoStats(track: LocalVideoTrack) {
        unsubscribeFromVideoStats()
        videoTrackJob = scope.launch {
            // LiveKit Android stats are usually handled differently or accessed via track.getStats()
            // In Flutter it was an event. For Android SDK 2.x, we might need to poll or check if there's a flow.
            // For now, let's assume we can get it or implement a placeholder.
            // (Note: actual stats implementation might vary based on LiveKit version)
        }
    }

    private fun subscribeToVideoStats(track: RemoteVideoTrack) {
        unsubscribeFromVideoStats()
        // ... same for remote
    }

    private fun unsubscribeFromVideoStats() {
        videoTrackJob?.cancel()
        videoTrackJob = null
        currentVideoInfo = TgoVideoInfo.EMPTY
    }

    fun isJoined(): Boolean = localParticipant != null || remoteParticipant != null

    fun getCameraPosition(): TgoCameraPosition? {
        val videoTrack = localParticipant?.getTrackPublication(Track.Source.CAMERA)
            ?.track as? LocalVideoTrack
            ?: return null

        return when (videoTrack.options.position) {
            CameraPosition.FRONT -> TgoCameraPosition.FRONT
            CameraPosition.BACK -> TgoCameraPosition.BACK
            else -> null
        }
    }

    fun switchCamera() {
        val videoTrack = localParticipant?.getTrackPublication(Track.Source.CAMERA)
            ?.track as? LocalVideoTrack
            ?: return

        val newPosition = when (videoTrack.options.position) {
            CameraPosition.FRONT -> CameraPosition.BACK
            CameraPosition.BACK -> CameraPosition.FRONT
            else -> null
        }
        videoTrack.switchCamera(position = newPosition)

    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        localParticipant?.setCameraEnabled(enabled)
    }

    suspend fun setMicrophoneEnabled(enabled: Boolean) {
        localParticipant?.setMicrophoneEnabled(enabled)
    }

    suspend fun setScreenShareEnabled(enabled: Boolean) {
        localParticipant?.setScreenShareEnabled(enabled)
    }

    fun getMicrophoneEnabled(): Boolean {
        return localParticipant?.isMicrophoneEnabled ?: remoteParticipant?.isMicrophoneEnabled
        ?: false
    }

    fun getCameraEnabled(): Boolean {
        return localParticipant?.isCameraEnabled ?: remoteParticipant?.isCameraEnabled ?: false
    }

    fun getScreenShareEnabled(): Boolean {
        return localParticipant?.isScreenShareEnabled ?: remoteParticipant?.isScreenShareEnabled
        ?: false
    }

    fun setLocalParticipant(participant: LocalParticipant) {
        localParticipant = participant
        setupListeners()
        notifyInitialState()
    }

    fun setRemoteParticipant(participant: RemoteParticipant) {
        remoteParticipant = participant
        setupListeners()
        notifyInitialState()
        notifyJoined()
    }

    private fun notifyInitialState() {
        val mic = getMicrophoneEnabled()
        microphoneListeners.toList().forEach { it(mic) }
        val cam = getCameraEnabled()
        cameraListeners.toList().forEach { it(cam) }
        val speaking = getIsSpeaking()
        val audioLevel = getAudioLevel()
        speakingListeners.toList().forEach { it(speaking, audioLevel) }
    }

    fun notifyJoined() {
        joinedListeners.toList().forEach { it() }
    }

    fun notifyLeave() {
        leaveListeners.toList().forEach { it() }
        dispose()
    }

    fun dispose() {
        stopAudioLevelMonitoring()
        scope.cancel()
        unsubscribeFromVideoStats()
        microphoneListeners.clear()
        cameraListeners.clear()
        speakerListeners.clear()
        screenShareListeners.clear()
        speakingListeners.clear()
        cameraPositionListeners.clear()
        connectionQualityListeners.clear()
        joinedListeners.clear()
        leaveListeners.clear()
        trackPublishedListeners.clear()
        trackUnpublishedListeners.clear()
        videoInfoListeners.clear()
    }
}
