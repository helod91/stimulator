package com.stim.domain.usecases

import com.stim.domain.repositories.UdpRepository
import io.reactivex.Observable
import java.net.InetAddress
import javax.inject.Inject

class SendData @Inject constructor(
    private val udpRepository: UdpRepository
) {

    fun execute(
        data: Int,
        deviceAddress: Int,
        address: Int,
        device: Int,
        deviceIp: InetAddress,
        syncDeviceIp: InetAddress
    ): Observable<Boolean> {
        return udpRepository.sendData(data, deviceAddress, address, device, deviceIp, syncDeviceIp)
    }
}