package com.grupo3.donapp_access.features.usuario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.core.network.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltViewModel
class MisReservasViewModel @Inject constructor() : ViewModel() {

    private val _reservasState = MutableStateFlow<UiState<List<ReservaDetalle>>>(UiState.Loading)
    val reservasState: StateFlow<UiState<List<ReservaDetalle>>> = _reservasState

    fun cargarMisReservas() {
        viewModelScope.launch {
            _reservasState.value = UiState.Loading
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _reservasState.value = UiState.Error("Usuario no autenticado")
                    return@launch
                }

                // FIX: 'estado' en minúscula
                val response = SupabaseClient.client.from("reservas")
                    .select(
                        Columns.raw("id_reservas, cantidad, estado, fecha_expiracion, lote(productos(nombre), tiendas(nombre))")
                    ) {
                        filter { eq("usuario_id", userId) }
                    }.decodeList<ReservaResponseDTO>()

                val reservasMapeadas = response.map { it.toDetalle() }
                _reservasState.value = UiState.Success(reservasMapeadas)

            } catch (e: Exception) {
                e.printStackTrace()
                _reservasState.value = UiState.Error(e.message ?: "Error al cargar reservas")
            }
        }
    }
}

// --- DTOs PRIVADOS ---
@Serializable
data class ReservaResponseDTO(
    val id_reservas: String,
    val cantidad: Int,
    @SerialName("estado") val estado: String? = "activa", // FIX: 'estado' en minúscula
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