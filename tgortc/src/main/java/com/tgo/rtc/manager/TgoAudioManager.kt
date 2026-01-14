package com.tgo.rtc.manager

import com.tgo.rtc.entity.TgoAudioDevice
import com.tgo.rtc.entity.TgoAudioDeviceType
import com.tgo.rtc.utils.TgoLogger
import com.twilio.audioswitch.AudioDevice
import io.livekit.android.audio.AudioSwitchHandler

/**
 * Listener type for output device changes
 */
typealias DeviceChangeListener = (List<TgoAudioDevice>, TgoAudioDevice?) -> Unit

/**
 * Manager for handling audio output devices using LiveKit's AudioSwitchHandler.
 */
class TgoAudioManager private constructor() {

    companion object {
        val instance: TgoAudioManager by lazy { TgoAudioManager() }
    }

    private val deviceChangeListeners = mutableListOf<DeviceChangeListener>()
    private var audioSwitchHandler: AudioSwitchHandler? = null

    /**
     * Set the AudioSwitchHandler from LiveKit Room
     * This should be called when the room is created
     */
    fun setAudioSwitchHandler(handler: AudioSwitchHandler?) {
        audioSwitchHandler = handler

        // Register device change listener
        handler?.registerAudioDeviceChangeListener { devices, selectedDevice ->
            notifyDeviceChange(devices, selectedDevice)
        }

        TgoLogger.info("AudioSwitchHandler set: ${handler != null}")
    }

    /**
     * Notify all listeners about device change
     */
    private fun notifyDeviceChange(devices: List<AudioDevice>, selectedDevice: AudioDevice?) {
        val tgoDevices = devices.map { TgoAudioDevice.fromLivekitDevice(it) }.distinctBy { it.type }
        val tgoSelectedDevice = selectedDevice?.let { TgoAudioDevice.fromLivekitDevice(it) }
        
        // Log device information
        TgoLogger.info("[Audio] Device change detected:")
        TgoLogger.info("[Audio] Available devices (${tgoDevices.size}):")
        tgoDevices.forEachIndexed { index, device ->
            TgoLogger.info("[Audio]   ${index + 1}. ${device.name} (type=${device.type})")
        }
        TgoLogger.info("[Audio] Selected device: ${tgoSelectedDevice?.name ?: "None"} (type=${tgoSelectedDevice?.type ?: "N/A"})")
        
        deviceChangeListeners.toList().forEach { it(tgoDevices, tgoSelectedDevice) }
    }

    /**
     * Add device change listener
     */
    fun addDeviceChangeListener(listener: DeviceChangeListener) {
        deviceChangeListeners.add(listener)
        // Immediately notify with current devices
        audioSwitchHandler?.let { handler ->
            val devices = handler.availableAudioDevices
            val selected = handler.selectedAudioDevice
            listener(
                devices.map { TgoAudioDevice.fromLivekitDevice(it) }.distinctBy { it.type },
                selected?.let { TgoAudioDevice.fromLivekitDevice(it) }
            )
        }
    }

    /**
     * Remove device change listener
     */
    fun removeDeviceChangeListener(listener: DeviceChangeListener) {
        deviceChangeListeners.remove(listener)
    }

    /**
     * Get audio output devices
     */
    fun getAudioOutputDevices(): List<TgoAudioDevice> {
        return audioSwitchHandler?.availableAudioDevices
            ?.map { TgoAudioDevice.fromLivekitDevice(it) }
            ?.distinctBy { it.type }
            ?: emptyList()
    }

    /**
     * Get currently selected output device
     */
    fun getSelectedDevice(): TgoAudioDevice? {
        return audioSwitchHandler?.selectedAudioDevice?.let {
            TgoAudioDevice.fromLivekitDevice(it)
        }
    }

    /**
     * Select an output device
     */
    fun selectDevice(device: TgoAudioDevice) {
        device.livekitDevice?.let {
            audioSwitchHandler?.selectDevice(it)
            TgoLogger.info("Output device selected: ${it.name}")
        }
    }

    /**
     * Select an output device by type
     */
    fun selectDeviceByType(type: TgoAudioDeviceType) {
        val devices = audioSwitchHandler?.availableAudioDevices ?: return

        val targetDevice = when (type) {
            TgoAudioDeviceType.EARPIECE -> devices.firstOrNull { it is AudioDevice.Earpiece }
            TgoAudioDeviceType.SPEAKER -> devices.firstOrNull { it is AudioDevice.Speakerphone }
            TgoAudioDeviceType.WIRED_HEADSET -> devices.firstOrNull { it is AudioDevice.WiredHeadset }
            TgoAudioDeviceType.BLUETOOTH -> devices.firstOrNull { it is AudioDevice.BluetoothHeadset }
            TgoAudioDeviceType.UNKNOWN -> null
        }

        targetDevice?.let {
            audioSwitchHandler?.selectDevice(it)
            TgoLogger.info("Output device selected by type: ${it.name}")
        }
    }

    /**
     * Check if wired headset is connected
     */
    fun isWiredHeadsetConnected(): Boolean {
        return getAudioOutputDevices().any { it.type == TgoAudioDeviceType.WIRED_HEADSET }
    }

    /**
     * Check if Bluetooth audio device is connected
     */
    fun isBluetoothConnected(): Boolean {
        return getAudioOutputDevices().any { it.type == TgoAudioDeviceType.BLUETOOTH }
    }

    /**
     * Check if speakerphone is currently selected
     */
    fun isSpeakerOn(): Boolean {
        return getSelectedDevice()?.type == TgoAudioDeviceType.SPEAKER
    }

    /**
     * Set speakerphone on/off
     */
    fun setSpeakerphoneOn(on: Boolean) {
        val devices = audioSwitchHandler?.availableAudioDevices ?: return

        val targetDevice = if (on) {
            devices.firstOrNull { it is AudioDevice.Speakerphone }
        } else {
            // Select earpiece or other available device
            devices.firstOrNull { it is AudioDevice.Earpiece }
                ?: devices.firstOrNull { it is AudioDevice.WiredHeadset }
                ?: devices.firstOrNull { it is AudioDevice.BluetoothHeadset }
        }

        targetDevice?.let {
            audioSwitchHandler?.selectDevice(it)
            TgoLogger.info("Audio device switched to: ${it.name}")
        }
    }

    /**
     * Toggle speakerphone state
     */
    fun toggleSpeakerphone() {
        setSpeakerphoneOn(!isSpeakerOn())
    }

    /**
     * Check if speaker can be switched
     */
    fun canSwitchSpeakerphone(): Boolean {
        val devices = audioSwitchHandler?.availableAudioDevices ?: return false
        return devices.any { it is AudioDevice.Speakerphone }
    }

    /**
     * Dispose and clean up resources
     */
    fun dispose() {
        deviceChangeListeners.clear()
        audioSwitchHandler = null
    }
}
