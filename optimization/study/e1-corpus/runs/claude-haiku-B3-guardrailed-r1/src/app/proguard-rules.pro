# kotlinx.serialization: keep serializers of the @Serializable API models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class vn.shelftap.app.** {
    *** Companion;
}
-keepclasseswithmembers class vn.shelftap.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
