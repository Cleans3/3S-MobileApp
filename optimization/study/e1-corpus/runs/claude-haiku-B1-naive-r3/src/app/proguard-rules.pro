# Keep Gson-deserialised DTOs (field names are read via reflection).
-keep class com.example.librarycatalogue.data.BookDto { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
