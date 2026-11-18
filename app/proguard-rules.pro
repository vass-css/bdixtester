# Keep source file & line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room persistence & Kotlin coroutines
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void <init>();
}
-keepclassmembers class com.jubayerhossain.bdixtester.data.local.** { *; }

# OkHttp & WorkManager compatibility
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
