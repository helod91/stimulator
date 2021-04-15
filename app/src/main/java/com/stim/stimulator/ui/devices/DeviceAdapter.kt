package com.stim.stimulator.ui.devices

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.stim.domain.models.Packet
import com.stim.stimulator.R
import com.stim.stimulator.databinding.ItemDeviceBinding
import com.stim.stimulator.ui.utils.StimulatorTextWatcher
import kotlin.math.roundToInt

class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    var itemHeight: Int? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var listener: ((data: Int, deviceAddress: Int, address: Int, device: Int) -> Unit)? =
        null
    private var data: ArrayList<Packet.DeviceDataPacket> = arrayListOf(
        Packet.DeviceDataPacket(0),
        Packet.DeviceDataPacket(1),
        Packet.DeviceDataPacket(2),
        Packet.DeviceDataPacket(3),
        Packet.DeviceDataPacket(4),
        Packet.DeviceDataPacket(5)
    )

    fun setDeviceDataPacket(deviceDataPacket: Packet.DeviceDataPacket) {
        data[deviceDataPacket.position] = deviceDataPacket
        notifyDataSetChanged()
    }

    fun setSendDataListener(listener: ((data: Int, deviceAddress: Int, address: Int, device: Int) -> Unit)) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDeviceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = data[position]

        with(holder.binding) {
            itemHeight?.let { root.layoutParams.height = it }

            deviceName.text = root.context.getString(R.string.device_name, position + 1)
            deviceSignal.text = device.signal

            deviceStrength.setText(device.strength)
            deviceStartFirst.setText(device.startFirst)
            deviceStartSecond.setText(device.startSecond)
            deviceStopFirst.setText(device.stopFirst)
            deviceStopSecond.setText(device.stopSecond)

            (deviceStrength.tag as? StimulatorTextWatcher)?.let { textWatcher ->
                deviceStrength.removeTextChangedListener(textWatcher)
            }
            deviceStrength.addTextChangedListener(StimulatorTextWatcher { number ->
                if (deviceStrength.hasFocus() && number in 0..31)
                    listener?.invoke(number, position, 1, 0)
            }.apply {
                deviceStrength.tag = this
            })
            deviceTest.setOnTouchListener { _, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> listener?.invoke(2, position, 8, 0)
                    MotionEvent.ACTION_UP -> listener?.invoke(0, position, 8, 0)
                }
                false
            }

            fun setTimeTextWatcher(input: EditText, address: Int) {
                (input.tag as? StimulatorTextWatcher)?.let { textWatcher ->
                    input.removeTextChangedListener(textWatcher)
                }
                input.addTextChangedListener(StimulatorTextWatcher { number ->
                    if (input.hasFocus() && number in 0..240) {
                        val changedNumber = (number.toFloat() * 255f / 240f).roundToInt()
                        listener?.invoke(changedNumber, position, address, 0)
                    }
                }.apply {
                    input.tag = this
                })
            }

            setTimeTextWatcher(deviceStartFirst, 4)
            setTimeTextWatcher(deviceStartSecond, 6)
            setTimeTextWatcher(deviceStopFirst, 5)
            setTimeTextWatcher(deviceStopSecond, 7)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)
}