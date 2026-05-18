package com.grupo3.donapp_access

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


//Estaas anotación es obligatoria para usar hilt
//Hilt prmite generar codigo base para la inyección de dependencias funciones en toda la app
@HiltAndroidApp
class DonappApplication : Application(), Configuration.Provider{

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()



}
