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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.databinding.FragmentReservasComercianteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReservasComercianteFragment : Fragment() {

    private var _binding: FragmentReservasComercianteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReservasComercianteViewModel by viewModels()
    private lateinit var adapter: ReservaComercianteAdapter

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
            mostrarDialogoConfirmacion(reserva)
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
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacion(reserva: ReservaComercianteDetalle) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Entrega")
            .setMessage("¿Estás seguro de que deseas marcar los ${reserva.cantidad}x '${reserva.nombreProducto}' como entregados a ${reserva.nombreCliente}?")
            .setPositiveButton("Sí, entregar") { dialog, _ ->
                viewModel.marcarReservaComoEntregada(reserva.id_reservas)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}