# Keep REST models so Gson can map JSON to fields after R8 renaming.
-keepattributes Signature, *Annotations*
-keep class com.example.library.data.** { *; }
