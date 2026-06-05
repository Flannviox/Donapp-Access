package com.grupo3.donapp_access.features.comerciante.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.databinding.FragmentInventarioBinding
import com.grupo3.donapp_access.features.comerciante.InventarioViewModel
import com.grupo3.donapp_access.features.lotes.LoteRepository
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InventarioFragment : Fragment() {

    @Inject
    lateinit var supabase: SupabaseClient

    private var _binding: FragmentInventarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by viewModels()
    private lateinit var adapter: InventarioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observarEstado()

        binding.fabAgregarLote.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PublicarLoteFragment())
                .addToBackStack(null)
                .commit()
        }

        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val tiendaId = LoteRepository().getIdTiendaDelUsuario(userId)
                    viewModel.obtenerInventario(tiendaId)
                }catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró tu tienda: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } else {
            viewModel.obtenerInventario("test_seller_id")
            Toast.makeText(requireContext(), "Modo Vista Previa: Usando ID de prueba", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = InventarioAdapter { loteSeleccionado ->
            val fragment = DetalleLoteFragment().apply {

                arguments = Bundle().apply {
                    putString("lote_id", loteSeleccionado.id_lote)

                }
            }
            (requireActivity() as MainActivity).navegarA(fragment)
        }
        binding.rvInventario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInventario.adapter = adapter
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lotesState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvInventario.visibility = View.GONE

                        binding.layoutInventarioVacio.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        adapter.submitList(state.data)

                        if (state.data.isEmpty()) {
                            // Si no hay productos: mostramos el mensaje, ocultamos la lista
                            binding.rvInventario.visibility = View.GONE
                            binding.layoutInventarioVacio.visibility = View.VISIBLE
                        } else {
                            // Si hay productos: mostramos la lista, ocultamos el mensaje
                            binding.rvInventario.visibility = View.VISIBLE
                            binding.layoutInventarioVacio.visibility = View.GONE
                        }
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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