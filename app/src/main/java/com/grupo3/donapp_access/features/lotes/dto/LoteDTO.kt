package com.grupo3.donapp_access.features.lotes.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoteDTO(
    @SerialName("id_lote")
    val idLote: String? = null,

    @SerialName("productos_id")
    val productosId: String,

    @SerialName("tiendas_id")
    val tiendasId: String,

    @SerialName("numero_lote")
    val numeroLote: String? = null,

    val cantidad: Int,


    @SerialName("fecha_vencimiento")
    val fechaVencimiento: String = "",

    @SerialName("precio_normal")
    val precioNormal: Double,

    @SerialName("precio_oferta")
    val precioOferta: Double? = null,

    @SerialName("fecha_limite_oferta")
    val fechaLimiteOferta: String? = null,

    val estado: String = "disponible",

    @SerialName("productos")
    val productos: ProductoDTO? = null
)