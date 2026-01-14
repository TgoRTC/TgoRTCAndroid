# TgoRTC SDK 开发指南

## 项目概述

TgoRTC 是一个基于 [LiveKit](https://livekit.io/) 的 Android 实时音视频通信 SDK，使用 Kotlin 开发。该 SDK 提供了简洁易用的 API，用于构建音视频通话应用。

### 技术栈

- **语言**: Kotlin
- **最低 SDK 版本**: Android API 24
- **底层 RTC**: LiveKit Android SDK 2.23.1
- **构建工具**: Gradle (Kotlin DSL)

### 项目结构

```
TgoRTCSDK/
├── app/                          # 示例应用
├── tgortc/                       # SDK 核心库
│   └── src/main/java/com/tgo/rtc/
│       ├── TgoRTC.kt             # SDK 入口单例
│       ├── entity/               # 数据实体类
│       │   ├── TgoAudioDevice.kt
│       │   ├── TgoCameraPosition.kt
│       │   ├── TgoConnectionQuality.kt
│       │   ├── TgoConnectStatus.kt
│       │   ├── TgoOptions.kt
│       │   ├── TgoRoomInfo.kt
│       │   ├── TgoRTCType.kt
│       │   └── TgoVideoInfo.kt
│       ├── manager/              # 管理器类
│       │   ├── TgoAudioManager.kt
│       │   ├── TgoParticipantManager.kt
│       │   └── TgoRoomManager.kt
│       ├── participant/          # 参与者相关
│       │   └── TgoParticipant.kt
│       ├── track/                # 轨道渲染
│       │   └── TgoVideoRenderer.kt
│       └── utils/                # 工具类
│           └── TgoLogger.kt
└── repo/                         # 本地 Maven 仓库
```

---

## 架构设计

### 核心设计原则

1. **单例模式**: 核心管理器使用单例模式，确保全局唯一实例
2. **事件驱动**: 使用监听器模式处理异步事件
3. **协程支持**: 使用 Kotlin 协程处理异步操作
4. **封装 LiveKit**: 对 LiveKit SDK 进行封装，提供更简洁的 API

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                        TgoRTC (入口)                         │
│  ┌──────────────────┬────────────────┬─────────────────┐   │
│  │  TgoRoomManager  │ TgoParticipant │ TgoAudioManager │   │
│  │                  │    Manager     │                 │   │
│  └────────┬─────────┴───────┬────────┴────────┬────────┘   │
│           │                 │                 │             │
│           ▼                 ▼                 ▼             │
│       LiveKit Room    TgoParticipant    AudioSwitch        │
│                                            Handler          │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心组件说明

### 1. TgoRTC (入口类)

SDK 的主入口，提供全局访问点。

```kotlin
// 初始化 SDK
TgoRTC.instance.init(context, TgoOptions().apply {
    debug = true
    mirror = false
})

// 访问各管理器
TgoRTC.instance.roomManager       // 房间管理
TgoRTC.instance.participantManager // 参与者管理
TgoRTC.instance.audioManager      // 音频管理
```

### 2. TgoRoomManager (房间管理器)

管理房间连接和事件。

**主要功能**:
- 加入/离开房间
- 连接状态监听
- 视频信息监听

**关键方法**:
```kotlin
suspend fun joinRoom(roomInfo: TgoRoomInfo, micEnabled: Boolean, cameraEnabled: Boolean)
fun leaveRoom()
fun addConnectListener(listener: (String, TgoConnectStatus, String) -> Unit)
fun addVideoInfoListener(listener: (TgoVideoInfo) -> Unit)
```

### 3. TgoParticipantManager (参与者管理器)

管理本地和远程参与者。

**主要功能**:
- 获取本地/远程参与者
- 邀请新参与者
- 参与者加入/离开事件

**关键方法**:
```kotlin
fun getLocalParticipant(): TgoParticipant
fun getRemoteParticipants(includeTimeout: Boolean = false): List<TgoParticipant>
fun getAllParticipants(includeTimeout: Boolean = false): List<TgoParticipant>
fun inviteParticipant(uids: List<String>)
fun addNewParticipantListener(listener: (TgoParticipant) -> Unit)
```

### 4. TgoAudioManager (音频管理器)

管理音频输出设备。

**主要功能**:
- 获取/切换音频设备
- 扬声器控制
- 设备变化监听

**关键方法**:
```kotlin
fun getAudioOutputDevices(): List<TgoAudioDevice>
fun getSelectedDevice(): TgoAudioDevice?
fun selectDevice(device: TgoAudioDevice)
fun selectDeviceByType(type: TgoAudioDeviceType)
fun setSpeakerphoneOn(on: Boolean)
fun toggleSpeakerphone()
fun addDeviceChangeListener(listener: DeviceChangeListener)
```

### 5. TgoParticipant (参与者)

代表房间中的一个参与者（本地或远程）。

**主要功能**:
- 音视频轨道控制
- 状态监听（麦克风、摄像头、说话状态等）
- 连接质量监控

**关键方法**:
```kotlin
// 媒体控制 (仅本地参与者)
suspend fun setCameraEnabled(enabled: Boolean)
suspend fun setMicrophoneEnabled(enabled: Boolean)
suspend fun setScreenShareEnabled(enabled: Boolean)
fun switchCamera()

// 状态查询
fun getCameraEnabled(): Boolean
fun getMicrophoneEnabled(): Boolean
fun getIsSpeaking(): Boolean
fun getAudioLevel(): Float
fun getCameraPosition(): TgoCameraPosition?

// 视频轨道
fun getVideoTrack(source: Track.Source = Track.Source.CAMERA): VideoTrack?

// 事件监听
fun addMicrophoneStatusListener(listener: (Boolean) -> Unit)
fun addCameraStatusListener(listener: (Boolean) -> Unit)
fun addSpeakingListener(listener: SpeakingListener)
fun addJoinedListener(listener: () -> Unit)
fun addLeaveListener(listener: () -> Unit)
fun addTimeoutListener(listener: () -> Unit)
```

### 6. TgoVideoRenderer (视频渲染器)

用于渲染视频轨道的自定义 View。

**使用方式**:
```kotlin
// 初始化
renderer.init()

// 设置参与者
renderer.setParticipant(participant)

// 或直接设置视频轨道
renderer.setVideoTrack(videoTrack)

// 配置
renderer.setRendererType(TgoRendererType.TEXTURE_VIEW)
renderer.setScaleType(TgoVideoScaleType.FILL)
renderer.setMirror(true)
```

---

## 数据实体

### TgoRoomInfo (房间信息)

```kotlin
data class TgoRoomInfo(
    var roomName: String,      // 房间名称
    var token: String,         // 访问令牌
    var url: String,           // LiveKit 服务器 URL
    var loginUID: String,      // 当前登录用户 UID
    var creatorUID: String     // 房间创建者 UID
) {
    var maxParticipants: Int = 2           // 最大参与者数
    var rtcType: TgoRTCType = TgoRTCType.AUDIO  // 通话类型
    var isP2P: Boolean = true              // 是否为点对点
    var uidList: MutableList<String>       // 参与者 UID 列表
    var timeout: Int = 30                  // 加入超时时间（秒）
}
```

### TgoOptions (SDK 配置)

```kotlin
class TgoOptions {
    var mirror: Boolean = false  // 是否镜像本地视频
    var debug: Boolean = true    // 是否开启调试日志
}
```

### TgoConnectStatus (连接状态)

```kotlin
enum class TgoConnectStatus {
    CONNECTING,    // 连接中
    CONNECTED,     // 已连接
    DISCONNECTED   // 已断开
}
```

### TgoAudioDeviceType (音频设备类型)

```kotlin
enum class TgoAudioDeviceType {
    EARPIECE,       // 听筒
    SPEAKER,        // 扬声器
    WIRED_HEADSET,  // 有线耳机
    BLUETOOTH,      // 蓝牙耳机
    UNKNOWN         // 未知
}
```

---

## 开发指南

### 基本使用流程

```kotlin
// 1. 初始化 SDK
TgoRTC.instance.init(applicationContext, TgoOptions().apply {
    debug = true
})

// 2. 准备房间信息
val roomInfo = TgoRoomInfo(
    roomName = "test-room",
    token = "your-token",
    url = "wss://your-livekit-server",
    loginUID = "user1",
    creatorUID = "user1"
).apply {
    uidList = mutableListOf("user1", "user2")
    rtcType = TgoRTCType.VIDEO
}

// 3. 添加连接状态监听
TgoRTC.instance.roomManager.addConnectListener { roomName, status, reason ->
    when (status) {
        TgoConnectStatus.CONNECTING -> { /* 连接中 */ }
        TgoConnectStatus.CONNECTED -> { /* 已连接 */ }
        TgoConnectStatus.DISCONNECTED -> { /* 已断开 */ }
    }
}

// 4. 加入房间
lifecycleScope.launch {
    TgoRTC.instance.roomManager.joinRoom(
        roomInfo = roomInfo,
        micEnabled = true,
        cameraEnabled = true
    )
}

// 5. 获取参与者并渲染视频
val localParticipant = TgoRTC.instance.participantManager.getLocalParticipant()
localVideoRenderer.init()
localVideoRenderer.setParticipant(localParticipant)

// 6. 监听远程参与者加入
TgoRTC.instance.participantManager.addNewParticipantListener { participant ->
    remoteVideoRenderer.setParticipant(participant)
}

// 7. 离开房间
TgoRTC.instance.roomManager.leaveRoom()
```

### 音频设备管理

```kotlin
// 监听设备变化
TgoRTC.instance.audioManager.addDeviceChangeListener { devices, selectedDevice ->
    // 更新 UI
}

// 切换扬声器
TgoRTC.instance.audioManager.toggleSpeakerphone()

// 选择蓝牙设备
TgoRTC.instance.audioManager.selectDeviceByType(TgoAudioDeviceType.BLUETOOTH)
```

### 参与者事件监听

```kotlin
participant.addSpeakingListener { isSpeaking, audioLevel ->
    // 更新说话状态 UI
}

participant.addCameraStatusListener { enabled ->
    // 更新摄像头状态
}

participant.addMicrophoneStatusListener { enabled ->
    // 更新麦克风状态
}
```

---

## 代码规范

### 命名规范

- **类名**: 使用 `Tgo` 前缀，如 `TgoRTC`, `TgoRoomManager`
- **实体类**: 放在 `entity` 包下
- **管理器**: 放在 `manager` 包下，使用单例模式
- **监听器类型别名**: 使用 `typealias`，如 `DeviceChangeListener`

### 单例模式

```kotlin
class TgoXxxManager private constructor() {
    companion object {
        val instance: TgoXxxManager by lazy { TgoXxxManager() }
    }
}
```

### 监听器模式

```kotlin
private val xxxListeners = mutableListOf<(T) -> Unit>()

fun addXxxListener(listener: (T) -> Unit) {
    xxxListeners.add(listener)
}

fun removeXxxListener(listener: (T) -> Unit) {
    xxxListeners.remove(listener)
}

private fun notifyXxx(value: T) {
    // 使用 toList() 避免 ConcurrentModificationException
    xxxListeners.toList().forEach { it(value) }
}
```

### 日志规范

使用 `TgoLogger` 进行日志输出：

```kotlin
TgoLogger.debug("调试信息")
TgoLogger.info("[模块] 操作描述")
TgoLogger.warn("警告信息")
TgoLogger.error("错误信息", throwable)
```

---

## 发布

### 本地发布

```bash
./publish_local.sh
```

发布后的文件位于 `repo/com/tgo/rtc/tgortc/` 目录。

### 依赖配置

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("path/to/repo") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.tgo.rtc:tgortc:1.0.0")
}
```

---

## 注意事项

1. **初始化顺序**: 必须先调用 `TgoRTC.instance.init()` 才能使用其他功能
2. **协程作用域**: 部分方法需要在协程中调用（如 `joinRoom`, `setCameraEnabled`）
3. **资源释放**: 离开房间后会自动清理资源，视频渲染器需手动调用 `release()`
4. **权限**: 需要在应用中请求相机、麦克风、蓝牙等权限
5. **超时处理**: 参与者加入超时会触发 `timeoutListener`，可通过 `inviteParticipant` 重新邀请
