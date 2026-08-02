package io.github.amedeoalf.dumb_controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import kotlin.time.Duration.Companion.milliseconds


class ServerConnection(val server: InetSocketAddress) {

    val sock = DatagramSocket()
    var controllerId = MutableSharedFlow<Int>(1, 0, BufferOverflow.DROP_OLDEST)
    var state = ControllerState()
        set(value) {
            field = value
            sendFlow.tryEmit(value)
        }

    private val sendFlow = MutableSharedFlow<ControllerState>(1, 0, BufferOverflow.DROP_OLDEST)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            delay(300.milliseconds)
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
        sendFlow.onEach {
            sendState(it)
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    companion object {
        val INPUT_PACKET = 0
        val PLAYER_NUM_PACKET = 1
        val BROADCAST_PACKET = 2

        val BROADCAST_MAGIC =
            " dumb_controller".toByteArray().apply { set(0, BROADCAST_PACKET.toByte()) }

        suspend fun connectWithBroadcast(broadCastAddress: InetAddress): ServerConnection? =
            withContext(Dispatchers.IO) {
                DatagramSocket().use {
                    it.broadcast = true

                    it.send(
                        DatagramPacket(
                            BROADCAST_MAGIC,
                            BROADCAST_MAGIC.size,
                            InetSocketAddress(broadCastAddress, 8081)
                        )
                    )

                    val pkt = DatagramPacket(ByteArray(BROADCAST_MAGIC.size), BROADCAST_MAGIC.size)
                    it.soTimeout = 1500
                    return@use try {
                        it.receive(pkt)
                        // TODO: check magic bytes
                        ServerConnection(InetSocketAddress(pkt.address, pkt.port))
                    } catch (_: SocketTimeoutException) {
                        null
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

    fun close() {
//        throw Exception("WHYYY")
        println("close")
        //sock.close()
    }
}