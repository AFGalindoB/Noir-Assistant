# Reglas para Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# Mantener los nombres de los modelos de datos (Domain/Entity) para evitar errores de persistencia/JSON
-keep class com.afgalindob.assistantapp.data.local.entity.** { *; }
-keep class com.afgalindob.assistantapp.domain.** { *; }

# Kotlin Serialization / GSON (si los usas para persistencia o red)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep @Serializable classes and their serializers
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}

# Keep navigation destinations specifically
-keep class com.afgalindob.assistantapp.navigation.** { *; }

-keep class kotlinx.serialization.** { *; }
-keep class com.google.gson.** { *; }

# Mantener números de línea para Firebase Crashlytics o logs de error legibles
-keepattributes SourceFile,LineNumberTable
