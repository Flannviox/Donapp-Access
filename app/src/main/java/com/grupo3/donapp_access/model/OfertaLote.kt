package com.grupo3.donapp_access.model

data class OfertaLote(
    val idLote: String,
    val tiendaId: String?,
    val productoNombre: String,
    val productoImagen: String?,
    val productoPresentacion: String?,
    val tiendaNombre: String,
    val tiendaDireccion: String?,
    val cantidad: Int,
    val fechaVencimiento: String,
    val precioNormal: Double,
    val precioOferta: Double,
    val numeroLote: String?,
    val ratingTienda: Double?
)
