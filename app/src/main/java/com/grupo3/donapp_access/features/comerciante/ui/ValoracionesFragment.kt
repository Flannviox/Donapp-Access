package com.grupo3.donapp_access.features.comerciante.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentValoracionesBinding
import com.grupo3.donapp_access.features.auth.ui.ValoracionesAdapter
import com.grupo3.donapp_access.features.cliente.data.dto.UsuarioNombreDTO
import com.grupo3.donapp_access.features.cliente.data.dto.ValoracionDTO

class ValoracionesFragment : Fragment() {

    private var _binding: FragmentValoracionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ValoracionesAdapter
    private var todasLasValoraciones = listOf<ValoracionDTO>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentValoracionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        cargarDatosSimulados()
    }

    private fun setupRecyclerView() {
        adapter = ValoracionesAdapter(emptyList())
        binding.rvValoraciones.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ValoracionesFragment.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Configuración de Filtros
        binding.btnFilterAll.setOnClickListener {
            actualizarEstiloFiltro(it)
            adapter.updateList(todasLasValoraciones)
        }
        binding.btnFilter5.setOnClickListener { filterByRating(5, it) }
        binding.btnFilter4.setOnClickListener { filterByRating(4, it) }
        binding.btnFilter3.setOnClickListener { filterByRating(3, it) }
        binding.btnFilter2.setOnClickListener { filterByRating(2, it) }
        binding.btnFilter1.setOnClickListener { filterByRating(1, it) }

        // Navegación inferior
        binding.btnNavInicio.setOnClickListener {
            Toast.makeText(requireContext(), "Ir a Inicio", Toast.LENGTH_SHORT).show()
        }
        binding.btnNavPerfil.setOnClickListener {
            Toast.makeText(requireContext(), "Ir a Perfil", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterByRating(stars: Int, view: View) {
        actualizarEstiloFiltro(view)
        val filtradas = todasLasValoraciones.filter { it.calificacion.toInt() == stars }
        adapter.updateList(filtradas)
        if (filtradas.isEmpty()) {
            Toast.makeText(context, "No hay valoraciones de $stars estrellas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarEstiloFiltro(selectedView: View) {
        // resetea todos los botones de filtro al estilo secundario
        val filtros = listOf(
            binding.btnFilterAll, binding.btnFilter5, binding.btnFilter4,
            binding.btnFilter3, binding.btnFilter2, binding.btnFilter1
        )

        filtros.forEach { button ->
            button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.donapp_surface)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.donapp_text_primary))
        }

        // Resaltar el seleccionado
        selectedView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.donapp_primary)
        if (selectedView is MaterialButton) {
            selectedView.setTextColor(ContextCompat.getColor(requireContext(), R.color.donapp_bg))
        }
    }

    private fun cargarDatosSimulados() {
        // Datos basados en la estructura DTO consolidada
        todasLasValoraciones = listOf(
            ValoracionDTO(
                "1",
                5.0,
                "Excelente atención, muy accesibles para sillas de ruedas.",
                "2023-10-25T10:00:00",
                UsuarioNombreDTO("Carlos", "García")
            ),
            ValoracionDTO(
                "2",
                4.0,
                "Local muy limpio y rampa bien ubicada.",
                "2023-10-24T15:30:00",
                UsuarioNombreDTO("María", "López")
            ),
            ValoracionDTO(
                "3",
                5.0,
                "Personal capacitado en trato inclusivo.",
                "2023-10-23T09:15:00",
                UsuarioNombreDTO("Juan", "Pérez")
            ),
            ValoracionDTO(
                "4",
                2.0,
                "Los pasillos están un poco obstruidos hoy.",
                "2023-10-22T12:00:00",
                UsuarioNombreDTO("Ana", "Rodríguez")
            ),
            ValoracionDTO(
                "5",
                5.0,
                "¡La mejor experiencia inclusiva!",
                "2023-10-21T18:45:00",
                UsuarioNombreDTO("Luis", "Sánchez")
            )
        )

        adapter.updateList(todasLasValoraciones)

        // Actualizar UI de estadísticas
        binding.tvAverageRating.text = "4.2"
        binding.tvTendencia.text = "+15% este mes"
        binding.tvTasaRespuesta.text = "92%"

        // Configurar todas las barras de progreso según IDs del XML
        binding.pb5Stars.progress = 60
        binding.tv5StarsPct.text = "60%"

        binding.pb4Stars.progress = 20
        binding.tv4StarsPct.text = "20%"

        binding.pb3Stars.progress = 0
        binding.tv3StarsPct.text = "0%"

        binding.pb2Stars.progress = 20
        binding.tv2StarsPct.text = "20%"

        binding.pb1Star.progress = 0
        binding.tv1StarPct.text = "0%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}