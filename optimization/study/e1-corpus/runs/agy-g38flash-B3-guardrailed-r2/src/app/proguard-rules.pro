# Proguard configuration for ShelfTap

# Kotlinx Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    companion object;
}

# Preserve app data models
-keep class vn.shelftap.app.data.model.** { *; }

# Retrofit interfaces and methods
-keep interface vn.shelftap.app.data.api.ShelfTapApi { *; }
-keepattributes Signature
-keepattributes Exceptions

# Android Keystore and security
-keepclassmembers class * implements java.security.Key { *; }
