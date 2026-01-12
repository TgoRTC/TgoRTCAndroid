package com.tgo.rtc.entity

import java.util.Locale

/**
 * Video stream information.
 */
data class TgoVideoInfo(
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val frameRate: Double,
    val layerId: String? = null,
    val qualityLimitationReason: String? = null
) {
    companion object {
        val EMPTY = TgoVideoInfo(0, 0, 0, 0.0)
    }

    val isValid: Boolean
        get() = width > 0 && height > 0

    val resolutionString: String
        get() = "${width}x$height"

    val bitrateString: String
        get() {
            return if (bitrate >= 1000000) {
                String.format(Locale.US, "%.1f Mbps", bitrate / 1000000.0)
            } else if (bitrate >= 1000) {
                String.format(Locale.US, "%d Kbps", bitrate / 1000)
            } else {
                String.format(Locale.US, "%d bps", bitrate)
            }
        }

    override fun toString(): String {
        return "TgoVideoInfo(resolution: $resolutionString, bitrate: $bitrateString, fps: ${String.format(Locale.US, "%.1f", frameRate)}, layer: $layerId)"
    }
}
