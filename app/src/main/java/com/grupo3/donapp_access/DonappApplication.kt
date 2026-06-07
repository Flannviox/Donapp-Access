package com.grupo3.donapp_access

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
// Importamos nuestro nuevo gestor de voz
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager

// Esta anotación es obligatoria para usar hilt
// Hilt permite generar codigo base para la inyección de dependencias en toda la app
@HiltAndroidApp
class DonappApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // Sobreescribimos onCreate para inicializar cosas globales
    override fun onCreate() {
        super.onCreate()

        // Inicializamos el motor de voz pasándole el contexto de la aplicación
        VoiceAssistantManager.init(this)
    }
}