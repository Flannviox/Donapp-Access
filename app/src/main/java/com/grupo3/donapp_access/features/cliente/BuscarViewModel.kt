package com.grupo3.donapp_access.features.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.data.model.Categoria
import com.grupo3.donapp_access.data.model.OfertaLote
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BuscarViewModel : ViewModel() {
    private val repository = ClienteRepository()
    private val categoriasCache = mutableListOf<Categoria>()

    private val _categorias = MutableStateFlow<UiState<List<Categoria>>>(UiState.Loading)
    val categorias: StateFlow<UiState<List<Categoria>>> = _categorias



    private val _ofertasBusqueda = MutableStateFlow<UiState<List<OfertaLote>>>(UiState.Success(emptyList()))
    val ofertasBusqueda: StateFlow<UiState<List<OfertaLote>>> = _ofertasBusqueda

    private var searchJob: Job? = null

    fun cargarCategorias() {
        viewModelScope.launch {
            _categorias.value = UiState.Loading
            try {
                val resultado = repository.obtenerCategorias()
                val data = resultado.ifEmpty { categoriasFigma() }
                categoriasCache.clear()
                categoriasCache.addAll(data)
                _categorias.value = UiState.Success(data)
            }catch (e: Exception){
                categoriasCache.clear()
                categoriasCache.addAll(categoriasFigma())
                _categorias.value = UiState.Success(categoriasFigma())
            }
        }

    }

    fun filtrarCategorias(texto: String) {
        val filtro = normalizarTexto(texto)

        val resultado = if (filtro.isBlank()) {
            categoriasCache
        } else {
            categoriasCache.filter {
                normalizarTexto(it.nombre).contains(filtro)
            }
        }
        _categorias.value = UiState.Success(resultado)
    }
    private fun normalizarTexto(texto: String): String {
        return texto.trim().lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
    }


    private fun categoriasFigma(): List<Categoria> = listOf(
        Categoria("demo-panaderia", "Panaderia", "ACTIVO", 12),
        Categoria("demo-lacteos", "Lacteos", "ACTIVO", 18),
        Categoria("demo-frutas", "Frutas", "ACTIVO", 8),
        Categoria("demo-abarrotes", "Abarrotes", "ACTIVO", 25),
        Categoria("demo-granja", "Granja", "ACTIVO", 6),
        Categoria("demo-bebidas", "Bebidas", "ACTIVO", 15)
    )



    fun buscarOfertas(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _ofertasBusqueda.value = UiState.Success(emptyList())
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce: espera medio segundo después de la última tecla
            _ofertasBusqueda.value = UiState.Loading
            try {
                val resultados = repository.buscarOfertas(query)
                _ofertasBusqueda.value = UiState.Success(resultados)
            } catch (e: Exception) {
                e.printStackTrace()
                _ofertasBusqueda.value = UiState.Error(e.message ?: "Error al buscar ofertas")
            }
        }
    }
}
