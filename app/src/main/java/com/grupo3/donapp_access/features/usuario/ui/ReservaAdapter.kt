package com.grupo3.donapp_access.features.usuario.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemReservaCardBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReservaAdapter(
    private val onVerQrClick: (ReservaDetalle) -> Unit
) : ListAdapter<ReservaDetalle, ReservaAdapter.ReservaViewHolder>(ReservaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding = ItemReservaCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ¡Vital! Cancelamos el reloj si la tarjeta se oculta al hacer scroll para evitar bugs visuales
    override fun onViewRecycled(holder: ReservaViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelTimer()
    }

    inner class ReservaViewHolder(private val binding: ItemReservaCardBinding) : RecyclerView.ViewHolder(binding.root) {
        private var timer: CountDownTimer? = null

        fun bind(reserva: ReservaDetalle) {
            binding.tvProductoReserva.text = reserva.nombreProducto ?: "Producto Reservado"
            binding.tvTiendaReserva.text = "Tienda: ${reserva.nombreTienda ?: "Local"} | Cantidad: ${reserva.cantidad}"

            binding.btnVerQR.setOnClickListener { onVerQrClick(reserva) }

            // Configurar colores y visibilidad según el estado
            val estado = reserva.estado?.lowercase() ?: "activa"
            binding.tvEstadoTexto.text = estado.uppercase()

            when (estado) {
                "activa" -> {
                    binding.tvEstadoTexto.setTextColor(Color.parseColor("#1A1A1A"))
                    binding.tvEstadoTexto.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5C518")) // Amarillo
                    binding.btnVerQR.visibility = View.VISIBLE
                    iniciarTemporizador(reserva.fecha_expiracion)
                }
                "completada" -> {
                    binding.tvEstadoTexto.setTextColor(Color.WHITE)
                    binding.tvEstadoTexto.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50")) // Verde
                    binding.tvTemporizador.text = "Entregado"
                    binding.btnVerQR.visibility = View.GONE
                    cancelTimer()
                }
                else -> { // Expirada
                    binding.tvEstadoTexto.setTextColor(Color.WHITE)
                    binding.tvEstadoTexto.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336")) // Rojo
                    binding.tvTemporizador.text = "Tiempo Agotado"
                    binding.btnVerQR.visibility = View.GONE
                    cancelTimer()
                }
            }
        }

        private fun iniciarTemporizador(fechaExpiracionStr: String?) {
            cancelTimer()
            if (fechaExpiracionStr == null) {
                binding.tvTemporizador.text = "00:00"
                return
            }

            try {
                // FIX PROACTIVO: Cortamos los milisegundos y la zona horaria extraña de Supabase
                val fechaLimpia = fechaExpiracionStr.substringBefore(".").substringBefore("+")

                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                val dateExpiracion = format.parse(fechaLimpia)

                if (dateExpiracion != null) {
                    val tiempoRestante = dateExpiracion.time - System.currentTimeMillis()

                    if (tiempoRestante > 0) {
                        timer = object : CountDownTimer(tiempoRestante, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val minutos = (millisUntilFinished / 1000) / 60
                                val segundos = (millisUntilFinished / 1000) % 60
                                binding.tvTemporizador.text = String.format(Locale.US, "%02d:%02d", minutos, segundos)
                            }
                            override fun onFinish() {
                                binding.tvTemporizador.text = "00:00"
                                binding.tvEstadoTexto.text = "EXPIRADA"
                                binding.tvEstadoTexto.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                                binding.btnVerQR.visibility = View.GONE
                            }
                        }.start()
                    } else {
                        binding.tvTemporizador.text = "00:00"
                        binding.tvEstadoTexto.text = "EXPIRADA"
                        binding.tvEstadoTexto.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                        binding.btnVerQR.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                binding.tvTemporizador.text = "--:--"
                android.util.Log.e("TIMER", "Error parseando fecha: ${e.message}")
            }
        }

        fun cancelTimer() {
            timer?.cancel()
            timer = null
        }
    }

    class ReservaDiffCallback : DiffUtil.ItemCallback<ReservaDetalle>() {
        override fun areItemsTheSame(oldItem: ReservaDetalle, newItem: ReservaDetalle): Boolean {
            return oldItem.id_reservas == newItem.id_reservas
        }

        override fun areContentsTheSame(oldItem: ReservaDetalle, newItem: ReservaDetalle): Boolean {
            return oldItem == newItem
        }
    }
}

// Este DTO servirá para guardar los datos cruzados (JOIN) de la reserva + lote + tienda
data class ReservaDetalle(
    val id_reservas: String,
    val cantidad: Int,
    val estado: String?,
    val fecha_expiracion: String?,
    val nombreProducto: String?,
    val nombreTienda: String?
)