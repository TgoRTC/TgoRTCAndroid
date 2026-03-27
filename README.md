# TgoRTC SDK

基于 LiveKit 的音视频通话 Kotlin SDK。

[![Build](https://github.com/TgoRTC/TgoRTCAndroid/actions/workflows/build.yml/badge.svg)](https://github.com/TgoRTC/TgoRTCAndroid/actions/workflows/build.yml)
[![](https://jitpack.io/v/TgoRTC/TgoRTCAndroid.svg)](https://jitpack.io/#TgoRTC/TgoRTCAndroid)

## 集成步骤

### 1. 添加 JitPack 仓库

在项目根目录的 `settings.gradle.kts` 中添加：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

如果使用 `settings.gradle`（Groovy）：

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. 添加依赖

在模块的 `build.gradle.kts` 中添加：

最新版本：[![](https://jitpack.io/v/TgoRTC/TgoRTCAndroid.svg)](https://jitpack.io/#TgoRTC/TgoRTCAndroid)

```kotlin
dependencies {
    implementation("com.github.TgoRTC:TgoRTCAndroid:版本看上面")
}
```

Groovy 写法：

```groovy
dependencies {
    implementation 'com.github.TgoRTC:TgoRTCAndroid:版本看上面
}
```

> 将版本号替换为上方徽章显示的最新版本，或在 [JitPack](https://jitpack.io/#TgoRTC/TgoRTCAndroid) 查看所有可用版本。

### 3. 同步项目

点击 Android Studio 中的 **Sync Now** 完成依赖同步。

## 环境要求

- Android SDK: minSdk 24 (Android 7.0+)
- JDK 17
- Kotlin 2.0+
