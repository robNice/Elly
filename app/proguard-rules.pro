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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# JSch instantiates its algorithms by class name. Of its Bouncy Castle algorithms only Ed25519 is used,
# so the others and the parts of Bouncy Castle they need can be removed.
-keep class !com.jcraft.jsch.bc.**, com.jcraft.jsch.** { *; }
-keep class com.jcraft.jsch.bc.SignatureEd25519, com.jcraft.jsch.bc.SignatureEdDSA, com.jcraft.jsch.bc.KeyPairGenEdDSA { *; }
# Optional JSch features Elly does not use: Windows agents, Kerberos, Unix sockets, external loggers.
-dontwarn com.sun.jna.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.ietf.jgss.**
-dontwarn org.newsclub.net.unix.**
-dontwarn org.slf4j.**