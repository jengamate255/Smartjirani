-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
# slf4j-api's optional runtime binding (pulled in via LiveKit/WebRTC deps); never present on Android
-dontwarn org.slf4j.impl.StaticLoggerBinder
