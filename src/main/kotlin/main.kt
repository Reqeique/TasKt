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
import io.data2viz.charts.chart.discrete
import io.data2viz.charts.chart.mark.line
import io.data2viz.charts.chart.mark.plot
import io.data2viz.charts.chart.quantitative
import io.data2viz.charts.config.ChartConfig
import io.data2viz.charts.config.configs.greenConfig
import io.data2viz.charts.config.configs.lightConfig
import io.data2viz.charts.configuration.ChartConfiguration
import io.data2viz.charts.dimension.Dimension
import io.data2viz.color.Colors
import io.data2viz.color.col
import io.data2viz.geom.Size
import io.data2viz.math.pct
import io.data2viz.viz.newVizContainer
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.chart.XYChart
import javafx.scene.layout.Pane
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import oshi.SystemInfo
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
        var totalMemory by remember { mutableStateOf("") }
        var memoryFree by remember { mutableStateOf("") }
        var memoryInUse by remember { mutableStateOf("") }
        val cpuInUse by remember { mutableStateOf("") }
        var memoryInUsePercentage by remember { mutableStateOf("") }
//    val osBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(
//        OperatingSystemMXBean::class.java
//    )
//
        CoroutineScope(Dispatchers.Main).launch {

            val systemInfo = SystemInfo()
            val hardware = systemInfo.hardware

            cpuName = systemInfo.hardware.processor.processorIdentifier.name.toString()
            totalMemory = hardware.memory.total.toString()

            while (true) {
                hardware.processor.currentFreq.toList().map {
                    (it / (10f).pow(9))
                }.forEach {

                }
                memoryFree = hardware.memory.available.bytesToMegabytes().megaBytes()
                memoryInUse = (hardware.memory.total - hardware.memory.available).bytesToMegabytes().megaBytes()
                memoryInUsePercentage = (((hardware.memory.total - hardware.memory.available).bytesToMegabytes()
                    .toFloat() / hardware.memory.total.bytesToMegabytes().toFloat()) * 100).roundToInt()
                    .toString() + "%"


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
                                """$memoryFree free of ${totalMemory.toLongOrNull()?.bytesToMegabytes()} MB
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
        fun Column2() {
            Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
                Box(Modifier.fillMaxWidth().fillMaxHeight()) {

                    val height = 400.0
                    val width = 450.0

                    JavaFXPanel(
                        panel = jfxpanel,
                        root = container
                    ) {
                        Platform.runLater {

                            val values = mutableListOf(0.0)
                            val p = Pane()
                            var x = 1.0
                            val viz = p.newVizContainer().apply {

                                size = Size(width, height)

                            }


                            CoroutineScope(Main).launch {
                                val systemInfo = SystemInfo()
                                val hardware = systemInfo.hardware
                                val y = mutableListOf(0.0)
                                var _y = 0.0
                                while (true) {
                                    delay(500L)
//                                    hardware.processor.currentFreq.toList().map {
//                                        (it / (10f).pow(9))
//                                    }.forEach {
                                    _y+=0.500
                                    y.add(0.500 + _y)
                                    val df = DecimalFormat("#.##")
                                    df.roundingMode = RoundingMode.CEILING
                                    val mem = (((hardware.memory.total - hardware.memory.available).bytesToMegabytes()
                                        .toFloat() / 1000)) //  (hardware.memory.total.bytesToMegabytes().toFloat()) * 100).roundToInt())
                                    if (values.size > 15) {
                                        values.removeAt(0)
                                        y.removeAt(0)
                                    }

                                    values.add(df.format(mem).toDouble())


                                    Platform.runLater {


                                        viz.chart(values, ChartConfig.light.apply {
                                            mark {
                                                markersSize = 30.0

                                                strokeWidth = 3.0

                                                strokeColors = listOf("#651fff".col)
//                                                fills = listOf(0x4A8F6E.col.withAlpha(80.pct))
//
//                                                strokeColorsHighlight = strokeColors.toList()
//                                                fillsHighlight = fills.toList()
//                                                strokeWidthHighlight = 4.0
//
//                                                strokeColorsSelect = strokeColors.toList()
//                                                fillsSelect = fills.toList()
//                                                strokeWidthSelect = 4.0
                                            }

                                        }) {

                                            val __values = quantitative({ indexInData.toDouble() }){
                                                formatter = {

                                                    "${this?.div(2)} sec"
                                                }
                                            }
                                            val _values = quantitative({ domain }) {

                                            }
                                            line(__values, _values) {

                                                y {
                                                    start = 0.000
                                                    end = 10.00
                                                    strokeColor = "#651fff".col

                                                }
                                                x {
                                                    strokeColor = "#651fff".col
                                                }

                                            }
                                        }
                                    }
//                                    }
                                }
                            }
                            val scene = Scene(p, width, height)
                            jfxpanel.scene = scene

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
public fun JavaFXPanel(
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