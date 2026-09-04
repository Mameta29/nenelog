import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared.data)
    implementation(projects.shared.domain)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
    add("debugImplementation", libs.vosk.android)
}

android {
    namespace = "app.nenelog.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // docs/15: 2026-08-15 Nenelog 確定に伴い app.<name>.<name> 形式で確定
        applicationId = "app.nenelog.nenelog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    sourceSets {
        getByName("debug") {
            providers.gradleProperty("nenelogVoskAssetsDir").orNull?.let(assets::srcDir)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
