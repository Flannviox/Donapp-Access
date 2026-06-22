package com.grupo3.donapp_access.features.comerciante.lotes.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoriaDTO(
    @SerialName("id_categoria")
    val idCategoria: String? = null,

    val nombre: String,

    val estado: String = "ACTIVO"
)
