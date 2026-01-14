package com.tgo.rtc.entity

import com.twilio.audioswitch.AudioDevice

/**
 * Represents an audio output device
 */
data class TgoAudioDevice(
    val name: String,
    val type: TgoAudioDeviceType,
    internal val livekitDevice: AudioDevice? = null
) {
    companion object {
        /**
         * Create TgoAudioDevice from LiveKit AudioDevice
         */
        fun fromLivekitDevice(device: AudioDevice): TgoAudioDevice {
            return TgoAudioDevice(
                name = device.name,
                type = TgoAudioDeviceType.fromLivekitDevice(device),
                livekitDevice = device
            )
        }
    }
}

/**
 * Audio device type enumeration
 */
enum class TgoAudioDeviceType {
    EARPIECE,
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH,
    UNKNOWN;

    companion object {
        fun fromLivekitDevice(device: AudioDevice): TgoAudioDeviceType {
            return when (device) {
                is AudioDevice.Earpiece -> EARPIECE
                is AudioDevice.Speakerphone -> SPEAKER
                is AudioDevice.WiredHeadset -> WIRED_HEADSET
                is AudioDevice.BluetoothHeadset -> BLUETOOTH
                else -> UNKNOWN
            }
        }
    }
}
