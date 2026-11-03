# Regras ProGuard / R8 para Manage Sound Mode
# Preserva serializadores gerados pelo Kotlinx Serialization no DataStore
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.managesoundmode.app.**$$serializer { *; }
-keepclassmembers class com.managesoundmode.app.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.managesoundmode.app.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.managesoundmode.app.domain.model.** { *; }

# Preserva RoomDatabase_Impl (usado internamente pelo WorkManager via reflexão)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
    *;
}

# Preserva Workers do WorkManager instanciados via reflexão
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Preserva ActionCallbacks e Receivers do Jetpack Glance instanciados via reflexão
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
    *;
}
-keep class * extends androidx.glance.appwidget.GlanceAppWidget {
    public <init>();
    *;
}
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver {
    public <init>();
    *;
}
