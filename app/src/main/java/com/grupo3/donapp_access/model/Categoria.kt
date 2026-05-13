package com.grupo3.donapp_access.model

data class Categoria(
    val idCategoria: String,
    val nombre: String,
    val estado: String,
    val totalOfertas: Int = 0
)
