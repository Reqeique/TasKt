package util

import org.jfree.data.time.DynamicTimeSeriesCollection
import org.jfree.data.time.Second
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.PhysicalMemory
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun Long.hertzToGigaHertz(): Float {
    return (this/(10f).pow(9))
}

fun Any.megaBytes(): String {
    return "$this MB"
}

fun Any.gigaByte(): String {
    return "$this GB"
}

fun Any.gigaHertz(): String {
    return "$this GHz"
}

fun Any.megaHertz(): String {
    return "$this MHz"
}
const val MEGABYTE = 1024L * 1024L
const val GIGABYTE = MEGABYTE * 1024L
fun Long.bytesToMegabytes() = this / MEGABYTE
fun Long.bytesToGigabytes() = this.toDouble() / GIGABYTE
inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
    val props = T::class.memberProperties.onEach { it.isAccessible = true}.associateBy { it.name }

    return props.keys.associateWith { props[it]?.get(this) }
}
class Mem(private val globalMemory: GlobalMemory){
    fun memoryInUse(): Long = globalMemory.total - globalMemory.available
    fun memoryFree(): Long = globalMemory.available
    fun memoryTotal(): Long = globalMemory.total
    fun memoryInUsePercentage(): Int = ((memoryInUse().toFloat()/memoryTotal().toFloat())*100).roundToInt()


}
class CPU(private val centralProcessor: CentralProcessor,systemInfo: SystemInfo){
    private var existingTick = LongArray(CentralProcessor.TickType.values().size)
    fun cpuMaxClock(): Long = centralProcessor.maxFreq
    fun cpuInUse(proc: CentralProcessor): Double {
        val t = proc.getSystemCpuLoadBetweenTicks(existingTick); existingTick = proc.systemCpuLoadTicks
        return t
    }
    fun cpuCurrentFreq() = centralProcessor.currentFreq.average().toLong()

    fun cpuContextSwitchers() = centralProcessor.contextSwitches

    fun cpuInterrupt() = centralProcessor.interrupts
    init {
        val date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())

        val sysData = DynamicTimeSeriesCollection(1, 60, Second())
        sysData.setTimeBase(Second(date))
        sysData.addSeries(floatArrayPercent(cpuInUse(systemInfo.hardware.processor)), 0, "All cpus")
    }
    private fun floatArrayPercent(d: Double): FloatArray {
        val f = FloatArray(1)
        f[0] = (100.0 * d).toFloat()
        return f
    }


}
fun GlobalMemory.calculate(): Mem{
    return Mem(this)
}
fun CentralProcessor.calculate(systemInfo: SystemInfo): CPU {
    return CPU(this, systemInfo)
}

data class ProcessorInfo(val Max_speed: String,val  Sockets: String,val Cores: String,val Logical_processor: String)
fun main(){
    SystemInfo().hardware.processor.currentFreq
}
class Hello(var hello: String,val why: String)