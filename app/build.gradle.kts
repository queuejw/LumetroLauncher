import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
    }
}

android {

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    namespace = "ru.queuejw.lumetro"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "ru.queuejw.lumetro"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "Sunflower 1 Preview 1"

    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.collection)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.material)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.skydoves.colorpickerview)
    implementation(libs.coil)

    debugImplementation(libs.leakcanary.android)
}