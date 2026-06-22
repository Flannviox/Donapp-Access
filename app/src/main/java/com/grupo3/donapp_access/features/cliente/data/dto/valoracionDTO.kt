package com.grupo3.donapp_access.features.cliente.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValoracionDTO(
    @SerialName("id_valoraciones")
    val idValoraciones: String,
    
    @SerialName("calificacion")
    val calificacion: Double,
    
    @SerialName("comentario")
    val comentario: String? = null,
    
    @SerialName("created_at")
    val createdAt: String,
    
    // Relación para el join con usuarios en Supabase
    @SerialName("usuarios")
    val usuarios: UsuarioNombreDTO? = null
)

@Serializable
data class UsuarioNombreDTO(
    @SerialName("nombres")
    val nombres: String,
    
    @SerialName("apellidos")
    val apellidos: String
)
