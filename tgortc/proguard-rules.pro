# TgoRTC SDK ProGuard Rules

# 保留所有公开 API
-keep public class com.tgo.rtc.** { public *; }
-keep public interface com.tgo.rtc.** { *; }
-keep public enum com.tgo.rtc.** { *; }

# 保留 Kotlin 相关
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留 TgoRTC 单例
-keep class com.tgo.rtc.TgoRTC {
    public static *** getInstance();
    public static *** instance$*;
    public *;
}

# 保留所有实体类
-keep class com.tgo.rtc.entity.** { *; }

# 保留所有管理器
-keep class com.tgo.rtc.manager.** { *; }

# 保留参与者类
-keep class com.tgo.rtc.participant.** { *; }

# 保留工具类
-keep class com.tgo.rtc.utils.** { *; }
