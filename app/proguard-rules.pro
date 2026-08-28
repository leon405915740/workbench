# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK tools.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for debugging
# stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the source
# file name.
#-renamesourcefileattribute SourceFile

# Keep all Room entities and DAOs
-keep class com.aigrowth.os.core.database.entity.** { *; }
-keep class com.aigrowth.os.core.database.dao.** { *; }

# Keep all AI Engine classes
-keep class com.aigrowth.os.core.aiengine.** { *; }