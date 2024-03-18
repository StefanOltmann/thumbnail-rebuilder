import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

val skikoVersion = "0.7.89.1"

val resourcesDir = "$buildDir/resources"
val skikoWasm by configurations.creating

val unzipTask = tasks.register("unzipWasm", Copy::class) {

    destinationDir = file(resourcesDir)
    from(skikoWasm.map { zipTree(it) })
}

dependencies {
    skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:$skikoVersion")
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

        /* Use Binaryen optimization to make it smaller & faster */
        applyBinaryen()
    }

    sourceSets {

        commonMain.dependencies {
            api("com.ashampoo:kim:0.16")
            api("org.jetbrains.skiko:skiko:$skikoVersion")
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
