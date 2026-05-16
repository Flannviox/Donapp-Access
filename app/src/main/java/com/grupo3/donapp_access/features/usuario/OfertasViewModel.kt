package com.grupo3.donapp_access.features.usuario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.model.OfertaLote
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class OfertasViewModel : ViewModel() {
    private val repository = ClienteRepository()

    private val _ofertas = MutableStateFlow<UiState<List<OfertaLote>>>(UiState.Loading)

    val ofertas: StateFlow<UiState<List<OfertaLote>>> = _ofertas

    fun cargarOfertas() {
        viewModelScope.launch {
            _ofertas.value = UiState.Loading
            try {
                val resultado = repository.obtenerOfertas()
                _ofertas.value = UiState.Success(resultado)

            }catch (e: Exception){
                _ofertas.value = UiState.Error(
                    e.message ?: "No se pudieron cargar las ofertas"
                )
            }
        }

    }


}
