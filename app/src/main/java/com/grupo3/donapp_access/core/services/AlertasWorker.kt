package com.grupo3.donapp_access.core.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.app.MainActivity
import com.grupo3.donapp_access.features.auth.AuthRepository
import com.grupo3.donapp_access.features.comerciante.lotes.LoteRepository
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import com.grupo3.donapp_access.features.mapa.LocationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@HiltWorker
class AlertasWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val supabase: SupabaseClient,
    private val authRepo: AuthRepository,
    private val loteRepo: LoteRepository,
    private val clienteRepo: ClienteRepository,
    private val locationRepo: LocationRepository
) : CoroutineWorker(context, workerParams) {

    private val CHANNEL_ID = "donapp_alertas_background"

    override suspend fun doWork(): Result {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.success()

            val rol = authRepo.obtenerRolUsuario(userId)

            if (rol == "comerciante") {
                val alertasLotes = revisarLotesPorVencer(userId)
                if (alertasLotes > 0) {
                    mostrarNotificacionSistema(
                        titulo = "¡Atención Comerciante!",
                        mensaje = "Tienes $alertasLotes producto(s) que vencen hoy o mañana. ¡Revisa tu inventario y ponlos en oferta!"
                    )
                }
            } else {
                val infoOferta = revisarOfertasCercanas()
                if (infoOferta != null) {
                    mostrarNotificacionSistema(
                        titulo = "¡Remate de precios cerca!",
                        mensaje = "La tienda '${infoOferta.nombreTienda}' tiene ofertas a menos de 5km de ti.",
                        imageUrl = infoOferta.imageUrl,
                        tiendaId = infoOferta.tiendaId
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ALERTAS_WORKER", "Error en doWork: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun revisarLotesPorVencer(userId: String): Int {
        return try {
            val idTienda = loteRepo.getIdTiendaDelUsuario(userId)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val hoyStr = dateFormat.format(Date())

            val manana = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            val mananaStr = dateFormat.format(manana)

            val lotesPorVencer = supabase.from("lote")
                .select(Columns.raw("id_lote")) {
                    filter {
                        eq("id_tienda", idTienda)
                        isIn("estado", listOf("disponible", "en_oferta"))
                        lte("fecha_vencimiento", mananaStr)
                        gte("fecha_vencimiento", hoyStr)
                    }
                }.decodeList<LoteIdSolo>()

            lotesPorVencer.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun revisarOfertasCercanas(): InfoOfertaNotificacion? {
        return try {
            val ubicacion = locationRepo.obtenerUbicacionActual()

            val tiendasCercanas = clienteRepo.obtenerTiendasCercanas(
                latUsuario = ubicacion.latitude,
                lngUsuario = ubicacion.longitude,
                radioMetros = 5000
            )

            if (tiendasCercanas.isEmpty()) return null

            val todasLasOfertas = clienteRepo.obtenerOfertas()
            val idsTiendasCercanas = tiendasCercanas.map { it.idTienda }

            val ofertasCerca = todasLasOfertas.firstOrNull { oferta ->
                idsTiendasCercanas.contains(oferta.tiendaId)
            }

            ofertasCerca?.let {
                InfoOfertaNotificacion(it.tiendaId ?: "", it.tiendaNombre, it.productoImagen)
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getBitmapFromUrl(urlString: String?): Bitmap? {
        if (urlString.isNullOrEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                android.util.Log.e("ALERTAS_WORKER", "Error descargando imagen: ${e.message}")
                null
            }
        }
    }

    private suspend fun mostrarNotificacionSistema(titulo: String, mensaje: String, imageUrl: String? = null, tiendaId: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas en Segundo Plano",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (tiendaId != null) {
                putExtra("EXTRA_ACCION_NOTIFICACION", "RESERVAR_OFERTA")
                putExtra("EXTRA_TIENDA_ID", tiendaId)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            flags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.logo)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (tiendaId != null) {
            builder.addAction(
                0,
                "¡Reservar Producto!",
                pendingIntent
            )
        }

        val bitmapImagen = getBitmapFromUrl(imageUrl)
        if (bitmapImagen != null) {
            builder.setLargeIcon(bitmapImagen)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmapImagen)
                    .bigLargeIcon(null as Bitmap?)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    @Serializable
    private data class LoteIdSolo(val id_lote: String)

    private data class InfoOfertaNotificacion(val tiendaId: String, val nombreTienda: String, val imageUrl: String?)
}