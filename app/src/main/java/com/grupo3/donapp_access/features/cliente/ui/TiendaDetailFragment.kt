package com.grupo3.donapp_access.features.cliente.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
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
import com.google.zxing.BarcodeFormat
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import com.grupo3.donapp_access.databinding.FragmentTiendaDetailBinding
import com.grupo3.donapp_access.features.auth.ui.ValoracionesAdapter
import com.grupo3.donapp_access.features.comerciante.lotes.dto.LoteDTO
import com.grupo3.donapp_access.features.cliente.TiendaDetailViewModel
import com.grupo3.donapp_access.data.model.OfertaLote
import com.grupo3.donapp_access.features.cliente.data.dto.TiendaDTO
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TiendaDetailFragment : Fragment() {

    private val detailViewModel: TiendaDetailViewModel by viewModels()
    private var _binding: FragmentTiendaDetailBinding? = null
    private val binding get() = _binding!!
    private var tiendaId: String? = null
    private var tiendaActual: TiendaDTO? = null
    private lateinit var reviewsAdapter: ValoracionesAdapter
    private lateinit var ofertasAdapter: OfertaAdapter

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTiendaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tiendaId = arguments?.getString("id_tienda")
        if (tiendaId != null) generarQRBodega(tiendaId!!)
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
                        is UiState.Success -> {
                            Toast.makeText(requireContext(), "¡Reserva realizada!", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                            detailViewModel.limpiarEstadoReserva()
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                            detailViewModel.limpiarEstadoReserva()
                        }
                        else -> {}
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
        ofertasAdapter = OfertaAdapter(requireContext()) { oferta -> mostrarDialogoReserva(oferta) }
        binding.rvAvailableOffers.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = ofertasAdapter
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCall.setOnClickListener {
            val tienda = tiendaActual
            val numeroTelefono = tiendaActual?.usuarios?.telefono
            if(tienda != null && !numeroTelefono.isNullOrEmpty()){
                try {
                    // 1. Limpiamos cualquier carácter que no sea dígito
                    val soloNumeros = numeroTelefono.replace(Regex("[^0-9]"), "")

                    // 2. Si el número tiene 9 dígitos (formato Perú), le añadimos el +51
                    // Si el usuario ya puso el código de país, no lo duplicamos.
                    val numeroFormateado = if (soloNumeros.length == 9) {
                        "+51$soloNumeros"
                    } else {
                        "+$soloNumeros" // Asume que si no tiene 9, ya viene con código de país
                    }

                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$numeroFormateado")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Número no disponible", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnHowToGet.setOnClickListener { abrirEnGoogleMaps() }
    }

    private fun abrirEnGoogleMaps() {
        val tienda = tiendaActual ?: run {
            Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        // Intentamos abrir directamente con la app de Google Maps
        val gmmIntentUri = Uri.parse("geo:${tienda.latitud},${tienda.longitud}?q=${tienda.latitud},${tienda.longitud}(${tienda.nombre})")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            // Intenta lanzar la aplicación
            startActivity(mapIntent)
        } catch (e: Exception) {
            // Si falla (no está instalada), abrimos la versión web en el navegador
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${tienda.latitud},${tienda.longitud}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }

    private fun cargarDatos() {
        val id = tiendaId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tienda = SupabaseClient.client.from("tiendas").select(Columns.list("*, usuarios(telefono)")) { filter { eq("id_tienda", id) } }.decodeSingle<TiendaDTO>()
                bindTienda(tienda)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val hoy = sdf.format(Date())
                val lotes = SupabaseClient.client.from("lote").select(Columns.list("*, productos(*)")) {
                    filter { eq("tiendas_id", id); eq("estado", "en_oferta"); gt("cantidad", 0); gte("fecha_vencimiento", hoy) }
                }.decodeList<LoteDTO>()

                val ofertasLote = lotes.map { lote ->
                    OfertaLote(
                        idLote = lote.idLote ?: "",
                        tiendaId = id,
                        productoNombre = lote.productos?.nombre ?: "",
                        productoImagen = lote.productos?.imagen,
                        productoPresentacion = lote.productos?.presentacion ?: "",
                        tiendaNombre = tiendaActual?.nombre ?: "",
                        tiendaDireccion = tiendaActual?.direccion ?: "",
                        cantidad = lote.cantidad,
                        fechaVencimiento = lote.fechaVencimiento,
                        precioNormal = lote.precioNormal,
                        precioOferta = lote.precioOferta ?: 0.0,
                        numeroLote = lote.numeroLote ?: "",
                        ratingTienda = tiendaActual?.ratingPromedio ?: 0.0
                    )
                }
                ofertasAdapter.submitList(ofertasLote)
            } catch (e: Exception) { Log.e("TiendaDetail", "Error", e) }
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




    private fun mostrarDialogoReserva(oferta: OfertaLote) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reserva, null)

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
    private fun generarQRBodega(idDeTienda: String) {
        try {
            val bitmap = BarcodeEncoder().encodeBitmap("https://Ale152277.github.io/donapp-web/tienda/$idDeTienda", BarcodeFormat.QR_CODE, 400, 400)
            binding.ivQrTienda.setImageBitmap(bitmap)
        } catch (e: Exception) { Log.e("TiendaDetail", "Error QR", e) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}