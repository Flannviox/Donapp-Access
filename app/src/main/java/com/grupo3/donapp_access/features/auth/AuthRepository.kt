package com.grupo3.donapp_access.features.auth

import com.grupo3.donapp_access.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import com.grupo3.donapp_access.features.auth.dto.UsuarioDTO

//@Inject constructor(): Le permite a Hilt suministrar esta clase a los viewmodels
class AuthRepository @Inject constructor(){

    //accedemos al modulo de autenticacion del cliente que creamos antes
    private val supabaseAuth = SupabaseClient.client.auth


    //suspend permite que la funcion se ejecute sin bloquear la UI
    //es una marca que le ponemos a una función como signIn o signUp para decir:
    //esta tarea va a tardar, así que no bloquees
    suspend fun signUp(email: String, pass: String): io.github.jan.supabase.auth.user.UserInfo?{


        val result = supabaseAuth.signUpWith(Email) {
            this.email = email
            password = pass
        }
    // Devolvemos la información del usuario (contiene el id de auth.users)
        return result
    }

    suspend fun signIn(email: String, pass: String){
        //Inicias sesion con las credenciales proporcionadas
        supabaseAuth.signInWith(Email){
            this.email = email
            password = pass
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

            android.util.Log.d("SUPABASE_OK", "¡Inserción exitosa para el usuario: $correo!")
        }catch (e: Exception){

            android.util.Log.e(
                "SUPABASE_ERROR",
                "Fallo al insertar en la tabla usuarios: ${e.message}",
                e
            )

            throw e
        }


    }




}