# Retrofit / Gson DTOs must keep their field names.
-keep class com.example.libcatalogue.data.remote.** { *; }
-keepattributes Signature, *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
