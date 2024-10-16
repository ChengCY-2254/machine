package com.github.cheng

fun main() {
    testAdd()
}
fun testAdd(){
    val machine = Machine(
        arrayOf(
            Function(
                funcName = "add function",
                nprams = 2,
                returns = true,
                code = arrayOf(
                    // a,b => a+b
                    Instruction(6,0),
                    Instruction(6,1),
                    Instruction(9)
                )
            ),
            Function(
                funcName = "A equals b",
                nprams = 2,
                returns = true,
                arrayOf(
                    Instruction(6,0),
                    Instruction(6,1),
                    Instruction(9),
                    Instruction(8)
                )

            )
        )
    )

    val result1Address = 1
    val result2Address = 5
    try {
        machine.execute(
            arrayOf(
                // push address 5
                Instruction(1,result2Address),
                // push address 1
                Instruction(1,result1Address),
                // push number 5
                Instruction(1,5),
                // push number 6
                Instruction(1,6),
                // call add function
                Instruction(5,0),
                // store
                Instruction(4),
                // push 11
                Instruction(1,11),
                // push address 1
                Instruction(1,result1Address),
                // load
                Instruction(3),
                // call A equals b
                Instruction(5,1),
                // store
                Instruction(4),
                ),null
        )
    }catch (e:Break){
        if (e.because is Because.Return){
            //ignore
        }else{
            throw e
        }
    }

    val result = machine.load(result1Address)
    val resultBool = machine.load(result2Address)
    println("machine: result is 11 $result")
    println("machine: result equals 22 $resultBool")
}