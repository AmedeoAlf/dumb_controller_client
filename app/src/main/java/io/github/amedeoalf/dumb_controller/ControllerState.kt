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

enum class ControllerAxis {
    X,
    Y,
    RX,
    RY
}

@JvmInline
value class HatValue(val serialValue: Int) {
    constructor(x: Int, y: Int) : this(
        (if (x == -1) 0xf else x)
                shl 4
                or (if (y == -1) 0xf else y)
    )

    companion object {
        val MID = HatValue(0, 0)
        val MIDL = HatValue(-1, 0)
        val MIDR = HatValue(1, 0)
        val TOP = HatValue(0, -1)
        val TOPL = HatValue(-1, -1)
        val TOPR = HatValue(1, -1)
        val BOT = HatValue(0, 1)
        val BOTL = HatValue(-1, 1)
        val BOTR = HatValue(1, 1)
    }
}

val BUTTON_COUNT = ControllerButton.entries.size
val AXIS_COUNT = ControllerAxis.entries.size

data class ControllerState(
    var buttons: BitSet = BitSet(BUTTON_COUNT),
    var axes: ShortArray = ShortArray(4),
    var lt: Byte = 0,
    var rt: Byte = 0,
    var hatValue: HatValue = HatValue.MID
) {
    var incremental: UInt = 0u
        private set

    fun setButton(button: ControllerButton, pressed: Boolean) {
        buttons[button.ordinal] = pressed
    }

    fun setAxis(axis: ControllerAxis, value: Short) {
        axes[axis.ordinal] = value
    }


    fun newSnapshot() {
        incremental += 1u
    }

    fun serialize(stream: DataOutputStream) {
        stream.writeInt(incremental.toInt())
        stream.writeInt(0) // hash

        buttons.set(17) // makes sure to pad correctly to a short
        stream.writeShort(buttons.toLongArray()[0].toInt())

        for (i in 0..<AXIS_COUNT) {
            stream.writeShort(axes[i].toInt())
        }

        stream.writeByte(lt.toInt())
        stream.writeByte(rt.toInt())

        stream.writeByte(hatValue.serialValue)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControllerState

        if (incremental != other.incremental) return false
        if (buttons != other.buttons) return false
        if (!axes.contentEquals(other.axes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = incremental.hashCode()
        result = 31 * result + buttons.hashCode()
        result = 31 * result + axes.contentHashCode()
        return result
    }
}