import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
    id("org.openjfx.javafxplugin") version "0.0.9"
}

group = "com.ultraone"
version = "1.0"
val ktor_version = "1.6.5"
val logback_version = "1.2.7"
repositories {
    jcenter()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven{
        url = uri("https://maven.pkg.jetbrains.space/data2viz/p/maven/dev")
    }
    maven{
        url = uri("https://maven.pkg.jetbrains.space/data2viz/p/maven/public")
    }

}
val d2vVersion="0.8.12"
val chartsVersion="1.1.0-eap1"
dependencies {
    // https://mvnrepository.com/artifact/io.data2viz/d2v-core-jvm
//    implementation("io.data2viz:d2v-core-jvm:0.8.0-RC5")
//    // https://mvnrepository.com/artifact/io.data2viz.charts/core-jfx
//    implementation("io.data2viz.charts:core-jfx:1.1.0-eap1")
//// https://mvnrepository.com/artifact/io.data2viz.charts/core-jvm
//    implementation("io.data2viz.charts:core-jvm:1.0.7-RC3")
    // https://mvnrepository.com/artifact/org.openjfx/javafx-swing
    implementation("org.openjfx:javafx-swing:11-ea+24")

    implementation ("io.data2viz.d2v:core-jvm:$d2vVersion")
    implementation ("io.data2viz.charts:core:$chartsVersion")
//    implementation("io.data2viz.d2v:d2v-core-jvm:0.10.0")
    implementation ("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation(compose.desktop.currentOs)
    implementation( "com.github.oshi", name= "oshi-core", version= "5.8.3")
    //implementation ("org.hyperic:sigar:1.6.5.132-6")

}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "15"
}
javafx {
    version = "15.0.1"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.swing", "javafx.base")
}

compose.desktop {
    application {
        javaHome = System.getProperty("java.home")
        mainClass = "MainKt"
        nativeDistributions {

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ProjectProfiler"
            packageVersion = "1.0.0"
            windows {
                this.shortcut = true
                console = true
                msiPackageVersion = "1.0.0"
                exePackageVersion = "1.0.0"
            }
           // this@application.appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }
    }
}