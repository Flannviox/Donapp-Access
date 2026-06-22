package com.grupo3.donapp_access.data.model

data class TiendaHome(
    val idTienda: String? = null,
    val nombre: String,
    val direccion: String?,
    val rating: Double?,
    val latitud: Double? = null,
    val longitud: Double? = null
)
