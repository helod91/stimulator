package com.stim.stimulator.ui.devices

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stim.domain.models.ConnectionStatus
import com.stim.domain.models.Packet
import com.stim.stimulator.R
import com.stim.stimulator.databinding.ActivityDevicesBinding
import com.stim.stimulator.databinding.ItemDeviceBinding
import com.stim.stimulator.di.ViewModelFactory
import com.stim.stimulator.ui.utils.StimulatorTextWatcher
import dagger.android.support.DaggerAppCompatActivity
import java.net.InetAddress
import javax.inject.Inject
import kotlin.math.roundToInt

private const val TEST = "TestLooper"
private const val TEST_DURATION = 1000L

class DevicesActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: DevicesViewModel
    private lateinit var binding: ActivityDevicesBinding

    private lateinit var deviceIp: InetAddress
    private lateinit var syncDeviceIp: InetAddress

    private var communicationError: Boolean = true
    private var motorStarted: Boolean = false

    private val observableList = ArrayList<LiveData<*>>()

    private val devicesBindings = ArrayList<ItemDeviceBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            deviceIp = InetAddress.getByName(getString(R.string.ip_device))
            syncDeviceIp = InetAddress.getByName(getString(R.string.ip_sync_device))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModel = ViewModelProvider(this, viewModelFactory).get(DevicesViewModel::class.java)

        binding = ActivityDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDeviceList()
        setListeners()

        observeConnectionStatus()
        observePackets()
        observeDataSent()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus)
            hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()

        observableList.forEach { liveData ->
            liveData.removeObservers(this)
        }
        observableList.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initDeviceList() {
        val positions = listOf(2, 1, 0, 5, 4, 3)
        for (position in 0 until 6) {
            val itemDevicesBinding =
                if (position < 3) {
                    ItemDeviceBinding.inflate(layoutInflater, binding.devicesTopRow, true)
                } else {
                    ItemDeviceBinding.inflate(layoutInflater, binding.devicesBottomRow, true)
                }

            with(itemDevicesBinding) {
                deviceName.text = getString(R.string.device_name, position + 1)
                (root.layoutParams as? LinearLayout.LayoutParams)?.weight = 1f

                val stopTestHandler = Handler(provideLooper(TEST))
                var stopTestRunnable: Runnable? = null
                deviceTest.setOnClickListener {
                    viewModel.sendData(
                        2,
                        positions[position],
                        8,
                        0,
                        deviceIp,
                        syncDeviceIp
                    )

                    stopTestRunnable?.let { stopTestHandler.removeCallbacks(it) }
                    stopTestRunnable = Runnable {
                        viewModel.sendData(
                            0,
                            positions[position],
                            8,
                            0,
                            deviceIp,
                            syncDeviceIp
                        )
                    }.apply { stopTestHandler.postDelayed(this, TEST_DURATION) }
                }

                fun sendDataForStartStop(input: EditText, address: Int) {
                    input.text.toString().toInt().let { number ->
                        val changedNumber = (number.toFloat() * 255f / 240f).roundToInt()
                        viewModel.sendData(
                            changedNumber,
                            positions[position],
                            address,
                            0,
                            deviceIp,
                            syncDeviceIp
                        )
                    }
                }

                fun setInputsEnabled(enabled: Boolean, vararg inputs: EditText) {
                    inputs.forEach { it.isEnabled = enabled }

                    deviceEditMode.tag = enabled
                    deviceEditMode.setImageResource(
                        if (enabled) R.drawable.ic_done
                        else R.drawable.ic_edit
                    )
                }

                deviceEditMode.isVisible = true
                deviceEditMode.tag = false
                deviceEditMode.setOnClickListener {
                    if (deviceEditMode.tag == true) {
                        sendDataForStartStop(deviceStartFirst, 4)
                        sendDataForStartStop(deviceStartSecond, 6)
                        sendDataForStartStop(deviceStopFirst, 5)
                        sendDataForStartStop(deviceStopSecond, 7)

                        deviceStrength.text.toString().toInt().let { number ->
                            viewModel.sendData(
                                number,
                                positions[position],
                                1,
                                0,
                                deviceIp,
                                syncDeviceIp
                            )
                        }

                        setInputsEnabled(
                            false,
                            deviceStrength,
                            deviceStartFirst,
                            deviceStartSecond,
                            deviceStopFirst,
                            deviceStopSecond
                        )

                        //TODO new thing, maybe wont work well
                        viewModel.receivePacket(syncDeviceIp)
                    } else {
                        //TODO new thing, maybe wont work well
                        viewModel.stopReceivingPacket()

                        setInputsEnabled(
                            true,
                            deviceStrength,
                            deviceStartFirst,
                            deviceStartSecond,
                            deviceStopFirst,
                            deviceStopSecond
                        )
                    }
                }
                setInputsEnabled(
                    false,
                    deviceStrength,
                    deviceStartFirst,
                    deviceStartSecond,
                    deviceStopFirst,
                    deviceStopSecond
                )

                devicesBindings.add(this)
            }
        }
    }

    private fun setListeners() {
        fun enableViews(enabled: Boolean) {
            binding.devicesSpeed.isEnabled = enabled
            binding.devicesOperationModes.isEnabled = enabled
            binding.devicesModeRussian.isEnabled = enabled
            binding.devicesModeSinglePulse.isEnabled = enabled

            devicesBindings.forEach { deviceBinding ->
                deviceBinding.deviceEditMode.isVisible = enabled
                deviceBinding.deviceTest.isEnabled = enabled
            }
        }

        binding.devicesStartMotor.setOnClickListener {
            if (communicationError)
                Toast.makeText(this, R.string.devices_com_error_start, Toast.LENGTH_SHORT).show()
            else {
                if (motorStarted) {
                    motorStarted = false

                    binding.devicesStartMotor.setText(R.string.devices_start_motor)
                    binding.devicesStartImp.isVisible = false

                    viewModel.sendData(0, 7, 2, 0, deviceIp, syncDeviceIp)
                    viewModel.sendData(0, 0, 1, 1, deviceIp, syncDeviceIp)
                    viewModel.sendData(0, 7, 0, 0, deviceIp, syncDeviceIp)

                    enableViews(true)
                } else {
                    motorStarted = true

                    binding.devicesStartMotor.setText(R.string.devices_stop_motor)
                    binding.devicesStartImp.isVisible = true

                    viewModel.sendData(1, 7, 2, 0, deviceIp, syncDeviceIp)
                    viewModel.sendData(1, 0, 1, 1, deviceIp, syncDeviceIp)
                }
            }
        }
        binding.devicesStartImp.isVisible = false
        binding.devicesStartImp.setOnClickListener {
            if (communicationError)
                Toast.makeText(this, R.string.devices_com_error_start, Toast.LENGTH_SHORT).show()
            else {
                viewModel.sendData(1, 7, 0, 0, deviceIp, syncDeviceIp)
                enableViews(false)
            }
        }
        binding.devicesTurnOff.setOnClickListener {
            motorStarted = false

            binding.devicesStartMotor.setText(R.string.devices_start_motor)
            binding.devicesStartImp.isVisible = false

            viewModel.sendData(0, 7, 2, 0, deviceIp, syncDeviceIp)
            viewModel.sendData(0, 0, 1, 1, deviceIp, syncDeviceIp)
            viewModel.sendData(0, 7, 0, 0, deviceIp, syncDeviceIp)
            viewModel.sendData(0, 7, 31, 0, deviceIp, syncDeviceIp)

            enableViews(true)
        }
        binding.devicesOperationModes.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.devices_mode_russian ->
                    viewModel.sendData(50, 0, 11, 0, deviceIp, syncDeviceIp)
                R.id.devices_mode_single_pulse ->
                    viewModel.sendData(2, 0, 11, 0, deviceIp, syncDeviceIp)
            }
        }
        binding.devicesSpeed.addTextChangedListener(StimulatorTextWatcher { number ->
            if (binding.devicesSpeed.hasFocus() && number in 0..100)
                viewModel.sendData(number * 158 / 100, 0, 2, 1, deviceIp, syncDeviceIp)
        })
    }

    private fun observeConnectionStatus() {
        viewModel.monitorConnectionStatus(deviceIp, syncDeviceIp)
        observe(viewModel.connectionStatus) { connectionStatus ->
            communicationError = true
            when (connectionStatus) {
                ConnectionStatus.Exception ->
                    binding.devicesConnectionStatus.setText(R.string.devices_connection_status_exception_error)
                ConnectionStatus.MasterError ->
                    binding.devicesConnectionStatus.setText(R.string.devices_connection_status_master_error)
                ConnectionStatus.Success -> {
                    communicationError = false
                    binding.devicesConnectionStatus.setText(R.string.devices_connection_status_success)
                }
                ConnectionStatus.SyncError ->
                    binding.devicesConnectionStatus.setText(R.string.devices_connection_status_sync_error)
                ConnectionStatus.SyncMasterError ->
                    binding.devicesConnectionStatus.setText(R.string.devices_connection_status_sync_master_error)
            }
        }
    }

    private fun observePackets() {
        viewModel.receivePacket(syncDeviceIp)
        observe(viewModel.receivedPacket) { packet ->
            when (packet) {
                is Packet.DeviceDataPacket -> setDataPacket(packet)
                is Packet.SyncDataPacket -> {
                    when (packet.address) {
                        0 -> Log.d(DevicesActivity::class.java.simpleName, "Status")
                        1 -> Log.d(DevicesActivity::class.java.simpleName, "Command")
                        2 -> if (binding.devicesSpeed.hasFocus().not())
                            binding.devicesSpeed.setText(packet.speed)
                        3 -> Log.d(DevicesActivity::class.java.simpleName, "PeriodH")
                        4 -> Log.d(DevicesActivity::class.java.simpleName, "PeriodL")
                    }
                }
                is Packet.Pulse -> {
                    if (packet.singlePulse) {
                        binding.devicesOperationModes.check(R.id.devices_mode_single_pulse)
                    } else {
                        binding.devicesOperationModes.check(R.id.devices_mode_russian)
                    }
                }
            }
        }
    }

    private fun observeDataSent() {
        observe(viewModel.dataSent) { success ->
            Log.d(DevicesActivity::class.java.simpleName, "Data was sent: $success")
        }
    }

    private fun setDataPacket(device: Packet.DeviceDataPacket) {
        val positions = listOf(2, 1, 0, 5, 4, 3)
        with(devicesBindings[positions[device.position]]) {
            deviceSignal.text = device.signal

            if (deviceEditMode.tag == false) {
                deviceStrength.setText(device.strength)
                deviceStartFirst.setText(device.startFirst)
                deviceStartSecond.setText(device.startSecond)
                deviceStopFirst.setText(device.stopFirst)
                deviceStopSecond.setText(device.stopSecond)
            }
        }
    }

    private fun <D> observe(liveData: LiveData<D>, observer: Observer<D>) {
        observableList.add(liveData)
        liveData.observe(this, observer)
    }

    private fun provideLooper(name: String): Looper {
        return HandlerThread(name).apply {
            start()
        }.looper
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}