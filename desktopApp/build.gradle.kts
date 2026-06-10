plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.richardyap.novelreader.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
            )
            packageName = "NovelReader"
            packageVersion = "1.0.0"
            windows {
                menuGroup = "NovelReader"
                upgradeUuid = "b5a1d2e3-f4a5-6789-abcd-ef0123456789"
            }
        }
    }
}
