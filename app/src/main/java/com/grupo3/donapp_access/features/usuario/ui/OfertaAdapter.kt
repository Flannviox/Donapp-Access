package com.grupo3.donapp_access.features.usuario.ui

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.ItemOfertaBinding
import com.grupo3.donapp_access.model.OfertaLote
import java.util.Locale
import kotlin.math.roundToInt
import android.speech.tts.TextToSpeech

class OfertaAdapter(
    private val context: Context,
    private val onItemClick: (OfertaLote) -> Unit = {}
) : RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder>() {
    private val items = mutableListOf<OfertaLote>()
    private var tts : TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
            }
        }
    }

    fun submitList(ofertas: List<OfertaLote>) {
        items.clear()
        items.addAll(ofertas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val binding = ItemOfertaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OfertaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    inner class OfertaViewHolder(
        private val binding: ItemOfertaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(oferta: OfertaLote, onItemClick: (OfertaLote) -> Unit) = with(binding) {
            val producto = oferta.productoNombre.ifBlank { "Producto en oferta" }
            textProductoNombre.text = producto
            textTiendaNombre.text = oferta.tiendaNombre
            textPresentacion.text = oferta.productoPresentacion ?: oferta.numeroLote?.let { "Lote: $it" }.orEmpty()
            textPrecioNormal.text = oferta.precioNormal.toSoles()
            textPrecioNormal.paintFlags = textPrecioNormal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            textPrecioOferta.text = oferta.precioOferta.toSoles()
            textStock.text = "Stock: ${oferta.cantidad} unidades"
            textVencimiento.text = "Vence: ${oferta.fechaVencimiento}"
            textDistancia.text = oferta.ratingTienda?.let { "Rating ${"%.1f".format(Locale.US, it)}" } ?: "Oferta activa"
            textDescuento.text = descuento(oferta.precioNormal, oferta.precioOferta)

            // AGREGADO: Cargar imagen con Glide
            if (!oferta.productoImagen.isNullOrEmpty()) {
                imageProducto.visibility = View.VISIBLE
                textOfertaIcon.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(oferta.productoImagen)
                    .placeholder(R.drawable.ic_bread_product)
                    .into(imageProducto)
            } else {
                imageProducto.visibility = View.GONE
                textOfertaIcon.visibility = View.VISIBLE
                textOfertaIcon.text = producto.first().uppercaseChar().toString()
            }

            // AGREGADO: Restaurar el clic de la tarjeta
            root.setOnClickListener { onItemClick(oferta) }
            btnAudio.setOnClickListener {
                val texto = """
                Producto $producto.
                Tienda ${oferta.tiendaNombre}.
                Precio en oferta ${oferta.precioOferta.toSoles()}.
                ${textStock.text}.
                ${textVencimiento.text}.
                ${textDistancia.text}.
            """.trimIndent()

                tts?.speak(
                    texto,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }


        }

        private fun descuento(precioNormal: Double, precioOferta: Double): String {
            if (precioNormal <= 0.0 || precioOferta <= 0.0 || precioOferta >= precioNormal) return "Oferta"
            val porcentaje = ((1 - (precioOferta / precioNormal)) * 100).roundToInt()
            return "-$porcentaje%"
        }

        private fun Double.toSoles(): String = "S/ %.2f".format(Locale.US, this)
    }
    fun releaseTTS(){
        tts?.stop()
        tts?.shutdown()
    }
}