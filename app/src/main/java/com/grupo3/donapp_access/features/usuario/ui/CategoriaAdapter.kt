package com.grupo3.donapp_access.features.usuario.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemCategoriaBinding
import com.grupo3.donapp_access.model.Categoria

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
            binding.textCategoriaInicial.text = nombre.first().uppercaseChar().toString()
            binding.textCategoriaNombre.text = nombre
            binding.textCategoriaConteo.text = if (categoria.totalOfertas > 0) {
                "${categoria.totalOfertas} ofertas"
            } else {
                "Ver ofertas"
            }
            binding.root.setOnClickListener { onCategoriaClick(categoria) }
        }
    }
}
