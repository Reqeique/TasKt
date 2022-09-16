//import com.sun.management.OperatingSystemMXBean

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb

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
import io.data2viz.charts.chart.mark.MarkCurves
import io.data2viz.charts.chart.mark.line
import io.data2viz.charts.chart.quantitative
import io.data2viz.charts.config.ChartConfig
import io.data2viz.charts.layout.sizeManager
import io.data2viz.charts.viz.newVizContainer
import io.data2viz.color.col
import io.data2viz.geom.Size
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Pane

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.jetbrains.skiko.currentSystemTheme
import oshi.SystemInfo
import oshi.hardware.GlobalMemory
import oshi.hardware.PhysicalMemory
import util.*
import java.awt.BorderLayout
import java.awt.Container
import java.util.*
import javax.swing.JPanel
import kotlin.math.pow
import kotlin.math.roundToInt
import javafx.scene.text.Text as JFXText


@OptIn(ExperimentalStdlibApi::class, ExperimentalD2V::class)

fun main(args: Array<String>) = application {
    val mainCoroutineScope = CoroutineScope(Main)
    val jfxpanel = remember { JFXPanel() }
    val jfxtext = remember { JFXText() }

    var maxCpuClock by remember { mutableStateOf("") }
    var cpuClock by remember { mutableStateOf("") }
    var cpuName by remember { mutableStateOf("") }

    var isCPUClicked by remember { mutableStateOf(true) }
    var isMemoryClicked by remember { mutableStateOf(false) }
    var totalMemory by remember { mutableStateOf(0L) }
    var physicalMemory by mutableStateOf(mutableListOf<PhysicalMemory>())
    var memoryFree by remember { mutableStateOf("") }
    var memoryInUse by remember { mutableStateOf("") }
    var memoryPageSize by remember { mutableStateOf("")}
    val cpuInUse by remember { mutableStateOf("") }
    var memoryInUsePercentage by remember { mutableStateOf("") }
    Window(onCloseRequest = ::exitApplication) {
        val container = this@Window.window

        /** used to send statstics to the graphs */
        val memoryChannel = Channel<GlobalMemory>()

        mainCoroutineScope.launch {

            val systemInfo = SystemInfo()
            val hardware = systemInfo.hardware




            cpuName = systemInfo.hardware.processor.processorIdentifier.name.toString()
            totalMemory = hardware.memory.total
            physicalMemory = hardware.memory.physicalMemory

            while (true) {

                hardware.processor.currentFreq.toList().map {
                    (it / (10f).pow(9))
                }

                memoryFree = hardware.memory.available.bytesToMegabytes().megaBytes()
                memoryInUse = (hardware.memory.total - hardware.memory.available).bytesToMegabytes().megaBytes()
                memoryPageSize = hardware.memory.pageSize.toString()

                memoryInUsePercentage = (((hardware.memory.total - hardware.memory.available).bytesToMegabytes()
                    .toFloat() / hardware.memory.total.bytesToMegabytes().toFloat()) * 100).roundToInt()
                    .toString() + "%"
                memoryChannel.send(hardware.memory)

                cpuClock = (hardware.processor.systemCpuLoadTicks).map { it }.average().gigaHertz()
                maxCpuClock = hardware.processor.maxFreq.hertzToGigaHertz().gigaHertz()
                delay(1000)

            }



        }

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
        fun MemoryGraph(jfxpanel: JFXPanel, container: Container, _size: Size) {
            val color = MaterialTheme.colors
            println("LOG199 $_size")
            JavaFXPanel(
                panel = jfxpanel,
                root = container
            ) {
                Platform.runLater {

                    val p = Pane()

                    val viz = p.newVizContainer().apply {

                        size = _size.copy(height = _size.height - 32)

                    }
                    p.widthProperty().addListener { obs, oldVal, newVal ->
                        viz.size = _size.copy(width = newVal.toDouble(), height = _size.height - 32)
                    }



                    val THRESHOLD = 15
                    mainCoroutineScope.launch {
                        val valuesForUsedMemory = mutableListOf(0.0)
                        val timedIndex = mutableListOf(0.0)
                        memoryChannel.consumeAsFlow().collectIndexed { index, it ->


                            valuesForUsedMemory.add((it.total - it.available).bytesToGigabytes().toDouble())

                            if (valuesForUsedMemory.size > THRESHOLD) valuesForUsedMemory.removeAt(0)
                            Platform.runLater {

                                viz.chart(valuesForUsedMemory, ChartConfig.default.apply {

                                    mark {


                                        // strokeWidth = 3.0
                                        fills = listOf("#FFFF00".col)


                                        strokeColors = listOf(color.primary.toArgb().col)
                                        fillsHighlight = strokeColors

                                    }
                                    yAxis {

                                        gridLinesColor = "#BBBBBB".col
                                        enableAxisLine = true
                                    }
                                    chart {
                                        backgroundColor = color.surface.toArgb().col

                                    }

                                }) {
                                    val xC = quantitative(
                                        {
                                            indexInData.toDouble()

                                        }

                                    ) {

                                        formatter = {

                                            if (this?.roundToInt()!! + 1 < THRESHOLD - 1) "$this" else "${this + index}"

                                        }
                                    }
                                    val yC = quantitative({ domain })
                                    line(xC, yC) {
                                        strokeWidth = constant(3.0)
                                        y {
                                            curve = MarkCurves.Curved
                                            enableTicks = false

                                            tickCount =
                                                it.physicalMemory.sumOf { it.capacity }.bytesToGigabytes().roundToInt()
                                            end = it.physicalMemory.sumOf { it.capacity }.bytesToGigabytes()

                                            start = 0.0
                                        }
                                        x {
                                            enableTicks = false
                                        }
                                    }

                                }
                            }

                        }


                    }

                    val scene = Scene(p, _size.width, _size.height)

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
                        Row(
                            Modifier.fillMaxWidth().wrapContentHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Memory",
                                fontSize = 18.sp,
                                fontFamily = Fonts().sofiaBold
                            )

                            Text(
                                physicalMemory.sumOf { it.capacity }.bytesToGigabytes().gigaByte(),
                                fontSize = 18.sp,
                                fontFamily = Fonts().sofiaRegular,

                                )

                        }
                        BoxWithConstraints(Modifier.height(200.dp).fillMaxWidth().padding(vertical = 16.dp)) {
                            print("LOG_12 constraint ${this.constraints}, h $this")
                            MemoryGraph(jfxpanel, container, Size(constraints.minWidth.toDouble(), 200.0))
                        }
                        Row(
                            Modifier.fillMaxWidth().fillMaxHeight(),
                        ) {
                            Row(Modifier.fillMaxHeight().weight(2f), horizontalArrangement = Arrangement.Start) {
                                Column(Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)) {
                                    Text("In use", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                    Text("$memoryInUse", fontFamily = Fonts().sofiaRegular, fontSize = 16.sp, modifier =Modifier.padding(bottom = 4.dp))
                                    Text("Page size", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp)
                                    Text("$memoryPageSize", fontFamily = Fonts().sofiaRegular, fontSize = 16.sp,modifier = Modifier.padding(bottom = 4.dp))
                                }
                                Column(Modifier.padding(4.dp)) {
                                    Text("Available", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp)
                                    Text("$memoryFree", fontFamily = Fonts().sofiaRegular, fontSize = 16.sp)
                                }
                            }


                            LazyColumn(Modifier.fillMaxHeight().weight(1f)) {
                                if (physicalMemory.size >= 1) {
                                    val formattedPhysicalMemory = physicalMemory[0].asMap().map {
                                        val key = it.key.capitalize()
                                        val value = when (it.key) {
                                            "bankLabel" -> (it.value as String).capitalize()
                                            "capacity" -> (it.value as Long).bytesToGigabytes().gigaByte()
                                            "clockSpeed" -> ((it.value as Long) / (10.0.pow(6.0))).megaHertz()
                                            "manufacture" -> (it.value as String).capitalize()

                                            else -> it.value
                                        }
                                        key to value
                                    }.toMap()

                                    items(items = formattedPhysicalMemory.map { it.value }) { item ->

                                        Row(
                                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                formattedPhysicalMemory.filterValues { v -> v == item }.keys.first(),
                                                fontFamily = Fonts().sofiaRegular,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "$item",
                                                fontFamily = Fonts().sofiaRegular,
                                                modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp),
                                                fontSize = 16.sp,

                                            )
                                        }

                                    }

                                }
                            }

                        }

                    }


                }

            }
        }


        MaterialTheme() {

            Row(Modifier.padding(top = 16.dp, start = 8.dp).background(MaterialTheme.colors.surface)) {
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
        content =
        {}
        ,
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