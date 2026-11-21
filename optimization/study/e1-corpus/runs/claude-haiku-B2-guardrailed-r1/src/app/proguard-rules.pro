# kotlinx.serialization: keep generated serializers and companion lookups for API DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class vn.shelftap.app.**$$serializer { *; }
-keepclassmembers class vn.shelftap.app.** {
    *** Companion;
}
-keepclasseswithmembers class vn.shelftap.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
