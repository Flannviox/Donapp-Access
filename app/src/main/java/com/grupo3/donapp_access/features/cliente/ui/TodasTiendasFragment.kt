package com.grupo3.donapp_access.features.cliente.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.grupo3.donapp_access.app.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import com.grupo3.donapp_access.databinding.FragmentTiendaDetailBinding
import com.grupo3.donapp_access.databinding.FragmentTodasTiendasBinding
import com.grupo3.donapp_access.features.auth.ui.ValoracionesAdapter
import com.grupo3.donapp_access.features.comerciante.lotes.dto.LoteDTO
import com.grupo3.donapp_access.features.cliente.ClienteRepository
import com.grupo3.donapp_access.features.cliente.TiendaDetailViewModel
import com.grupo3.donapp_access.data.model.OfertaLote
import com.grupo3.donapp_access.features.cliente.data.dto.TiendaDTO
import com.grupo3.donapp_access.features.cliente.data.dto.ValoracionDTO
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TodasTiendasFragment : Fragment() {

    private var _binding: FragmentTodasTiendasBinding? = null
    private val binding get() = _binding!!
    private lateinit var tiendasAdapter: TodasTiendasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodasTiendasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        cargarTodasLasTiendas()
    }

    private fun setupUI() {
        // Configurar botón de retroceso
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        //Configurar RecyclerView en una sola lista vertical
        binding.rvTodasTiendas.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar el adaptador con las tarjetas anchasto
        tiendasAdapter = TodasTiendasAdapter(requireContext()) { tienda ->
            val id = tienda.idTienda
            if (id != null && id.isNotBlank()) {
                (requireActivity() as MainActivity).navegarA(
                    TiendaDetailFragment.newInstance(id, tienda.nombre)
                )
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error: La tienda no tiene ID",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.rvTodasTiendas.adapter = tiendasAdapter
    }

    private fun cargarTodasLasTiendas() {
        binding.progressBar.visibility = View.VISIBLE

        val repository = ClienteRepository()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tiendas = withContext(Dispatchers.IO) {
                    repository.obtenerTiendas()
                }

                binding.progressBar.visibility = View.GONE
                tiendasAdapter.submitList(tiendas)

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                android.util.Log.e("TodasTiendas", "Error al cargar tiendas: ${e.message}", e)
                android.widget.Toast.makeText(context, "Error al cargar las bodegas", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class TiendaDetailFragment : Fragment() {

    private val detailViewModel: TiendaDetailViewModel by viewModels()
    private var _binding: FragmentTiendaDetailBinding? = null
    private val binding get() = _binding!!
    private var tiendaId: String? = null
    private var tiendaActual: TiendaDTO? = null
    private lateinit var reviewsAdapter: ValoracionesAdapter
    private lateinit var ofertasAdapter: OfertaAdapter

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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailViewModel.reservaState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            // Opcional: Mostrar un progreso de carga en la pantalla - Aun no se implementa xd
                        }
                        is UiState.Success -> {
                            Toast.makeText(requireContext(), "¡Reserva realizada con éxito! Tienes 1 hora para recogerla.", Toast.LENGTH_LONG).show()

                            // Volvemos a cargar los datos para que el stock se actualice en la pantalla
                            cargarDatos()

                            detailViewModel.limpiarEstadoReserva()
                        }
                        is UiState.Error -> {
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

        ofertasAdapter = OfertaAdapter(requireContext()) { oferta ->
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
                    .select(Columns.Companion.raw("*, productos(*)")) {
                        filter {
                            eq("tiendas_id", id)
                            eq("estado", "en_oferta")
                        }
                    }.decodeList<LoteDTO>()

                binding.tvActiveOffersCount.text = getString(R.string.offers_count_format, lotes.size)

                // AGREGADO: Transformar LoteDTO a OfertaLote y enviarlo al adaptador
                // AGREGADO: Transformar LoteDTO a OfertaLote y enviarlo al adaptador
                val ofertasLote = lotes.map { lote ->
                    OfertaLote(
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
                ofertasAdapter.submitList(ofertasLote)

                val valoraciones = SupabaseClient.client.from("valoraciones")
                    .select(Columns.Companion.raw("*, usuarios(nombres, apellidos)")) {
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
        binding.tvRatingValue.text = String.Companion.format(Locale.getDefault(), "%.1f", tienda.ratingPromedio)
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

            // Conteo de estrellas para las barras de progreso
            val conteo = IntArray(6) // 0-5
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

    private fun mostrarDialogoReserva(oferta: OfertaLote) {
        // Inflamos el XML que tiene los botones integrados
        val dialogView = layoutInflater.inflate(R.layout.dialog_reserva, null)

        // Buscamos los elementos del XML
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnMinus = dialogView.findViewById<Button>(R.id.btnMinus)
        val btnPlus = dialogView.findViewById<Button>(R.id.btnPlus)
        val tvQuantity = dialogView.findViewById<TextView>(R.id.tvQuantity)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btnCancelarReserva)
        val btnConfirmar = dialogView.findViewById<MaterialButton>(R.id.btnConfirmarReserva)

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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Configuración de accesibilidad y diseño
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.8f) // Oscurecimiento fuerte para enfoque visual

        // Lógica de nuestros botones propios
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
