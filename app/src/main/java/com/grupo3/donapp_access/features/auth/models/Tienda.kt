package com.grupo3.donapp_access.features.auth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//convierte el objeto en json (supabase devuelve json)
@Serializable
data class Tienda(
    @SerialName("id_tienda")            val id: String,
    @SerialName("nombre")               val nombre: String,
    @SerialName("direccion")            val direccion: String,
    @SerialName("latitud")              val latitud: Double,
    @SerialName("longitud")             val longitud: Double,
    @SerialName("rating_promedio")      val rating: Double = 0.0,
    @SerialName("imagen_referencia")    val imagen: String?=null,
    @SerialName("hora_atencion")        val horario: String?= null,
    @SerialName("lotes_activos")        val lotesActivos: Int = 0

)