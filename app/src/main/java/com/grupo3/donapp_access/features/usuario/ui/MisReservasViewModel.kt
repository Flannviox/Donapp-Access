package com.grupo3.donapp_access.features.usuario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.core.network.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltViewModel
class MisReservasViewModel @Inject constructor() : ViewModel() {

    private val _reservasState = MutableStateFlow<UiState<List<ReservaDetalle>>>(UiState.Loading)
    val reservasState: StateFlow<UiState<List<ReservaDetalle>>> = _reservasState

    // Control para no suscribirnos varias veces si el usuario sale y vuelve a entrar
    private var suscritoARealtime = false

    fun cargarMisReservas() {
        viewModelScope.launch {
            // Solo mostramos Loading si es la primera vez que carga
            if (_reservasState.value !is UiState.Success) {
                _reservasState.value = UiState.Loading
            }

            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _reservasState.value = UiState.Error("Usuario no autenticado")
                    return@launch
                }

                // 1. Cargamos los datos normales iniciales
                obtenerDatosDeSupabase(userId)

                // 2. Nos suscribimos a cambios en vivo
                if (!suscritoARealtime) {
                    suscribirseACambiosEnVivo(userId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _reservasState.value = UiState.Error(e.message ?: "Error al cargar reservas")
            }
        }
    }

    private suspend fun obtenerDatosDeSupabase(userId: String) {
        val response = SupabaseClient.client.from("reservas")
            .select(
                Columns.raw("id_reservas, cantidad, estado, fecha_expiracion, lote(productos(nombre), tiendas(nombre))")
            ) {
                filter { eq("usuario_id", userId) }
            }.decodeList<ReservaResponseDTO>()

        // AQUÍ ESTÁ EL CAMBIO: Ordenamos la lista antes de enviarla a la vista
        val reservasMapeadas = response.map { it.toDetalle() }.sortedByDescending { it.fecha_expiracion }

        _reservasState.value = UiState.Success(reservasMapeadas)
    }

    private suspend fun suscribirseACambiosEnVivo(userId: String) {
        try {
            suscritoARealtime = true

            // Creamos un canal exclusivo para escuchar
            val channel = SupabaseClient.client.channel("reservas_cliente_$userId")

            // SOLUCIÓN: Quitamos el filtro "usuario_id=eq.$userId"
            // Ahora escuchará los UPDATEs de la tabla sin ser bloqueado por Supabase
            val cambios = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "reservas"
            }

            cambios.onEach {
                // En cuanto alguien actualice una reserva en la base de datos,
                // nosotros actualizamos visualmente las nuestras de inmediato
                obtenerDatosDeSupabase(userId)
            }.launchIn(viewModelScope)

            // Conectamos y empezamos a escuchar
            SupabaseClient.client.realtime.connect()
            channel.subscribe()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// --- DTOs PRIVADOS ---
@Serializable
data class ReservaResponseDTO(
    val id_reservas: String,
    val cantidad: Int,
    @SerialName("estado") val estado: String? = "activa",
    val fecha_expiracion: String? = null,
    val lote: LoteReservaDTO? = null
) {
    fun toDetalle() = ReservaDetalle(
        id_reservas = id_reservas,
        cantidad = cantidad,
        estado = estado,
        fecha_expiracion = fecha_expiracion,
        nombreProducto = lote?.productos?.nombre,
        nombreTienda = lote?.tiendas?.nombre
    )
}

@Serializable
data class LoteReservaDTO(
    val productos: ProductoRefDTO? = null,
    val tiendas: TiendaRefDTO? = null
)

@Serializable
data class ProductoRefDTO(val nombre: String)

@Serializable
data class TiendaRefDTO(val nombre: String)