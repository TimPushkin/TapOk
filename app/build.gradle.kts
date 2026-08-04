import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
}

object Version {
    private const val MAJOR = 1
    private const val MINOR = 0
    private const val PATCH = 0

    const val CODE = MAJOR * 10000 + MINOR * 100 + PATCH
    const val NAME = "$MAJOR.$MINOR.$PATCH"
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "io.github.timpushkin.tapok"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.timpushkin.tapok"
        minSdk = 34 // Scanning (e.g. screenshotting) is more difficult on older APIs
        targetSdk = 37
        versionCode = Version.CODE
        versionName = Version.NAME
    }

    signingConfigs {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            val keystoreProperties = keystorePropertiesFile.inputStream().use {
                Properties().apply { load(it) }
            }
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.lifecycle.runtimeKtx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.google.dagger.hilt)
    ksp(libs.google.dagger.hiltCompiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigationsuite)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.google.material)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    implementation(libs.google.accompanist.drawablepainter)
}
