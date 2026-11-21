# kotlinx.serialization: keep generated serializers of the API DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class vn.shelftap.app.** {
    *** Companion;
}
-keepclasseswithmembers class vn.shelftap.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
