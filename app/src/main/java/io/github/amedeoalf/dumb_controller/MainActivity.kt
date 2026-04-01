package io.github.amedeoalf.dumb_controller

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class MainActivity : ComponentActivity() {
    lateinit var conn: MutableState<ServerConnection>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        conn = mutableStateOf(
            ServerConnection(
                InetSocketAddress(
                    "192.168.188.26", 8081
                )
            )
        )
        setContent {
            ControllerScreen(conn.value) {
                CoroutineScope(Dispatchers.IO).launch {
                    conn.value = ServerConnection(InetSocketAddress(it, 8081))
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val repeat = event == null || event.repeatCount != 0
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (!repeat) conn.value.mutateState { rt = 255.toByte() }
            KeyEvent.KEYCODE_VOLUME_UP -> if (!repeat) conn.value.mutateState { lt = 255.toByte() }
            else -> return false
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> conn.value.mutateState { rt = 0.toByte() }
            KeyEvent.KEYCODE_VOLUME_UP -> conn.value.mutateState { lt = 0.toByte() }
            else -> return false
        }
        return true
    }
}