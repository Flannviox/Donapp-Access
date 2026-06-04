package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.databinding.FragmentTodasTiendasBinding
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import com.grupo3.donapp_access.usuario.ui.TiendaDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TodasTiendasFragment : Fragment() {

    private var _binding: FragmentTodasTiendasBinding? = null
    private val binding get() = _binding!!
    private lateinit var tiendasAdapter: TodasTiendasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodasTiendasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        cargarTodasLasTiendas()
    }

    private fun setupUI() {
        // Configurar botón de retroceso
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        //Configurar RecyclerView en una sola lista vertical
        binding.rvTodasTiendas.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar el adaptador con las tarjetas anchasto
        tiendasAdapter = TodasTiendasAdapter(requireContext()) { tienda ->
            val id = tienda.idTienda
            if (id != null && id.isNotBlank()) {
                (requireActivity() as MainActivity).navegarA(
                    TiendaDetailFragment.newInstance(id, tienda.nombre)
                )
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error: La tienda no tiene ID",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.rvTodasTiendas.adapter = tiendasAdapter
    }

    private fun cargarTodasLasTiendas() {
        binding.progressBar.visibility = View.VISIBLE

        val repository = ClienteRepository()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tiendas = withContext(Dispatchers.IO) {
                    repository.obtenerTiendas()
                }

                binding.progressBar.visibility = View.GONE
                tiendasAdapter.submitList(tiendas)

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                android.util.Log.e("TodasTiendas", "Error al cargar tiendas: ${e.message}", e)
                android.widget.Toast.makeText(context, "Error al cargar las bodegas", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}