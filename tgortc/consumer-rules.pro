# TgoRTC SDK - 保留所有公开 API
-keep class com.tgo.rtc.** { *; }
-keepclassmembers class com.tgo.rtc.** { *; }

# 保留 Kotlin 元数据
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# 保留 TgoRTC 单例
-keep class com.tgo.rtc.TgoRTC {
    public static ** getInstance();
    public static ** instance;
    *;
}

# 保留所有实体类
-keep class com.tgo.rtc.entity.** { *; }

# 保留所有管理器
-keep class com.tgo.rtc.manager.** { *; }

# 保留参与者类
-keep class com.tgo.rtc.participant.** { *; }
