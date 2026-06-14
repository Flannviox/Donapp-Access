import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.grupo3.donapp_access"
    compileSdk = 35

    defaultConfig {

        applicationId = "com.donapp_access"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL",
            "\"${localProps["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",
            "\"${localProps["SUPABASE_ANON_KEY"]}\"")
        buildConfigField("String", "MAPBOX_TOKEN",
            "\"${localProps["MAPBOX_TOKEN"]}\"")

        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] =
            localProps["MAPBOX_TOKEN"] ?: ""

        // Solución a la advertencia del idioma no reconocido ("bw") en la Play Console
        resourceConfigurations += setOf("es", "en")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("*/libmapbox-common.so")
            keepDebugSymbols.add("*/libmapbox-maps.so")
        }
    }
}

dependencies {

    // Google Play Services para obtener la ubicación exacta
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Herramientas matemáticas de Mapbox (Turf y GeoJSON) para calcular distancias
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:6.15.0")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-geojson:6.15.0")

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)

    // Ktor
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)

    // Mapbox
    implementation(libs.mapbox.android)

    // Glide
    implementation(libs.glide)

    // Location
    implementation(libs.play.services.location)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt para WorkManager
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0") // O ksp si ya migraste a KSP


    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}