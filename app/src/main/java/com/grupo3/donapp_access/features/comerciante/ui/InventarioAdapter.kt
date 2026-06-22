package com.grupo3.donapp_access.features.comerciante.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.ItemLoteCardBinding
import com.grupo3.donapp_access.data.model.Lote
import java.util.Locale
import kotlin.math.roundToInt

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
            val nombreProducto = lote.productos_id ?: "Producto sin nombre"

            binding.tvNombreProducto.text = if (nombreProducto.length > 20) {
                "Producto: ${nombreProducto.take(8)}..."
            } else {
                nombreProducto
            }

            binding.tvStockCantidad.text = "Stock: ${lote.cantidad} uds."
            binding.tvExpiracion.text = "Vence: ${lote.fecha_vencimiento ?: "---"}"

            binding.tvPrecioNormal.text = "S/ %.2f".format(Locale.US, lote.precio_normal)

            binding.viewTachado.post {
                binding.viewTachado.layoutParams.width = binding.tvPrecioNormal.width
                binding.viewTachado.requestLayout()
            }

            val urlImagen = lote.imagenUrl

            Glide.with(itemView.context)
                .load(urlImagen)
                .placeholder(R.drawable.ic_bread_product)
                .error(R.drawable.ic_bread_product)
                .into(binding.ivProductoImagen)

            val estadoLote = lote.estado?.lowercase() ?: "disponible"

            when (estadoLote) {
                "en_oferta" -> {
                    binding.tvDescuento.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5C518"))
                    binding.tvDescuento.setTextColor(Color.parseColor("#1A1A1A"))

                    lote.precio_oferta?.let { po ->
                        binding.tvPrecioOferta.text = "S/ %.2f".format(Locale.US, po)
                        binding.tvPrecioOferta.visibility = View.VISIBLE
                        binding.tvPrecioNormal.setTextColor(Color.parseColor("#9E9E9E"))
                        binding.viewTachado.visibility = View.VISIBLE

                        if (lote.precio_normal > 0) {
                            val desc = ((1 - (po / lote.precio_normal)) * 100).roundToInt()
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

                    binding.tvPrecioOferta.visibility = View.GONE
                    binding.viewTachado.visibility = View.GONE
                    binding.tvPrecioNormal.setTextColor(Color.WHITE)
                }
                "agotado" -> {
                    binding.tvDescuento.text = "AGOTADO"
                    binding.tvDescuento.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                    binding.tvDescuento.setTextColor(Color.WHITE)
                    binding.tvDescuento.visibility = View.VISIBLE

                    binding.tvPrecioOferta.visibility = View.GONE
                    binding.viewTachado.visibility = View.GONE
                    binding.tvPrecioNormal.setTextColor(Color.parseColor("#9E9E9E"))
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