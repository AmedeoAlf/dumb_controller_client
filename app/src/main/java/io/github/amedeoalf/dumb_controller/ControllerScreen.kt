package io.github.amedeoalf.dumb_controller

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.amedeoalf.dumb_controller.ui.theme.DumbControllerTheme

@Preview(name = "Telefono", device = Devices.PHONE + ",orientation=landscape", showSystemUi = true)
@Preview(
    name = "Telefono dark",
    device = Devices.PHONE + ",orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(name = "Tablet", device = Devices.TABLET)
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
                Row(Modifier.fillMaxWidth()) {
                    Stick("L") { x, y ->
                        fun Float.asShort() = (this * 0x7FFF).toInt().toShort()

                        conn?.mutateState {
                            setAxis(ControllerAxis.X, x.asShort())
                            setAxis(ControllerAxis.Y, y.asShort())
                        }
                    }
                    ButtonElement("LT") {
                        conn?.mutateState { lt = (if (it) 255 else 0).toByte() }
                    }
                    ButtonElement("RT") {
                        conn?.mutateState { rt = (if (it) 255 else 0).toByte() }
                    }
                    Spacer(Modifier.weight(1f))
                    FaceButtons(
                        conn,
                        Modifier
                            .heightIn(max = 300.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ButtonElement(
    name: String,
    modifier: Modifier = Modifier,
    onAction: suspend (press: Boolean) -> Unit,
) {
    Box(
        modifier
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

fun Modifier.offsetByPercent(x: Float, y: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(
                x = (constraints.maxWidth * x).toInt(),
                y = (constraints.maxHeight * y).toInt()
            )
        }
    }
)

@Composable
fun FaceButtons(conn: ServerConnection?, modifier: Modifier = Modifier) {
    val oneThird = 1f / 3
    Box(modifier) {
        ButtonElement(
            "Y", Modifier
                .offsetByPercent(oneThird, 0f)
                .fillMaxSize(oneThird)
        ) {
            conn?.mutateState { setButton(ControllerButton.NORTH, it) }
        }
        ButtonElement(
            "X", Modifier
                .offsetByPercent(0f, oneThird)
                .fillMaxSize(oneThird)
        ) {
            conn?.mutateState { setButton(ControllerButton.WEST, it) }
        }
        ButtonElement(
            "B", Modifier
                .offsetByPercent(2 * oneThird, oneThird)
                .fillMaxSize(oneThird)
        ) {
            conn?.mutateState { setButton(ControllerButton.EAST, it) }
        }
        ButtonElement(
            "A", Modifier
                .offsetByPercent(oneThird, oneThird * 2)
                .fillMaxSize(oneThird)
        ) {
            conn?.mutateState { setButton(ControllerButton.SOUTH, it) }
        }
    }
}
