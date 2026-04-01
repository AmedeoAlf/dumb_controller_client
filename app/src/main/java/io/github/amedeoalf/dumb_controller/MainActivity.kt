package io.github.amedeoalf.dumb_controller

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        setContent {
            var conn by remember {
                mutableStateOf(
                    ServerConnection(
                        InetSocketAddress(
                            "192.168.188.26", 8081
                        )
                    )
                )
            }
            ControllerScreen(conn) {
                CoroutineScope(Dispatchers.IO).launch {
                    conn = ServerConnection(InetSocketAddress(it, 8081))
                }
            }
        }
    }
}