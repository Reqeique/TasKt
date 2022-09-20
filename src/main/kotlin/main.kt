//import com.sun.management.OperatingSystemMXBean

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
import com.sun.javafx.application.PlatformImpl
import io.data2viz.ExperimentalD2V
import io.data2viz.charts.chart.chart
import io.data2viz.charts.chart.constant
import io.data2viz.charts.chart.discrete
import io.data2viz.charts.chart.mark.MarkCurves
import io.data2viz.charts.chart.mark.area
import io.data2viz.charts.chart.quantitative
import io.data2viz.charts.config.ChartConfig
import io.data2viz.charts.viz.newVizContainer
import io.data2viz.color.col
import io.data2viz.geom.Size
import io.data2viz.math.pct
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color.GRAY
import javafx.scene.text.Font
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.javafx.JavaFx
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.PhysicalMemory
import util.*
import java.awt.BorderLayout
import java.awt.Container
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.pow
import kotlin.math.roundToInt
import javafx.scene.text.Text as JFXText


@OptIn(ExperimentalStdlibApi::class, ExperimentalD2V::class)

fun main(args: Array<String>) = application {
    val mainJob = SupervisorJob()
    val mainCoroutineScope = rememberCoroutineScope { Main }
   // val jfxpanel = remember { JFXPanel() }
    val jfxtext = remember { JFXText() }
    var pageState by remember { mutableStateOf<Pages>(Pages.CPU) }





    val isCPUClicked = mutableStateListOf(true)

    var isMemoryClicked by remember { mutableStateOf(false) }

    /** memory states */
    var memoryFree by remember { mutableStateOf("") }
    var memoryInUse by remember { mutableStateOf("") }
    var memoryPageSize by remember { mutableStateOf("") }
    var totalMemory by remember { mutableStateOf(0L) }
    var physicalMemory by mutableStateOf(mutableListOf<PhysicalMemory>())
    var memoryInUsePercentage by remember { mutableStateOf("") }


    /** cpu states */
    var maxCpuClock by remember { mutableStateOf("") }
    var cpuClock by remember { mutableStateOf("") }
    var cpuName by remember { mutableStateOf("") }
    var processorInformation by remember { mutableStateOf<ProcessorInfo?>(null)}
    var cpuInUse by remember { mutableStateOf("") }
    var cpuInterrupt by remember { mutableStateOf("") }
    var cpuContextSwitchers by remember { mutableStateOf("") }


    Window(onCloseRequest = ::exitApplication) {
        val container = this@Window.window

        /** used to send statstics to the graphs */
        val memoryChannel = Channel<GlobalMemory>()
        val cpuChannel = Channel<CentralProcessor>()

        LaunchedEffect(Unit) {

            //   mainJob.

            val systemInfo = SystemInfo()
            val hardware = systemInfo.hardware




            cpuName = systemInfo.hardware.processor.processorIdentifier.name.toString()
            totalMemory = hardware.memory.total
            physicalMemory = hardware.memory.physicalMemory
            processorInformation = ProcessorInfo(hardware.processor.maxFreq.hertzToGigaHertz().gigaHertz(), hardware.processor.physicalPackageCount.toString(), hardware.processor.physicalProcessorCount.toString(), hardware.processor.logicalProcessorCount.toString())
            print(processorInformation)
            while (true) {

                memoryFree = hardware.memory.calculate().memoryFree().bytesToMegabytes().megaBytes()
                memoryInUse = hardware.memory.calculate().memoryInUse().bytesToMegabytes().megaBytes()
                memoryPageSize = hardware.memory.pageSize.toString()
                memoryInUsePercentage = "${hardware.memory.calculate().memoryInUsePercentage()} %"

                cpuClock = hardware.processor.calculate(SystemInfo()).cpuCurrentFreq().hertzToGigaHertz().gigaHertz()
                cpuInUse = "${((hardware.processor.calculate(SystemInfo()).cpuInUse(SystemInfo().hardware.processor)*100f).roundToInt() )} %"
                cpuInterrupt = hardware.processor.calculate(SystemInfo()).cpuInterrupt().toString()
                cpuContextSwitchers = hardware.processor.calculate(SystemInfo()).cpuContextSwitchers().toString()
                maxCpuClock = hardware.processor.maxFreq.hertzToGigaHertz().gigaHertz()

                launch {
                    cpuChannel.send(hardware.processor)
                    memoryChannel.send(hardware.memory)
                }

                delay(500)

            }


        }

        @Preview
        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun Column1(cpuClickListener: () -> Unit, memoryClickListener: () -> Unit) {
            Column {
                //CPU
                Button(

                    onClick = cpuClickListener,
                    content = {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
                            Text(
                                "CPU ($maxCpuClock) ${isCPUClicked.toList()}",
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
                        .align(Alignment.Start)
                        .background(ButtonDefaults.buttonColors().backgroundColor(isCPUClicked.last()).value),

                    colors = ButtonDefaults.buttonColors()
                )
                // Memory
                Button(
                    onClick = memoryClickListener,
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
                    enabled = true
                )

            }
        }

        @Composable
        fun MemoryGraph(jfxpanel: JFXPanel = JFXPanel(), container: Container, _size: Size) {
            val color = MaterialTheme.colors

            JavaFXPanel(
                panel = jfxpanel,
                root = container, onCreate =
                {

                    mainCoroutineScope.launch {
                        print("invoked")
                        val p = Pane()

                        val viz = p.newVizContainer().apply {

                            size = _size.copy(height = _size.height - 32)

                        }
                        p.widthProperty().addListener { obs, oldVal, newVal ->
                            viz.size = _size.copy(width = newVal.toDouble(), height = _size.height)
                        }
                        p.heightProperty().addListener { obs, oldVal, newVal ->
                            viz.size = _size.copy(width = _size.width, height = newVal.toDouble())

                        }


                        val THRESHOLD = 15

                        launch {


                            val valuesForUsedMemory = mutableListOf(0.0)

                            memoryChannel.consumeAsFlow().collectIndexed { index, it ->


                                valuesForUsedMemory.add((it.total - it.available).bytesToGigabytes().toDouble())

                                if (valuesForUsedMemory.size > THRESHOLD) valuesForUsedMemory.removeAt(0)


                                viz.chart(valuesForUsedMemory, ChartConfig.default.apply {
                                    strokeColor = color.onBackground.toArgb().col
                                    fontColor = color.onBackground.toArgb().col
                                    fontWeight = io.data2viz.viz.FontWeight.BOLD
                                    mark {


                                        // strokeWidth = 3.0
                                        fills = listOf(color.primary.toArgb().col.withAlpha(99.pct))


                                        strokeColors = listOf(color.primary.toArgb().col, color.secondary.toArgb().col)
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
                                    val xC = discrete(
                                        {
                                            indexInData.toDouble()

                                        }

                                    ) {

                                        formatter = {

                                            if (this.roundToInt()!! + 1 < THRESHOLD - 1) "$this" else "${this + index}"

                                        }
                                    }
                                    val yC = quantitative({ domain })
                                    area(xC, yC) {
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


                        }.ensureActive()


                        val scene = Scene(p, _size.width, _size.height)

                        jfxpanel.scene = scene
                    }
                    Platform.setImplicitExit(false)

                })


        }


        @Composable
        fun CPUGraph(jfxpanel: JFXPanel = JFXPanel(), container: Container, _size: Size) {
            val color = MaterialTheme.colors

            JavaFXPanel(
                panel = jfxpanel,
                root = container, onCreate =
                {

                    mainCoroutineScope.launch {
                        print("invoked")
                        val p = Pane()

                        val viz = p.newVizContainer().apply {

                            size = _size.copy(height = _size.height - 32)

                        }
                        p.widthProperty().addListener { obs, oldVal, newVal ->
                            viz.size = _size.copy(width = newVal.toDouble(), height = _size.height)
                        }
                        p.heightProperty().addListener { obs, oldVal, newVal ->
                            viz.size = _size.copy(width = _size.width, height = newVal.toDouble())

                        }


                        val THRESHOLD = 15
                        print("test")
                        launch {


                            val valuesForUsedCPU = mutableListOf(0.0)
                            val timedIndex = mutableListOf(0.0)

                            cpuChannel.consumeAsFlow().collectIndexed { index, it ->

                                //it.tick
                                valuesForUsedCPU.add((it.calculate(SystemInfo()).cpuInUse(SystemInfo().hardware.processor))*100)

                                if (valuesForUsedCPU.size > THRESHOLD) valuesForUsedCPU.removeAt(0)


                                viz.chart(valuesForUsedCPU, ChartConfig.default.apply {
                                    strokeColor = color.onBackground.toArgb().col
                                    fontColor = color.onBackground.toArgb().col
                                    fontWeight = io.data2viz.viz.FontWeight.BOLD
                                    mark {


                                        // strokeWidth = 3.0
                                        fills = listOf(color.primary.toArgb().col.withAlpha(99.pct))


                                        strokeColors = listOf(color.primary.toArgb().col, color.secondary.toArgb().col)
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
                                    val xC = discrete(
                                        {
                                            indexInData.toDouble()

                                        }

                                    ) {

                                        formatter = {

                                            if (this.roundToInt()!! + 1 < THRESHOLD - 1) "$this" else "${this + index}"

                                        }
                                    }
                                    val yC = quantitative({ domain }){
                                        formatter = {
                                            "$this %"
                                        }
                                    }
                                    area(xC, yC) {
                                        strokeWidth = constant(3.0)
                                        y {
                                            curve = MarkCurves.Curved
                                            enableTicks = false
                                            end = 100.0
                                            start = 0.0
                                        }
                                        x {
                                            enableTicks = false
                                        }
                                    }

                                }


                            }


                        }


                        val scene = Scene(p, _size.width, _size.height)

                        jfxpanel.scene = scene
                    }
                    Platform.setImplicitExit(false)


                })


        }

        @Preview
        @Composable
        fun MemoryColumn() {
            Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp)) {
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
                BoxWithConstraints(
                    Modifier.defaultMinSize(minHeight = 200.dp).fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    print("LOG_12 constraint ${this.constraints}, h $this")
                    MemoryGraph(
                     //   jfxpanel,
                        container = container,
                        _size = Size(constraints.minWidth.toDouble(), constraints.minHeight.toDouble())
                    )
                }
                Row(
                    Modifier.fillMaxWidth().wrapContentHeight(),
                ) {
                    Row(Modifier.fillMaxHeight().weight(2f), horizontalArrangement = Arrangement.Start) {
                        Column(Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)) {
                            Text(
                                "In use",
                                fontFamily = Fonts().sofiaRegular,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "$memoryInUse",
                                fontFamily = Fonts().sofiaRegular,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text("Page size", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp)
                            Text(
                                "$memoryPageSize",
                                fontFamily = Fonts().sofiaRegular,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
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

        @Composable
        fun CPUColumn() {
            Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().wrapContentHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "CPU",
                        fontSize = 18.sp,
                        fontFamily = Fonts().sofiaBold
                    )

                    Text(
                        cpuName,
                        fontSize = 18.sp,
                        fontFamily = Fonts().sofiaRegular,

                        )

                }
                BoxWithConstraints(
                    Modifier.defaultMinSize(minHeight = 200.dp).fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    CPUGraph(
                        //jfxpanel,
                        container = container,
                        _size = Size(constraints.minWidth. toDouble (),constraints. minHeight .toDouble()))
                }
                Row(Modifier.fillMaxWidth().wrapContentHeight()){
                    Row(Modifier.fillMaxHeight().weight(2f), horizontalArrangement = Arrangement.Start) {
                        Column(Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)) {
                            Text(
                                "Utilization",
                                fontFamily = Fonts().sofiaRegular,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "$cpuInUse",
                                fontFamily = Fonts().sofiaRegular,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text("Interrupt", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp)
                            Text(
                                "$cpuInterrupt",
                                fontFamily = Fonts().sofiaRegular,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Column(Modifier.padding(4.dp)) {
                            Text("Speed", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp)
                            Text("$cpuClock", fontFamily = Fonts().sofiaRegular, fontSize = 16.sp)
                            Text("Context Switchers", fontFamily = Fonts().sofiaRegular, fontSize = 12.sp)
                            Text("$cpuContextSwitchers", fontFamily = Fonts().sofiaRegular, fontSize = 16.sp)
                        }
                    }
                    LazyColumn(Modifier.fillMaxHeight().weight(1f)) {
                        if (processorInformation != null) {
                            val formattedProcessorInformation = processorInformation!!.asMap().map {
                                val key = it.key.capitalize().replace("_"," ")
                                key to it.value
                            }.toMap()

                            items(items = formattedProcessorInformation.map { it.value }) { item ->

                                Row(
                                    modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        formattedProcessorInformation.filterValues { v -> v == item }.keys.first(),
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

//

        MaterialTheme(lightColors()) {

            Scaffold(Modifier.padding(top = 16.dp, start = 8.dp)) {
                Row() {
                    Column1(cpuClickListener = {
                        pageState = Pages.CPU
                    }, memoryClickListener = {
                        pageState = Pages.Memory
                        print("Log: ")
                    })


                    //   CPUColumn()
                    print("Log: $pageState")
                    when (pageState) {
                        is Pages.CPU -> {

                            CPUColumn()
                        }

                        is Pages.Memory -> MemoryColumn()

                    }
                }

            }
        }


    }
}


@Composable
@Preview
fun JavaFXPanel(
    root: Container,
    panel: JFXPanel,
    onCreate: () -> Unit


) {
    val container = remember { JPanel() }
    val density = LocalDensity.current.density

    Layout(
        content =
        {},
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
        onCreate()
        root.add(container)


        onDispose {

           root.remove(container)
        }
    }






}


sealed class Pages {
    object CPU : Pages()
    object Memory : Pages()
}