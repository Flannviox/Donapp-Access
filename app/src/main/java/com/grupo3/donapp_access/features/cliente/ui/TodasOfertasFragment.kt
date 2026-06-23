package com.grupo3.donapp_access.features.cliente.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.app.MainActivity
import com.grupo3.donapp_access.databinding.FragmentTodasOfertasBinding
import com.grupo3.donapp_access.features.cliente.ClienteRepository
import com.grupo3.donapp_access.features.cliente.ui.TiendaDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TodasOfertasFragment : Fragment() {

    private var _binding: FragmentTodasOfertasBinding? = null
    private val binding get() = _binding!!

    // Reutilizamos tu adaptador de ofertas del Home
    private lateinit var ofertasAdapter: HomeOfertaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodasOfertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        cargarTodasLasOfertas()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvTodasOfertas.layoutManager = LinearLayoutManager(requireContext())

        ofertasAdapter = HomeOfertaAdapter(requireContext()) { oferta ->
            oferta.tiendaId?.let { id ->
                (requireActivity() as MainActivity).navegarA(
                    TiendaDetailFragment.newInstance(id, oferta.tiendaNombre)
                )
            }
        }
        binding.rvTodasOfertas.adapter = ofertasAdapter
    }

    private fun cargarTodasLasOfertas() {
        binding.progressBar.visibility = View.VISIBLE
        val repository = ClienteRepository()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ofertas = withContext(Dispatchers.IO) {
                    repository.obtenerOfertas()
                }
                binding.progressBar.visibility = View.GONE
                ofertasAdapter.submitList(ofertas)

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.e("TodasOfertas", "Error al cargar ofertas: ${e.message}", e)
                Toast.makeText(context, "Error al cargar las ofertas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}