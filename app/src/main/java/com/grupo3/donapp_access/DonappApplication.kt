package com.grupo3.donapp_access

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
    Clase principal de la aplicación.

    @HiltAndroidApp inicializa Hilt
    hilt maneja la inyección de dependencias en toda la app.

    Esta clase se crea una sola vez al iniciar
    la aplicación y sirve para configuraciones globales.
 */


@HiltAndroidApp
class DonappApplication : Application()


/*
    Hilt genera código automáticamente para:

    crear contenedores de dependencias
    manejar ciclos de vida
    compartir instancias
    inyectar objetos en Activities/Fragments/ViewModels
 */
