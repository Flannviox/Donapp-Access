package com.grupo3.donapp_access.features.comerciante.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemLoteCardBinding
import com.grupo3.donapp_access.model.Lote

class InventarioAdapter(
    private val onLoteClick: (Lote) -> Unit
) : ListAdapter<Lote, InventarioAdapter.LoteViewHolder>(LoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoteViewHolder {
        val binding = ItemLoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoteViewHolder, position: Int) {
        val lote = getItem(position)
        holder.bind(lote)
    }

    inner class LoteViewHolder(private val binding: ItemLoteCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lote: Lote) {
            // Nombre del producto o ID acortado
            binding.tvNombreProducto.text = if (lote.productos_id.length > 20) {
                "Producto: ${lote.productos_id.take(8)}..."
            } else {
                lote.productos_id
            }

            binding.tvStockCantidad.text = lote.cantidad.toString()
            binding.tvExpiracion.text = "Vence: ${lote.fecha_vencimiento}"

            // Lógica de colores y estados dinámicos
            when (lote.estado.lowercase()) {
                "en_oferta" -> {
                    binding.tvDescuento.text = "OFERTA"
                    binding.tvDescuento.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5C518"))
                    binding.tvDescuento.setTextColor(Color.parseColor("#1A1A1A"))

                    lote.precio_oferta?.let { po ->
                        if (lote.precio_normal > 0) {
                            val desc = ((1 - (po / lote.precio_normal)) * 100).toInt()
                            binding.tvDescuento.text = "-$desc%"
                        }
                    }
                    binding.tvDescuento.visibility = View.VISIBLE
                }
                "disponible" -> {
                    binding.tvDescuento.text = "STOCK"
                    binding.tvDescuento.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                    binding.tvDescuento.setTextColor(Color.WHITE)
                    binding.tvDescuento.visibility = View.VISIBLE
                }
                "agotado" -> {
                    binding.tvDescuento.text = "AGOTADO"
                    binding.tvDescuento.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                    binding.tvDescuento.setTextColor(Color.WHITE)
                    binding.tvDescuento.visibility = View.VISIBLE
                }
                else -> {
                    binding.tvDescuento.visibility = View.GONE
                }
            }

            binding.ivConfiguracion.setOnClickListener {
                onLoteClick(lote)
            }
        }
    }

    class LoteDiffCallback : DiffUtil.ItemCallback<Lote>() {
        override fun areItemsTheSame(oldItem: Lote, newItem: Lote): Boolean {
            return oldItem.id_lote == newItem.id_lote
        }

        override fun areContentsTheSame(oldItem: Lote, newItem: Lote): Boolean {
            return oldItem == newItem
        }
    }
}