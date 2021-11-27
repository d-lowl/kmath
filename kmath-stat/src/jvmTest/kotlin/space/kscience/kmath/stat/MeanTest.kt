package space.kscience.kmath.stat

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import space.kscience.kmath.structures.DoubleBuffer

internal class MeanTest {

    @Test
    fun evaluate() {
        assertEquals(Double.mean(DoubleBuffer(1.0, 2.0, 3.0, 4.0, 5.0)), 3.0)
    }
}