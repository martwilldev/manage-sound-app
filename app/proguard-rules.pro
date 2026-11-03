# Regras ProGuard / R8 para Manage Sound Mode
# Preserva metadados Kotlin, anotações e serializadores gerados pelo Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserva todas as classes @Serializable do app (incluindo DashboardKey e modelos de domínio)
-keep,includedescriptorclasses class com.managesoundmode.app.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.managesoundmode.app.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.managesoundmode.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.managesoundmode.app.DashboardKey { *; }
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
-keep class com.managesoundmode.app.domain.model.** { *; }
-keep class com.managesoundmode.app.data.local.PersistedSoundPrefs { *; }

# Preserva RoomDatabase_Impl (usado internamente pelo WorkManager via reflexão)
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
    <init>();
    *;
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
    <init>();
    *;
}
-keep class androidx.work.** { *; }

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
-keep class com.managesoundmode.app.widget.** { *; }
-keep class com.managesoundmode.app.tile.** { *; }
-keep class com.managesoundmode.app.receiver.** { *; }
