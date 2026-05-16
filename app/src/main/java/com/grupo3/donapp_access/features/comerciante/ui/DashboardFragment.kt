package com.grupo3.donapp_access.features.comerciante.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.comerciante.ui.InventarioAdapter
import com.grupo3.donapp_access.databinding.FragmentDashboardBinding
import com.grupo3.donapp_access.features.comerciante.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    @Inject
    lateinit var supabase: SupabaseClient

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Observar datos del ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nombreTienda.collect { nombre ->
                binding.tvNombreTienda.text = nombre
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalLotes.collect { cantidad ->
                binding.tvOfertasActivas.text = cantidad.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clientesAlcanzados.collect { cantidad ->
                binding.tvClientesAlcanzados.text = cantidad.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lotesActivos.collect { lotes ->
                (binding.rvOfertasDashboard.adapter as? InventarioAdapter)?.submitList(lotes)
            }
        }

        // Obtener el ID del usuario actual de Supabase Auth para cargar estadísticas
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            viewModel.cargarDatosDashboard(userId)
        }

        // NAVEGACIÓN AL INVENTARIO
        binding.tvVerTodas.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(InventarioFragment())
        }

        // Botón para publicar nuevo lote
        binding.btnNuevaOferta.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(PublicarLoteFragment())
        }
    }

    private fun setupRecyclerView() {
        binding.rvOfertasDashboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOfertasDashboard.adapter = InventarioAdapter { lote ->
            val fragment = DetalleLoteFragment()
            val bundle = Bundle().apply {
                putString("lote_id", lote.id_lote)
            }
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}