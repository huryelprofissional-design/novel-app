plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.huryel.novely"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.huryel.novely"
        minSdk = 26
        targetSdk = 35
        // número do build no GitHub = número da versão (o app compara com a tag build-N para se atualizar)
        val numeroBuild = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = numeroBuild
        versionName = "2.3 · build $numeroBuild"
        // só celulares de 64 bits (quase todos desde 2017): o tradutor no aparelho pesa 16 MB por tipo de processador
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Duas lojas: "github" (APK do GitHub, com atualizador próprio) e "play" (Google Play: atualiza pela loja,
    // sem REQUEST_INSTALL_PACKAGES — a Play não aceita app que se atualiza sozinho). Ver PLAY-01.
    flavorDimensions += "loja"
    productFlavors {
        create("github") { dimension = "loja"; buildConfigField("boolean", "ATUALIZADOR", "true") }
        create("play") { dimension = "loja"; buildConfigField("boolean", "ATUALIZADOR", "false") }
    }

    // Chave fixa: cada APK novo instala POR CIMA do anterior, sem apagar suas obras.
    signingConfigs {
        getByName("debug") {
            storeFile = file("novel-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("debug") }
        // O APK publicado é o "release": código otimizado pelo R8 e perfis de inicialização do Compose.
        // Fica bem mais leve e fluido que o debug. Mesma chave, então instala por cima sem perder nada.
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    // bibliotecas nativas comprimidas dentro do APK (o download fica bem menor)
    packaging { jniLibs { useLegacyPackaging = true } }
    // Testes de tela (Robolectric + Roborazzi): desenham telas no computador e salvam PNG em build/telas
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // vozes neurais do Novely (Piper e Kokoro, sem internet). O AAR é baixado pelo CI (não vai para o repositório).
    implementation(files("libs/sherpa-onnx.aar"))
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    // tradução e detecção de idioma no aparelho (grátis, offline depois de baixar o idioma)
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.26.0")
    testImplementation("androidx.test:core:1.6.1")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // Conta e nuvem (Firebase): login Google/e-mail e sincronização
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
