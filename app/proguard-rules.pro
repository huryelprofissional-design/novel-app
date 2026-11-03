# Novely: o app não usa reflexão própria; as bibliotecas (Room, Firebase, WorkManager) trazem as regras delas.
# Mensagens de erro mostram o nome da exceção: mantém nomes legíveis nos stack traces do diagnóstico.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Login com Google (Credential Manager carrega o provedor por reflexão)
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Avisos de anotações opcionais de bibliotecas
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
