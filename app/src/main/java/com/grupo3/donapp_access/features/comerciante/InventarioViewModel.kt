package com.grupo3.donapp_access.features.comerciante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.grupo3.donapp_access.core.network.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.grupo3.donapp_access.model.Lote
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {


    // Estado para la lista del inventario (InventarioFragment)
    private val _lotesState = MutableStateFlow<UiState<List<Lote>>>(UiState.Loading)
    val lotesState: StateFlow<UiState<List<Lote>>> = _lotesState

    // Estado para autocompletar un solo lote en la edición (DetalleLoteFragment)
    private val _loteActual : MutableStateFlow<Lote?> = MutableStateFlow(null)
    val loteActual: StateFlow<Lote?> = _loteActual


    fun obtenerInventario(tiendaId: String) {
        viewModelScope.launch {
            _lotesState.value = UiState.Loading
            try {
                // ...código anterior...
                val lotes = SupabaseClient.client.from("lote")
                    .select(
                        Columns.raw(
                            "id_lote,productos_id,tiendas_id,numero_lote,cantidad," +
                                    "fecha_vencimiento,precio_normal,precio_oferta,estado," +
                                    "productos(nombre, imagen)" // <--- AGREGAMOS 'imagen' AQUÍ
                        )) {
                        filter { eq("tiendas_id", tiendaId) }
                    }.decodeList<LoteConProducto>()

                if (lotes.isEmpty() && (tiendaId == "test_seller_id" || tiendaId == "demo_id")) {
                    _lotesState.value = UiState.Success(generarLotesFalsos(tiendaId))
                } else {
                    _lotesState.value = UiState.Success(lotes.map { it.toLote() })
                }
            } catch (e: Exception) {
                if (tiendaId == "test_seller_id" || tiendaId == "demo_id") {
                    _lotesState.value = UiState.Success(generarLotesFalsos(tiendaId))
                } else {
                    _lotesState.value = UiState.Error(e.message ?: "Error al obtener el inventario")
                }
            }
        }
    }

    private fun generarLotesFalsos(tiendaId: String): List<Lote> {
        return listOf(
            Lote(
                id_lote = "mock_1",
                productos_id = "Arroz Extra",
                tiendas_id = tiendaId,
                numero_lote = "L-9921",
                cantidad = 45,
                fecha_vencimiento = "2024-12-20",
                precio_normal = 5.50,
                precio_oferta = 3.20,
                estado = "en_oferta"
            ),
            Lote(
                id_lote = "mock_2",
                productos_id = "Leche Entera",
                tiendas_id = tiendaId,
                numero_lote = "L-8840",
                cantidad = 12,
                fecha_vencimiento = "2024-06-15",
                precio_normal = 4.20,
                precio_oferta = null,
                estado = "disponible"
            ),
            Lote(
                id_lote = "mock_3",
                productos_id = "Aceite de Girasol",
                tiendas_id = tiendaId,
                numero_lote = "L-7712",
                cantidad = 0,
                fecha_vencimiento = "2024-11-10",
                precio_normal = 12.00,
                precio_oferta = 8.50,
                estado = "agotado"
            ),
            Lote(
                id_lote = "mock_4",
                productos_id = "Atún en Conserva",
                tiendas_id = tiendaId,
                numero_lote = "L-5532",
                cantidad = 120,
                fecha_vencimiento = "2025-01-05",
                precio_normal = 3.80,
                precio_oferta = 1.90,
                estado = "en_oferta"
            )
        )
    }

    /**
     * Obtiene los datos en tiempo real de un solo lote para llenar el formulario de edición.
     */
    fun cargarLoteSeleccionado(idLote: String) {
        viewModelScope.launch {
            try {
                if (idLote.startsWith("mock_")) {
                    val mockLote = generarLotesFalsos("test_seller_id").find { it.id_lote == idLote }
                    _loteActual.value = mockLote
                    return@launch
                }

                val lote = SupabaseClient.client.from("lote")
                    .select { filter { eq("id_lote", idLote) } }
                    .decodeSingle<Lote>()

                _loteActual.value = lote
            } catch (e: Exception) {
                _loteActual.value = null
            }
        }
    }


    // =========================================================================
    // FUNCIONES DE ESCRITURA (UPDATE / DELETE)
    // =========================================================================

    /**
     * Guarda los cambios del comerciante (cantidad y precios) y recalcula el estado.
     */
    fun actualizarDatosLote(idLote: String, tiendaId: String, cantidad: Int, precioNormal: Double, precioOferta: Double?) {
        viewModelScope.launch {
            try {
                val nuevoEstado = if (cantidad <= 0) "agotado" else if (precioOferta != null && precioOferta > 0) "en_oferta" else "disponible"

                SupabaseClient.client.from("lote").update(
                    {
                        set("cantidad", cantidad)
                        set("precio_normal", precioNormal)
                        set("precio_oferta", precioOferta)
                        set("estado", nuevoEstado)
                    }
                ) {
                    filter { eq("id_lote", idLote) }
                }
                // Recargamos la lista para que el cambio se vea en el inventario
                obtenerInventario(tiendaId)

                // NUEVO: Recargamos el lote para que la pantalla de detalles se actualice en vivo
                cargarLoteSeleccionado(idLote)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Botón de acción rápida: Baja el stock a 0 y cambia el estado a 'agotado'.
     */
    fun marcarComoAgotado(idLote: String, tiendaId: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.from("lote").update(
                    {
                        set("cantidad", 0)
                        set("estado", "agotado")
                    }
                ) {
                    filter { eq("id_lote", idLote) }
                }
                // Actualizamos la lista local
                obtenerInventario(tiendaId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Elimina el registro del lote definitivamente de la base de datos.
     */
    fun eliminarLote(idLote: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.from("lote").delete {
                    filter { eq("id_lote", idLote) }
                }
            } catch (e: Exception) {
                // Manejo de errores en caso de fallo en la red
            }
        }
    }


}

@Serializable
data class LoteConProducto(
    @SerialName("id_lote")           val id_lote: String,
    @SerialName("productos_id")      val productos_id: String,
    @SerialName("tiendas_id")        val tiendas_id: String,
    @SerialName("numero_lote")       val numero_lote: String? = null,
    @SerialName("cantidad")          val cantidad: Int,
    @SerialName("fecha_vencimiento") val fecha_vencimiento: String,
    @SerialName("precio_normal")     val precio_normal: Double,
    @SerialName("precio_oferta")     val precio_oferta: Double? = null,
    @SerialName("estado")            val estado: String = "disponible",
    @SerialName("productos")         val producto: ProductoRef? = null
) {
    // Convierte a Lote para mantener compatibilidad con el adapter
    fun toLote() = Lote(
        id_lote = id_lote,
        productos_id = producto?.nombre ?: productos_id,
        tiendas_id = tiendas_id,
        numero_lote = numero_lote,
        cantidad = cantidad,
        fecha_vencimiento = fecha_vencimiento,
        precio_normal = precio_normal,
        precio_oferta = precio_oferta,
        estado = estado,
        imagenUrl = producto?.imagen
    )
}

@Serializable
data class ProductoRef(
    @SerialName("nombre") val nombre: String,
    @SerialName("imagen") val imagen: String? = null
)