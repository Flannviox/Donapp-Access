package com.grupo3.donapp_access.usuario.dto

import com.grupo3.donapp_access.features.auth.models.Tienda
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TiendaDTO(
    @SerialName("id_tienda")
    val idTienda: String,

    @SerialName("usuarios_id")
    val usuariosId: String,

    val nombre: String,
    val direccion: String,
    val referencia: String? = null,
    val latitud: Double,
    val longitud: Double,

    @SerialName("imagen_referencia")
    val imagenReferencia: String? = null,

    @SerialName("rating_promedio")
    val ratingPromedio: Double = 0.0,

    @SerialName("hora_atencion")
    val horaAtencion: String? = null
) {
    companion object {
        fun from(tienda: Tienda): TiendaDTO {
            return TiendaDTO(
                idTienda = tienda.id,
                usuariosId = tienda.usuariosId,
                nombre = tienda.nombre,
                direccion = tienda.direccion,
                referencia = tienda.referencia,
                latitud = tienda.latitud,
                longitud = tienda.longitud,
                imagenReferencia = tienda.imagen,
                ratingPromedio = tienda.rating,
                horaAtencion = tienda.horario

            )
        }
    }
}

@Serializable
data class LoteDTO(
    @SerialName("id_lote")
    val idLote: String,
    
    @SerialName("precio_normal")
    val precioNormal: Double,
    
    @SerialName("precio_oferta")
    val precioOferta: Double? = null,
    
    val cantidad: Int,
    val estado: String,
    
    // Relación para el join con productos
    val productos: ProductoDTO? = null
)

@Serializable
data class ProductoDTO(
    @SerialName("id_producto")
    val idProducto: String,
    val nombre: String,
    val imagen: String? = null
)
