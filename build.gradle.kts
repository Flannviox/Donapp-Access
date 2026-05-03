// build.gradle.kts (Project: Donapp-Access)
/*"Estos plugins existen en el proyecto, pero cada módulo decidirá si los usa."**/
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.kapt)         apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.navigation.safeargs) apply false
}
