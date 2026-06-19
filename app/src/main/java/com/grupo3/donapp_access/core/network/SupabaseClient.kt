package com.grupo3.donapp_access.core.network

import com.grupo3.donapp_access.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClient {

    //Iniciamos el cliente con la URL y la Anon Key del proyecto
    val client = createSupabaseClient(

        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ){
        //instalamos los módulos necesarios para el mvp
        install(Auth) //login, signup, sesiones, currentuser, JWT
        install(Postgrest) //Permite trabajar con tablas sql(comandos)
        install(Storage) // para subir las fotos de productos y tiendas
        install(Realtime) //Para notificaciones y cambios de estado en vivo
    }
}