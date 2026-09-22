# R8 / Play DEX optimization (obfuscation, shrinking, optimization).
# Play Console requires >= 25% on each metric from Feb 2027 for apps with DEX > 10 MB.
# Do not add -dontobfuscate / -dontoptimize / -dontshrink.
# Do not -keep entire Firebase / Play Services: that tanks the obfuscation score.

-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson: keep DTO classes (names may change). Field names must NOT be renamed —
# otherwise JSON keys like "main"/"temp" do not bind and UI NPEs.
-keep,allowobfuscation class com.taxi.easy.ua.** {
    <init>();
}
-keepclassmembers class com.taxi.easy.ua.** {
    <fields>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retrofit (R8 full mode — from retrofit2.pro, plus Call keep).
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# EventBus 3
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# WorkManager: workers are constructed reflectively.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Reflection-heavy SDKs (small vs Play Services — keep is acceptable).
-keep class im.crisp.** { *; }
-dontwarn im.crisp.**
-keep class com.uxcam.** { *; }
-dontwarn com.uxcam.**
-keep class com.pusher.** { *; }
-dontwarn com.pusher.**
-keep class io.github.centrifugal.** { *; }
-dontwarn io.github.centrifugal.**
-keep class com.redmadrobot.inputmask.** { *; }

# Picasso 2.x still references old OkHttp packages.
-dontwarn com.squareup.okhttp.**
-dontwarn com.squareup.picasso.**

# Optional / compile-only classes pulled by libraries.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.parse.**
-dontwarn org.osmdroid.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn org.java_websocket.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
