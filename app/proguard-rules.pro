# Keep Gson-serialized model fields, since R8 renaming/removing them breaks reflection-based
# (de)serialization silently rather than failing to compile.
-keep class com.aura.app.data.drive.model.** { *; }
-keep class com.aura.app.data.upload.UploadHistoryEntry { *; }
-keepattributes Signature, InnerClasses, *Annotation*

# Gson's own recommended R8 rule: anonymous TypeToken<List<...>>() {} subclasses (used in
# UploadHistoryStore) lose their generic signature under R8 without this, crashing at runtime
# with "TypeToken must be created with a type argument".
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Exceptions

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
