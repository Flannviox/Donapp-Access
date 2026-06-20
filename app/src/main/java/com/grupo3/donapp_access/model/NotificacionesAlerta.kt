package com.grupo3.donapp_access.model

data class NotificacionesAlerta(
    val fechaHora: String,
    val mensaje: String,
    val tiendaId: String,
    val distancia: Double
)
