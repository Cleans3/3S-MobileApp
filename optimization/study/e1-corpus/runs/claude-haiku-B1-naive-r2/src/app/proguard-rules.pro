# Keep Gson-deserialised model classes (field names are read via reflection).
-keep class com.example.libcatalog.data.Book { *; }
-keepattributes Signature, *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
