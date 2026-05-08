package com.grupo3.donapp_access.features.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioDTO(
    @SerialName("id_usuarios")
    val idUsuarios: String,

    val nombres: String,
    val apellidos: String,
    val correo: String,
    val dni: String,
    val rol: String,
    val telefono: String,

    @SerialName("tipo_discapacidad")
    val tipoDiscapacidad: String? = null,

    @SerialName("correo_apoderado")
    val correoApoderado: String? = null

)
