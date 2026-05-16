package com.grupo3.donapp_access.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Producto(
    @SerialName("id_producto")  val id_producto: String,
    @SerialName("categoria_id") val categoria_id: String? = null,
    @SerialName("nombre")       val nombre: String,
    @SerialName("descripcion")  val descripcion: String? = null,
    @SerialName("presentacion") val presentacion: String? = null,
    @SerialName("imagen")       val imagen: String? = null,
    @SerialName("precio_base")  val precio_base: Double? = null
)