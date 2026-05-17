package com.grupo3.donapp_access.features.usuario.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemHomeOfferCardBinding
import com.grupo3.donapp_access.model.OfertaLote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import android.speech.tts.TextToSpeech
import android.content.Context


class HomeOfertaAdapter(
    private val context: Context,
    private val onClick: (OfertaLote) -> Unit
) : RecyclerView.Adapter<HomeOfertaAdapter.OfertaViewHolder>() {

    private val items = mutableListOf<OfertaLote>()
    private var tts : TextToSpeech? = null

    init{
        tts = TextToSpeech(
            context
        ){status->
            if(status == TextToSpeech.SUCCESS){
                tts?.language= Locale("es", "ES")

            }
        }
    }

    fun submitList(ofertas: List<OfertaLote>) {
        items.clear()
        items.addAll(ofertas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val binding = ItemHomeOfferCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OfertaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OfertaViewHolder(
        private val binding: ItemHomeOfferCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(oferta: OfertaLote) = with(binding) {
            textProductoNombre.text = oferta.productoNombre.ifBlank { "Producto en oferta" }
            textTiendaNombre.text = oferta.tiendaNombre
            textPrecioNormal.text = oferta.precioNormal.toSoles()
            textPrecioOferta.text = oferta.precioOferta.toSoles()
            textDescuento.text = descuento(oferta.precioNormal, oferta.precioOferta)
            textDistancia.text = oferta.tiendaDireccion?.takeIf { it.isNotBlank() } ?: "Cerca de ti"
            textExpiracion.text = tiempoRestante(oferta.fechaVencimiento)

            btnAudio.setOnClickListener {
                val texto = """
                    Oferta disponible
                    Producto ${oferta.productoNombre}
                    Tienda ${oferta.tiendaNombre}
                    Precio oferta ${oferta.precioOferta.toSoles()}
                    ${textExpiracion.text}
                    Direccion ${textDistancia.text}
                """.trimIndent()

                tts?.speak(
                    texto,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )

            }



            // Ajustar ancho de la línea de tachado al texto
            viewTachado.post {
                viewTachado.layoutParams.width = textPrecioNormal.width
                viewTachado.requestLayout()
            }
        }

        private fun descuento(precioNormal: Double, precioOferta: Double): String {
            if (precioNormal <= 0.0 || precioOferta <= 0.0 || precioOferta >= precioNormal) {
                return "Oferta"
            }
            val porcentaje = ((1 - (precioOferta / precioNormal)) * 100).roundToInt()
            return "-$porcentaje%"
        }

        private fun tiempoRestante(fechaVencimiento: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val fechaFin = sdf.parse(fechaVencimiento) ?: return "Pronto"
                val ahora = Date()
                val diffMillis = fechaFin.time - ahora.time
                if (diffMillis <= 0) return "Vence hoy"

                val dias = TimeUnit.MILLISECONDS.toDays(diffMillis)
                val horas = TimeUnit.MILLISECONDS.toHours(diffMillis) % 24
                val minutos = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60

                when {
                    dias > 0 -> "Expira en\n${dias}d ${horas}h"
                    horas > 0 -> "Expira en\n${horas}h ${minutos}min"
                    else -> "Expira en\n${minutos}min"
                }
            } catch (_: Exception) {
                "Pronto"
            }
        }

        private fun Double.toSoles(): String = "S/ %.2f".format(Locale.US, this)
    }

    fun releaseTTS(){
        tts?.stop()
        tts?.shutdown()
    }
}
