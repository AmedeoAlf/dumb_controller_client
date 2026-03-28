package io.github.amedeoalf.dumb_controller

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.amedeoalf.dumb_controller.ui.theme.DumbControllerTheme
import kotlinx.coroutines.flow.first

@Preview
@Composable
fun ControllerScreen(conn: ServerConnection? = null) {
    DumbControllerTheme {
        Surface {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                verticalArrangement = Arrangement.Top
            ) {
                ControllerButton.entries.map { btn ->
                    item {
                        Box(
                            Modifier
                                .pointerInput(Unit) {
                                    if (conn == null) return@pointerInput
                                    detectTapGestures(onPress = {
                                        try {
                                            val last = conn.state.first()
                                            conn.state.emit(
                                                last.withSetBtn(btn, true)
                                            )
                                            awaitRelease()
                                            conn.state.emit(
                                                conn.state.first().withSetBtn(btn, false)
                                            )
                                        } finally {
                                            conn.state.emit(
                                                conn.state.first().withSetBtn(btn, false)
                                            )
                                        }
                                    })
                                }
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .size(70.dp)
                        ) {
                            Text(btn.name)
                        }
                    }
                }

            }
        }
    }
}