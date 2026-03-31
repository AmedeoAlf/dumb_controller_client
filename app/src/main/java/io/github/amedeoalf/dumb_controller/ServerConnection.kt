package io.github.amedeoalf.dumb_controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class ServerConnection(val server: InetSocketAddress) {
    val INPUT_PACKET = 0
    val PLAYER_NUM_PACKET = 1

    val sock = DatagramSocket()
    var controllerId = MutableSharedFlow<Int>(1, 0, BufferOverflow.DROP_OLDEST)
    var state = ControllerState()
        set(value) {
            CoroutineScope(Dispatchers.IO).launch { sendState(value) }
            field = value
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            delay(300)
            makeControllerIdRequest()

            val buffer = ByteArray(3)
            val datagramPacket = DatagramPacket(buffer, 3)
            while (true) {
                sock.receive(datagramPacket)
                when (buffer[0].toInt()) {
                    INPUT_PACKET -> sendState(state)
                    PLAYER_NUM_PACKET -> controllerId.emit(buffer[1].toInt() shl 8 or buffer[0].toInt())
                }
            }
        }
    }

    fun mutateState(mutate: ControllerState.() -> Unit) {
        state = state.copy().apply {
            newSnapshot()
            mutate()
        }
    }

    fun sendState(state: ControllerState) {
        ByteArrayOutputStream().use {
            it.write(INPUT_PACKET)
            state.serialize(DataOutputStream(it))
            sock.send(
                DatagramPacket(it.toByteArray(), it.size(), server)
            )
        }
    }

    fun makeControllerIdRequest() {
        sock.send(
            DatagramPacket(byteArrayOf(PLAYER_NUM_PACKET.toByte()), 1, server)
        )
    }

}