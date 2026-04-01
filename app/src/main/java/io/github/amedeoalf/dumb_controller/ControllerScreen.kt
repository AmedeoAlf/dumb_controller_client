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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
    conn: ServerConnection? = null, connectTo: ((String) -> Unit)? = null
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
                    Stick("L") { offset ->
                        val (x, y) = offset / 100f
                        fun Float.normalize() =
                            (Math.clamp(this, -1f, 1f) * 0x7FFF).toInt().toShort()

                        conn?.mutateState {
                            setAxis(ControllerAxis.X, x.normalize())
                            setAxis(ControllerAxis.Y, y.normalize())
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
                        conn, Modifier
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
    Box(modifier
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
                })
        }
        .clip(RoundedCornerShape(30.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)) {
        Text(name, Modifier.align(Alignment.Center))
    }
}

@Composable
fun Stick(
    name: String, onDrag: (Offset) -> Unit
) {
    var dragStart: Offset? by remember { mutableStateOf(null) }
    Box(Modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragStart = it },
                onDragEnd = { onDrag(Offset.Zero) },
                onDragCancel = { onDrag(Offset.Zero) }) { change, _ ->
                onDrag(change.position - (dragStart ?: Offset.Zero))
            }
        }
        .background(MaterialTheme.colorScheme.primaryContainer)
        .size(150.dp)) {
        Text(name, Modifier.align(Alignment.Center))
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
            textFieldState, label = { Text("Indirizzo del server") })
        Button({
            connectTo?.invoke(textFieldState.text.toString())
        }) { Text("Connetti") }
    }
}

@Composable
fun FaceButtons(conn: ServerConnection?, modifier: Modifier = Modifier) {
    val oneThird = 1f / 3
    fun placedAt(x: Float, y: Float) =
        Modifier.then(Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(
                        x = (constraints.maxWidth * x).toInt(),
                        y = (constraints.maxHeight * y).toInt()
                    )
                }
            }
            .fillMaxSize(oneThird))

    Box(modifier) {
        ButtonElement("Y", placedAt(oneThird, 0f)) {
            conn?.mutateState { setButton(ControllerButton.NORTH, it) }
        }
        ButtonElement(
            "X", placedAt(0f, oneThird)
        ) {
            conn?.mutateState { setButton(ControllerButton.WEST, it) }
        }
        ButtonElement(
            "B", placedAt(2 * oneThird, oneThird)
        ) {
            conn?.mutateState { setButton(ControllerButton.EAST, it) }
        }
        ButtonElement(
            "A", placedAt(oneThird, oneThird * 2)
        ) {
            conn?.mutateState { setButton(ControllerButton.SOUTH, it) }
        }
    }
}
