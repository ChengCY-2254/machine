# Machine

使用kotlin制作的解释器，可进行一系列数学运算，并支持方法调用。

| 操作符                | 作用                                              |
|--------------------|-------------------------------------------------|
| 1(push)  value     | push一个值到stack中                                  |
| 3(load)            | 出栈一个地址，并根据地址从heap中获取值并放入stack中                  |
| 4(store)           | 出栈一个值和一个地址，并加载到stack中                           |
| 5(call) func_id    | 方法调用，调用Machine初始化时传入的function                   |
| 6(local.get) value | 用于方法获取值，是一个临时堆，方法结束即销毁                          |
| 7(local.set) value | 用于设置临时区的值，注意坐标不要超过声明的长度                         |
| 8(return)          | 结束方法,在没有方法包裹的时候（也就是call）会直接抛出`Because.Return`异常 |
| 9(add)             | +                                               |
| 10(sub)            | -                                               |
| 11(mul)            | *                                               |
| 12(div)            | /                                               |
| 13(and)            | 与运算                                             |
| 14(or)             | 或运算                                             |
| 15(xor)            | 异或运算                                            |
| 16(rem)            | %                                               |
| 17(eq)             | ==                                              |
| 18(nq)             | !=                                              |
| 19(lt)             | <                                               |
| 20(le)             | <=                                              |
| 21(ge)             | \>                                              |
| 22(gt)             | \>=                                             |