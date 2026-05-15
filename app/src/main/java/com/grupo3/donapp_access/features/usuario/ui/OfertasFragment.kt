package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.databinding.FragmentOfertasBinding
import com.grupo3.donapp_access.features.usuario.OfertasViewModel
import com.grupo3.donapp_access.model.OfertaLote

class OfertasFragment : Fragment() {
    private var _binding: FragmentOfertasBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: OfertasViewModel
    private val ofertaAdapter = OfertaAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[OfertasViewModel::class.java]
        configurarLista()
        observarOfertas()

        binding.buttonRecargarOfertas.setOnClickListener {
            viewModel.cargarOfertas()
        }

        viewModel.cargarOfertas()
    }

    private fun configurarLista() {
        binding.recyclerOfertas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ofertaAdapter
        }
    }

    private fun observarOfertas() {
        viewModel.ofertas.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> mostrarCarga()
                is UiState.Success -> mostrarOfertas(state.data)
                is UiState.Error -> mostrarError(state.message)
            }
        }
    }

    private fun mostrarCarga() = with(binding) {
        progressOfertas.isVisible = true
        recyclerOfertas.isVisible = false
        textEstadoOfertas.isVisible = false
    }

    private fun mostrarOfertas(ofertas: List<OfertaLote>) = with(binding) {
        progressOfertas.isVisible = false
        recyclerOfertas.isVisible = ofertas.isNotEmpty()
        textEstadoOfertas.isVisible = ofertas.isEmpty()
        textEstadoOfertas.text = "No hay ofertas activas"
        ofertaAdapter.submitList(ofertas)
    }

    private fun mostrarError(message: String) = with(binding) {
        progressOfertas.isVisible = false
        recyclerOfertas.isVisible = false
        textEstadoOfertas.isVisible = true
        textEstadoOfertas.text = message
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
