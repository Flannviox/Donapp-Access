package com.grupo3.donapp_access.features.comerciante.lotes.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductoDTO(
    @SerialName("id_producto")
    val idProducto: String? = null,

    @SerialName("categoria_id")
    val categoriaId: String,

    @SerialName("tiendas_id")
    val tiendas_Id: String? = null,

    val nombre: String,

    val descripcion: String? = null,
    val presentacion: String? = null,
    val imagen: String? = null
)
