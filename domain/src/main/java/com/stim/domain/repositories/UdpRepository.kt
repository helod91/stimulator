package com.stim.domain.repositories

import com.stim.domain.models.ConnectionStatus
import com.stim.domain.models.Packet
import io.reactivex.Observable
import java.net.InetAddress

interface UdpRepository {

    fun monitorConnectionStatus(
        deviceIp: InetAddress,
        syncDeviceIp: InetAddress
    ): Observable<ConnectionStatus>

    fun receivePacket(syncDeviceIp: InetAddress): Observable<Packet>

    fun sendData(
        data: Int,
        deviceAddress: Int,
        address: Int,
        device: Int,
        deviceIp: InetAddress,
        syncDeviceIp: InetAddress
    ): Observable<Boolean>
}