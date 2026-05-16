package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.databinding.FragmentBuscarBinding
import com.grupo3.donapp_access.features.usuario.BuscarViewModel
import com.grupo3.donapp_access.model.Categoria
import kotlinx.coroutines.launch

class BuscarFragment : Fragment() {
    private var _binding: FragmentBuscarBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BuscarViewModel
    private lateinit var categoriaAdapter: CategoriaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuscarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[BuscarViewModel::class.java]
        configurarCategorias()
        observarCategorias()

        binding.inputBuscar.doAfterTextChanged { editable ->
            viewModel.filtrarCategorias(editable?.toString().orEmpty())
        }
        binding.buttonRecargarCategorias.setOnClickListener {
            viewModel.cargarCategorias()
        }

        viewModel.cargarCategorias()
    }

    private fun configurarCategorias() {
        categoriaAdapter = CategoriaAdapter(::seleccionarCategoria)
        binding.recyclerCategorias.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = categoriaAdapter
        }
    }

    private fun observarCategorias() {
       viewLifecycleOwner.lifecycleScope.launch {
           repeatOnLifecycle(Lifecycle.State.STARTED){
               viewModel.categorias.collect { state ->
                   when (state) {
                       is UiState.Loading -> mostrarCarga()
                       is UiState.Success -> mostrarCategorias(state.data)
                       is UiState.Error -> mostrarError(state.message)
                   }
               }
           }
       }
    }

    private fun seleccionarCategoria(categoria: Categoria) {
        binding.inputBuscar.setText(categoria.nombre)
        binding.inputBuscar.setSelection(categoria.nombre.length)
        Toast.makeText(requireContext(), categoria.nombre, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarCarga() = with(binding) {
        progressCategorias.isVisible = true
        textEstadoCategorias.isVisible = false
        buttonRecargarCategorias.isVisible = false
        recyclerCategorias.isVisible = false
    }

    private fun mostrarCategorias(categorias: List<Categoria>) = with(binding) {
        progressCategorias.isVisible = false
        buttonRecargarCategorias.isVisible = false
        recyclerCategorias.isVisible = categorias.isNotEmpty()
        textEstadoCategorias.isVisible = categorias.isEmpty()
        textEstadoCategorias.text = "No encontramos categorias"
        categoriaAdapter.submitList(categorias)
    }

    private fun mostrarError(message: String) = with(binding) {
        progressCategorias.isVisible = false
        recyclerCategorias.isVisible = false
        textEstadoCategorias.isVisible = true
        buttonRecargarCategorias.isVisible = true
        textEstadoCategorias.text = message
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
