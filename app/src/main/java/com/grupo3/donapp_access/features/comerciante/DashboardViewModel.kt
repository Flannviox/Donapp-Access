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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _nombreTienda = MutableStateFlow("Cargando...")
    val nombreTienda: StateFlow<String> = _nombreTienda

    private val _imagenTienda = MutableStateFlow<String?>(null)
    val imagenTienda: StateFlow<String?> = _imagenTienda

    private val _lotesActivos = MutableStateFlow<List<Lote>>(emptyList())
    val lotesActivos: StateFlow<List<Lote>> = _lotesActivos

    private val _totalLotes = MutableStateFlow(0)
    val totalLotes: StateFlow<Int> = _totalLotes

    private val _clientesAlcanzados = MutableStateFlow(0) // Nota: Esta métrica sigue siendo un número estático, puedes conectarla luego
    val clientesAlcanzados: StateFlow<Int> = _clientesAlcanzados

    private val _mensajeAlerta = MutableStateFlow<String?>(null)
    val mensajeAlerta: StateFlow<String?> = _mensajeAlerta
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val hoy = sdf.format(Date())

    fun cargarDatosDashboard(tiendaId: String) {
        viewModelScope.launch {
            try {
                val tienda = supabase.client.from("tiendas")
                    .select { filter { eq("usuarios_id", tiendaId) } }
                    .decodeSingleOrNull<TiendaSimple>()

                _nombreTienda.value = tienda?.nombre ?: "Mi Bodega"
                _imagenTienda.value = tienda?.imagenReferencia



                val idTiendaReal = tienda?.idTienda ?: return@launch
                val lotes = SupabaseClient.client.from("lote")

                    .select (
                        Columns.raw(
                            "id_lote,productos_id,tiendas_id,numero_lote,cantidad," +
                                    "fecha_vencimiento,precio_normal,precio_oferta,estado," +
                                    "productos(nombre, imagen)"
                        )){
                        filter {
                            eq("tiendas_id", idTiendaReal)
                            neq("estado", "agotado")
                            neq("estado", "vencido")
                            gte("fecha_vencimiento", hoy)
                        }
                    }.decodeList<LoteConProducto>()

                _lotesActivos.value = lotes.map { it.toLote() }
                _totalLotes.value = lotes.size

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val hoy = Date()
                var lotesPorVencer = 0

                lotes.forEach { loteDto ->
                    try {
                        val fechaVenc = sdf.parse(loteDto.fecha_vencimiento)
                        if (fechaVenc != null) {
                            val diffMillis = fechaVenc.time - hoy.time
                            val dias = TimeUnit.MILLISECONDS.toDays(diffMillis)

                            // Si el producto vence en 3 días o menos (incluso si ya venció)
                            if (dias <= 3) {
                                lotesPorVencer++
                            }
                        }
                    } catch (e: Exception) {
                    }
                }

                if (lotesPorVencer > 0) {
                    _mensajeAlerta.value = "Tienes $lotesPorVencer lote(s) próximo(s) a vencer o ya vencidos. ¡Revisa tu inventario!"
                } else {
                    _mensajeAlerta.value = null
                }

            } catch (e: Exception) {
                _nombreTienda.value = "Error al cargar"
            }
        }
    }
}

@Serializable
private data class TiendaSimple(
    @SerialName("id_tienda") val idTienda: String,
    @SerialName("nombre")    val nombre: String,
    @SerialName("imagen_referencia") val imagenReferencia: String? = null
)