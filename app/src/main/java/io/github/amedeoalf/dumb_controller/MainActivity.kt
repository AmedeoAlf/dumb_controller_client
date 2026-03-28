package io.github.amedeoalf.dumb_controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.net.InetSocketAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val conn = ServerConnection(
            InetSocketAddress(
                "192.168.188.90", 8081
            )
        )
        setContent {
            ControllerScreen(conn)
        }
    }
}