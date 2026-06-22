package com.grupo3.donapp_access.features.usuario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.core.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TiendaDetailViewModel : ViewModel() {

    private val clienteRepository = ClienteRepository()

    private val _reservaState = MutableStateFlow<UiState<Unit>?>(null)
    val reservaState: StateFlow<UiState<Unit>?> = _reservaState

    fun reservarLote(usuarioId: String, loteId: String, cantidad: Int) {
        viewModelScope.launch {
            _reservaState.value = UiState.Loading

            val resultado = clienteRepository.crearReserva(usuarioId, loteId, cantidad)

            if (resultado.isSuccess) {
                _reservaState.value = UiState.Success(Unit)
            } else {
                _reservaState.value = UiState.Error(resultado.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun limpiarEstadoReserva() {
        _reservaState.value = null
    }
}