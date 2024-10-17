package com.github.cheng;

class Machine {
    // initialize stack
    private val stack: IntArray = IntArray(STACK_MAX_SIZE) { 0 }

    // stack pointer
    private var sp: Int = 0

    // initialize heap
    private val heap: IntArray = IntArray(HEAP_MAX_SIZE) { 0 }

    // functions...
    private val functions: Array<Func>

    /**
     * Initialize machine with functions
     */
    constructor(functions: Array<Func>) {
        this.functions = functions
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
                    func.call.invoke(args)
                } catch (cause: Break) {
                    when (cause.because) {
                        is Because.InstructionException -> {
                            println("System Instruction error : ${cause.because.instruction}")
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

    companion object;

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
    val call: (IntArray) -> Unit
) :
    Func(funcName, nprams, returns)

/**
 * Instruction op:1 args:1 args1:0 should be push 1 on stack
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

class Break(val because: Because) : Throwable()

sealed class Because {
    data object Return : Because()
    class InstructionException(val instruction: Instruction) : Because()
    data class FunctionEmptyReturn(val fname: String) : Because()
    data object LocalsIsNull : Because()
}
