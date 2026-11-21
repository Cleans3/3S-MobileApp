# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# Retrofit and OkHttp
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep models
-keepclassmembers class vn.shelftap.app.data.api.** { *; }
-keepclassmembers class vn.shelftap.app.data.local.** { *; }
-keepclassmembers class vn.shelftap.app.data.mediator.** { *; }
-keepclassmembers class vn.shelftap.app.domain.model.** { *; }
