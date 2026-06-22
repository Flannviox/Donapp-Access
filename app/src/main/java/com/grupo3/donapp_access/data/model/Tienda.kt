package com.grupo3.donapp_access.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//convierte el objeto en json (supabase devuelve json)
@Serializable
data class Tienda(
    @SerialName("id_tienda")            val id: String,
    @SerialName("usuarios_id")          val usuariosId: String,
    @SerialName("nombre")               val nombre: String,
    @SerialName("direccion")            val direccion: String,
    @SerialName("referencia")           val referencia: String,
    @SerialName("latitud")              val latitud: Double,
    @SerialName("longitud")             val longitud: Double,
    @SerialName("rating_promedio")      val rating: Double = 0.0,
    @SerialName("imagen_referencia")    val imagen: String?=null,
    @SerialName("hora_atencion")        val horario: String?= null,

    )