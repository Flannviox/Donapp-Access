package com.grupo3.donapp_access.features.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductoDTO(
    @SerialName("id_producto") val id_producto: String,
    val nombre: String,
    val imagen: String? = null
)

@Serializable
data class LoteDTO(
    @SerialName("id_lote") val id_lote: String,
    val cantidad: Int,
    @SerialName("precio_normal") val precio_normal: Double,
    @SerialName("precio_oferta") val precio_oferta: Double,
    @SerialName("fecha_vencimiento") val fecha_vencimiento: String,
    // Usamos SerialName para que coincida exactamente con tu columna "productos_id"
    @SerialName("productos") val producto: ProductoDTO? = null
) // EL PARÉNTESIS DEBE CERRAR AQUÍ