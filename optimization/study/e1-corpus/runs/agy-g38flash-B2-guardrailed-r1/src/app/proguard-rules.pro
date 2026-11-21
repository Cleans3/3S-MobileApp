# Keep data models used by Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @org.jetbrains.annotations.Nullable *;
    @kotlinx.serialization.Serializable *;
}

-keepclassmembers class **$$serializer {
    *;
}

-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

-keep class vn.shelftap.app.data.models.** { *; }

# Keep Android Keystore classes
-keep class android.security.keystore.** { *; }
