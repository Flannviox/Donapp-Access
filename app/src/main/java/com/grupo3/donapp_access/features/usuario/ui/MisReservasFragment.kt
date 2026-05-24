package com.grupo3.donapp_access.features.usuario.ui

import android.app.AlertDialog
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

@AndroidEntryPoint
class MisReservasFragment : Fragment() {

    private var _binding: FragmentMisReservasBinding? = null
    private val binding get() = _binding!!

    // Inyectamos el ViewModel que acabamos de arreglar
    private val viewModel: MisReservasViewModel by viewModels()
    private lateinit var adapter: ReservaAdapter

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

        // Disparar la carga inicial a Supabase
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
                            // Si la lista está vacía, mostramos el texto "Aún no tienes reservas"
                            binding.tvSinReservas.isVisible = reservas.isEmpty()
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

    private fun mostrarCodigoQR(reserva: ReservaDetalle) {
        // Mostrar un diálogo con el mockup del QR que tienes en res/drawable
        val imageView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.qr_image)
            setPadding(32, 32, 32, 32)
        }

        // AHORA USAMOS EL BUILDER MODERNO DE MATERIAL DESIGN
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Código de Recojo")
            .setMessage("Muestra este código en '${reserva.nombreTienda}' para recoger tu ${reserva.nombreProducto}.")
            .setView(imageView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}