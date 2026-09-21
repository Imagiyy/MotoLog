# ProGuard / R8 rules for MotoLog
# ================================

# Keep Room entities, DAOs, and database
-keep class com.abrar.motolog.data.local.entity.** { *; }
-keep class com.abrar.motolog.data.local.dao.** { *; }
-keep class com.abrar.motolog.data.local.MotoLogDatabase { *; }

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep DataStore serializers
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    public static *** Companion;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** $serializer;
}

# MapLibre Native & Compose
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.abrar.motolog.worker.** { *; }

# Remove logging in release builds to ensure no coordinates or sensitive info are logged
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

