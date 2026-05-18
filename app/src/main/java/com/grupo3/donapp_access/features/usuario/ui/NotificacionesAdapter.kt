package com.grupo3.donapp_access.features.usuario.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grupo3.donapp_access.databinding.ItemNotificacionCardBinding

class NotificacionesAdapter(
    private val registros: List<String>,
    private val onVerOfertasClicked: (tiendaId: String) -> Unit // Recibimos el evento de click
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val binding = ItemNotificacionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificacionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val registro = registros[position]
        val partes = registro.split("|")

        val fechaHora = if (partes.size >= 1) partes[0].trim() else ""
        val textoAlerta = if (partes.size >= 2) partes[1].trim() else registro

        val tiendaId = if (partes.size >= 3) partes[2].trim() else "1"

        holder.binding.tvTextoAlerta.text = textoAlerta
        holder.binding.tvFechaAlerta.text = "Hoy $fechaHora"


        if (textoAlerta.contains("km")) {
            val dist = textoAlerta.substringAfter("está a ").substringBefore(" km")
            holder.binding.tvDistancia.text = "$dist km"
        }

        // Configuramos la acción del clic pasando el ID REAL de la tienda de la alerta
        holder.binding.btnVerOfertas.setOnClickListener {
            onVerOfertasClicked(tiendaId)
        }
    }


    override fun getItemCount(): Int = registros.size

    class NotificacionViewHolder(val binding: ItemNotificacionCardBinding) :
        RecyclerView.ViewHolder(binding.root)
}