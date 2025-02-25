package space.kscience.kmath.operations

import space.kscience.kmath.commons.linear.CMLinearSpace
import space.kscience.kmath.linear.matrix
import space.kscience.kmath.nd.DoubleBufferND
import space.kscience.kmath.nd.Shape
import space.kscience.kmath.nd.Structure2D
import space.kscience.kmath.nd.ndAlgebra
import space.kscience.kmath.viktor.ViktorStructureND
import space.kscience.kmath.viktor.viktorAlgebra

fun main() {
    val viktorStructure: ViktorStructureND = DoubleField.viktorAlgebra.structureND(Shape(2, 2)) { (i, j) ->
        if (i == j) 2.0 else 0.0
    }

    val cmMatrix: Structure2D<Double> = CMLinearSpace.matrix(2, 2)(0.0, 1.0, 0.0, 3.0)

    val res: DoubleBufferND = DoubleField.ndAlgebra {
        exp(viktorStructure) + 2.0 * cmMatrix
    }

    println(res)
}