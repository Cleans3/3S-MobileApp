# kotlinx.serialization: keep generated serializers for the REST models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.example.libraryapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.libraryapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit generic signatures for the API interface.
-keepattributes Signature
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
