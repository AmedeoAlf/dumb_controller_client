package io.github.amedeoalf.dumb_controller

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ServerConnectCard(conn, connectTo)
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Stick(
                        onDrag = { offset ->
                            val (x, y) = offset / 100f
                            fun Float.normalize() =
                                (Math.clamp(this, -1f, 1f) * 0x7FFF).toInt().toShort()

                            conn?.mutateState {
                                setAxis(ControllerAxis.X, x.normalize())
                                setAxis(ControllerAxis.Y, y.normalize())
                            }
                        }, modifier = Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                    ) {

                    }

                    val modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                    LazyHorizontalGrid(
                        rows = GridCells.FixedSize(80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
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
                    RightSide(
                        conn, Modifier
                            .fillMaxHeight()
                            .padding(5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StickScope.DraggableButton(
    name: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    onAction: (press: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .aspectRatio(1f)
            .then(modifier)
            .indication(interactionSource, ripple())
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            onAction(true)
                            awaitRelease()
                            onAction(false)
                        } catch (_: GestureCancellationException) {
                            // It is fine, the user likely started a drag
                        }
                    })
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = {
                    dragStart.value = it
                }, onDragEnd = {
                    onDrag(Offset.Zero)
                    onAction(false)
                }, onDragCancel = {
                    onDrag(Offset.Zero)
                    onAction(false)
                }) { change, _ ->
                    val diff = change.position - (dragStart.value ?: Offset.Zero)
                    if (diff.x > 10 || diff.y > 10) onDrag(diff)
                }
            }) {
        Text(name, modifier = Modifier.align(Alignment.Center), color = textColor)
    }
}

@Composable
fun ButtonElement(
    name: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    onAction: suspend (press: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .aspectRatio(1f)
            .then(modifier)
            .indication(interactionSource, ripple())
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
            }) {
        Text(name, modifier = Modifier.align(Alignment.Center), color = textColor)
    }
}

data class StickScope(val onDrag: (Offset) -> Unit, var dragStart: MutableState<Offset?>)

@Composable
fun Stick(
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable StickScope.() -> Unit
) {
    val dragStart = remember { mutableStateOf<Offset?>(null) }
    Box(Modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragStart.value = it },
                onDragEnd = { onDrag(Offset.Zero) },
                onDragCancel = { onDrag(Offset.Zero) }) { change, _ ->
                onDrag(change.position - (dragStart.value ?: Offset.Zero))
            }
        }
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceBright)
        .then(modifier), contentAlignment = Alignment.TopEnd) {
        StickScope(onDrag, dragStart).content()
    }
}

@Composable
fun ServerConnectCard(conn: ServerConnection?, connectTo: ((String) -> Unit)?) {
    val textFieldState = rememberTextFieldState(conn?.server?.hostString ?: "192.168.1.1")
    Card {
        Row(
            Modifier.padding(10.dp),
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
}

@Composable
fun StickScope.FaceButtons(conn: ServerConnection?, modifier: Modifier = Modifier) {

    @Composable
    fun PrimaryBtn(name: String, btn: ControllerButton) = DraggableButton(
        name,
        Modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.primaryContainer),
        MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        conn?.mutateState { setButton(btn, it) }
    }

    @Composable
    fun SecondaryBtn(btn: ControllerButton) = DraggableButton(
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        item { SecondaryBtn(ControllerButton.LB) }
        item { PrimaryBtn("X", ControllerButton.WEST) }
        item { SecondaryBtn(ControllerButton.LS) }
        item { PrimaryBtn("Y", ControllerButton.NORTH) }
        item {
            Box(
                Modifier.aspectRatio(1f), contentAlignment = Alignment.Center
            ) { Text("R") }
        }
        item { PrimaryBtn("A", ControllerButton.SOUTH) }
        item { SecondaryBtn(ControllerButton.RB) }
        item { PrimaryBtn("B", ControllerButton.EAST) }
        item { SecondaryBtn(ControllerButton.RS) }
    }

}

@Composable
fun RightSide(conn: ServerConnection?, modifier: Modifier = Modifier) {
    Stick(modifier = modifier, onDrag = {
        val (x, y) = it / 100f
        fun Float.normalize() = (Math.clamp(this, -1f, 1f) * 0x7FFF).toInt().toShort()

        conn?.mutateState {
            setAxis(ControllerAxis.RX, x.normalize())
            setAxis(ControllerAxis.RY, y.normalize())
        }
    }) {
        FaceButtons(conn, Modifier.heightIn(max = 300.dp))
    }
}
