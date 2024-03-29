# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

#-libraryjars libs/ijkplayer-java-debug.aar
-keep class tv.danmaku.ijk.media.player.** {*;}

-keepclassmembers class * extends android.webkit.WebChromeClient {
   public void openFileChooser(...);
}


#极光推送混淆配置
-dontoptimize
-dontpreverify

-dontwarn cn.jpush.**
-keep class cn.jpush.** { *; }
-keep class * extends cn.jpush.android.helpers.JPushMessageReceiver { *; }

-dontwarn cn.jiguang.**
-keep class cn.jiguang.** { *; }

#百度云消息推送混淆配置
-libraryjars libs/pushservice-7.0.0.27.jar
-dontwarn com.baidu.**
-keep class com.baidu.**{*; }

-keep class com.baidu.**{*; }

#个推消息推送混淆配置
-dontwarn com.igexin.**
-keep class com.igexin.** { *; }

#科大讯飞语音识别
-keep class com.iflytek.**{*;}
-keepattributes Signature

#百度语音识别
-keep class com.baidu.speech.**{ *;}