package com.stim.domain.models

sealed class Packet {
    data class SyncDataPacket(
        val address: Int,
        val speed: String
    ) : Packet()

    data class DeviceDataPacket(
        val position: Int,
        val signal: String? = null,
        val strength: String? = null,
        val startFirst: String? = null,
        val startSecond: String? = null,
        val stopFirst: String? = null,
        val stopSecond: String? = null
    ) : Packet()

    data class Pulse(
        val singlePulse: Boolean
    ) : Packet()
}
