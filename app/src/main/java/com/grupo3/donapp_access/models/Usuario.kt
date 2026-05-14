package com.grupo3.donapp_access.models

data class Usuario (
    val id: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val dni: String,
    val rol: String,
    val telefono: String,
    val tipoDiscapacidad: String?,
    val correoApoderado: String
)