package com.grupo3.donapp_access.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Message
import com.grupo3.donapp_access.core.network.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    //recibimos el repositorio auth mediante inyeccion de dependencias
    private val repository: AuthRepository
) : ViewModel(){

    //estados de la UI
    private  val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> =_registerState


    fun login(email: String, pass: String){
        //viewModelScope.Launch: inicia una corrutina que se cancela si el usuario sale de la pantalla

        viewModelScope.launch {
            _loginState.value = AuthState.Loading

            try {
                //llamamos a la funcion suspendia del repositorio
                repository.signIn(email, pass)

                // Obtenemos el ID del usuario que acaba de entrar
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                val userId = currentUser?.id

                if (userId != null) {
                    // Consultamos el rol usando la función que agregamos al repositorio
                    val rolUsuario = repository.obtenerRolUsuario(userId)
                    _loginState.value = AuthState.Success(rolUsuario)
                } else {
                    _loginState.value = AuthState.Error("No se encontró el ID del usuario")
                }
            }catch (e: Exception){
                //si hay un error como datos incorrectos, se captura
                e.printStackTrace()
                _loginState.value = AuthState.Error(e.message ?: "Error al iniciar sesión")

            }
        }
    }


    fun crearCuentaCompleta(
        email: String,
        pass: String,
        nombres: String,
        apellidos: String,
        dni: String,
        discapacidad: String?,
        rol: String,
        correoApoderado: String?,
        telefono: String
    ){
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            try {
                //el signup de la libreria devuelve la info del usuario creado
                repository.signUp(email, pass)

                val currentUser = SupabaseClient.client.auth.currentUserOrNull()

                val userId = currentUser?.id

                android.util.Log.d("SUPABASE_DEBUG", "CURRENT USER ID: $userId")
                if(userId != null){
                    repository.registrarEnTablaUsuarios(
                        id = userId,
                        nombres = nombres,
                        apellidos = apellidos,
                        correo = email,
                        dni = dni,
                        rol = rol,
                        discapacidad = discapacidad,
                        correoApoderado = correoApoderado,
                        telefono = telefono

                        )

                    _registerState.value = AuthState.Success(rol)

                }else {
                    _registerState.value = AuthState.Error("No se pudo obtener el ID del usuario")
                }
            }catch (e: Exception){
                _registerState.value = AuthState.Error(e.message ?: "Error en el registro")
                android.util.Log.e("SUPABASE_DATABASE", "Error detallado: ${e.message}")
                e.printStackTrace()

            }
        }

    }

    sealed class AuthState{
        object Idle : AuthState() //sin accion
        object Loading : AuthState() //cargando
        data class Success(val rol: String) : AuthState() // Ahora recibe el rol
        data class Error (val message: String) : AuthState()

    }
}