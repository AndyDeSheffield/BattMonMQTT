plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}


android {
    lint { disable += setOf("ExpiredTargetSdkVersion")
        abortOnError = false // 👈 allows build to continue even if other issues exist
         }

    namespace = "com.grafton.battmonmqtt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grafton.battmonmqtt"
        minSdk = 28          // covers Fire OS 7 (Android 9)
        targetSdk = 30       // Fire OS 8 baseline (Android 11)
        versionCode = 1
        versionName = "1.0"
        setProperty("archivesBaseName", applicationId + "-v" + versionCode + "(" + versionName + ")")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "META-INF/io.netty.versions.properties"   // <— add this line
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Proto DataStore (for custom Serializable types)
    implementation("androidx.datastore:datastore:1.1.1")

    // HiveMQ MQTT client
    implementation("com.hivemq:hivemq-mqtt-client:1.3.0")

}
