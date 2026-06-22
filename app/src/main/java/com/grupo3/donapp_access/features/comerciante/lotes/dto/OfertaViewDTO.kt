package com.grupo3.donapp_access.features.comerciante.lotes.dto

import com.grupo3.donapp_access.data.model.OfertaLote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfertaViewDTO(
    @SerialName("id_lote") val idLote: String,
    @SerialName("id_tienda") val idTienda: String? = null,
    @SerialName("numero_lote") val numeroLote: String? = null,
    @SerialName("cantidad") val cantidad: Int,
    @SerialName("fecha_vencimiento") val fechaVencimiento: String,
    @SerialName("precio_normal") val precioNormal: Double,
    @SerialName("precio_oferta") val precioOferta: Double? = null,
    @SerialName("estado") val estado: String,
    @SerialName("producto_nombre") val productoNombre: String,
    @SerialName("producto_imagen") val productoImagen: String? = null,
    @SerialName("producto_presentacion") val productoPresentacion: String? = null,
    @SerialName("categoria_nombre") val categoriaNombre: String,
    @SerialName("tienda_nombre") val tiendaNombre: String,
    @SerialName("tienda_direccion") val tiendaDireccion: String? = null,
    @SerialName("tienda_rating") val tiendaRating: Double? = null
) {
    fun toOfertaLote() = OfertaLote(
        idLote = idLote,
        productoNombre = productoNombre,
        tiendaId = idTienda,
        productoImagen = productoImagen,
        productoPresentacion = productoPresentacion,
        tiendaNombre = tiendaNombre,
        tiendaDireccion = tiendaDireccion,
        cantidad = cantidad,
        fechaVencimiento = fechaVencimiento,
        precioNormal = precioNormal,
        precioOferta = precioOferta ?: 0.0,
        numeroLote = numeroLote,
        ratingTienda = tiendaRating
    )
}