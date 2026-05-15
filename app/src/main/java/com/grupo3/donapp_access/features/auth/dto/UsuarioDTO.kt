package com.grupo3.donapp_access.features.auth.dto

import com.grupo3.donapp_access.models.Usuario
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
){
    companion object{
        fun from(usuario: Usuario): UsuarioDTO{
            return UsuarioDTO(
                idUsuarios = usuario.id,
                nombres = usuario.nombres,
                apellidos = usuario.apellidos,
                correo = usuario.correo,
                dni = usuario.dni,
                rol = usuario.rol,
                telefono = usuario.telefono,
                tipoDiscapacidad = usuario.tipoDiscapacidad,
                correoApoderado = usuario.correoApoderado
            )
        }
    }
}