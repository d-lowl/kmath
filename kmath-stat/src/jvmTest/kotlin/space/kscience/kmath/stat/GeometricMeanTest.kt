package space.kscience.kmath.stat

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import space.kscience.kmath.structures.DoubleBuffer

internal class GeometricMeanTest {

    fun testGeoMeanEqual(data: DoubleBuffer, expected: Double) {
        assertEquals(expected, Double.geometricMean(data))
    }

    @Test
    fun evaluate() {
        testGeoMeanEqual(
            DoubleBuffer(10.0),
            10.0
        )

        testGeoMeanEqual(
            DoubleBuffer(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0),
            45.28728688116766
        )
    }
}