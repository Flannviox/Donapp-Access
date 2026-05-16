package com.grupo3.donapp_access.features.usuario.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemHomeStoreCardBinding
import com.grupo3.donapp_access.model.TiendaHome
import java.util.Locale

class HomeTiendaAdapter(
    private val onItemClick: (TiendaHome) -> Unit = {}
) : RecyclerView.Adapter<HomeTiendaAdapter.TiendaViewHolder>() {
    private val items = mutableListOf<TiendaHome>()

    fun submitList(tiendas: List<TiendaHome>) {
        items.clear()
        items.addAll(tiendas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiendaViewHolder {
        val binding = ItemHomeStoreCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TiendaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TiendaViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class TiendaViewHolder(
        private val binding: ItemHomeStoreCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tienda: TiendaHome, onItemClick: (TiendaHome) -> Unit) = with(binding) {
            textTiendaNombre.text = tienda.nombre
            textDistancia.text = tienda.direccion?.takeIf { it.isNotBlank() } ?: "Cerca de ti"

            val rating = tienda.rating ?: 0.0
            textEstrellas.text = if (rating > 0.0) "%.1f".format(Locale.US, rating) else "Sin rating"
            textRating.text = if (rating > 0.0) "(%.1f)".format(Locale.US, rating) else ""
            root.setOnClickListener { onItemClick(tienda) }

        }
    }
}
