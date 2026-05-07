pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                val props = java.util.Properties()
                val f = file("local.properties")
                if (f.exists()) props.load(f.inputStream())
                value = "Bearer ${props["MAPBOX_SECRET_TOKEN"]}"
            }
        }
    }
}

rootProject.name = "Donapp-Access"
include(":app")