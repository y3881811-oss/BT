// 根目录的构建文件，在这里配置全局插件版本
plugins {
    id("com.android.application") version "8.2.2" apply false
    // 升级到 Kotlin 2.0.21
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}