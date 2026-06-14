package com.grupo3.donapp_access.usuario.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.auth.ui.ValoracionesAdapter
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentTiendaDetailBinding
import com.grupo3.donapp_access.features.lotes.dto.LoteDTO
import com.grupo3.donapp_access.features.usuario.TiendaDetailViewModel
import com.grupo3.donapp_access.usuario.dto.TiendaDTO
import com.grupo3.donapp_access.usuario.dto.ValoracionDTO
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.jan.supabase.auth.auth
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager

class TiendaDetailFragment : Fragment() {

    private val detailViewModel: TiendaDetailViewModel by viewModels()
    private var _binding: FragmentTiendaDetailBinding? = null
    private val binding get() = _binding!!
    private var tiendaId: String? = null
    private var tiendaActual: TiendaDTO? = null
    private lateinit var reviewsAdapter: ValoracionesAdapter
    private lateinit var ofertasAdapter: com.grupo3.donapp_access.features.usuario.ui.OfertaAdapter

    companion object {
        fun newInstance(tiendaId: String, tiendaNombre: String, autoOpenLoteId: String? = null): TiendaDetailFragment {
            return TiendaDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("id_tienda", tiendaId)
                    putString("nombre_tienda", tiendaNombre)
                    putString("auto_open_lote_id", autoOpenLoteId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTiendaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tiendaId = arguments?.getString("id_tienda")

        if (tiendaId == null) {
            Log.e("TiendaDetail", "No se recibió el ID de la tienda")
        }

        setupRecyclerViews()
        setupButtons()
        setupObservers()
        cargarDatos()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                detailViewModel.reservaState.collect { state ->
                    when (state) {
                        is com.grupo3.donapp_access.core.common.UiState.Loading -> {}
                        is com.grupo3.donapp_access.core.common.UiState.Success -> {
                            Toast.makeText(requireContext(), "¡Reserva realizada con éxito! Tienes 1 hora para recogerla.", Toast.LENGTH_LONG).show()
                            cargarDatos()
                            detailViewModel.limpiarEstadoReserva()
                        }
                        is com.grupo3.donapp_access.core.common.UiState.Error -> {
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                            detailViewModel.limpiarEstadoReserva()
                        }
                        null -> {}
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        reviewsAdapter = ValoracionesAdapter(emptyList())
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
            isNestedScrollingEnabled = false
        }

        ofertasAdapter = com.grupo3.donapp_access.features.usuario.ui.OfertaAdapter(requireContext()) { oferta ->
            mostrarDialogoReserva(oferta)
        }

        binding.rvAvailableOffers.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = ofertasAdapter
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAddReview.setOnClickListener {
            Toast.makeText(context, "Próximamente: Añadir reseña", Toast.LENGTH_SHORT).show()
        }

        binding.btnCall.setOnClickListener {
            Toast.makeText(context, "Llamando a la tienda...", Toast.LENGTH_SHORT).show()
        }

        binding.btnHowToGet.setOnClickListener {
            abrirEnGoogleMaps()
        }
    }

    private fun abrirEnGoogleMaps(){
        val tienda = tiendaActual?: run {
            Toast.makeText(context, "No se pudo obtener la ubicacion", Toast.LENGTH_SHORT)
            return
        }

        val uri = Uri.parse(
            "geo:${tienda.latitud},${tienda.longitud}?" +
                    "q=${tienda.latitud},${tienda.longitud}(${tienda.nombre})"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if(intent.resolveActivity(requireActivity().packageManager)!= null){
            startActivity(intent)
        }else{
            val webUri = Uri.parse(
                "https://www.google.com/maps/search/?api=1" +
                        "&query=${tienda.latitud},${tienda.longitud}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun cargarDatos() {
        val id = tiendaId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tienda = SupabaseClient.client.from("tiendas")
                    .select {
                        filter { eq("id_tienda", id) }
                    }.decodeSingle<TiendaDTO>()

                bindTienda(tienda)

                val lotes = SupabaseClient.client.from("lote")
                    .select(Columns.raw("*, productos(*)")) {
                        filter {
                            eq("tiendas_id", id)
                            eq("estado", "en_oferta")
                            gt("cantidad", 0) // NUEVO: Oculta los productos sin stock en el perfil de la tienda
                        }
                    }.decodeList<LoteDTO>()

                binding.tvActiveOffersCount.text = getString(R.string.offers_count_format, lotes.size)

                val ofertasLote = lotes.map { lote ->
                    com.grupo3.donapp_access.model.OfertaLote(
                        idLote = lote.idLote ?: "",
                        tiendaId = id,
                        productoNombre = lote.productos?.nombre ?: "Producto",
                        productoImagen = lote.productos?.imagen,
                        productoPresentacion = lote.productos?.presentacion,
                        tiendaNombre = tiendaActual?.nombre ?: "",
                        tiendaDireccion = tiendaActual?.direccion ?: "",
                        cantidad = lote.cantidad,
                        fechaVencimiento = lote.fechaVencimiento,
                        precioNormal = lote.precioNormal,
                        precioOferta = lote.precioOferta ?: 0.0,
                        numeroLote = lote.numeroLote,
                        ratingTienda = tiendaActual?.ratingPromedio ?: 0.0
                    )
                }

                ofertasAdapter.submitList(ofertasLote)
                // ¡Magia! Pintamos las cartas en la UI
                ofertasAdapter.submitList(ofertasLote)

                // Lógica mejorada para buscar, hacer scroll y abrir el diálogo
                val autoOpenId = arguments?.getString("auto_open_lote_id")
                if (autoOpenId != null) {
                    // Buscamos en qué posición de la lista está el producto exacto
                    val index = ofertasLote.indexOfFirst { it.idLote == autoOpenId }

                    if (index != -1) {
                        // 1. Deslizamos la lista automáticamente hasta el producto
                        binding.rvAvailableOffers.scrollToPosition(index)

                        // 2. Abrimos el cuadro de diálogo
                        mostrarDialogoReserva(ofertasLote[index])
                    }
                    // Lo borramos para que no se repita al girar la pantalla
                    arguments?.remove("auto_open_lote_id")
                }

                val valoraciones = SupabaseClient.client.from("valoraciones")
                    .select(Columns.raw("*, usuarios(nombres, apellidos)")) {
                        filter {
                            eq("tiendas_id", id)
                            eq("estado", "ACTIVO")
                        }
                    }.decodeList<ValoracionDTO>()

                bindResenas(valoraciones)
                reviewsAdapter.updateList(valoraciones)

            } catch (e: Exception) {
                Log.e("TiendaDetail", "Error al cargar datos: ${e.message}", e)
                Toast.makeText(context, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindTienda(tienda: TiendaDTO) {
        tiendaActual = tienda
        binding.tvStoreName.text = tienda.nombre
        binding.tvAddress.text = tienda.direccion
        binding.tvRatingValue.text = String.format(Locale.getDefault(), "%.1f", tienda.ratingPromedio)
        binding.tvSchedule.text = tienda.horaAtencion ?: "Horario no disponible"

        tienda.imagenReferencia?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.logo)
                .into(binding.ivStoreLogo)
        }
    }

    private fun bindResenas(valoraciones: List<ValoracionDTO>) {
        val total = valoraciones.size
        binding.tvReviewsLabel.text = getString(R.string.reviews_label_format, total)
        binding.tvTotalReviewsText.text = getString(R.string.reviews_total_format, total)

        if (total > 0) {
            val promedio = valoraciones.map { it.calificacion }.average().toFloat()
            binding.rbAverage.rating = promedio

            val conteo = IntArray(6)
            valoraciones.forEach { v ->
                val nota = v.calificacion.toInt().coerceIn(1, 5)
                conteo[nota]++
            }

            binding.pb5Stars.progress = (conteo[5] * 100) / total
            binding.tv5StarsPct.text = "${(conteo[5] * 100) / total}%"

            binding.pb4Stars.progress = (conteo[4] * 100) / total
            binding.tv4StarsPct.text = "${(conteo[4] * 100) / total}%"

            binding.pb3Stars.progress = (conteo[3] * 100) / total
            binding.tv3StarsPct.text = "${(conteo[3] * 100) / total}%"

            binding.pb2Stars.progress = (conteo[2] * 100) / total
            binding.tv2StarsPct.text = "${(conteo[2] * 100) / total}%"

            binding.pb1Star.progress = (conteo[1] * 100) / total
            binding.tv1StarPct.text = "${(conteo[1] * 100) / total}%"
        } else {
            binding.rbAverage.rating = 0f
            limpiarBarrasProgreso()
        }
    }

    private fun limpiarBarrasProgreso() {
        listOf(binding.pb5Stars, binding.pb4Stars, binding.pb3Stars, binding.pb2Stars, binding.pb1Star).forEach { it.progress = 0 }
        listOf(binding.tv5StarsPct, binding.tv4StarsPct, binding.tv3StarsPct, binding.tv2StarsPct, binding.tv1StarPct).forEach { it.text = "0%" }
    }

    override fun onDestroyView() {
        ofertasAdapter.releaseTTS()
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarDialogoReserva(oferta: com.grupo3.donapp_access.model.OfertaLote) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reserva, null)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val btnMinus = dialogView.findViewById<android.widget.Button>(R.id.btnMinus)
        val btnPlus = dialogView.findViewById<android.widget.Button>(R.id.btnPlus)
        val tvQuantity = dialogView.findViewById<android.widget.TextView>(R.id.tvQuantity)
        val btnCancelar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelarReserva)
        val btnConfirmar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmarReserva)

        tvTitle.text = "Reservar ${oferta.productoNombre}"

        VoiceAssistantManager.speak("Estás por reservar el producto ${oferta.productoNombre}. Presiona los botones de más o menos para ajustar la cantidad, y el botón confirmar para finalizar.")

        var cantidadSeleccionada = 1
        val stockDisponible = oferta.cantidad

        btnMinus.setOnClickListener {
            if (cantidadSeleccionada > 1) {
                cantidadSeleccionada--
                tvQuantity.text = cantidadSeleccionada.toString()
            }
        }

        btnPlus.setOnClickListener {
            if (cantidadSeleccionada < stockDisponible) {
                cantidadSeleccionada++
                tvQuantity.text = cantidadSeleccionada.toString()
            } else {
                Toast.makeText(requireContext(), "Solo quedan $stockDisponible disponibles", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.8f)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmar.setOnClickListener {
            val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (idUsuarioActual != null) {
                detailViewModel.reservarLote(idUsuarioActual, oferta.idLote, cantidadSeleccionada)
            } else {
                Toast.makeText(requireContext(), "Tu sesión ha expirado.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}