# R8 Keep Rules for ShelfTap

# Retain serialization annotations and models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class vn.shelftap.app.data.model.** { *; }

# Retrofit & OkHttp
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Exceptions
