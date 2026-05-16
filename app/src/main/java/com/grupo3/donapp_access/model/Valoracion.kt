package com.grupo3.donapp_access.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Valoracion(
    @SerialName("id_valoraciones")
    val idValoraciones: String? = null,

    @SerialName("tiendas_id")
    val tiendasId: String,

    @SerialName("usuarios_id")
    val usuariosId: String,

    @SerialName("calificacion")
    val calificacion: Double,

    @SerialName("comentario")
    val comentario: String? = null,

    @SerialName("estado")
    val estado: String = "ACTIVO",

    @SerialName("created_at")
    val createdAt: String? = null
)