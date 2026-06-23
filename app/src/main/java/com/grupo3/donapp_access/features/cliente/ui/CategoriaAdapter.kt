package com.grupo3.donapp_access.features.cliente.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemCategoriaBinding
import com.grupo3.donapp_access.data.model.Categoria

class CategoriaAdapter(
    private val onCategoriaClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {
    private val items = mutableListOf<Categoria>()

    fun submitList(categorias: List<Categoria>) {
        items.clear()
        items.addAll(categorias)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CategoriaViewHolder(
        private val binding: ItemCategoriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(categoria: Categoria) {
            val nombre = categoria.nombre.ifBlank { "Categoria" }
            binding.textCategoriaInicial.text = iconoCategoria(nombre)
            binding.textCategoriaNombre.text = nombre
            binding.textCategoriaConteo.text = if (categoria.totalOfertas > 0) {
                "${categoria.totalOfertas} ofertas"
            } else {
                "Ver ofertas"
            }
            binding.root.setOnClickListener { onCategoriaClick(categoria) }
        }

        private fun iconoCategoria(nombre: String): String {
            return when (nombre.lowercase()) {
                "panaderia", "panadería" -> "🥐"
                "lacteos", "lácteos" -> "🥛"
                "frutas y verduras" -> "\uD83C\uDF4E"
                "abarrotes" -> "🧺"
                "granja" -> "🏚"
                "bebidas" -> "🍹"
                "carnes y embutidos" -> "\uD83E\uDD69"
                "snacks y golosinas" -> "\uD83C\uDF7F"
                "otros" -> "\uD83D\uDCE6"

                else -> nombre.first().uppercaseChar().toString()
            }
        }
    }
}
