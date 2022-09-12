import java.lang.Exception
import java.lang.management.ManagementFactory
import java.lang.reflect.Modifier

    fun printUsage() {
        val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()
        for (method in operatingSystemMXBean.javaClass.declaredMethods) {
            method.isAccessible = true
            if (method.name.startsWith("get")
                && Modifier.isPublic(method.modifiers)
            ) {
                var value: Any
                value = try {
                    method.invoke(operatingSystemMXBean)
                } catch (e: Exception) {
                    e
                } // try
                println(method.name + " = " + value)
            } // if
        } // for
    }

const val MEGABYTE = 1024L * 1024L
fun Long.bytesToMegabytes() = this / MEGABYTE




