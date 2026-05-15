package com.grupo3.donapp_access.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.donapp_access.features.auth.models.Tienda
import com.grupo3.donapp_access.features.map.TiendaConLotes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


//Este viewModel será administrado automaticamente

@HiltViewModel
//hilt injecta locationRepository y TiendaDao
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val tiendaDao: TiendaDao
): ViewModel() {
    private val _ubicacionState = MutableStateFlow<UbicacionState>(UbicacionState.Idle)
    val ubicacionState : StateFlow<UbicacionState> = _ubicacionState


    private val _tiendasState = MutableStateFlow<TiendasState>(TiendasState.Idle)
    val tiendasState : StateFlow<TiendasState> = _tiendasState


    fun obtenerUbicacionYTiendas(){
        viewModelScope.launch {
            _ubicacionState.value = UbicacionState.Loading

            try {
                val location = locationRepository.obtenerUbicacionActual()
                _ubicacionState.value = UbicacionState.Success(location)

                cargarTiendasCercanas(location.latitude, location.longitude)



            }catch (e: Exception){
                _ubicacionState.value = UbicacionState.Error(
                    e.message ?: "Error al obtener ubicación"
                )

            }
        }
    }

    private suspend fun cargarTiendasCercanas(lat: Double, lng: Double){
        _tiendasState.value = TiendasState.Loading

        try {
            val tiendas = tiendaDao.getTiendasCercanas(lat, lng)
            _tiendasState.value = TiendasState.Success(tiendas)
        }catch (e: Exception){
            _tiendasState.value = TiendasState.Error(
                e.message ?: "Error al cargar tiendas"
            )
        }
    }


    sealed class UbicacionState {
        object Idle : UbicacionState()
        object Loading : UbicacionState()
        data class Success(val location: Location) : UbicacionState()
        data class Error(val message: String) : UbicacionState()
    }

    // Estados de tiendas
    sealed class TiendasState {
        object Idle : TiendasState()
        object Loading : TiendasState()
        data class Success(val tiendas: List<TiendaConLotes>) : TiendasState()
        data class Error(val message: String) : TiendasState()
    }
}




