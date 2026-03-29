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
        (if (x == -1) 0xf else 0)
                shl 4
                or (if (y == -1) 0xf else 0)
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
    val incremental: UInt,
    val buttons: BitSet = BitSet(BUTTON_COUNT),
    val axes: ShortArray = ShortArray(4),
    val lt: Byte = 0,
    val rt: Byte = 0,
    val hatValue: HatValue = HatValue.MID
) {

    fun withSetBtn(button: ControllerButton, pressed: Boolean) = copy(
        incremental = incremental + 1u,
        buttons = (buttons.clone() as BitSet).also { it[button.ordinal] = pressed }
    )

    fun withAxis(axis: ControllerAxis, value: Short) = copy(
        incremental = incremental + 1u,
        axes = axes.also { it[axis.ordinal] = value }
    )

    fun withLt(value: Byte) = copy(
        incremental = incremental + 1u,
        lt = value
    )

    fun withRt(value: Byte) = copy(
        incremental = incremental + 1u,
        rt = value
    )

    fun withHat(hat: HatValue) = copy(
        incremental = incremental + 1u,
        hatValue = hat
    )

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