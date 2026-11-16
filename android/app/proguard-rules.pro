# Release logging is stripped: nothing the app or a library logs survives R8 (tokens and PII are never logged anyway).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
    public static boolean isLoggable(java.lang.String, int) return false;
}

# Retrofit reads annotations and generic signatures reflectively; DTO serializers are kept by the serialization library's own rules.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
