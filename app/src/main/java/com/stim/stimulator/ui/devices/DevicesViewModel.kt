package com.stim.stimulator.ui.devices

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stim.domain.models.ConnectionStatus
import com.stim.domain.models.Packet
import com.stim.domain.usecases.MonitorConnectionStatus
import com.stim.domain.usecases.ReceivePacket
import com.stim.domain.usecases.SendData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.InetAddress
import javax.inject.Inject

class DevicesViewModel @Inject constructor(
    private val monitorConnectionStatusUseCase: MonitorConnectionStatus,
    private val receivePacketUseCase: ReceivePacket,
    private val sendDataUseCase: SendData
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private val disposables = HashMap<String, Disposable>()

    val connectionStatus = MutableLiveData<ConnectionStatus>()
    val receivedPacket = MutableLiveData<Packet>()
    val dataSent = MutableLiveData<Boolean>()

    override fun onCleared() {
        super.onCleared()

        disposables.values.forEach { it.dispose() }
        disposables.clear()

        compositeDisposable.clear()
    }

    fun monitorConnectionStatus(deviceIp: InetAddress, syncDeviceIp: InetAddress) {
        disposables["MONITOR_CONNECTION_STATUS"]?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }

        monitorConnectionStatusUseCase.execute(deviceIp, syncDeviceIp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { status ->
                    connectionStatus.value = status
                },
                { error ->
                    Log.e(DevicesViewModel::class.java.simpleName, error.message, error)
                }
            ).let {
                disposables["MONITOR_CONNECTION_STATUS"] = it
                compositeDisposable.add(it)
            }
    }

    fun receivePacket(syncDeviceIp: InetAddress) {
        disposables["RECEIVE_PACKET"]?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }

        receivePacketUseCase.execute(syncDeviceIp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { packet ->
                    receivedPacket.value = packet
                },
                { error ->
                    Log.e(DevicesViewModel::class.java.simpleName, error.message, error)
                }
            ).let {
                disposables["RECEIVE_PACKET"] = it
                compositeDisposable.add(it)
            }
    }

    fun stopReceivingPacket() {
        disposables["RECEIVE_PACKET"]?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }
        disposables.remove("RECEIVE_PACKET")
    }

    fun sendData(
        data: Int,
        deviceAddress: Int,
        address: Int,
        device: Int,
        deviceIp: InetAddress,
        syncDeviceIp: InetAddress
    ) {
        disposables["SEND_DATA"]?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }

        sendDataUseCase.execute(data, deviceAddress, address, device, deviceIp, syncDeviceIp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { success ->
                    dataSent.value = success
                },
                { error ->
                    Log.e(DevicesViewModel::class.java.simpleName, error.message, error)
                }
            ).let {
                disposables["SEND_DATA"] = it
                compositeDisposable.add(it)
            }
    }
}