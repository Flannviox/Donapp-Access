import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)

}
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}
android {
    namespace = "com.grupo3.donapp_access"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grupo3.donapp_access"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Supabase — reemplaza con tus credenciales del Project Settings → API
        defaultConfig {
            buildConfigField("String", "SUPABASE_URL",
                "\"${localProps["SUPABASE_URL"]}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY",
                "\"${localProps["SUPABASE_ANON_KEY"]}\"")
            buildConfigField("String", "MAPBOX_TOKEN",
                "\"${localProps["MAPBOX_TOKEN"]}\"")
        }
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
        viewBinding = true  // acceso a vistas XML sin findViewById
        buildConfig = true  // para leer SUPABASE_URL, SUPABASE_ANON_KEY, MAPBOX_TOKEN
    }
}

dependencies {
    // ── AndroidX Core ─────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ── Lifecycle + ViewModel + LiveData ──────────────────────
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // ── Navigation Component ──────────────────────────────────
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // ── Room (caché local offline) ────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // ── Hilt (inyección de dependencias) ─────────────────────
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ── Coroutines ────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Supabase ──────────────────────────────────────────────
    implementation(libs.supabase.postgrest)  // consultas a la BD
    implementation(libs.supabase.auth)       // login / registro
    implementation(libs.supabase.storage)    // imágenes de productos
    implementation(libs.supabase.realtime)   // notificaciones en tiempo real

    // ── Ktor (cliente HTTP requerido por Supabase) ────────────
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)

    // ── Mapbox ────────────────────────────────────────────────
    implementation(libs.mapbox.android)

    // ── Glide (carga de imágenes desde URL) ──────────────────
    implementation(libs.glide)


    implementation(libs.play.services.location)

    // ── Testing ───────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}