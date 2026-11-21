# kotlinx.serialization: keep generated serializers for the API models
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class vn.shelftap.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class vn.shelftap.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class vn.shelftap.app.data.**$$serializer { *; }
