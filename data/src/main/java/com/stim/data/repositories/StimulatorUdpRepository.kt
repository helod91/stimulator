package com.stim.data.repositories

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.stim.domain.models.ConnectionStatus
import com.stim.domain.models.Packet
import com.stim.domain.repositories.UdpRepository
import io.reactivex.Observable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import kotlin.experimental.or
import kotlin.math.roundToInt

private const val REPEAT_INTERVAL = 300L

private const val SOCKET_PORT = 55100
private const val SEND_DATA_PORT = 1000

private const val MONITOR_NAME = "MonitorHandler"
private const val RECEIVE_NAME = "ReceiveHandler"

@ExperimentalUnsignedTypes
class StimulatorUdpRepository @Inject constructor() : UdpRepository {

    private lateinit var monitorRepeatHandler: Handler
    private lateinit var monitorRepeatRunnable: Runnable

    private lateinit var receiveRepeatHandler: Handler
    private lateinit var receiveRepeatRunnable: Runnable

    private var readAddress = 0
    private var timeoutCntSync = 0
    private var timeoutCntMaster = 0

    override fun monitorConnectionStatus(
        deviceIp: InetAddress,
        syncDeviceIp: InetAddress
    ): Observable<ConnectionStatus> {
        return Observable.create { emitter ->
            monitorRepeatHandler = Handler(provideLooper(MONITOR_NAME))
            monitorRepeatRunnable = Runnable {
                try {
                    val message = ByteArray(5)
                    message[0] = 0x55
                    message[1] = 'r'.toByte()
                    message[3] = 0

                    val packet: DatagramPacket
                    val socket = DatagramSocket()

                    if (readAddress < 6) {
                        message[2] = readAddress.toByte()
                        message[4] = (message[1] + message[2] + message[3]).toByte()

                        packet = DatagramPacket(message, 5, deviceIp, 1000)

                        socket.send(packet)
                        socket.close()

                        readAddress++
                        timeoutCntMaster++
                    } else {
                        message[2] = 2
                        message[4] = (message[1] + message[2] + message[3]).toByte()

                        packet = DatagramPacket(message, 5, syncDeviceIp, 1000)

                        socket.send(packet)
                        socket.close()

                        readAddress = 0
                        timeoutCntSync++
                    }
                    if (timeoutCntMaster > 10)
                        if (timeoutCntSync > 3)
                            emitter.onNext(ConnectionStatus.SyncMasterError)
                        else
                            emitter.onNext(ConnectionStatus.MasterError)
                    else
                        if (timeoutCntSync > 3)
                            emitter.onNext(ConnectionStatus.SyncError)
                        else
                            emitter.onNext(ConnectionStatus.Success)
                } catch (e: Exception) {
                    emitter.onNext(ConnectionStatus.Exception)
                }

                monitorRepeatHandler.postDelayed(monitorRepeatRunnable, REPEAT_INTERVAL)
            }

            monitorRepeatHandler.post(monitorRepeatRunnable)
        }
    }

    override fun receivePacket(syncDeviceIp: InetAddress): Observable<Packet> {
        return Observable.create { emitter ->
            var buffer = ByteArray(255)

            val datagramPacket = DatagramPacket(buffer, buffer.size)
            val socket = DatagramSocket(SOCKET_PORT)

            receiveRepeatHandler = Handler(provideLooper(RECEIVE_NAME))
            receiveRepeatRunnable = Runnable {
                try {
                    socket.receive(datagramPacket)
                } catch (e: Exception) {
                    Log.e(StimulatorUdpRepository::class.java.simpleName, e.message, e)
                }

                buffer = datagramPacket.data
                buffer[datagramPacket.length] = 0

                if (datagramPacket.address == syncDeviceIp) {
                    timeoutCntSync = 0

                    emitter.onNext(
                        Packet.SyncDataPacket(
                            buffer[1].toInt(),
                            ((buffer[2].toUByte()).toInt() * 100 / 158).toString()
                        )
                    )
                } else {
                    timeoutCntMaster = 0

                    emitter.onNext(
                        Packet.DeviceDataPacket(
                            buffer[1] % 6,
                            if (buffer[2] == 0xFF.toByte()) "connection lost" else "supply OK",
                            buffer[3].toString(),
                            ((buffer[6].toUByte()).toFloat() * 240f / 255f).roundToInt()
                                .toString(),
                            ((buffer[8].toUByte()).toFloat() * 240f / 255f).roundToInt()
                                .toString(),
                            ((buffer[7].toUByte()).toFloat() * 240f / 255f).roundToInt()
                                .toString(),
                            ((buffer[9].toUByte()).toFloat() * 240f / 255f).roundToInt()
                                .toString()
                        )
                    )
                    emitter.onNext(
                        Packet.Pulse(
                            buffer[13] == 2.toByte()
                        )
                    )
                }

                receiveRepeatHandler.post(receiveRepeatRunnable)
            }
            receiveRepeatHandler.post(receiveRepeatRunnable)
        }
    }

    override fun sendData(
        data: Int,
        deviceAddress: Int,
        address: Int,
        device: Int,
        deviceIp: InetAddress,
        syncDeviceIp: InetAddress
    ): Observable<Boolean> {
        return Observable.create { emitter ->
            val deviceAddressByte = deviceAddress.toByte()

            var addressByte = address.toByte()
            addressByte = ((deviceAddressByte.toInt() shl 5).toByte() or addressByte)

            val message = ByteArray(5)
            message[0] = 0x55
            message[1] = 'w'.toByte()
            message[2] = addressByte
            message[3] = data.toByte()
            message[4] = (message[1] + message[2] + message[3]).toByte()

            try {
                val packet =
                    if (device == 0) DatagramPacket(message, message.size, deviceIp, SEND_DATA_PORT)
                    else DatagramPacket(message, message.size, syncDeviceIp, SEND_DATA_PORT)

                val socket = DatagramSocket()
                socket.send(packet)
                socket.close()

                emitter.onNext(true)
            } catch (e: Exception) {
                Log.e(StimulatorUdpRepository::class.java.simpleName, e.message, e)

                emitter.onNext(false)
            } finally {
                emitter.onComplete()
            }
        }
    }

    private fun provideLooper(name: String): Looper {
        return HandlerThread(name).apply {
            start()
        }.looper
    }
}