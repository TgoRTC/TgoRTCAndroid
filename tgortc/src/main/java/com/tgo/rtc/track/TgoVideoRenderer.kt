package com.tgo.rtc.track

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.tgo.rtc.TgoRTC
import com.tgo.rtc.participant.TgoParticipant
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.renderer.TextureViewRenderer

/**
 * Video scale type enumeration.
 */
enum class TgoVideoScaleType {
    /** Scale to fill the view, cropping if needed */
    FILL,
    /** Scale to fit the view, adding letterboxing if needed */
    FIT
}

/**
 * Renderer type enumeration.
 */
enum class TgoRendererType {
    /** Use SurfaceViewRenderer (better performance, but can't be overlapped) */
    SURFACE_VIEW,
    /** Use TextureViewRenderer (supports animations and overlapping) */
    TEXTURE_VIEW
}

/**
 * A custom View for rendering video tracks from TgoParticipant.
 *
 * This is a wrapper around LiveKit's SurfaceViewRenderer and TextureViewRenderer.
 *
 * ## Usage (XML)
 * ```xml
 * <com.tgo.rtc.track.TgoVideoRenderer
 *     android:id="@+id/videoRenderer"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 * ```
 *
 * ## Usage (Kotlin)
 * ```kotlin
 * val renderer = TgoVideoRenderer(context)
 * renderer.setParticipant(participant)
 * // or
 * renderer.setVideoTrack(videoTrack)
 * ```
 */
class TgoVideoRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var surfaceViewRenderer: SurfaceViewRenderer? = null
    private var textureViewRenderer: TextureViewRenderer? = null
    
    private var currentTrack: VideoTrack? = null
    private var participant: TgoParticipant? = null
    private var trackSource: Track.Source = Track.Source.CAMERA
    
    private var rendererType: TgoRendererType = TgoRendererType.TEXTURE_VIEW
    private var scaleType: TgoVideoScaleType = TgoVideoScaleType.FILL
    private var mirror: Boolean = false
    private var isInitialized: Boolean = false

    private val cameraListener: (Boolean) -> Unit = { enabled ->
        updateTrack()
    }

    private val joinedListener: () -> Unit = {
        updateTrack()
    }

    init {
        initRenderer()
    }

    /**
     * Initialize the renderer with room context.
     * Call this before setting participant or track.
     */
    fun init(room: Room) {
        if (isInitialized) return
        
        when (rendererType) {
            TgoRendererType.SURFACE_VIEW -> {
                surfaceViewRenderer?.let { room.initVideoRenderer(it) }
            }
            TgoRendererType.TEXTURE_VIEW -> {
                textureViewRenderer?.let { room.initVideoRenderer(it) }
            }
        }
        isInitialized = true
    }

    /**
     * Initialize with TgoRTC room manager.
     * If room is not available yet, will auto-initialize when room connects.
     */
    fun init() {
        if (isInitialized) return
        
        val room = TgoRTC.instance.roomManager.room
        if (room != null) {
            init(room)
        } else {
            // Room not ready, add listener to init when connected
            TgoRTC.instance.roomManager.addConnectListener { _, status, _ ->
                if (status == com.tgo.rtc.entity.TgoConnectStatus.CONNECTED && !isInitialized) {
                    TgoRTC.instance.roomManager.room?.let { r ->
                        init(r)
                    }
                }
            }
        }
    }

    private fun initRenderer() {
        removeAllViews()
        surfaceViewRenderer = null
        textureViewRenderer = null

        when (rendererType) {
            TgoRendererType.SURFACE_VIEW -> {
                surfaceViewRenderer = SurfaceViewRenderer(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
                addView(surfaceViewRenderer)
            }
            TgoRendererType.TEXTURE_VIEW -> {
                textureViewRenderer = TextureViewRenderer(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
                addView(textureViewRenderer)
            }
        }
        
        applyMirror()
    }

    /**
     * Set the renderer type (SURFACE_VIEW or TEXTURE_VIEW).
     * This will reinitialize the renderer.
     */
    fun setRendererType(type: TgoRendererType) {
        if (rendererType != type) {
            rendererType = type
            val track = currentTrack
            release()
            initRenderer()
            if (isInitialized) {
                init()
            }
            track?.let { setVideoTrack(it) }
        }
    }

    /**
     * Set the scale type for video rendering.
     */
    fun setScaleType(type: TgoVideoScaleType) {
        scaleType = type
        // Scale type is typically handled by the VideoTrack's scalingType
        // This can be implemented based on specific needs
    }

    /**
     * Set mirror mode.
     */
    fun setMirror(mirror: Boolean) {
        this.mirror = mirror
        applyMirror()
    }

    private fun applyMirror() {
        val shouldMirror = mirror || TgoRTC.instance.options.mirror
        surfaceViewRenderer?.setMirror(shouldMirror)
        textureViewRenderer?.setMirror(shouldMirror)
    }

    /**
     * Set the track source to render (CAMERA or SCREEN_SHARE).
     */
    fun setTrackSource(source: Track.Source) {
        trackSource = source
        updateTrack()
    }

    /**
     * Set the participant whose video should be rendered.
     */
    fun setParticipant(participant: TgoParticipant?) {
        // Remove old listeners
        this.participant?.removeCameraStatusListener(cameraListener)
        this.participant?.removeJoinedListener(joinedListener)
        
        this.participant = participant
        
        // Add new listeners
        participant?.addCameraStatusListener(cameraListener)
        participant?.addJoinedListener(joinedListener)
        
        updateTrack()
    }

    /**
     * Set the video track directly.
     */
    fun setVideoTrack(track: VideoTrack?) {
        if (currentTrack == track) return
        
        // Remove from old track
        currentTrack?.let { oldTrack ->
            surfaceViewRenderer?.let { oldTrack.removeRenderer(it) }
            textureViewRenderer?.let { oldTrack.removeRenderer(it) }
        }
        
        currentTrack = track
        
        // Add to new track
        track?.let { newTrack ->
            surfaceViewRenderer?.let { newTrack.addRenderer(it) }
            textureViewRenderer?.let { newTrack.addRenderer(it) }
        }
        
        visibility = if (track != null) View.VISIBLE else View.GONE
    }

    private fun updateTrack() {
        val track = participant?.getVideoTrack(trackSource)
        setVideoTrack(track)
    }

    /**
     * Release resources. Call this when the view is no longer needed.
     */
    fun release() {
        participant?.removeCameraStatusListener(cameraListener)
        participant?.removeJoinedListener(joinedListener)
        participant = null
        
        currentTrack?.let { track ->
            surfaceViewRenderer?.let { track.removeRenderer(it) }
            textureViewRenderer?.let { track.removeRenderer(it) }
        }
        currentTrack = null
        
        surfaceViewRenderer?.release()
        textureViewRenderer?.release()
        
        isInitialized = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
