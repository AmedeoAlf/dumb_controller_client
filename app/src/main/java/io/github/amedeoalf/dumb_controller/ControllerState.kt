package io.github.amedeoalf.dumb_controller

import java.io.DataOutputStream
import java.util.BitSet

enum class ControllerButton {
    SOUTH,
    WEST,
    EAST,
    NORTH,
    START,
    SELECT,
    LB,
    RB,
    LS,
    RS
}

val BUTTON_COUNT = ControllerButton.entries.size

data class ControllerState(
    val incremental: UInt,
    val buttons: BitSet = BitSet(BUTTON_COUNT),
) {

    fun withSetBtn(button: ControllerButton, pressed: Boolean) =
        ControllerState(
            incremental + 1u,
            (buttons.clone() as BitSet).also { it[button.ordinal] = pressed })

    fun serialize(stream: DataOutputStream) {
        stream.writeInt(incremental.toInt())
        stream.writeInt(0) // hash
        buttons.set(17)
        stream.writeShort(buttons.toLongArray()[0].toInt())
    }
}