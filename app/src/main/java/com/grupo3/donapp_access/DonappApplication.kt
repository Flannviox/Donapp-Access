package com.grupo3.donapp_access

import android.app.Application
import dagger.hilt.android.HiltAndroidApp



//Estaas anotación es obligatoria para usar hilt
//Hilt prmite generar codigo base para la inyección de dependencias funciones en toda la app
@HiltAndroidApp
class DonappApplication : Application()

//al heredar de applicattion, esta clase vive mientras la app esté abierta
