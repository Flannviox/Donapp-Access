package com.grupo3.donapp_access.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reserva(
    @SerialName("id_reservas")
    val idReservas: String? = null, // Null por defecto porque Supabase lo genera automáticamente

    @SerialName("usuario_id")
    val usuarioId: String,

    @SerialName("id_lote")
    val idLote: String,

    @SerialName("cantidad")
    val cantidad: Int,

    @SerialName("estado")
    val estado: String = "activa",

    @SerialName("fecha_reserva")
    val fechaReserva: String? = null, // Null por defecto

    @SerialName("fecha_expiracion")
    val fechaExpiracion: String? = null // Null por defecto
)