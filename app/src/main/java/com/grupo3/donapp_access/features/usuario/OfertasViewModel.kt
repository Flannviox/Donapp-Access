package com.grupo3.donapp_access.features.usuario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.model.OfertaLote
import java.util.concurrent.Executors

class OfertasViewModel : ViewModel() {
    private val repository = ClienteRepository()
    private val executor = Executors.newSingleThreadExecutor()

    private val _ofertas = MutableLiveData<UiState<List<OfertaLote>>>()
    val ofertas: LiveData<UiState<List<OfertaLote>>> = _ofertas

    fun cargarOfertas() {
        _ofertas.value = UiState.Loading
        executor.execute {
            runCatching { repository.obtenerOfertas() }
                .onSuccess { ofertas ->
                    _ofertas.postValue(UiState.Success(ofertas))
                }
                .onFailure { error ->
                    _ofertas.postValue(
                        UiState.Error(error.message ?: "No se pudieron cargar las ofertas")
                    )
                }
        }
    }

    override fun onCleared() {
        executor.shutdown()
        super.onCleared()
    }
}
