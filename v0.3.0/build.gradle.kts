plugins {
    // AGP 8.7.0 is compatible with Gradle 9.x — 8.2.0 is not
    id("com.android.application")                   version "8.7.0"  apply false
    id("org.jetbrains.kotlin.android")              version "2.0.0"  apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"  apply false
    id("org.jetbrains.kotlin.plugin.compose")       version "2.0.0"  apply false
}
