plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.yourname.bt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourname.bt"
        minSdk = 21 
        targetSdk = 34
        versionCode = 1
        versionName = "v0.1.0"

        // 强制包含 32 位 (armeabi-v7a) 和 64 位
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // TV 专属 Compose 库
    implementation("androidx.tv:tv-foundation:1.0.0")
    implementation("androidx.tv:tv-material:1.1.0")

    // 网络请求与解析库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // 图片加载库 (Coil)
    implementation("io.coil-kt:coil-compose:2.5.0")
}