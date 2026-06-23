package com.grupo3.donapp_access.features.comerciante.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.app.PortraitCaptureActivity
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.databinding.FragmentReservasComercianteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
@AndroidEntryPoint
class ReservasComercianteFragment : Fragment() {

    private var _binding: FragmentReservasComercianteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReservasComercianteViewModel by viewModels()
    private lateinit var adapter: ReservaComercianteAdapter
    private var haHabladoReservas = false

    // AQUÍ RESOLVEMOS LA DUDA 3 (El resultado de escanear y actualizar la Base de datos)
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val idReservaEscaneado = result.contents

            // Mostramos el diálogo de confirmación antes de actualizar la base de datos
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("¡QR Escaneado con Éxito!")
                .setMessage("¿Deseas confirmar la entrega de esta reserva?")
                .setPositiveButton("Confirmar Entrega") { dialog, _ ->
                    viewModel.marcarReservaComoEntregada(idReservaEscaneado)
                    Toast.makeText(requireContext(), "Entrega confirmada exitosamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false) // Obliga a presionar un botón
                .show()

        } else {
            Toast.makeText(requireContext(), "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        haHabladoReservas = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservasComercianteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observarViewModel()
        viewModel.cargarReservasDeMiTienda()
    }

    private fun setupRecyclerView() {
        adapter = ReservaComercianteAdapter { reserva ->
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Escanea el QR del cliente para confirmar la entrega")
                setCameraId(0) // Usa la cámara trasera
                setBeepEnabled(true) // Sonido al escanear
                setBarcodeImageEnabled(false)

                // NUEVO: Bloqueamos la rotación y usamos nuestra actividad vertical
                setOrientationLocked(true)
                setCaptureActivity(PortraitCaptureActivity::class.java)
            }
            barcodeLauncher.launch(options)
        }

        binding.rvReservasComerciante.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReservasComerciante.adapter = adapter
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reservasState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.tvSinReservasComerciante.isVisible = false
                        }
                        is UiState.Success -> {
                            val reservas = state.data
                            binding.tvSinReservasComerciante.isVisible = reservas.isEmpty()
                            adapter.submitList(reservas)

                            if (!haHabladoReservas) {
                                val cantidadActivas = reservas.count { it.estado.equals("ACTIVA", ignoreCase = true) }
                                when (cantidadActivas) {
                                    0 -> VoiceAssistantManager.speak("No tienes reservas activas.")
                                    1 -> VoiceAssistantManager.speak("Tienes una reserva activa.")
                                    else -> VoiceAssistantManager.speak("Tienes $cantidadActivas reservas activas.")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}