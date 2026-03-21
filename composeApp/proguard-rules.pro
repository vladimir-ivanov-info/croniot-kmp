# ============================================================
# ProGuard / R8 rules for croniot composeApp
# ============================================================
# PRINCIPIO: solo añadir -keep para lo que usa reflexión.
# R8 ya sabe analizar código normal; las reglas excesivas
# IMPIDEN que R8 elimine código, aumentando el tamaño.
# ============================================================

# ----------------------------------------------------------
# Google Generative AI (Gemini SDK)
# Referencia ktor-client-mock (solo para sus tests internos).
# ----------------------------------------------------------
-dontwarn io.ktor.client.engine.mock.**

# ----------------------------------------------------------
# kotlinx.serialization
# Solo necesitamos mantener los serializers generados y los
# Companion objects que exponen serializer().
# ----------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.croniot.**$$serializer { *; }
-keepclassmembers class com.croniot.** {
    *** Companion;
}
-keepclasseswithmembers class com.croniot.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------------------------
# Retrofit
# Solo mantener los métodos anotados con @GET, @POST, etc.
# en interfaces. El resto (clases internas) R8 lo maneja bien.
# ----------------------------------------------------------
-keepattributes Signature, Exceptions
-keep,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ----------------------------------------------------------
# OkHttp — platform adapters opcionales
# ----------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ----------------------------------------------------------
# Paho MQTT
# Solo el Service (registrado en Manifest, accedido por reflexión)
# y las clases de persistencia que usa internamente.
# ----------------------------------------------------------
-keep class org.eclipse.paho.android.service.MqttService { *; }
-keep class org.eclipse.paho.client.mqttv3.persist.** { *; }
-keep class org.eclipse.paho.client.mqttv3.logging.** { *; }

# ----------------------------------------------------------
# Coroutines — ServiceLoader para MainDispatcherFactory
# ----------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ----------------------------------------------------------
# Stack traces legibles en crash reports
# ----------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile