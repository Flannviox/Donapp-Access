package com.grupo3.donapp_access.features.usuario.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentNotificationsHistorialBinding
import com.grupo3.donapp_access.features.cliente.ui.TiendaDetailFragment

class NotificacionesHistorialFragment : Fragment() {

    private var _binding: FragmentNotificationsHistorialBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolverPerfil.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val sharedPreferences = requireContext().getSharedPreferences("donapp_alertas", Context.MODE_PRIVATE)
        val historialSet = sharedPreferences.getStringSet("historial", emptySet()) ?: emptySet()

        val listaAlertas = historialSet.toList().sortedDescending()

        binding.rvNotificaciones.layoutManager = LinearLayoutManager(requireContext())

        val adapter = NotificacionesAdapter(listaAlertas) { tiendaId ->

            val detalleTiendaFragment = TiendaDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("id_tienda", tiendaId)
                }
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, detalleTiendaFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvNotificaciones.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}