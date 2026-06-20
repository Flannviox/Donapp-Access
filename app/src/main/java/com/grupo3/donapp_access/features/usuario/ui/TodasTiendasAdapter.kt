package com.grupo3.donapp_access.features.usuario.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.R

import com.grupo3.donapp_access.databinding.ItemTiendaVerticalBinding
import com.grupo3.donapp_access.model.TiendaHome
import java.util.Locale

class TodasTiendasAdapter(
    private val context : Context,
    private val onItemClick: (TiendaHome) -> Unit = {}
) : RecyclerView.Adapter<TodasTiendasAdapter.TiendaViewHolder>() {

    private val items = mutableListOf<TiendaHome>()

    fun submitList(tiendas: List<TiendaHome>) {
        items.clear()
        items.addAll(tiendas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiendaViewHolder {
        // 👇 2. Inflamos la vista usando el nuevo Binding correctamente
        val binding = ItemTiendaVerticalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // Tu lógica de temas (¡muy buena por cierto!)
        val temaActual = context.getSharedPreferences("donapp_prefs", Context.MODE_PRIVATE)
            .getString("tema_actual", "normal")

        val bgDrawable = when (temaActual) {
            "black_white" -> R.drawable.bg_store_card_bw
            "high_contrast" -> R.drawable.bg_store_card_contrast
            else -> R.drawable.bg_store_card
        }

        // Ahora esto sí funcionará porque "binding" ya existe
        binding.root.setBackgroundResource(bgDrawable)

        return TiendaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TiendaViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class TiendaViewHolder(
        private val binding: ItemTiendaVerticalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tienda: TiendaHome, onItemClick: (TiendaHome) -> Unit) = with(binding) {
            textTiendaNombre.text = tienda.nombre
            textDistancia.text = tienda.direccion?.takeIf { it.isNotBlank() } ?: "Cerca de ti"

            val rating = tienda.rating ?: 0.0
            textEstrellas.text = if (rating > 0.0) "%.1f".format(Locale.US, rating) else "Sin calificación"
            textRating.text = if (rating > 0.0) "(%.1f)".format(Locale.US, rating) else ""

            root.setOnClickListener { onItemClick(tienda) }
        }
    }
}