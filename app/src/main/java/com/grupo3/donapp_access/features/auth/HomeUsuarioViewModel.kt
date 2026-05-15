package com.grupo3.donapp_access.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.features.auth.dto.LoteDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeUsuarioViewModel @Inject constructor(
    private val repository: LoteRepository
) : ViewModel() {

    private val _lotes = MutableStateFlow<List<LoteDTO>>(emptyList())
    val lotes: StateFlow<List<LoteDTO>> = _lotes

    fun fetchLotesVencenHoy() {
        viewModelScope.launch {
            try {
                Log.d("HOME_VM", "Iniciando consulta a Supabase...")
                val result = repository.getLotesVencenHoy()

                // Imprimimos cuántos llegaron, aunque sean 0
                Log.d("HOME_VM", "Consulta exitosa. Lotes recibidos: ${result.size}")

                _lotes.value = result
            } catch (e: Exception) {
                // Imprimimos el error EXACTO si algo falla al mapear el JSON
                Log.e("HOME_VM", "Fallo catastrófico en la consulta: ", e)
            }
        }
    }
}