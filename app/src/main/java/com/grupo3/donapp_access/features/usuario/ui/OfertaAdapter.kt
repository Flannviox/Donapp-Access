package com.grupo3.donapp_access.features.usuario.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemOfertaBinding
import com.grupo3.donapp_access.model.OfertaLote
import java.util.Locale
import kotlin.math.roundToInt

class OfertaAdapter : RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder>() {
    private val items = mutableListOf<OfertaLote>()

    fun submitList(ofertas: List<OfertaLote>) {
        items.clear()
        items.addAll(ofertas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val binding = ItemOfertaBinding.inflate(
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

    class OfertaViewHolder(
        private val binding: ItemOfertaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(oferta: OfertaLote) = with(binding) {
            val producto = oferta.productoNombre.ifBlank { "Producto en oferta" }
            textOfertaIcon.text = producto.first().uppercaseChar().toString()
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
        }

        private fun descuento(precioNormal: Double, precioOferta: Double): String {
            if (precioNormal <= 0.0 || precioOferta <= 0.0 || precioOferta >= precioNormal) {
                return "Oferta"
            }
            val porcentaje = ((1 - (precioOferta / precioNormal)) * 100).roundToInt()
            return "-$porcentaje%"
        }

        private fun Double.toSoles(): String = "S/ %.2f".format(Locale.US, this)
    }
}
