package com.tgo.rtc

import android.content.Context
import com.tgo.rtc.entity.TgoOptions
import com.tgo.rtc.manager.TgoAudioManager
import com.tgo.rtc.manager.TgoParticipantManager
import com.tgo.rtc.manager.TgoRoomManager

/**
 * The main entry point for the TgoRTC SDK.
 * This is a singleton class that provides access to all SDK functionality.
 */
class TgoRTC private constructor() {

    companion object {
        /**
         * Returns the singleton instance of TgoRTC.
         */
        val instance: TgoRTC by lazy { TgoRTC() }
    }

    /** SDK configuration options. */
    var options: TgoOptions = TgoOptions()
        private set

    private var context: Context? = null

    /**
     * Initialize the SDK with the given options.
     * Must be called before using other SDK features.
     *
     * @param context Android application context.
     * @param options Configuration options for the SDK.
     */
    fun init(context: Context, options: TgoOptions = TgoOptions()) {
        this.context = context.applicationContext
        this.options = options
    }

    /**
     * Returns the application context.
     */
    fun getContext(): Context {
        return context
            ?: throw IllegalStateException("TgoRTC must be initialized with init(context) first")
    }

    /** Room manager for handling room connection and events. */
    val roomManager: TgoRoomManager get() = TgoRoomManager.instance

    /** Participant manager for handling local and remote participants. */
    val participantManager: TgoParticipantManager get() = TgoParticipantManager.instance

    /** Audio manager for handling audio output. */
    val audioManager: TgoAudioManager get() = TgoAudioManager.instance
}
