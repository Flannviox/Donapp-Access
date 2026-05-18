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
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.usuario.dto.UsuarioNombreDTO
import com.grupo3.donapp_access.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from


class BuscarFragment : Fragment() {
    private var _binding: FragmentBuscarBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BuscarViewModel
    private lateinit var categoriaAdapter: CategoriaAdapter

    private lateinit var ofertaAdapter: OfertaAdapter

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
        configurarOfertas() // NUEVA CONFIGURACIÓN

        observarCategorias()
        observarOfertas() // NUEVO OBSERVADOR
        cargarNombreUsuario()

        binding.inputBuscar.doAfterTextChanged { editable ->
            val texto = editable?.toString().orEmpty()
            viewModel.filtrarCategorias(texto)
            viewModel.buscarOfertas(texto) // Disparamos la búsqueda real

            // Lógica de visibilidad: si hay texto, ocultamos categorías
            binding.recyclerCategorias.isVisible = texto.isEmpty()
            // Asumiendo que agregaste recyclerOfertas en tu XML
            binding.recyclerOfertas.isVisible = texto.isNotEmpty()
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
    private fun configurarOfertas() {
        ofertaAdapter = OfertaAdapter { oferta ->
            // Aquí luego haremos la navegación al detalle de la tienda/oferta
            Toast.makeText(requireContext(), "Seleccionaste: ${oferta.productoNombre}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerOfertas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ofertaAdapter
        }

    }

    // NUEVO OBSERVADOR
    private fun observarOfertas() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ofertasBusqueda.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            // Puedes mostrar un progress bar de ofertas aquí
                        }
                        is UiState.Success -> {
                            ofertaAdapter.submitList(state.data)
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
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

    private fun cargarNombreUsuario() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter { eq("id_usuarios", userId) }
                    }
                    .decodeSingle<UsuarioNombreDTO>()

                if (_binding == null) return@launch

                binding.tvNombreUsuario.text = usuario.nombres
                binding.tvAvatarInicial.text = usuario.nombres.first().uppercase()

            } catch (e: Exception) {
                android.util.Log.e("BUSCAR_USER", "Error: ${e.message}", e)
                if (_binding == null) return@launch
                binding.tvNombreUsuario.text = "Usuario"
            }
        }
    }
}
