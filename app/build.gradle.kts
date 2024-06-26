import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

val resourcesDir = "$buildDir/resources"
val skikoWasm by configurations.creating

val unzipTask = tasks.register("unzipWasm", Copy::class) {

    destinationDir = file(resourcesDir)
    from(skikoWasm.map { zipTree(it) })
}

dependencies {
    skikoWasm(libs.skiko.wasmJsRuntime)
}

kotlin {

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
        dependsOn(unzipTask)
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {

        moduleName = "app"

        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }

        binaries.executable()
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.ashampoo.kim)
            implementation(libs.skiko)
        }

        val wasmJsMain by getting {

            resources.setSrcDirs(resources.srcDirs)
            resources.srcDirs(unzipTask.map { it.destinationDir })
        }

        wasmJsMain.dependencies {
            implementation(npm("pako", "2.1.0"))
        }
    }
}
