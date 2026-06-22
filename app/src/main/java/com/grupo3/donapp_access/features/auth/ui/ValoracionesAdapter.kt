package com.grupo3.donapp_access.features.auth.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemValoracionBinding
import com.grupo3.donapp_access.features.cliente.dto.ValoracionDTO

class ValoracionesAdapter(private var valoraciones: List<ValoracionDTO>) :
    RecyclerView.Adapter<ValoracionesAdapter.ValoracionViewHolder>() {

    inner class ValoracionViewHolder(private val binding: ItemValoracionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(valoracion: ValoracionDTO) {
            val usuario = valoracion.usuarios
            binding.tvUsuarioNombre.text = if (usuario != null) {
                "${usuario.nombres} ${usuario.apellidos}"
            } else {
                "Usuario Anónimo"
            }

            binding.tvFecha.text = valoracion.createdAt.split("T").firstOrNull() ?: ""
            binding.rbCalificacion.rating = valoracion.calificacion.toFloat()
            binding.tvComentario.text = valoracion.comentario ?: "Sin comentario"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValoracionViewHolder {
        val binding = ItemValoracionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ValoracionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ValoracionViewHolder, position: Int) {
        holder.bind(valoraciones[position])
    }

    override fun getItemCount(): Int = valoraciones.size

    fun updateList(newList: List<ValoracionDTO>) {
        valoraciones = newList
        notifyDataSetChanged()
    }
}