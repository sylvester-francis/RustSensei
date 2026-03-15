# P1 Fix #6: Keep JNI-referenced classes and methods
-keep class com.sylvester.rustsensei.llm.LlamaEngine {
    void onNativeToken(java.lang.String);
    void onNativeComplete();
    void onNativeError(java.lang.String);
}

# Keep Room entities (Room uses reflection for some operations)
-keep class com.sylvester.rustsensei.data.** { *; }

# Keep all native method declarations
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
