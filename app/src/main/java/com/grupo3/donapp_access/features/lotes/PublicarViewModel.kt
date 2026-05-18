package com.grupo3.donapp_access.features.lotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.features.lotes.dto.CategoriaDTO
import com.grupo3.donapp_access.features.lotes.dto.LoteDTO
import com.grupo3.donapp_access.features.lotes.dto.ProductoDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PublicarViewModel @Inject constructor(
    private val repository: LoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PublicarState>(PublicarState.Idle)
    val state: StateFlow<PublicarState> = _state

    private val _categorias = MutableStateFlow<List<CategoriaDTO>>(emptyList())
    val categorias: StateFlow<List<CategoriaDTO>> = _categorias

    private val _productos = MutableStateFlow<List<ProductoDTO>>(emptyList())
    val productos: StateFlow<List<ProductoDTO>> = _productos

    private var idTiendaCache: String? = null
    private var idProductoSeleccionado: String? = null

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _state.value = PublicarState.Loading
            try {
                _categorias.value = repository.fetchCategorias()
                _productos.value = repository.fetchProductos()

                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (uid == null) {
                    _state.value = PublicarState.Error("No hay sesión activa.")
                    return@launch
                }
                idTiendaCache = repository.getIdTiendaDelUsuario(uid)

                _state.value = PublicarState.Idle
                android.util.Log.d("PUBLICAR_VM", "Datos iniciales cargados. id_tienda=$idTiendaCache")
            } catch (e: Exception) {
                android.util.Log.e("PUBLICAR_VM", "Error cargando datos iniciales: ${e.message}", e)
                _state.value = PublicarState.Error(e.message ?: "Error cargando datos iniciales.")
            }
        }
    }

    fun seleccionarProductoExistente(idProducto: String) {
        idProductoSeleccionado = idProducto
        _state.value = PublicarState.ProductoCreado(idProducto)
        android.util.Log.d("PUBLICAR_VM", "Producto existente seleccionado: $idProducto")
    }

    fun crearProductoNuevo(
        nombre: String,
        categoriaId: String,
        imagenBase64: String, // Se cambió a String para recibir Base64
        extension: String
    ) {
        val idTienda = idTiendaCache
        if (idTienda == null) {
            _state.value = PublicarState.Error("No se pudo identificar tu tienda. Reintentá.")
            return
        }

        viewModelScope.launch {
            _state.value = PublicarState.Loading
            try {
                // Pasamos el String de Base64 al repositorio
                val urlImagen = repository.uploadImagenProducto(imagenBase64, idTienda, extension)

                val dto = ProductoDTO(
                    categoriaId = categoriaId,
                    nombre = nombre,
                    descripcion = null,
                    presentacion = null,
                    imagen = urlImagen
                )

                val idProducto = repository.insertarProducto(dto)
                idProductoSeleccionado = idProducto
                _state.value = PublicarState.ProductoCreado(idProducto)
                android.util.Log.d("PUBLICAR_VM", "Producto creado: $idProducto")
            } catch (e: Exception) {
                android.util.Log.e("PUBLICAR_VM", "Error creando producto: ${e.message}", e)
                _state.value = PublicarState.Error("No se pudo crear el producto: ${e.message ?: "error desconocido"}")
            }
        }
    }
    fun publicarLote(
        cantidad: Int,
        fechaVencimiento: String,
        precioNormal: Double,
        precioOferta: Double?
    ) {
        val idTienda = idTiendaCache
        val idProducto = idProductoSeleccionado

        if (idTienda == null) {
            _state.value = PublicarState.Error("No se pudo identificar tu tienda. Reintentá.")
            return
        }
        if (idProducto == null) {
            _state.value = PublicarState.Error("Primero seleccioná o creá un producto.")
            return
        }

        val estado = if (precioOferta != null && precioOferta > 0) "en_oferta" else "disponible"
        val fechaLimiteOferta =
            if (precioOferta != null && precioOferta > 0) fechaVencimiento else null
        val numeroLote = "LOTE-${UUID.randomUUID().toString().take(6).uppercase()}"

        viewModelScope.launch {
            _state.value = PublicarState.Loading
            try {
                val dto = LoteDTO(
                    productosId = idProducto,
                    tiendasId = idTienda,
                    numeroLote = numeroLote,
                    cantidad = cantidad,
                    fechaVencimiento = fechaVencimiento,
                    precioNormal = precioNormal,
                    precioOferta = precioOferta,
                    fechaLimiteOferta = fechaLimiteOferta,
                    estado = estado
                )

                repository.insertarLote(dto)
                _state.value = PublicarState.LotePublicado
                android.util.Log.d("PUBLICAR_VM", "Lote publicado: $numeroLote ($estado)")
            } catch (e: Exception) {
                android.util.Log.e("PUBLICAR_VM", "Error publicando lote: ${e.message}", e)
                _state.value = PublicarState.Error("No se pudo publicar el lote: ${e.message ?: "error desconocido"}")
            }
        }
    }

    fun resetearEstado() {
        _state.value = PublicarState.Idle
    }

    sealed class PublicarState {
        object Idle : PublicarState()
        object Loading : PublicarState()
        data class ProductoCreado(val idProducto: String) : PublicarState()
        object LotePublicado : PublicarState()
        data class Error(val mensaje: String) : PublicarState()
    }
}
