// 顶层（项目级）构建脚本：仅声明插件版本，不在此应用
plugins {
    // 注意：spec 原指定 AGP 8.1.4，但 8.1.x 的 JdkImageTransform(jlink)
    // 与本机仅有的 JDK 21 不兼容（AGP 8.3 起支持 JDK 21，且 8.3 要求的最小
    // Gradle 正是 8.4，与项目一致）。故采用 8.3.2，其余版本(Kotlin/KSP/Room/Gradle)不变。
    id("com.android.application") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    // 使用 KSP 处理 Room 编译器，性能优于 KAPT
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
}
