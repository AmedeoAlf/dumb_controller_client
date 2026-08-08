package io.github.amedeoalf.dumb_controller

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    lateinit var conn: MutableState<ServerConnection?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        val reconnectTo: (ServerConnection) -> Unit = {
            val old = conn.value
            conn.value = it
            old?.close()
        }
        setContent {
            conn = remember { mutableStateOf(null) }
            ControllerScreen(
                conn.value,
                sync = {
                    getBroadcastAddress().also {
                        println("broadcast addr: $it")
                    }?.let { ServerConnection.connectWithBroadcast(it) }
                        ?.also {
                            reconnectTo(it)
                        }
                },
                connectTo = {
                    CoroutineScope(Dispatchers.IO).launch {
                        reconnectTo(ServerConnection(InetSocketAddress(it, 8081)))
                    }
                })
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val repeat = event == null || event.repeatCount != 0
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (!repeat) conn.value?.mutateState {
                rt = 255.toByte()
            }

            KeyEvent.KEYCODE_VOLUME_UP -> if (!repeat) conn.value?.mutateState { lt = 255.toByte() }
            else -> return false
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> conn.value?.mutateState { rt = 0.toByte() }
            KeyEvent.KEYCODE_VOLUME_UP -> conn.value?.mutateState { lt = 0.toByte() }
            else -> return false
        }
        return true
    }

    @Throws(IOException::class)
    fun getBroadcastAddress(): InetAddress? {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .find { !it.isLoopback }
            ?.interfaceAddresses
            ?.find { it.broadcast != null }
            ?.broadcast
    }
}