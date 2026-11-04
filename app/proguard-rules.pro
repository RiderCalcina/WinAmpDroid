# ProGuard & R8 Rules for WinAmpDroid

# 1. Preserve JavaScript Interface methods (vital for WebGL Milkdrop Bridge)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 2. Preserve Media3 / ExoPlayer reflection and internal classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# 3. Preserve Gson serialization models (Track, Equalizer presets, WindowStates, DownloadedSkin)
-keepclassmembers class com.winampdroid.core.model.** { *; }
-keepclassmembers class com.winampdroid.core.storage.** { *; }
-keepclassmembers class com.winampdroid.core.audio.WinampEqualizerPreset { *; }
-keepclassmembers class com.winampdroid.core.storage.DownloadedSkin { *; }
-keepclassmembers class com.winampdroid.core.storage.WindowStates { *; }

# 4. Preserve Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 5. Coil Image Loader for Skin Museum
-keep class coil.** { *; }
-dontwarn coil.**

