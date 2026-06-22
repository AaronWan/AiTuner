-keep class com.example.aituner.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
