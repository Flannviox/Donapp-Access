package com.grupo3.donapp_access.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lote(
    @SerialName("id_lote")           val id_lote: String,
    @SerialName("productos_id")      val productos_id: String,
    @SerialName("tiendas_id")        val tiendas_id: String,
    @SerialName("numero_lote")       val numero_lote: String? = null,
    @SerialName("cantidad")          val cantidad: Int,
    @SerialName("fecha_vencimiento") val fecha_vencimiento: String,
    @SerialName("precio_normal")     val precio_normal: Double,
    @SerialName("precio_oferta")     val precio_oferta: Double? = null,
    @SerialName("fecha_limite_oferta") val fecha_limite_oferta: String? = null,
    @SerialName("estado")            val estado: String = "disponible"
)