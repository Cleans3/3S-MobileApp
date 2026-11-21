# Proguard rules for Library Account app
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.library.account.data.model.** { *; }
