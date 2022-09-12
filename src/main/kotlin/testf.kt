import kotlin.math.roundToInt

fun main() {
    val var1 = 1.0+3.3
    val var2 = 5.8-1.99
    val sum = var1.roundToInt() + var2.roundToInt()
    println(var1.roundToInt() + var2.roundToInt())
    println(sum)
}