plugins {
    id("com.android.application")
    // Kotlin compilation comes from AGP 9's built-in support — no separate kotlin.android plugin.
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.inialpha.executiveai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.inialpha.executiveai"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Consumed by AndroidManifest placeholder + AuthorizationClient scope wiring.
        // The value itself (public OAuth client id) lives in res/values/strings.xml.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
            // Fixed, committed debug keystore (keystore/debug.keystore) so the debug signing
            // certificate's SHA-1 is identical across every machine and every CI run — Android
            // Studio's per-machine ~/.android/debug.keystore is NOT used for this project. This
            // is what makes the Android OAuth client's registered SHA-1 (see docs/SETUP.md)
            // actually match the APK you're running, both locally and in CI. Without this, Google
            // Identity's AuthorizationClient fails with Status=UNREGISTERED_ON_API_CONSOLE because
            // no two builds are signed with the same key.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // NOTE: no explicit `kotlinOptions { jvmTarget = ... }` block here — that DSL is provided by
    // the separate org.jetbrains.kotlin.android plugin, which this project deliberately does not
    // apply (AGP 9 built-in Kotlin support instead; see build.gradle.kts). AGP 9's built-in Kotlin
    // compilation derives its JVM target from `compileOptions` above. If a real Gradle sync shows
    // this isn't sufficient, the current AGP 9 equivalent (e.g. a top-level `kotlin { jvmToolchain(17) }`
    // block) should be added instead of reintroducing the kotlin.android plugin.
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // --- Compose / Material 3 ---
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Core / lifecycle ---
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- Serialization / networking (AI backend + Gmail/Calendar REST) ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Room (local persistence) ---
    // Pinned to 2.8.4+ deliberately: earlier Room/KSP2 combinations hit a known KSP bug
    // ("unexpected jvm signature V") on suspend DAO functions returning Unit — fixed in Room
    // 2.7.0-alpha11+ (see https://github.com/google/ksp/issues/2957). Do not downgrade below 2.7.
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // --- Google Sign-In (Credential Manager) + Authorization (Gmail/Calendar scopes) ---
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // --- Background work / scheduling foundation ---
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
