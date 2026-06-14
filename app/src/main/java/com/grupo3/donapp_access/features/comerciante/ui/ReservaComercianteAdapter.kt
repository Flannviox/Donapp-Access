package com.grupo3.donapp_access.features.comerciante.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemReservaComercianteCardBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.core.graphics.toColorInt

class ReservaComercianteAdapter(
    private val onEntregarClick: (ReservaComercianteDetalle) -> Unit
) : ListAdapter<ReservaComercianteDetalle, ReservaComercianteAdapter.ViewHolder>(ReservaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReservaComercianteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelTimer()
    }

    inner class ViewHolder(private val binding: ItemReservaComercianteCardBinding) : RecyclerView.ViewHolder(binding.root) {
        private var timer: CountDownTimer? = null

        @SuppressLint("SetTextI18n", "UseKtx")
        fun bind(reserva: ReservaComercianteDetalle) {
            binding.tvProductoReservaCom.text = "${reserva.nombreProducto ?: "Producto"} (x${reserva.cantidad})"
            binding.tvClienteReserva.text = "Cliente: ${reserva.nombreCliente ?: "Desconocido"}"

            binding.btnEntregar.setOnClickListener { onEntregarClick(reserva) }

            val estado = reserva.estado?.lowercase() ?: "activa"
            binding.tvEstadoTextoCom.text = estado.uppercase()

            when (estado) {
                "activa" -> {
                    binding.tvEstadoTextoCom.setTextColor("#1A1A1A".toColorInt())
                    binding.tvEstadoTextoCom.backgroundTintList = ColorStateList.valueOf("#F5C518".toColorInt())
                    binding.btnEntregar.visibility = View.VISIBLE
                    binding.progressBarReservaCom.visibility = View.VISIBLE
                    iniciarTemporizador(reserva.fecha_expiracion)
                }
                "completada" -> {
                    binding.tvEstadoTextoCom.setTextColor(Color.WHITE)
                    binding.tvEstadoTextoCom.backgroundTintList = ColorStateList.valueOf("#4CAF50".toColorInt())
                    binding.tvTemporizadorCom.text = "Entregado con éxito"
                    binding.btnEntregar.visibility = View.GONE
                    binding.progressBarReservaCom.visibility = View.GONE
                    cancelTimer()
                }
                else -> { // Expirada
                    binding.tvEstadoTextoCom.setTextColor(Color.WHITE)
                    binding.tvEstadoTextoCom.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                    binding.tvTemporizadorCom.text = "El cliente no llegó"
                    binding.btnEntregar.visibility = View.GONE
                    binding.progressBarReservaCom.visibility = View.GONE
                    cancelTimer()
                }
            }
        }

        @SuppressLint("SetTextI18n")
        private fun iniciarTemporizador(fechaExpiracionStr: String?) {
            cancelTimer()
            if (fechaExpiracionStr == null) {
                binding.tvTemporizadorCom.text = "00:00"
                binding.progressBarReservaCom.progress = 100
                return
            }

            try {
                val fechaLimpia = fechaExpiracionStr.substringBefore(".").substringBefore("+")
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                val dateExpiracion = format.parse(fechaLimpia)

                if (dateExpiracion != null) {
                    val tiempoRestante = dateExpiracion.time - System.currentTimeMillis()
                    val tiempoTotalMs = 3600000L // 1 hora en milisegundos

                    if (tiempoRestante > 0) {
                        binding.progressBarReservaCom.max = 100

                        timer = object : CountDownTimer(tiempoRestante, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val minutos = (millisUntilFinished / 1000) / 60
                                val segundos = (millisUntilFinished / 1000) % 60
                                binding.tvTemporizadorCom.text = String.format(Locale.US, "%02d:%02d", minutos, segundos)

                                val tiempoTranscurrido = tiempoTotalMs - millisUntilFinished
                                val porcentajeTranscurrido = ((tiempoTranscurrido.toDouble() / tiempoTotalMs) * 100).toInt()
                                binding.progressBarReservaCom.progress = porcentajeTranscurrido.coerceIn(0, 100)
                            }

                            override fun onFinish() {
                                binding.tvTemporizadorCom.text = "00:00"
                                binding.progressBarReservaCom.progress = 100
                                binding.tvEstadoTextoCom.text = "EXPIRADA"
                                binding.tvEstadoTextoCom.backgroundTintList = ColorStateList.valueOf(
                                    "#F44336".toColorInt())
                                binding.btnEntregar.visibility = View.GONE
                            }
                        }.start()
                    } else {
                        binding.tvTemporizadorCom.text = "00:00"
                        binding.progressBarReservaCom.progress = 100
                        binding.tvEstadoTextoCom.text = "EXPIRADA"
                        binding.tvEstadoTextoCom.backgroundTintList = ColorStateList.valueOf("#F44336".toColorInt())
                        binding.btnEntregar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                binding.tvTemporizadorCom.text = "--:--"
                binding.progressBarReservaCom.progress = 0
            }
        }

        fun cancelTimer() {
            timer?.cancel()
            timer = null
        }
    }

    class ReservaDiffCallback : DiffUtil.ItemCallback<ReservaComercianteDetalle>() {
        override fun areItemsTheSame(oldItem: ReservaComercianteDetalle, newItem: ReservaComercianteDetalle) = oldItem.id_reservas == newItem.id_reservas
        override fun areContentsTheSame(oldItem: ReservaComercianteDetalle, newItem: ReservaComercianteDetalle) = oldItem == newItem
    }
}

data class ReservaComercianteDetalle(
    val id_reservas: String,
    val cantidad: Int,
    val estado: String?,
    val fecha_expiracion: String?,
    val nombreProducto: String?,
    val nombreCliente: String?
)