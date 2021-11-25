/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.asm.internal

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.commons.InstructionAdapter
import space.kscience.kmath.expressions.*
import space.kscience.kmath.misc.UnstableKMathAPI
import space.kscience.kmath.operations.*
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.file.Paths
import kotlin.io.path.writeBytes

@UnstableKMathAPI
internal sealed class PrimitiveAsmBuilder<T : Number, E : Expression<T>>(
    protected val algebra: Algebra<T>,
    classOfT: Class<*>,
    protected val classOfTPrimitive: Class<*>,
    expressionParent: Class<E>,
    protected val target: MST,
) : AsmBuilder() {
    private val className: String = buildName(target)

    /**
     * ASM type for [tType].
     */
    private val tType: Type = classOfT.asm

    /**
     * ASM type for [classOfTPrimitive].
     */
    protected val tTypePrimitive: Type = classOfTPrimitive.asm

    /**
     * ASM type for array of [classOfTPrimitive].
     */
    protected val tTypePrimitiveArray: Type = getType("[" + classOfTPrimitive.asm.descriptor)

    /**
     * ASM type for expression parent.
     */
    private val expressionParentType = expressionParent.asm

    /**
     * ASM type for new class.
     */
    private val classType: Type = getObjectType(className.replace(oldChar = '.', newChar = '/'))

    /**
     * Method visitor of `invoke` method of the subclass.
     */
    protected lateinit var invokeMethodVisitor: InstructionAdapter

    /**
     * Local variables indices are indices of symbols in this list.
     */
    private val argumentsLocals = mutableListOf<Symbol>()

    /**
     * Subclasses, loads and instantiates [Expression] for given parameters.
     *
     * The built instance is cached.
     */
    @Suppress("UNCHECKED_CAST", "UNUSED_VARIABLE")
    val instance: E by lazy {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            visit(
                V1_8,
                ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
                classType.internalName,
                "${OBJECT_TYPE.descriptor}${expressionParentType.descriptor}",
                OBJECT_TYPE.internalName,
                arrayOf(expressionParentType.internalName),
            )

            visitField(
                access = ACC_PRIVATE or ACC_FINAL,
                name = "indexer",
                descriptor = SYMBOL_INDEXER_TYPE.descriptor,
                signature = null,
                value = null,
                block = FieldVisitor::visitEnd,
            )

            visitMethod(
                ACC_PUBLIC,
                "getIndexer",
                getMethodDescriptor(SYMBOL_INDEXER_TYPE),
                null,
                null,
            ).instructionAdapter {
                visitCode()
                val start = label()
                load(0, classType)
                getfield(classType.internalName, "indexer", SYMBOL_INDEXER_TYPE.descriptor)
                areturn(SYMBOL_INDEXER_TYPE)
                val end = label()

                visitLocalVariable(
                    "this",
                    classType.descriptor,
                    null,
                    start,
                    end,
                    0,
                )

                visitMaxs(0, 0)
                visitEnd()
            }

            visitMethod(
                ACC_PUBLIC,
                "invoke",
                getMethodDescriptor(tTypePrimitive, tTypePrimitiveArray),
                null,
                null,
            ).instructionAdapter {
                invokeMethodVisitor = this
                visitCode()
                val start = label()
                visitVariables(target, arrayMode = true)
                visitExpression(target)
                areturn(tTypePrimitive)
                val end = label()

                visitLocalVariable(
                    "this",
                    classType.descriptor,
                    null,
                    start,
                    end,
                    0,
                )

                visitLocalVariable(
                    "arguments",
                    tTypePrimitiveArray.descriptor,
                    null,
                    start,
                    end,
                    1,
                )

                visitMaxs(0, 0)
                visitEnd()
            }

            visitMethod(
                ACC_PUBLIC or ACC_FINAL,
                "invoke",
                getMethodDescriptor(tType, MAP_TYPE),
                "(L${MAP_TYPE.internalName}<${SYMBOL_TYPE.descriptor}+${tType.descriptor}>;)${tType.descriptor}",
                null,
            ).instructionAdapter {
                invokeMethodVisitor = this
                visitCode()
                val start = label()
                visitVariables(target, arrayMode = false)
                visitExpression(target)
                box()
                areturn(tType)
                val end = label()

                visitLocalVariable(
                    "this",
                    classType.descriptor,
                    null,
                    start,
                    end,
                    0,
                )

                visitLocalVariable(
                    "arguments",
                    MAP_TYPE.descriptor,
                    "L${MAP_TYPE.internalName}<${SYMBOL_TYPE.descriptor}+${tType.descriptor}>;",
                    start,
                    end,
                    1,
                )

                visitMaxs(0, 0)
                visitEnd()
            }

            visitMethod(
                ACC_PUBLIC or ACC_FINAL or ACC_BRIDGE or ACC_SYNTHETIC,
                "invoke",
                getMethodDescriptor(OBJECT_TYPE, MAP_TYPE),
                null,
                null,
            ).instructionAdapter {
                visitCode()
                val start = label()
                load(0, OBJECT_TYPE)
                load(1, MAP_TYPE)
                invokevirtual(classType.internalName, "invoke", getMethodDescriptor(tType, MAP_TYPE), false)
                areturn(tType)
                val end = label()

                visitLocalVariable(
                    "this",
                    classType.descriptor,
                    null,
                    start,
                    end,
                    0,
                )

                visitMaxs(0, 0)
                visitEnd()
            }

            visitMethod(
                ACC_PUBLIC or ACC_SYNTHETIC,
                "<init>",
                getMethodDescriptor(VOID_TYPE, SYMBOL_INDEXER_TYPE),
                null,
                null,
            ).instructionAdapter {
                val start = label()
                load(0, classType)
                invokespecial(OBJECT_TYPE.internalName, "<init>", getMethodDescriptor(VOID_TYPE), false)
                load(0, classType)
                load(1, SYMBOL_INDEXER_TYPE)
                putfield(classType.internalName, "indexer", SYMBOL_INDEXER_TYPE.descriptor)
                areturn(VOID_TYPE)
                val end = label()
                visitLocalVariable("this", classType.descriptor, null, start, end, 0)
                visitLocalVariable("indexer", SYMBOL_INDEXER_TYPE.descriptor, null, start, end, 1)
                visitMaxs(0, 0)
                visitEnd()
            }

            visitEnd()
        }

        val binary = classWriter.toByteArray()
        val cls = classLoader.defineClass(className, binary)

        if (System.getProperty("space.kscience.kmath.ast.dump.generated.classes") == "1")
            Paths.get("${className.split('.').last()}.class").writeBytes(binary)

        MethodHandles
            .publicLookup()
            .findConstructor(cls, MethodType.methodType(Void.TYPE, SymbolIndexer::class.java))
            .invoke(SimpleSymbolIndexer(argumentsLocals)) as E
    }

    /**
     * Loads a numeric constant [value] from the class's constants.
     */
    fun loadNumberConstant(value: Number) {
        when (tTypePrimitive) {
            BYTE_TYPE -> invokeMethodVisitor.iconst(value.toInt())
            DOUBLE_TYPE -> invokeMethodVisitor.dconst(value.toDouble())
            FLOAT_TYPE -> invokeMethodVisitor.fconst(value.toFloat())
            LONG_TYPE -> invokeMethodVisitor.lconst(value.toLong())
            INT_TYPE -> invokeMethodVisitor.iconst(value.toInt())
            SHORT_TYPE -> invokeMethodVisitor.iconst(value.toInt())
        }
    }

    /**
     * Stores value variable [name] into a local. Should be called before using [loadVariable]. Should be called only
     * once for a variable.
     */
    fun prepareVariable(name: Symbol, arrayMode: Boolean): Unit = invokeMethodVisitor.run {
        var idx = argumentsLocals.indexOf(name)

        if (idx == -1) {
            argumentsLocals += name
            idx = argumentsLocals.lastIndex
        }

        if (arrayMode) {
            load(1, tTypePrimitiveArray)
            iconst(idx)
            aload(tTypePrimitive)
            store(2 + idx, tTypePrimitive)
        } else {
            load(1, MAP_TYPE)
            aconst(name.identity)

            invokestatic(
                MAP_INTRINSICS_TYPE.internalName,
                "getOrFail",
                getMethodDescriptor(OBJECT_TYPE, MAP_TYPE, STRING_TYPE),
                false,
            )

            checkcast(tType)
            unbox()
            store(2 + idx, tTypePrimitive)
        }
    }

    /**
     * Loads a variable [name] from arguments [Map] parameter of [Expression.invoke]. The variable should be stored
     * with [prepareVariable] first.
     */
    fun loadVariable(name: Symbol): Unit = invokeMethodVisitor.load(2 + argumentsLocals.indexOf(name), tTypePrimitive)

    private fun unbox() = invokeMethodVisitor.run {
        invokevirtual(
            NUMBER_TYPE.internalName,
            "${classOfTPrimitive.simpleName}Value",
            getMethodDescriptor(tTypePrimitive),
            false
        )
    }

    private fun box() = invokeMethodVisitor.run {
        invokestatic(tType.internalName, "valueOf", getMethodDescriptor(tType, tTypePrimitive), false)
    }

    private fun visitVariables(
        node: MST,
        arrayMode: Boolean,
        alreadyLoaded: MutableList<Symbol> = mutableListOf()
    ): Unit = when (node) {
        is Symbol -> when (node) {
            !in alreadyLoaded -> {
                alreadyLoaded += node
                prepareVariable(node, arrayMode)
            }
            else -> {
            }
        }

        is MST.Unary -> visitVariables(node.value, arrayMode, alreadyLoaded)

        is MST.Binary -> {
            visitVariables(node.left, arrayMode, alreadyLoaded)
            visitVariables(node.right, arrayMode, alreadyLoaded)
        }

        else -> Unit
    }

    private fun visitExpression(mst: MST): Unit = when (mst) {
        is Symbol -> loadVariable(mst)
        is MST.Numeric -> loadNumberConstant(mst.value)

        is MST.Unary -> when {
            algebra is NumericAlgebra && mst.value is MST.Numeric -> {
                loadNumberConstant(
                    MST.Numeric(
                        algebra.unaryOperationFunction(mst.operation)(algebra.number((mst.value as MST.Numeric).value)),
                    ).value,
                )
            }

            else -> visitUnary(mst)
        }

        is MST.Binary -> when {
            algebra is NumericAlgebra && mst.left is MST.Numeric && mst.right is MST.Numeric -> {
                loadNumberConstant(
                    MST.Numeric(
                        algebra.binaryOperationFunction(mst.operation)(
                            algebra.number((mst.left as MST.Numeric).value),
                            algebra.number((mst.right as MST.Numeric).value),
                        ),
                    ).value,
                )
            }

            else -> visitBinary(mst)
        }
    }

    protected open fun visitUnary(mst: MST.Unary) {
        visitExpression(mst.value)
    }

    protected open fun visitBinary(mst: MST.Binary) {
        visitExpression(mst.left)
        visitExpression(mst.right)
    }

    protected companion object {
        /**
         * ASM type for [java.lang.Number].
         */
        val NUMBER_TYPE: Type = getObjectType("java/lang/Number")

        /**
         * ASM type for [SymbolIndexer].
         */
        val SYMBOL_INDEXER_TYPE: Type = getObjectType("space/kscience/kmath/expressions/SymbolIndexer")
    }
}

@UnstableKMathAPI
internal class DoubleAsmBuilder(target: MST) : PrimitiveAsmBuilder<Double, DoubleExpression>(
    DoubleField,
    java.lang.Double::class.java,
    java.lang.Double.TYPE,
    DoubleExpression::class.java,
    target,
) {

    private fun buildUnaryJavaMathCall(name: String) {
        invokeMethodVisitor.invokestatic(
            MATH_TYPE.internalName,
            name,
            getMethodDescriptor(tTypePrimitive, tTypePrimitive),
            false,
        )
    }

    private fun buildBinaryJavaMathCall(name: String) {
        invokeMethodVisitor.invokestatic(
            MATH_TYPE.internalName,
            name,
            getMethodDescriptor(tTypePrimitive, tTypePrimitive, tTypePrimitive),
            false,
        )
    }

    private fun buildUnaryKotlinMathCall(name: String) {
        invokeMethodVisitor.invokestatic(
            MATH_KT_TYPE.internalName,
            name,
            getMethodDescriptor(tTypePrimitive, tTypePrimitive),
            false,
        )
    }

    override fun visitUnary(mst: MST.Unary) {
        super.visitUnary(mst)

        when (mst.operation) {
            GroupOps.MINUS_OPERATION -> invokeMethodVisitor.visitInsn(DNEG)
            GroupOps.PLUS_OPERATION -> Unit
            PowerOperations.SQRT_OPERATION -> buildUnaryJavaMathCall("sqrt")
            TrigonometricOperations.SIN_OPERATION -> buildUnaryJavaMathCall("sin")
            TrigonometricOperations.COS_OPERATION -> buildUnaryJavaMathCall("cos")
            TrigonometricOperations.TAN_OPERATION -> buildUnaryJavaMathCall("tan")
            TrigonometricOperations.ASIN_OPERATION -> buildUnaryJavaMathCall("asin")
            TrigonometricOperations.ACOS_OPERATION -> buildUnaryJavaMathCall("acos")
            TrigonometricOperations.ATAN_OPERATION -> buildUnaryJavaMathCall("atan")
            ExponentialOperations.SINH_OPERATION -> buildUnaryJavaMathCall("sqrt")
            ExponentialOperations.COSH_OPERATION -> buildUnaryJavaMathCall("cosh")
            ExponentialOperations.TANH_OPERATION -> buildUnaryJavaMathCall("tanh")
            ExponentialOperations.ASINH_OPERATION -> buildUnaryKotlinMathCall("asinh")
            ExponentialOperations.ACOSH_OPERATION -> buildUnaryKotlinMathCall("acosh")
            ExponentialOperations.ATANH_OPERATION -> buildUnaryKotlinMathCall("atanh")
            ExponentialOperations.EXP_OPERATION -> buildUnaryJavaMathCall("exp")
            ExponentialOperations.LN_OPERATION -> buildUnaryJavaMathCall("log")
            else -> super.visitUnary(mst)
        }
    }

    override fun visitBinary(mst: MST.Binary) {
        super.visitBinary(mst)

        when (mst.operation) {
            GroupOps.PLUS_OPERATION -> invokeMethodVisitor.visitInsn(DADD)
            GroupOps.MINUS_OPERATION -> invokeMethodVisitor.visitInsn(DSUB)
            RingOps.TIMES_OPERATION -> invokeMethodVisitor.visitInsn(DMUL)
            FieldOps.DIV_OPERATION -> invokeMethodVisitor.visitInsn(DDIV)
            PowerOperations.POW_OPERATION -> buildBinaryJavaMathCall("pow")
            else -> super.visitBinary(mst)
        }
    }

    companion object {
        val MATH_TYPE: Type = getObjectType("java/lang/Math")
        val MATH_KT_TYPE: Type = getObjectType("kotlin/math/MathKt")
    }
}

@UnstableKMathAPI
internal class IntAsmBuilder(target: MST) :
    PrimitiveAsmBuilder<Int, IntExpression>(
        IntRing,
        Integer::class.java,
        Integer.TYPE,
        IntExpression::class.java,
        target
    ) {
    override fun visitUnary(mst: MST.Unary) {
        super.visitUnary(mst)

        when (mst.operation) {
            GroupOps.MINUS_OPERATION -> invokeMethodVisitor.visitInsn(INEG)
            GroupOps.PLUS_OPERATION -> Unit
            else -> super.visitUnary(mst)
        }
    }

    override fun visitBinary(mst: MST.Binary) {
        super.visitBinary(mst)

        when (mst.operation) {
            GroupOps.PLUS_OPERATION -> invokeMethodVisitor.visitInsn(IADD)
            GroupOps.MINUS_OPERATION -> invokeMethodVisitor.visitInsn(ISUB)
            RingOps.TIMES_OPERATION -> invokeMethodVisitor.visitInsn(IMUL)
            else -> super.visitBinary(mst)
        }
    }
}

@UnstableKMathAPI
internal class LongAsmBuilder(target: MST) : PrimitiveAsmBuilder<Long, LongExpression>(
    LongRing,
    java.lang.Long::class.java,
    java.lang.Long.TYPE,
    LongExpression::class.java,
    target,
) {
    override fun visitUnary(mst: MST.Unary) {
        super.visitUnary(mst)

        when (mst.operation) {
            GroupOps.MINUS_OPERATION -> invokeMethodVisitor.visitInsn(LNEG)
            GroupOps.PLUS_OPERATION -> Unit
            else -> super.visitUnary(mst)
        }
    }

    override fun visitBinary(mst: MST.Binary) {
        super.visitBinary(mst)

        when (mst.operation) {
            GroupOps.PLUS_OPERATION -> invokeMethodVisitor.visitInsn(LADD)
            GroupOps.MINUS_OPERATION -> invokeMethodVisitor.visitInsn(LSUB)
            RingOps.TIMES_OPERATION -> invokeMethodVisitor.visitInsn(LMUL)
            else -> super.visitBinary(mst)
        }
    }
}
