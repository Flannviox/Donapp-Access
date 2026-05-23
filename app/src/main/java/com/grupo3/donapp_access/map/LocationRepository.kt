package com.grupo3.donapp_access.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority//que tan precisa queires la ubicacion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    //fusedlocationprovider combna varias fuentes
    //GPS
    //WIFI
    //Antenas
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)


    //No mostrar esta advertencia en especifico
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): Location {
        return suspendCancellableCoroutine { cont ->
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
            ).addOnSuccessListener { location ->
                if(location != null){
                    cont.resume(location)
                }else{
                    cont.resumeWithException(
                        Exception("No se pudo obtener la ubicación. Activa el GPS")
                    )
                }
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
        }
    }
}