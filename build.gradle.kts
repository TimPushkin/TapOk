plugins {
    alias(libs.plugins.android.application) apply false

    // Must be declared in the same scope as AGP to fix https://github.com/google/dagger/issues/3068
    alias(libs.plugins.hilt.android) apply false
    // Must be declared in the same scope as Hilt
    alias(libs.plugins.google.ksp) apply false
}

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}
