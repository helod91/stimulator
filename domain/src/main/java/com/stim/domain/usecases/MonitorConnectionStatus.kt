package com.stim.domain.usecases

import com.stim.domain.models.ConnectionStatus
import com.stim.domain.repositories.UdpRepository
import io.reactivex.Observable
import java.net.InetAddress
import javax.inject.Inject

class MonitorConnectionStatus @Inject constructor(
    private val updRepository: UdpRepository
) {

    fun execute(deviceIp: InetAddress, syncDeviceIp: InetAddress): Observable<ConnectionStatus> {
        return updRepository.monitorConnectionStatus(deviceIp, syncDeviceIp)
    }
}