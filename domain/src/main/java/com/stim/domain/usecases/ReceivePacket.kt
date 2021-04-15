package com.stim.domain.usecases

import com.stim.domain.models.Packet
import com.stim.domain.repositories.UdpRepository
import io.reactivex.Observable
import java.net.InetAddress
import javax.inject.Inject

class ReceivePacket @Inject constructor(
    private val udpRepository: UdpRepository
) {

    fun execute(syncDeviceIp: InetAddress): Observable<Packet> {
        return udpRepository.receivePacket(syncDeviceIp)
    }
}