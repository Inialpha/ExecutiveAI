plugins {
    id("com.android.application") version "9.4.0" apply false
    // NOTE: AGP 9 has built-in Kotlin compilation support — do not re-add
    // org.jetbrains.kotlin.android here (see commits 8c7e926 / 0f21b94, which removed it after
    // it broke the build). kotlin.plugin.compose/serialization and KSP are still separate
    // plugins and are declared explicitly.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    // KSP version must match the Kotlin plugin version above (format: <kotlinVersion>-<kspVersion>).
    // Verify the exact current patch against https://github.com/google/ksp/releases before building.
    id("com.google.devtools.ksp") version "2.3.21-2.0.4" apply false
}
