package com.grupo3.donapp_access.features.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TiendaRegistroDTO(
    @SerialName("usuarios_id") val usuariosId: String,
    val nombre: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    @SerialName("hora_atencion") val horaAtencion: String,
    val referencia: String? = null,
    @SerialName("imagen_referencia") val imagenReferencia: String? = null
)

