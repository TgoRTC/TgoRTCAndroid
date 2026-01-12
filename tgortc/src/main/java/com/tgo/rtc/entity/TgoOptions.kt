package com.tgo.rtc.entity

/**
 * SDK configuration options.
 */
class TgoOptions {
    /**
     * Whether to mirror local video (front camera).
     * Defaults to `false`.
     */
    var mirror: Boolean = false

    /**
     * Whether to enable debug logging.
     * Defaults to `true`.
     */
    var debug: Boolean = true
}
