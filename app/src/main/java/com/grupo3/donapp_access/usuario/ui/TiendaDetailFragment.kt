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

class TiendaDetailFragment : Fragment() {

    private val detailViewModel: TiendaDetailViewModel by viewModels()
    private var _binding: FragmentTiendaDetailBinding? = null
    private val binding get() = _binding!!
    private var tiendaId: String? = null
    private var tiendaActual: TiendaDTO? = null
    private lateinit var reviewsAdapter: ValoracionesAdapter
    private lateinit var ofertasAdapter: com.grupo3.donapp_access.features.usuario.ui.OfertaAdapter

    //AGREGADO
    companion object{
        fun newInstance(tiendaId: String, tiendaNombre: String): TiendaDetailFragment{
            return TiendaDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("id_tienda", tiendaId)
                    putString("nombre_tienda", tiendaNombre)
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

        // Recuperamos el id_tienda de los argumentos de navegación
        tiendaId = arguments?.getString("id_tienda")
        
        // Si no hay ID, mostramos error y volvemos (para evitar crashes en pruebas)
        if (tiendaId == null) {
            Log.e("TiendaDetail", "No se recibió el ID de la tienda")
            // Descomenta la siguiente línea cuando tengas la navegación configurada
            // parentFragmentManager.popBackStack() 
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
                        is com.grupo3.donapp_access.core.common.UiState.Loading -> {
                            // Opcional: Mostrar un progreso de carga en la pantalla - Aun no se implementa xd
                        }
                        is com.grupo3.donapp_access.core.common.UiState.Success -> {
                            Toast.makeText(requireContext(), "¡Reserva realizada con éxito! Tienes 1 hora para recogerla.", Toast.LENGTH_LONG).show()

                            // Volvemos a cargar los datos para que el stock se actualice en la pantalla
                            cargarDatos()

                            detailViewModel.limpiarEstadoReserva()
                        }
                        is com.grupo3.donapp_access.core.common.UiState.Error -> {
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                            detailViewModel.limpiarEstadoReserva()
                        }
                        null -> { /* Estado inicial, no hacemos nada */ }
                    }
                }
            }
        }
    }
    private fun setupRecyclerViews() {
        // Inicializar el adaptador de reseñas
        reviewsAdapter = ValoracionesAdapter(emptyList())
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
            isNestedScrollingEnabled = false // Importante para scroll fluido dentro de NestedScrollView
        }

        ofertasAdapter = com.grupo3.donapp_access.features.usuario.ui.OfertaAdapter(requireContext()) { oferta ->
            // Al hacer clic, abrimos el diálogo y le pasamos los datos del producto
            mostrarDialogoReserva(oferta)
        }

        binding.rvAvailableOffers.apply {
            // Se usa LayoutManager Horizontal para deslizar de lado, tal como en el Home
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
            abrirEnGoogleMaps() //AGREGADO
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

        //intent pasará las coordenadas al google maps
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if(intent.resolveActivity(requireActivity().packageManager)!= null){
            startActivity(intent)
        }else{
            //si no tiene google maps, abrir en el navegador
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
                // 1. Obtener detalles básicos de la tienda
                val tienda = SupabaseClient.client.from("tiendas")
                    .select {
                        filter { eq("id_tienda", id) }
                    }.decodeSingle<TiendaDTO>()

                bindTienda(tienda)

                // 2. Obtener lotes en oferta (con información del producto relacionada)
                val lotes = SupabaseClient.client.from("lote")
                    .select(Columns.raw("*, productos(*)")) {
                        filter {
                            eq("tiendas_id", id)
                            eq("estado", "en_oferta")
                        }
                    }.decodeList<LoteDTO>()

                binding.tvActiveOffersCount.text = getString(R.string.offers_count_format, lotes.size)

                // AGREGADO: Transformar LoteDTO a OfertaLote y enviarlo al adaptador
                // AGREGADO: Transformar LoteDTO a OfertaLote y enviarlo al adaptador
                val ofertasLote = lotes.map { lote ->
                    com.grupo3.donapp_access.model.OfertaLote(
                        idLote = lote.idLote ?: "", // Faltaba incluir este campo
                        tiendaId = id,
                        productoNombre = lote.productos?.nombre ?: "Producto",
                        productoImagen = lote.productos?.imagen, // Faltaba incluir este campo
                        productoPresentacion = lote.productos?.presentacion,
                        tiendaNombre = tiendaActual?.nombre ?: "",
                        tiendaDireccion = tiendaActual?.direccion ?: "",
                        cantidad = lote.cantidad, // Ya no necesita ?: porque no es nulo en el DTO
                        fechaVencimiento = lote.fechaVencimiento, // Ya no necesita ?: porque no es nulo
                        precioNormal = lote.precioNormal, // Ya no necesita ?: porque no es nulo
                        precioOferta = lote.precioOferta ?: 0.0,
                        numeroLote = lote.numeroLote,
                        ratingTienda = tiendaActual?.ratingPromedio ?: 0.0
                    )
                }
                // ¡Magia! Pintamos las cartas en la UI
                ofertasAdapter.submitList(ofertasLote)

                // 3. Obtener valoraciones (con información del usuario que la hizo)
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

        // Cargar logo con Glide
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

            // Conteo de estrellas para las barras de progreso
            val conteo = IntArray(6) // 0-5
            valoraciones.forEach { v ->
                val nota = v.calificacion.toInt().coerceIn(1, 5)
                conteo[nota]++
            }

            // Actualizar barras de progreso y textos de porcentaje
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
        // Inflamos el XML
        val dialogView = layoutInflater.inflate(R.layout.dialog_reserva, null)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val btnMinus = dialogView.findViewById<android.widget.Button>(R.id.btnMinus)
        val btnPlus = dialogView.findViewById<android.widget.Button>(R.id.btnPlus)
        val tvQuantity = dialogView.findViewById<android.widget.TextView>(R.id.tvQuantity)

        tvTitle.text = "Reservar ${oferta.productoNombre}"

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
            .setPositiveButton("Confirmar") { dialogInterface, _ ->
                val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id

                if (idUsuarioActual != null) {
                    // Si hay un usuario logueado, hacemos la reserva real
                    detailViewModel.reservarLote(idUsuarioActual, oferta.idLote, cantidadSeleccionada)
                } else {
                    // Si la sesión no existe o expiró, mostramos un error para que no colapse la app
                    Toast.makeText(requireContext(), "Tu sesión ha expirado. Vuelve a iniciar sesión.", Toast.LENGTH_SHORT).show()
                }

                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancelar") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()


        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

}
