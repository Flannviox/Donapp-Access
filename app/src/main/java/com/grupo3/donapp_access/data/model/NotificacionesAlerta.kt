package com.grupo3.donapp_access.data.model

data class NotificacionesAlerta(
    val fechaHora: String,
    val mensaje: String,
    val tiendaId: String,
    val distancia: Double
)
