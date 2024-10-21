package com.github.cheng;

import kotlin.experimental.ExperimentalTypeInference

class Machine {
    // initialize stack
    internal val stack: IntArray

    // stack pointer
    internal var sp: Int = 0

    // initialize heap
    internal val heap: IntArray

    // functions...
    internal val functions: Array<Func>

    /**
     * Initialize machine with functions
     */
    constructor(functions: Array<Func>) {
        this.functions = functions
        this.stack = IntArray(STACK_MAX_SIZE) { 0 }
        this.heap = IntArray(HEAP_MAX_SIZE) { 0 }
    }

    constructor(functions: Array<Func>, maxStack: Int, maxHeap: Int) {
        this.functions = functions
        this.stack = IntArray(maxStack)
        this.heap = IntArray(maxHeap)
    }


    fun pop(): Int {
        this.sp -= 1
        val element = this.stack[this.sp]
        //clean
        this.stack[this.sp] = 0
        return element
    }


    fun push(item: Int) {
        this.stack[this.sp] = item
        this.sp += 1
    }

    fun load(addr: Int): Int {
        return this.heap[addr]
    }

    fun store(addr: Int, value: Int) {
        this.heap[addr] = value
    }

    @Throws(Break::class)
    fun call(func: Func, args: IntArray): Int? {
        if (func.nprams != args.size) {
            throw IllegalArgumentException("Wrong number of arguments")
        }
        when (func) {
            is Function -> {
                try {
                    this.execute(func.code, locals = args)
                } catch (cause: Break) {
                    when (cause.because) {
                        is Because.InstructionException -> {
                            println("System Instruction error : ${cause.because.instruction}")
                            throw cause
                        }

                        Because.Return -> {
                            //ignore
                        }

                        else -> {
                            throw cause
                        }
                    }
                }
            }

            is NativeFunction -> {
                try {
                    func.call.invoke(this, args)
                } catch (cause: Break) {
                    when (cause.because) {
                        is Because.InstructionException -> {
                            println("System Instruction error : ${cause.because.instruction}")
                            throw cause
                        }

                        Because.Return -> {
                            //ignore
                        }

                        else -> {
                            throw cause
                        }
                    }
                }
            }
        }

        return if (func.returns) {
            this.pop()
        } else {
            null
        }

    }

    @Throws(Break::class)
    fun execute(code: Array<Instruction>, locals: IntArray?) {
        var pc = 0
        while (pc != code.size) {
            val instruction = code[pc]
            val (op, args, args1) = instruction
            when (op) {
                // push
                1 -> {
                    this.push(args)
                }
                //load
                3 -> {
                    val addr = this.pop()
                    val variable = this.load(addr)
                    this.push(variable)
                }
                //store
                4 -> {
                    val value = this.pop()
                    val address = this.pop()
                    this.store(address, value)
                }
                //call
                5 -> {
                    val function = this.functions[args]
                    val functionArgs = if (function.nprams > 0) {
                        (1..function.nprams).map { this.pop() }.reversed().toIntArray()
                    } else {
                        IntArray(0)
                    }
                    val result = this.call(function, functionArgs)
                    if (function.returns) {
                        if (result == null) {
                            throw Break(because = Because.FunctionEmptyReturn(function.funcName))
                        }
                        this.push(result)
                    }
                }
                // local.get
                6 -> {
                    val locals = locals ?: throw Break(because = Because.LocalsIsNull)
                    val data = locals[args]
                    this.push(data)
                }
                // local.set
                7 -> {
                    val locals = locals ?: throw Break(because = Because.LocalsIsNull)
                    locals[args] = this.pop()
                }
                //return
                8 -> {
                    throw Break(because = Because.Return)
                }
                // add
                9 -> {
                    binOp { left, right -> left + right }
                }
                // sub
                10 -> {
                    binOp { left, right -> left - right }
                }
                // mul
                11 -> {
                    binOp { left, right -> left * right }
                }
                //div
                12 -> {
                    binOp { left, right -> left / right }
                }
                // and
                13 -> {
                    binOp { left, right -> left and right }
                }
                // or
                14 -> {
                    binOp { left, right -> left or right }
                }
                // xor
                15 -> {
                    binOp { left, right -> left xor right }
                }
                // rem
                16 -> {
                    binOp { left, right -> left % right }
                }
                // eq ==
                17 -> {
                    logOp { left, right -> left == right }
                }
                // nq !=
                18 -> {
                    logOp { left, right -> left != right }
                }
                // lt <
                19 -> {
                    logOp { left, right -> left < right }
                }
                // le <=
                20 -> {
                    logOp { left, right -> left <= right }
                }
                // ge >
                21 -> {
                    logOp { left, right -> left > right }
                }
                // gt >=
                22 -> {
                    logOp { left, right -> left >= right }
                }
                // shl <<
                23 -> {
                    binOp { left, right -> left shl right }
                }
                // shr >>
                24 -> {
                    binOp { left, right -> left shr right }
                }

                else -> {
                    throw Break(because = Because.InstructionException(instruction))
                }
            }
            pc += 1
        }
    }

    companion object {

        class MachineBuilder(var maxStack: Int, var maxHeap: Int, var functions: Array<Func>)

        fun MachineBuilder.create(): Machine {
            if (this.maxStack <= 0 || this.maxHeap <= 0) {
                throw IllegalArgumentException("error stack or heap size")
            }
            return Machine(functions, maxStack, maxHeap)
        }

        fun new(): MachineBuilder {
            return MachineBuilder(STACK_MAX_SIZE, HEAP_MAX_SIZE, emptyArray())
        }

        fun defaultNew(functions: Array<Func>): Machine {
            return Machine(functions)
        }
    }

    private fun binOp(logic: (left: Int, right: Int) -> Int) {
        val left = this.pop()
        val right = this.pop()
        this.push(logic(left, right))
    }

    private fun logOp(logic: (left: Int, right: Int) -> Boolean) {
        val left = this.pop()
        val right = this.pop()
        if (logic(left, right)) {
            this.push(1)
        } else {
            this.push(0)
        }
    }
}

sealed class Func(
    /**
     * The function name
     */
    open val funcName: String,
    /**
     * number of parameters
     */
    open val nprams: Int,

    /**
     * Whether function to return
     */
    open val returns: Boolean
)

data class Function(
    override val funcName: String,
    override val nprams: Int,
    override val returns: Boolean,
    val code: Array<Instruction>
) :
    Func(funcName, nprams, returns)

data class NativeFunction(
    override val funcName: String,
    override val nprams: Int,
    override val returns: Boolean,
    val call: Machine.(IntArray) -> Unit
) :
    Func(funcName, nprams, returns)

/**
 * 一个指令模型
 */
data class Instruction(val op: Int, val args: Int, val args1: Int, val body: Array<Instruction>?) {
    constructor(op: Int) : this(op, 0, 0, null)
    constructor(op: Int, args: Int) : this(op, args, 0, null)

    //暂时无用
    constructor(op: Int, args: Int, body: Array<Instruction>) : this(op, args, 0, body)

    override fun toString(): String {
        return "Instruction(op=$op, args=$args, args1=$args1, body=${body?.contentToString()})"
    }
}

/**
 * 执行中断
 */
class Break(val because: Because) : Throwable()

/**
 * 中断原因
 */
sealed class Because {
    /**
     * 方法返回
     */
    data object Return : Because()

    /**
     * 指令错误
     */
    class InstructionException(val instruction: Instruction) : Because()

    /**
     * 方法应返回一个值，但没有返回
     */
    data class FunctionEmptyReturn(val fname: String) : Because()

    /**
     * 不在方法里调用local指令
     */
    data object LocalsIsNull : Because()
}

/**
 * 一个指令的快捷构建方法
 * ```
 * buildInstruction {
 *    pair(6, 0)
 *    pair(6, 1)
 *    single(9)
 *    single(8)
 * }
 * ```
 */
@OptIn(ExperimentalTypeInference::class)
fun buildInstruction(@BuilderInference scope: InstructionBuildScope.() -> Unit): Array<Instruction> {
    return InstructionBuildScope().apply(scope).toArray()
}

class InstructionBuildScope {
    internal var list: MutableList<Instruction> = arrayListOf()

    fun single(op: Int) {
        add(Instruction(op))
    }

    fun pair(op: Int, arg: Int) {
        add(Instruction(op, arg))
    }

    fun add(instruction: Instruction) {
        list.add(instruction)
    }

    internal fun toArray(): Array<Instruction> {
        return list.toTypedArray()
    }
}

fun Machine.run(code: Array<Instruction>) {
    try {
        this.execute(code, null)
    } catch (e: Break) {
        if (e.because !is Because.Return) {
            throw e
        }
    }
}

fun Machine.run(scope: InstructionBuildScope.() -> Unit) {
    val instructions = buildInstruction(scope)
    this.run(instructions)
}