package com.grupo3.donapp_access.core.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import com.grupo3.donapp_access.model.TiendaHome
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.*
import java.util.Locale

class GeofenceService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository = ClienteRepository()

    private var listaTiendas = listOf<TiendaHome>()
    private val tiendasNotificadas = mutableSetOf<String>()

    private val RADIO_ALERTA_KM = 1.0
    private val CHANNEL_ID = "donapp_geofence_channel"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForegroundService()

        serviceScope.launch {
            try {
                listaTiendas = repository.obtenerTiendas()
            } catch (e: Exception) {
                android.util.Log.e("GeofenceService", "Error: ${e.message}")
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000)
            .setMinUpdateDistanceMeters(20f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val userLocation = locationResult.lastLocation ?: return
                verificarCercaniaTiendas(userLocation.latitude, userLocation.longitude)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun verificarCercaniaTiendas(userLat: Double, userLng: Double) {
        val userPoint = Point.fromLngLat(userLng, userLat)

        for (tienda in listaTiendas) {
            val tiendaLat = tienda.latitud ?: continue
            val tiendaLng = tienda.longitud ?: continue
            val tiendaId = tienda.idTienda ?: continue

            val tiendaPoint = Point.fromLngLat(tiendaLng, tiendaLat)

            val distanciaKm = TurfMeasurement.distance(userPoint, tiendaPoint, "kilometers")

            if (distanciaKm <= RADIO_ALERTA_KM && !tiendasNotificadas.contains(tiendaId)) {
                dispararNotificacion(tienda, distanciaKm)
                tiendasNotificadas.add(tiendaId)
            } else if (distanciaKm > RADIO_ALERTA_KM + 0.5) {
                tiendasNotificadas.remove(tiendaId)
            }
        }
    }

    private fun dispararNotificacion(tienda: TiendaHome, distancia: Double) {
        val intent = Intent(this, MainActivity::class.java).apply{
            action = "ACCION_MAPA_${tienda.idTienda}"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navegar_a", "mapa")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            tienda.idTienda.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val textoDistancia = String.format(Locale.US, "%.1f", distancia)
        val mensajeHistorial = "La tienda '${tienda.nombre}' está a $textoDistancia km con productos para salvar."

        try {
            val sharedPreferences = getSharedPreferences("donapp_alertas", MODE_PRIVATE)
            val historialSet = sharedPreferences.getStringSet("historial", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

            val timestamp = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(java.util.Date())


            val tiendaIdReal = tienda.idTienda ?: "1"
            historialSet.add("$timestamp|$mensajeHistorial|$tiendaIdReal")

            sharedPreferences.edit().putStringSet("historial", historialSet).apply()
        } catch (e: Exception) {
            android.util.Log.e("GeofenceService", "Error guardando historial local: ${e.message}")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("¡Ofertas cerca de ti! 🍏")
            .setContentText(mensajeHistorial) // Cambiado para usar la misma variable limpia
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(tienda.idTienda.hashCode(), notification)
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Donapp activo")
            .setContentText("Buscando comida cerca a tu posición...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(9999, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Alertas Donapp", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}