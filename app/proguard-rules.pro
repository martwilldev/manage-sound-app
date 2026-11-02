# Regras ProGuard / R8 para Manage Sound Mode
# Preserva serializadores gerados pelo Kotlinx Serialization no DataStore
-keepattributes *Annotation*, InnerClasses
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
