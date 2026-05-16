package com.grupo3.donapp_access.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.features.lotes.dto.LoteDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.grupo3.donapp_access.features.lotes.LoteRepository


@HiltViewModel
class HomeUsuarioViewModel @Inject constructor(
    private val repository: LoteRepository
) : ViewModel() {

    private val _lotes = MutableStateFlow<List<LoteDTO>>(emptyList())
    val lotes: StateFlow<List<LoteDTO>> = _lotes

    fun fetchLotesVencenHoy() {
        viewModelScope.launch {

        }
    }
}