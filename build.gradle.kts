plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    // KSP version must match the Kotlin plugin version above (format: <kotlinVersion>-<kspVersion>).
    // Verify the exact current patch against https://github.com/google/ksp/releases before building.
    id("com.google.devtools.ksp") version "2.3.21-2.0.4" apply false
}
