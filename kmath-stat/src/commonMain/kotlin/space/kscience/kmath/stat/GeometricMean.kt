/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package space.kscience.kmath.stat

import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.operations.FloatField
import space.kscience.kmath.operations.Ring
import space.kscience.kmath.operations.invoke
import space.kscience.kmath.structures.Buffer

public class GeometricMean<T>(
    private val group: Ring<T>,
    private val root: (product: T, count: Int) -> T,
) : BlockingStatistic<T, T> {
    override fun evaluateBlocking(data: Buffer<T>): T =
        group {
            var res = one
            data.iterator().forEach {
                res *= it
            }
            root(res, data.size)
        }
}

public val Double.Companion.geometricMean: GeometricMean<Double> get() = GeometricMean(DoubleField) {product, count -> DoubleField.power(product, 1.0 / count) }
public val Float.Companion.geometricMean: GeometricMean<Float> get() = GeometricMean(FloatField) {product, count -> FloatField.power(product, 1.0 / count) }