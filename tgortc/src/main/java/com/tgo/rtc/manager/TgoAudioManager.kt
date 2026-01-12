package com.tgo.rtc.manager

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.tgo.rtc.TgoRTC

/**
 * Manager for handling audio output and device changes.
 */
class TgoAudioManager private constructor() {

    companion object {
        val instance: TgoAudioManager by lazy { TgoAudioManager() }
    }

    private var isSpeakerOn = false

    /**
     * Set speakerphone on/off.
     */
    fun setSpeakerphoneOn(on: Boolean) {
        isSpeakerOn = on
        val context = TgoRTC.instance.getContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setSpeakerphoneApi31(context, on)
        } else {
            setSpeakerphoneLegacy(context, on)
        }
    }

    /**
     * Android 12+ (API 31+) implementation using setCommunicationDevice
     */
    private fun setSpeakerphoneApi31(context: Context, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (enable) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val speakerphone = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                speakerphone?.let {
                    audioManager.setCommunicationDevice(it)
                }
            } else {
                audioManager.clearCommunicationDevice()
            }
        }
    }

    /**
     * Legacy implementation for Android 11 and below
     */
    @Suppress("DEPRECATION")
    private fun setSpeakerphoneLegacy(context: Context, enable: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = enable
    }

    /**
     * Toggle speakerphone state.
     */
    fun toggleSpeakerphone() {
        setSpeakerphoneOn(!isSpeakerOn)
    }

    fun isSpeakerOn(): Boolean = isSpeakerOn

    fun dispose() {
        // Reset to default when disposing
        val context = TgoRTC.instance.getContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.clearCommunicationDevice()
        }
    }
}
