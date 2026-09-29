# BiliPai release R8 configuration.
# Keep the configuration intentionally minimal so R8 can shrink, optimize and
# obfuscate all statically reachable app and library code.

# Runtime reflection metadata used by Retrofit/kotlinx.serialization and
# framework callbacks. These preserve metadata only; they do not keep classes.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Optional classes referenced by third-party libraries on specific code paths.
# Consumer rules supplied by each dependency remain authoritative.
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn dev.chrisbanes.haze.**
-dontwarn io.github.alexzhirkevich.cupertino.**
-dontwarn androidx.room.paging.**
-dontwarn androidx.media3.**
-dontwarn coil3.**
-dontwarn com.google.zxing.**
-dontwarn org.fourthline.cling.**
-dontwarn javax.enterprise.context.**
-dontwarn javax.inject.**
-dontwarn org.seamless.**

# WebView JavaScript bridges are discovered by annotation at runtime.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# R8 优化 bug 规避（AGP 9.3.1）：合并/优化后的 HeightInLinesModifierKt 在 ART 上
# 校验失败（VerifyError: Low-half Constant unexpected as arg to if-eqz/if-nez），
# 经 WorkManager Operation$State / GMS cast 初始化链触发启动即崩。
# keep 该类使其退出类合并与相关优化路径；若 R8 升级修复后可移除。
-keep class androidx.compose.foundation.text.HeightInLinesModifierKt { *; }
