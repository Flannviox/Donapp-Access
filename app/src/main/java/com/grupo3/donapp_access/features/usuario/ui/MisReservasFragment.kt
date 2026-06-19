package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.databinding.FragmentMisReservasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat

@AndroidEntryPoint
class MisReservasFragment : Fragment() {

    private var _binding: FragmentMisReservasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MisReservasViewModel by viewModels()
    private lateinit var adapter: ReservaAdapter
    private var haHabladoReservas = false

    private var qrDialog: android.app.AlertDialog? = null
    private var reservaAbiertaId: String? = null

    private var jobPolling: Job? = null

    override fun onResume() {
        super.onResume()
        haHabladoReservas = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisReservasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observarViewModel()
        viewModel.cargarMisReservas()
    }

    private fun setupRecyclerView() {
        adapter = ReservaAdapter { reserva ->
            mostrarCodigoQR(reserva)
        }
        binding.rvReservas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservas.adapter = adapter
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reservasState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.tvSinReservas.isVisible = false
                        }
                        is UiState.Success -> {
                            val reservas = state.data
                            binding.tvSinReservas.isVisible = reservas.isEmpty()
                            adapter.submitList(reservas)

                            if (reservaAbiertaId != null && qrDialog?.isShowing == true) {
                                val reservaActual = reservas.find { it.id_reservas == reservaAbiertaId }
                                if (reservaActual != null && reservaActual.estado?.lowercase() == "completada") {
                                    qrDialog?.dismiss()
                                    Toast.makeText(requireContext(), "¡Reserva entregada con éxito!", Toast.LENGTH_LONG).show()
                                    VoiceAssistantManager.speak("Tu reserva ha sido entregada con éxito en la tienda.")
                                }
                            }

                            if (!haHabladoReservas) {
                                val cantidadActivas = reservas.count { it.estado.equals("ACTIVA", ignoreCase = true) }
                                when (cantidadActivas) {
                                    0 -> VoiceAssistantManager.speak("No tienes reservas activas por recoger.")
                                    1 -> VoiceAssistantManager.speak("Tienes una reserva activa pendiente de recojo.")
                                    else -> VoiceAssistantManager.speak("Tienes $cantidadActivas reservas activas pendientes de recojo.")
                                }
                                haHabladoReservas = true
                            }
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun mostrarCodigoQR(reserva: ReservaDetalle) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(reserva.id_reservas, BarcodeFormat.QR_CODE, 600, 600)

            val dialogView = layoutInflater.inflate(R.layout.dialog_qr_reserva, null)
            val ivQr = dialogView.findViewById<ImageView>(R.id.ivCodigoQR)
            val tvMensaje = dialogView.findViewById<android.widget.TextView>(R.id.tvMensajeQR)
            val btnCerrar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCerrarQR)

            ivQr.setImageBitmap(bitmap)
            tvMensaje.text = "Muestra este código en '${reserva.nombreTienda}' para recoger tu ${reserva.nombreProducto}."

            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            qrDialog = dialog
            reservaAbiertaId = reserva.id_reservas
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCerrar.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setOnDismissListener {
                qrDialog = null
                reservaAbiertaId = null
                jobPolling?.cancel()
            }

            dialog.show()

            jobPolling = viewLifecycleOwner.lifecycleScope.launch {
                while (isActive && dialog.isShowing) {
                    delay(3000)
                    viewModel.cargarMisReservas()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al generar el QR", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        qrDialog?.dismiss()
        qrDialog = null
        reservaAbiertaId = null
        jobPolling?.cancel()

        super.onDestroyView()
        _binding = null
    }
}