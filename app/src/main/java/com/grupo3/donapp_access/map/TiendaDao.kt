package com.grupo3.donapp_access.map

import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.model.Tienda
import com.grupo3.donapp_access.features.map.TiendaConLotes
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

//esta es la conexion entre android y Postgis/Supabase para traer tiendas cercanas

//Android -> supabaseRPC -> Postgresql-> PostGIS -> ST_DWithin

//funciona como un repositorio remoto

@Singleton
class TiendaDao @Inject constructor() {
    suspend fun getTiendasCercanas(
        latitud: Double,
        longitud: Double,
        radioMetros: Int = 5000
    ): List<TiendaConLotes>{
        return SupabaseClient.client.postgrest //activa el modulo PostgREST de supa (queries, insert, rpc, filters)
            .rpc( //llamar a una funcion del server desde android ("SELECT * FROM tiendas_cercanas)
            function = "tiendas_cercanas",
            parameters = BusquedaParams(
                lat = latitud,
                lng = longitud,
                radio = radioMetros
            )
        )
            .decodeList<TiendaConLotes>()
    }
}

@Serializable
private data class BusquedaParams(
    val lat: Double,
    val lng : Double,
    val radio: Int

)