package com.grupo3.donapp_access.features.auth

import androidx.browser.trusted.Token
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Message
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.core.network.SupabaseClient.client
import com.grupo3.donapp_access.features.auth.dto.TiendaDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.jan.supabase.auth.providers.builtin.OTP
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


    fun login(email: String, pass: String, captchaToken: String){

        viewModelScope.launch {
            _loginState.value = AuthState.Loading

            try {
                repository.signIn(email, pass, captchaToken)


                //obtenemos el ID del usuario que acaba de entrar
                val userId = repository.obtenerIdUsuarioActual()

                if (userId == null) {
                    _loginState.value = AuthState.Error("No se encontro el ID del usuario")
                    return@launch
                }


                val verificado = repository.correoEstaVerificado()
                if(!verificado){
                    repository.cerrarSesion()
                    _loginState.value = AuthState.Error(
                        "Debes verificar tu correo antes de ingresar .\n" +
                        "Revisa tu bandeja de entrada (Y EL SPAM)"
                    )
                    return@launch

                }

                val rolUsuario = repository.obtenerRolUsuario(userId)
                _loginState.value = AuthState.Success(rolUsuario)

            }catch (e: Exception){
                val mensajeUsuario = if(e.message?.contains("Invalid login credential", ignoreCase = true)== true)
                    "Correo o contraseña incorrectos"
                else{
                    e.message ?: "Error al iniciar sesión"
                }
                _loginState.value = AuthState.Error(mensajeUsuario)

            }
        }
    }


    fun crearCuentaCompleta(
        email: String,
        pass: String,
        captchaToken: String,
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
                //id desde el resultado del signUp
                repository.verificarDniYTelefono(dni, telefono)
                val resultado = repository.signUp(email, pass, captchaToken)
                val userId = resultado?.id
                    ?: throw Exception("No se pudo obtener el ID de usuario")


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

    fun crearCuentaComerciante(
        email: String,
        pass: String,
        captchaToken: String,
        nombres: String,
        apellidos: String,
        dni: String,
        telefono: String,
        nombreTienda: String,
        direccion: String,
        lat: Double,
        lng: Double,
        horario: String
    ) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            try {
                repository.verificarDniYTelefono(dni, telefono)
                val resultado = repository.signUp(email, pass, captchaToken)
                val userId = resultado?.id
                    ?: throw Exception("No se pudo obtener el ID de usuario")



                repository.registrarEnTablaUsuarios(
                    id = userId,
                    nombres = nombres,
                    apellidos = apellidos,
                    correo = email,
                    dni = dni,
                    rol = "Comerciante",
                    discapacidad = null,
                    correoApoderado = null,
                    telefono = telefono
                )

                val tienda = TiendaDTO(
                    usuariosId = userId,
                    nombre = nombreTienda,
                    direccion = direccion,
                    latitud = lat,
                    longitud = lng,
                    horaAtencion = horario
                )
                repository.registrarTienda(tienda)

                _registerState.value = AuthState.Success("Comerciante")
            } catch (e: Exception) {
                _registerState.value = AuthState.Error(e.message ?: "Error al registrar comerciante")
            }
        }
    }



    sealed class AuthState{
        object Idle : AuthState() //sin accion
        object Loading : AuthState() //cargando
        data class Success(val rol: String) : AuthState() // Ahora recibe el rol
        data class Error (val message: String) : AuthState()
        data class VerificacionPendiente(val email: String): AuthState()

    }


    fun reenviarCorreoVerificacion(email: String) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            try {
                repository.reenviarCorreo(email)
                _registerState.value = AuthState.VerificacionPendiente(email)
            } catch (e: Exception) {
                _registerState.value = AuthState.Error("No se pudo reenviar: ${e.message}")
            }
        }
    }



}