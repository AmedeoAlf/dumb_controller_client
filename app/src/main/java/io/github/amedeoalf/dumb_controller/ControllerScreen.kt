package io.github.amedeoalf.dumb_controller

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.amedeoalf.dumb_controller.ui.theme.DumbControllerTheme

@Preview(name = "Telefono", device = Devices.PHONE + ",orientation=landscape", showSystemUi = true)
@Preview(name = "Telefono", device = Devices.PHONE + ",orientation=landscape", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ControllerScreen(
    conn: ServerConnection? = null,
    connectTo: ((String) -> Unit)? = null
) {
    DumbControllerTheme {
        Surface {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ServerConnectWidget(conn, connectTo)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top)
                ) {
                    ControllerButton.entries.forEach { btn ->
                        item {
                            ButtonElement(btn.name) {
                                conn?.mutateState { setButton(btn, it) }
                            }
                        }
                    }
                    item {
                        ButtonElement("LT") {
                            conn?.mutateState { lt = (if (it) 255 else 0).toByte() }
                        }
                    }
                    item {
                        ButtonElement("RT") {
                            conn?.mutateState { rt = (if (it) 255 else 0).toByte() }
                        }
                    }
                    item(span = { GridItemSpan(2) }) {
                        Stick("L") { x, y ->
                            fun Float.asShort() = (this * 0x7FFF).toInt().toShort()

                            conn?.mutateState {
                                setAxis(ControllerAxis.X, x.asShort())
                                setAxis(ControllerAxis.Y, y.asShort())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonElement(
    name: String,
    onAction: suspend (press: Boolean) -> Unit
) {
    Box(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            onAction(true)
                            awaitRelease()
                            onAction(false)
                        } finally {
                            onAction(false)
                        }
                    }
                )
            }
            .background(MaterialTheme.colorScheme.primaryContainer)
            .size(70.dp)
    ) {
        Text(name, Modifier.align(Alignment.Center))
    }
}

@Composable
fun Stick(
    name: String,
    onDrag: (Float, Float) -> Unit
) {
    var dragStart: Offset? by remember { mutableStateOf(null) }
    Box(Modifier.size(150.dp)) {
        Box(
            Modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragStart = it },
                        onDragEnd = { onDrag(0f, 0f) },
                        onDragCancel = { onDrag(0f, 0f) }
                    ) { change, _ ->
                        fun Float.normalize() = Math.clamp(this / 150, -1f, 1f)
                        val moved = change.position - dragStart!!
                        onDrag(
                            moved.x.normalize(),
                            moved.y.normalize()
                        )
                    }
                }
                .background(MaterialTheme.colorScheme.primaryContainer)
                .size(150.dp)
                .align(Alignment.Center)
        ) {
            Text(name, Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ServerConnectWidget(conn: ServerConnection?, connectTo: ((String) -> Unit)?) {
    val textFieldState = rememberTextFieldState(conn?.server?.hostString ?: "192.168.1.1")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextField(
            textFieldState,
            label = { Text("Indirizzo del server") }
        )
        Button({
            connectTo?.invoke(textFieldState.text.toString())
        }) { Text("Connetti") }
    }
}