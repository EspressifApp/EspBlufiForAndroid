# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/xuxiangjun/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-ignorewarning
-dontshrink

#保持哪些类不被混淆
-keep class com.esp.iot.blufi.communiation.BlufiCommunicator{*;}
-keep class com.esp.iot.blufi.communiation.IBlufiCommunicator{*;}
-keep class com.esp.iot.blufi.communiation.BlufiConfigureParams{*;}
-keep class com.esp.iot.blufi.communiation.BlufiProtocol{*;}
-keep class com.esp.iot.blufi.communiation.response.**{*;}

-keep class blufi.espressif.params.**{*;}
-keep class blufi.espressif.response.**{*;}
-keep class blufi.espressif.BlufiCallback{*;}
-keep class blufi.espressif.BlufiClient{*;}
-keep class blufi.espressif.BlufiUtils{*;}

#保持范型
-keepattributes Signature

