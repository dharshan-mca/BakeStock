# Add project specific ProGuard rules here.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room Entities & DAOs
-keep class com.example.data.models.** { *; }
-keep class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Firebase Models & Firestore Data Classes
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.google.firebase.** { *; }

# Keep Jetpack Compose & Material 3
-keep class androidx.compose.** { *; }

# Keep Kotlin Serialization / Metadata
-keepclassmembers class **$serializer {
    public static final **$serializer INSTANCE;
}
-keepclassmembers class * {
    *** Companion;
}

