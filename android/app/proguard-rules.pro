# Room y Compose traen sus propias reglas. Solo se conservan los modelos que se
# serializan a JSON en las exportaciones.
-keepclassmembers class com.misaludyfuerza.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.misaludyfuerza.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
