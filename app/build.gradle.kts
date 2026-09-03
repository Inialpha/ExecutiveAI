plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
    }
}

dependencies {
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
}
