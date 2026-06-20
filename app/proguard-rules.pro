# FinSplit ProGuard rules

# Keep Firebase model classes (Firestore POJO deserialization)
-keep class com.finsplit.app.models.** { *; }

# Keep WorkManager workers
-keep class com.finsplit.app.workers.** { *; }

# Firebase / Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Material / AndroidX
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }
