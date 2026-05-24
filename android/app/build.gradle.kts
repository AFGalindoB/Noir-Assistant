import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization") version "2.0.21"
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.afgalindob.assistantapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.afgalindob.assistantapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                keystorePropertiesFile.inputStream().use { properties.load(it) }
                
                val storeFilePath = properties.getProperty("MYAPP_RELEASE_STORE_FILE")
                val keystoreFile = if (storeFilePath != null) rootProject.file(storeFilePath) else null

                if (keystoreFile != null && keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = properties.getProperty("MYAPP_RELEASE_STORE_PASSWORD")
                    keyAlias = properties.getProperty("MYAPP_RELEASE_KEY_ALIAS")
                    keyPassword = properties.getProperty("MYAPP_RELEASE_KEY_PASSWORD")
                    println("INFO: Usando llave de producción oficial.")
                } else {
                    initWith(getByName("debug"))
                    println("WARNING: Keystore file '${storeFilePath}' no encontrado en la raíz. Usando llave de debug.")
                }
            } else {
                initWith(getByName("debug"))
                println("WARNING: keystore.properties no encontrado. Usando llave de debug.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

baselineProfile {
    saveInSrc = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation(libs.androidx.navigation.compose)

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.remote.creation.core)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.foundation)
    "baselineProfile"(project(":baselineprofile"))
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.accompanist.permissions)

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("dev.chrisbanes.snapper:snapper:0.3.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")

    implementation(libs.androidx.security.crypto)
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")

    implementation("com.google.mlkit:barcode-scanning:17.2.0")
}