package com.grupo3.donapp_access.core.network

//aqui estamos importante nuestra bilbioteca BuildConfig para obtener las credenciales
import com.grupo3.donapp_access.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

//object crea un singleton: solo habra una instancia en toda la app
object SupabaseClient {

    //Iniciamos el cliente con la URL y la Anon Key del proyecto
    val client = createSupabaseClient(

        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ){
        //instalamos los módulos necesarios para el mvp
        install(Auth) //registro y login
        install(Postgrest) //consultar tablas como tiendas o lotes
        install(Storage) // para gestionar las fotos de productos y tiendas
        install(Realtime) //Para notificaciones y cambios de estado en vivo
    }
}