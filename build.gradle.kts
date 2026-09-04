plugins {
    id("com.android.application") version "9.4.0" apply false
    // NOTE: AGP 9 has built-in Kotlin compilation support — do not re-add
    // org.jetbrains.kotlin.android here (see commits 8c7e926 / 0f21b94, which removed it after
    // it broke the build). kotlin.plugin.compose/serialization and KSP are still separate
    // plugins and are declared explicitly.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    // KSP's versioning is independent of the Kotlin version as of KSP 2.3.0 (see
    // https://github.com/google/ksp/releases) — do not use the old <kotlinVersion>-<kspVersion>
    // format. 2.3.11 is confirmed compatible with AGP 9 built-in Kotlin (see 2.3.10 release notes,
    // "Fix R-class resolution in KSP when AGP 9 built-in Kotlin is enabled"). Re-check
    // https://github.com/google/ksp/releases for anything newer before building.
    id("com.google.devtools.ksp") version "2.3.11" apply false
}
