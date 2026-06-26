# Keep Drive REST API model fields for Gson reflection
-keep class com.aura.app.data.drive.model.** { *; }
-keepattributes Signature, InnerClasses, *Annotation*

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Exceptions

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
