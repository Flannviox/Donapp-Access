package com.grupo3.donapp_access.features.usuario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.model.Categoria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class BuscarViewModel : ViewModel() {
    private val repository = ClienteRepository()
    private val categoriasCache = mutableListOf<Categoria>()

    private val _categorias = MutableStateFlow<UiState<List<Categoria>>>(UiState.Loading)
    val categorias: StateFlow<UiState<List<Categoria>>> = _categorias
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
        val filtro = texto.trim().lowercase()
        val resultado = if (filtro.isBlank()) {
            categoriasCache
        } else {
            categoriasCache.filter { it.nombre.lowercase().contains(filtro) }
        }
        _categorias.value = UiState.Success(resultado)
    }


    private fun categoriasFigma(): List<Categoria> = listOf(
        Categoria("demo-panaderia", "Panaderia", "ACTIVO", 12),
        Categoria("demo-lacteos", "Lacteos", "ACTIVO", 18),
        Categoria("demo-frutas", "Frutas", "ACTIVO", 8),
        Categoria("demo-abarrotes", "Abarrotes", "ACTIVO", 25),
        Categoria("demo-granja", "Granja", "ACTIVO", 6),
        Categoria("demo-bebidas", "Bebidas", "ACTIVO", 15)
    )
}
