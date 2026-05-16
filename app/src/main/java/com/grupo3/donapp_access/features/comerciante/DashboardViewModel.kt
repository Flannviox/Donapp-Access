package com.grupo3.donapp_access.features.comerciante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class DashboardViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _nombreTienda = MutableStateFlow("Cargando...")
    val nombreTienda: StateFlow<String> = _nombreTienda

    private val _lotesActivos = MutableStateFlow<List<Lote>>(emptyList())
    val lotesActivos: StateFlow<List<Lote>> = _lotesActivos

    private val _totalLotes = MutableStateFlow(0)
    val totalLotes: StateFlow<Int> = _totalLotes

    private val _clientesAlcanzados = MutableStateFlow(142) // Simulado
    val clientesAlcanzados: StateFlow<Int> = _clientesAlcanzados

    fun cargarDatosDashboard(tiendaId: String) {
        viewModelScope.launch {
            if (tiendaId == "test_seller_id" || tiendaId == "demo_id") {
                _nombreTienda.value = "Tienda de Prueba (Demo)"
                _totalLotes.value = 4
                _lotesActivos.value = listOf(
                    Lote("mock_1", "Arroz Extra", tiendaId, "L-9921", 45, "2024-12-20", 5.5, 3.2, null, "en_oferta"),
                    Lote("mock_2", "Leche Entera", tiendaId, "L-8840", 12, "2024-06-15", 4.2, null, null, "disponible"),
                    Lote("mock_4", "Atún en Conserva", tiendaId, "L-5532", 120, "2025-01-05", 3.8, 1.9, null, "en_oferta")
                )
                _clientesAlcanzados.value = 142
                return@launch
            }

            try {
                // 1. Obtener el nombre de la tienda
                val tienda = supabase.client.from("tiendas")
                    .select { filter { eq("usuarios_id", tiendaId) } }
                    .decodeSingleOrNull<TiendaSimple>()

                _nombreTienda.value = tienda?.nombre  ?: "Mi Bodega"

                // 2. Obtener los lotes activos
                val idTiendaReal = tienda?.idTienda?: return@launch
                val lotes = SupabaseClient.client.from("lote")
                    .select (
                        Columns.raw(
                        "id_lote,productos_id,tiendas_id,numero_lote,cantidad," +
                                "fecha_vencimiento,precio_normal,precio_oferta,estado," +
                                "productos(nombre)"
                    )){
                        filter {
                            eq("tiendas_id", idTiendaReal)
                            neq("estado", "agotado")
                        }
                    }.decodeList<LoteConProducto>()

                _lotesActivos.value = lotes.map { it.toLote() }
                _totalLotes.value = lotes.size

            } catch (e: Exception) {
                _nombreTienda.value = "Error al cargar"
            }
        }
    }
}

@Serializable
private data class TiendaSimple(
    @SerialName("id_tienda") val idTienda: String,
    @SerialName("nombre")    val nombre: String
)