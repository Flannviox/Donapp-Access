package com.grupo3.donapp_access.models

data class Tienda(
    val idTienda: String,
    val usuariosId: String,
    val nombre: String,
    val direccion: String,
    val referencia: String?,
    val latitud: Double,
    val longitud: Double,
    val imagenReferencia: String?,
    val ratingPromedio: Double,
    val horaAtencion: String?
)
