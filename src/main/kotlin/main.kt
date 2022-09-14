//import com.sun.management.OperatingSystemMXBean

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.data2viz.ExperimentalD2V
import io.data2viz.charts.chart.chart
import io.data2viz.charts.chart.constant
import io.data2viz.charts.chart.discrete
import io.data2viz.charts.chart.mark.MarkCurves
import io.data2viz.charts.chart.mark.bar
import io.data2viz.charts.chart.mark.line
import io.data2viz.charts.chart.mark.plot
import io.data2viz.charts.chart.quantitative
import io.data2viz.charts.config.ChartConfig
import io.data2viz.charts.config.configs.greenConfig
import io.data2viz.charts.config.configs.lightConfig
import io.data2viz.charts.configuration.ChartConfiguration
import io.data2viz.charts.core.CursorType
import io.data2viz.charts.dimension.Dimension
import io.data2viz.charts.viz.newVizContainer
import io.data2viz.color.Colors
import io.data2viz.color.col
import io.data2viz.format.Locale
import io.data2viz.geom.Size
import io.data2viz.math.pct
import io.data2viz.shape.Symbols

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.chart.XYChart
import javafx.scene.layout.Pane
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import oshi.SystemInfo
import oshi.hardware.GlobalMemory
import util.gigaHertz
import util.hertzToGigaHertz
import util.megaBytes
import java.awt.BorderLayout
import java.awt.Container
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import javax.swing.JPanel
import kotlin.math.pow
import kotlin.math.roundToInt
import javafx.scene.text.Text as JFXText


@OptIn(ExperimentalStdlibApi::class, ExperimentalD2V::class)

fun main(args: Array<String>) = application {
    Window(onCloseRequest = ::exitApplication) {
        val jfxpanel = remember { JFXPanel() }
        val jfxtext = remember { JFXText() }
        val container = this@Window.window
        var maxCpuClock by remember { mutableStateOf("") }
        var cpuClock by remember { mutableStateOf("") }
        var cpuName by remember { mutableStateOf("") }
        var isCPUClicked by remember { mutableStateOf(true) }
        var isMemoryClicked by remember { mutableStateOf(false) }
        var totalMemory by remember { mutableStateOf(0L) }
        var memoryFree by remember { mutableStateOf("") }
        var memoryInUse by remember { mutableStateOf("") }
        val cpuInUse by remember { mutableStateOf("") }
        var memoryInUsePercentage by remember { mutableStateOf("") }
//    val osBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(
//        OperatingSystemMXBean::class.java
//    )
        /** used to send statstics to the graphs */
        val memoryChannel = Channel<GlobalMemory>()
//
        CoroutineScope(Main).launch {

            val systemInfo = SystemInfo()
            val hardware = systemInfo.hardware

            cpuName = systemInfo.hardware.processor.processorIdentifier.name.toString()
            totalMemory = hardware.memory.total


            while (true) {

                hardware.processor.currentFreq.toList().map {
                    (it / (10f).pow(9))
                }
                memoryFree = hardware.memory.available.bytesToMegabytes().megaBytes()

                memoryInUse = (hardware.memory.total - hardware.memory.available).bytesToMegabytes().megaBytes()
                memoryInUsePercentage = (((hardware.memory.total - hardware.memory.available).bytesToMegabytes()
                    .toFloat() / hardware.memory.total.bytesToMegabytes().toFloat()) * 100).roundToInt()
                    .toString() + "%"
                memoryChannel.send(hardware.memory)

                hardware.processor.currentFreq.forEach {
                    cpuClock = it.hertzToGigaHertz().gigaHertz()
                }
                maxCpuClock = hardware.processor.maxFreq.hertzToGigaHertz().gigaHertz()
                delay(1000)

            }


        }
        //    }
        @Preview
        @Composable
        fun Column1() {
            Column {
                //CPU
                Button(
                    onClick = {
                        isCPUClicked = true

                    },
                    content = {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
                            Text(
                                "CPU ($maxCpuClock)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = Fonts().sofiaBold
                            )
                            Text(
                                """$cpuInUse ($cpuClock) in use""",
                                Modifier.padding(start = 4.dp),
                                fontFamily = Fonts().sofiaRegular
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp).size(width = 230.dp, height = 80.dp)
                        .align(Alignment.Start),
                    enabled = isCPUClicked
                )
                // Memory
                Button(
                    onClick = {
                        isMemoryClicked = true
                    },
                    content = {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
                            Text(
                                "Memory",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = Fonts().sofiaBold
                            )
                            Text(
                                """$memoryFree free of ${totalMemory.bytesToMegabytes()} MB
                            |$memoryInUse ($memoryInUsePercentage) in use
                        """.trimMargin(),
                                Modifier.padding(start = 4.dp),
                                fontFamily = Fonts().sofiaRegular
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp).size(width = 230.dp, height = 80.dp),
                    enabled = isMemoryClicked
                )

            }
        }
        @Composable
        fun MemoryGraph(jfxpanel: JFXPanel, container: Container, _size: Size){

            JavaFXPanel(
                panel = jfxpanel,
                root = container
            ) {
                Platform.runLater {
                    val p = Pane()


                    val viz = p.newVizContainer().apply {

                        size = _size

                    }

                    val THRESHOLD = 15
                    CoroutineScope(Main).launch {
                        val valuesForUsedMemory = mutableListOf(0.0)
                        memoryChannel.consumeAsFlow().collectIndexed { index,it ->

                            valuesForUsedMemory.add((it.total - it.available).bytesToGigabytes().toDouble())
                            print((it.total - it.available).bytesToMegabytes()/1000)
                            if(valuesForUsedMemory.size > THRESHOLD) valuesForUsedMemory.removeAt(0)
                            Platform.runLater {

                                viz.chart(valuesForUsedMemory,  ChartConfig.default.apply {
                                    mark {


                                       // strokeWidth = 3.0
                                        fills = listOf("#FFFF00".col)

                                        strokeColors = listOf("#651fff".col)

                                    }

                                   }){
                                    val xC = quantitative ({ this.indexInData.toDouble() }){
                                        formatter = {

                                            if(this!!.roundToInt() < THRESHOLD ) "${this/2.0}" else "${this.plus(index)}"
                                        }
                                    }
                                    val yC = quantitative ({ domain})
                                    line(xC, yC){
                                        strokeWidth = constant(3.0)
                                        y {
                                            curve = MarkCurves.Curved
                                            enableTicks = true

                                            tickCount = (it.total).bytesToGigabytes().toInt() + 1
                                            end = (it.total).bytesToGigabytes().toDouble()

                                            start = 0.0
                                        }
                                    }

                                }
                            }

                        }


                    }
                    val scene = Scene(p,_size.width, _size.height)
                    jfxpanel.scene = scene

                }
            }
        }

        @Preview
        @Composable
        fun Column2() {
            Column(Modifier.padding(start = 16.dp, end = 16.dp)) {

                Box(Modifier.fillMaxWidth().fillMaxHeight()) {
                   Column(Modifier.fillMaxSize()) {
                       Row(Modifier.fillMaxWidth().wrapContentHeight(), horizontalArrangement = Arrangement.SpaceBetween) {
                           Text(
                               "Memory",
                               fontSize = 18.sp,
                               fontFamily = Fonts().sofiaBold
                           )

                           Text(
                               "${totalMemory.bytesToGigabytes().roundToInt()} GB",
                               fontSize = 18.sp,
                               fontFamily = Fonts().sofiaRegular,

                           )

                       }
                       Box(Modifier.height(200.dp).width(450.dp).padding(vertical = 16.dp)) {
                           MemoryGraph(jfxpanel, container, Size(450.0, 200.0))
                       }

                   }



                }
            }
        }


        MaterialTheme {
            Row(Modifier.padding(top = 16.dp, start = 8.dp)) {
                Column1()
                Column2()
            }
        }




    }
}



@Composable
fun JavaFXPanel(
    root: Container,
    panel: JFXPanel,
    onCreate: () -> Unit
) {
    val container = remember { JPanel() }
    val density = LocalDensity.current.density

    Layout(
        content = {

        },
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val location = coordinates.localToWindow(Offset.Zero).round()
            val size = coordinates.size
            container.setBounds(
                (location.x / density).toInt(),
                (location.y / density).toInt(),
                (size.width / density).toInt(),
                (size.height / density).toInt()
            )
            container.validate()
            container.repaint()
        },
        measurePolicy = { _, _ ->
            layout(0, 0) {}
        }
    )

    DisposableEffect(Unit) {
        container.apply {
            layout = BorderLayout(0, 0)
            add(panel)
        }
        root.add(container)
        onCreate.invoke()
        onDispose {
            root.remove(container)
        }
    }
}