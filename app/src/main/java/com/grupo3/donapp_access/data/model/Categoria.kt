package com.grupo3.donapp_access.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Categoria(
    @SerialName("id_categoria")  val idCategoria: String,
    @SerialName("nombre")        val nombre: String,
    @SerialName("estado")        val estado: String = "ACTIVO",
    val totalOfertas: Int = 0
)