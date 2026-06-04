package com.grupo3.donapp_access.features.auth

import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.features.auth.dto.TiendaDTO
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import com.grupo3.donapp_access.features.auth.dto.UsuarioDTO

//@Inject constructor(): Le permite a Hilt suministrar esta clase a los viewmodels
class AuthRepository @Inject constructor(
    private val client: io.github.jan.supabase.SupabaseClient
){

    //accedemos al modulo de autenticacion del cliente que creamos antes
    private val supabaseAuth = client.auth


    //suspend permite que la funcion se ejecute sin bloquear la UI
    //es una marca que le ponemos a una función como signIn o signUp para decir:
    //esta tarea va a tardar, así que no bloquees
    suspend fun signUp(email: String, pass: String, captchaToken: String): io.github.jan.supabase.auth.user.UserInfo? {
        return supabaseAuth.signUpWith(Email) {
            this.email = email
            this.password = pass
            this.captchaToken = captchaToken // Aquí está la clave
        }
    }


    suspend fun signIn(email: String, pass: String, captchaToken: String? = null) {
        supabaseAuth.signInWith(Email) {
            this.email = email
            password = pass
            if (!captchaToken.isNullOrEmpty()) {
                this.captchaToken = captchaToken
            }
        }
    }


    suspend fun registrarEnTablaUsuarios(
        id: String,
        nombres: String,
        apellidos: String,
        correo: String,
        dni: String,
        rol: String,
        discapacidad: String?,
        correoApoderado: String?,
        telefono: String
    ) {
        try {
            val nuevoUsuario = UsuarioDTO(
                idUsuarios = id,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                dni = dni,
                rol = rol,
                tipoDiscapacidad = discapacidad,
                correoApoderado = correoApoderado,
                telefono = telefono
            )
            SupabaseClient.client.from("usuarios").insert(nuevoUsuario)

            android.util.Log.d("SUPABASE_OK", "Inserción exitosa para el usuario: $correo")
        }catch (e: Exception){

            android.util.Log.e(
                "SUPABASE_ERROR",
                "Fallo al insertar en la tabla usuarios: ${e.message}",
                e
            )

            throw e
        }


    }
    suspend fun obtenerRolUsuario(id: String): String {
        try {
            val usuario = SupabaseClient.client.from("usuarios")
                .select {
                    filter { eq("id_usuarios", id) }
                }.decodeSingle<UsuarioDTO>()

            return usuario.rol
        } catch (e: Exception) {
            android.util.Log.e("SUPABASE_ERROR", "Error al obtener rol: ${e.message}", e)
            throw e
        }
    }

    suspend fun registrarTienda(tienda: TiendaDTO) {
        client.from("tiendas").insert(tienda)
    }

    suspend fun correoEstaVerificado(): Boolean{
        return try {
            client.auth.retrieveUser(client.auth.currentAccessTokenOrNull()?: return false)
            val user = client.auth.currentUserOrNull()

            user?.emailConfirmedAt !=null
        }catch (e: Exception){
            android.util.Log.e("AUTH_REPO", "Error verificando correo: ${e.message}")
            false
        }
    }
    suspend fun verificarDniYTelefono(dni: String, telefono: String) {
        //verificar si el DNI ya existe
        val usuariosConDni = SupabaseClient.client.from("usuarios")
            .select { filter { eq("dni", dni) } }
            .decodeList<UsuarioDTO>()

        if (usuariosConDni.isNotEmpty()) {
            throw Exception("El DNI ingresado ya se encuentra registrado en otra cuenta.")
        }

        //verificar si el Teléfono ya existe
        val usuariosConTelefono = SupabaseClient.client.from("usuarios")
            .select { filter { eq("telefono", telefono) } }
            .decodeList<UsuarioDTO>()

        if (usuariosConTelefono.isNotEmpty()) {
            throw Exception("El número de teléfono ya se encuentra vinculado a otra cuenta.")
        }
    }




}