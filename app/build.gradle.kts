plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
}

dependencies {
    // 基础 AndroidX 依赖
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // 【修复】引入 lifecycle 库，解决 lifecycleScope 报错
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // 传统 View 核心组件：RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // 网络请求与解析库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // 图片加载库 (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}