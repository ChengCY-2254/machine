package com.github.cheng

import com.github.cheng.Machine.Companion.create

fun main() {
    testAdd()
}

fun testAdd() {
    val machine = Machine.new().apply {
        this.functions = arrayOf(
            Function(
                funcName = "add function",
                nprams = 2,
                returns = true,
                code = arrayOf(
                    // a,b => a+b
                    Instruction(6, 0),
                    Instruction(6, 1),
                    Instruction(9)
                )
            ),
            Function(
                funcName = "A equals b",
                nprams = 2,
                returns = true,
                buildInstruction {
                    pair(6, 0)
                    pair(6, 1)
                    single(9)
                    single(8)
                }
            ),
            NativeFunction(
                funcName = "print current value",
                nprams = 1,
                returns = false
            ) {
                val cur = it[0]
                println("current value is $cur")
                this.push(cur)
            }
        )
    }.create()

    val result1Address = 1
    val result2Address = 5

    machine.run {
        // push address 5
        pair(1, result2Address)
        // push address 1
        pair(1, result1Address)
        // push number 5
        pair(1, 5)
        // push number 6
        pair(1, 6)
        // call add function
        pair(5, 0)
        // store
        single(4)
        // push 11
        pair(1, 11)
        // push address 1
        pair(1, result1Address)
        // load
        single(3)
        // call println function
        pair(5, 2)
        // call A equals b
        pair(5, 1)
        // store
        single(4)
    }

    val result = machine.load(result1Address) == 11
    val result2 = machine.load(result2Address) == 22
    println("machine: result is 11 $result")
    println("machine: result equals 22 $result2")
}