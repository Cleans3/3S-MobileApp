# Keep kotlinx.serialization generated serializers for the API DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.librarycatalogue.data.**$$serializer { *; }
-keepclassmembers class com.example.librarycatalogue.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.librarycatalogue.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
