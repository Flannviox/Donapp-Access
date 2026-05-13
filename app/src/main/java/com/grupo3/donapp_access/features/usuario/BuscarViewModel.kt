package com.grupo3.donapp_access.features.usuario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.model.Categoria
import java.util.concurrent.Executors

class BuscarViewModel : ViewModel() {
    private val repository = ClienteRepository()
    private val executor = Executors.newSingleThreadExecutor()
    private val categoriasCache = mutableListOf<Categoria>()

    private val _categorias = MutableLiveData<UiState<List<Categoria>>>()
    val categorias: LiveData<UiState<List<Categoria>>> = _categorias

    fun cargarCategorias() {
        _categorias.value = UiState.Loading
        executor.execute {
            runCatching { repository.obtenerCategorias() }
                .onSuccess { categorias ->
                    categoriasCache.clear()
                    categoriasCache.addAll(categorias)
                    _categorias.postValue(UiState.Success(categorias))
                }
                .onFailure { error ->
                    _categorias.postValue(
                        UiState.Error(error.message ?: "No se pudieron cargar las categorias")
                    )
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

    override fun onCleared() {
        executor.shutdown()
        super.onCleared()
    }
}
