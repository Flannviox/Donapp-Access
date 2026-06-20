package com.grupo3.donapp_access.features.comerciante.ui

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
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltViewModel
class ReservasComercianteViewModel @Inject constructor() : ViewModel() {

    private val _reservasState = MutableStateFlow<UiState<List<ReservaComercianteDetalle>>>(UiState.Loading)
    val reservasState: StateFlow<UiState<List<ReservaComercianteDetalle>>> = _reservasState

    fun cargarReservasDeMiTienda() {
        viewModelScope.launch {
            _reservasState.value = UiState.Loading
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _reservasState.value = UiState.Error("Comerciante no autenticado")
                    return@launch
                }

                // 1. Obtener el ID de la tienda (¡CORREGIDO: usuarios_id!)
                val tienda = SupabaseClient.client.from("tiendas")
                    .select(Columns.raw("id_tienda")) {
                        filter { eq("usuarios_id", userId) }
                    }.decodeSingleOrNull<TiendaIdDTO>()

                if (tienda == null) {
                    _reservasState.value = UiState.Error("No se encontró una tienda asociada a este usuario.")
                    return@launch
                }

                // 2. Obtener los IDs de todos los lotes de esta tienda (¡CORREGIDO: tiendas_id!)
                val lotes = SupabaseClient.client.from("lote")
                    .select(Columns.raw("id_lote")) {
                        filter { eq("tiendas_id", tienda.id_tienda) }
                    }.decodeList<LoteIdDTO>()

                if (lotes.isEmpty()) {
                    _reservasState.value = UiState.Success(emptyList())
                    return@launch
                }
                val loteIds = lotes.map { it.id_lote }

                // 3. Obtener las reservas cruzando con lote->producto y usuarios
                val response = SupabaseClient.client.from("reservas")
                    .select(
                        Columns.raw("id_reservas, cantidad, estado, fecha_expiracion, lote(productos(nombre)), usuarios(nombres, apellidos)")
                    ) {
                        filter { isIn("id_lote", loteIds) }
                    }.decodeList<ReservaComResponseDTO>()

                // Mapear al DTO y ordenar para que las "activas" salgan arriba
                val reservasMapeadas = response.map { it.toDetalle() }
                val reservasOrdenadas = reservasMapeadas.sortedBy { if (it.estado == "activa") 0 else 1 }

                _reservasState.value = UiState.Success(reservasOrdenadas)

            } catch (e: Exception) {
                e.printStackTrace()
                _reservasState.value = UiState.Error(e.message ?: "Error al cargar las reservas de tu negocio")
            }
        }
    }

    fun marcarReservaComoEntregada(idReserva: String) {
        viewModelScope.launch {
            try {
                // Actualizamos el estado a completada en Supabase
                SupabaseClient.client.from("reservas")
                    .update(
                        {
                            set("estado", "completada")
                        }
                    ) {
                        filter { eq("id_reservas", idReserva) }
                    }
                // Volvemos a cargar la lista para que la tarjeta cambie a color verde
                cargarReservasDeMiTienda()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// --- DTOs PRIVADOS PARA MAPEAR LA RESPUESTA JSON ---
@Serializable
private data class TiendaIdDTO(val id_tienda: String)

@Serializable
private data class LoteIdDTO(val id_lote: String)

@Serializable
private data class ReservaComResponseDTO(
    val id_reservas: String,
    val cantidad: Int,
    val estado: String? = "activa",
    val fecha_expiracion: String? = null,
    val lote: LoteResComDTO? = null,
    val usuarios: UsuarioResComDTO? = null
) {
    fun toDetalle() = ReservaComercianteDetalle(
        id_reservas = id_reservas,
        cantidad = cantidad,
        estado = estado,
        fecha_expiracion = fecha_expiracion,
        nombreProducto = lote?.productos?.nombre,
        nombreCliente = "${usuarios?.nombres ?: ""} ${usuarios?.apellidos ?: ""}".trim()
    )
}

@Serializable
private data class LoteResComDTO(val productos: ProductoResComDTO? = null)

@Serializable
private data class ProductoResComDTO(val nombre: String)

@Serializable
private data class UsuarioResComDTO(val nombres: String? = null, val apellidos: String? = null)