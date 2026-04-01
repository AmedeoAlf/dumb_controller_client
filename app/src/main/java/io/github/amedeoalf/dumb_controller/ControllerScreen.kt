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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Stick(
                        "L", Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                    ) { offset ->
                        val (x, y) = offset / 100f
                        fun Float.normalize() =
                            (Math.clamp(this, -1f, 1f) * 0x7FFF).toInt().toShort()

                        conn?.mutateState {
                            setAxis(ControllerAxis.X, x.normalize())
                            setAxis(ControllerAxis.Y, y.normalize())
                        }
                    }
                    val modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                    LazyHorizontalGrid(
                        rows = GridCells.FixedSize(80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            ButtonElement("LT", modifier) {
                                conn?.mutateState { lt = (if (it) 255 else 0).toByte() }
                            }
                        }
                        item {
                            ButtonElement("RT", modifier) {
                                conn?.mutateState { rt = (if (it) 255 else 0).toByte() }
                            }
                        }
                        for (btn in listOf(
                            ControllerButton.LB,
                            ControllerButton.RB,
                            ControllerButton.LS,
                            ControllerButton.RS,
                            ControllerButton.START,
                            ControllerButton.SELECT
                        )) item {
                            ButtonElement(btn.name, modifier) { pressed ->
                                conn?.mutateState {
                                    setButton(btn, pressed)
                                }
                            }
                        }

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
    textColor: Color = Color.Unspecified,
    onAction: suspend (press: Boolean) -> Unit,
) {
    Box(Modifier
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
        .aspectRatio(1f)
        .then(modifier)) {
        Text(name, modifier = Modifier.align(Alignment.Center), color = textColor)
    }
}

@Composable
fun Stick(
    name: String,
    modifier: Modifier = Modifier,
    onDrag: (Offset) -> Unit,
) {
    var dragStart: Offset? by remember { mutableStateOf(null) }
    Box(modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragStart = it },
                onDragEnd = { onDrag(Offset.Zero) },
                onDragCancel = { onDrag(Offset.Zero) }) { change, _ ->
                onDrag(change.position - (dragStart ?: Offset.Zero))
            }
        }
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.secondaryContainer)) {
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

    @Composable
    fun PrimaryBtn(name: String, btn: ControllerButton) = ButtonElement(
        name,
        Modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.primaryContainer),
        MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        conn?.mutateState { setButton(btn, it) }
    }

    @Composable
    fun SecondaryBtn(btn: ControllerButton) = ButtonElement(
        btn.name,
        Modifier
            .padding(10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        conn?.mutateState { setButton(btn, it) }
    }

    LazyHorizontalGrid(
        rows = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { SecondaryBtn(ControllerButton.LB) }
        item { PrimaryBtn("X", ControllerButton.WEST) }
        item { SecondaryBtn(ControllerButton.LS) }
        item { PrimaryBtn("Y", ControllerButton.NORTH) }
        item {
            Stick("R", Modifier.aspectRatio(1f)) {
                val (x, y) = it / 100f
                fun Float.normalize() = (Math.clamp(this, -1f, 1f) * 0x7FFF).toInt().toShort()

                conn?.mutateState {
                    setAxis(ControllerAxis.RX, x.normalize())
                    setAxis(ControllerAxis.RY, y.normalize())
                }
            }
        }
        item { PrimaryBtn("A", ControllerButton.SOUTH) }
        item { SecondaryBtn(ControllerButton.RB) }
        item { PrimaryBtn("B", ControllerButton.EAST) }
        item { SecondaryBtn(ControllerButton.RS) }
    }
}
