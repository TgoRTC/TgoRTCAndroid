package com.tgo.rtc.entity

/**
 * Room connection information.
 */
data class TgoRoomInfo(
    /** The name of the room. */
    var roomName: String,
    /** The access token for authentication. */
    var token: String,
    /** The LiveKit server URL. */
    var url: String,
    /** The UID of the current logged-in user. */
    var loginUID: String,
    /** The UID of the room creator. */
    var creatorUID: String
) {
    /** Maximum number of participants allowed in the room. Defaults to 2. */
    var maxParticipants: Int = 2

    /** The type of RTC call (audio or video). */
    var rtcType: TgoRTCType = TgoRTCType.AUDIO

    /** Whether this is a P2P (peer-to-peer) call. Defaults to true. */
    var isP2P: Boolean = true

    /** List of participant UIDs in the room. */
    var uidList: MutableList<String> = mutableListOf()

    /** Timeout in seconds for waiting for participants to join. Defaults to 30. */
    var timeout: Int = 30

    /**
     * Gets the UID of the other participant in a P2P call.
     */
    fun getP2PToUID(): String {
        if (uidList.isEmpty()) return ""
        for (uid in uidList) {
            if (uid != loginUID) return uid
        }
        return creatorUID
    }

    /**
     * Returns true if the current user is the room creator.
     */
    fun isCreator(): Boolean {
        return loginUID == creatorUID
    }
}
