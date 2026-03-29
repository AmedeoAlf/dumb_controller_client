package io.github.amedeoalf.dumb_controller

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.amedeoalf.dumb_controller.ui.theme.DumbControllerTheme

@Preview
@Preview(device = Devices.DESKTOP)
@Composable
fun ControllerScreen(
    conn: ServerConnection? = null,
    connectTo: ((String) -> Unit)? = null
) {
    val textFieldState = rememberTextFieldState(conn?.server?.hostString ?: "192.168.1.1")
    DumbControllerTheme {
        Surface {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top)
                ) {
                    ControllerButton.entries.forEach { btn ->
                        item {
                            ButtonElement(btn.name) {
                                conn?.mutateState { withSetBtn(btn, it) }
                            }
                        }
                    }
                    item {
                        ButtonElement("LT") {
                            conn?.mutateState { withLt((if (it) 255 else 0).toByte()) }
                        }
                    }
                    item {
                        ButtonElement("RT") {
                            conn?.mutateState { withRt((if (it) 255 else 0).toByte()) }
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