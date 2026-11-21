# Proguard rules for ShelfTap
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class vn.shelftap.app.data.model.** { *; }
