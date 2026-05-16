package com.grupo3.donapp_access.features.auth.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.auth.HomeUsuarioViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeUsuarioFragment : Fragment() {

    // Hilt inyecta el ViewModel automáticamente
    private val viewModel: HomeUsuarioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_usuario, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Disparamos la consulta a Supabase al abrir la pantalla
// 2. Escuchamos la respuesta de la base de datos
        viewModel.fetchLotesVencenHoy()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lotes.collect { listaOfertas ->
                if (listaOfertas.isNotEmpty()) {
                    Log.d("OFERTAS", "¡Éxito! Encontramos ${listaOfertas.size} lotes.")
                    Toast.makeText(requireContext(), "Ofertas de hoy: ${listaOfertas.size}", Toast.LENGTH_LONG).show()

                    // CAMBIO AQUÍ: Ahora usamos .producto (en singular) como pusimos en el LoteDTO
                    val primerProducto = listaOfertas[0].productosId
                    Log.d("OFERTAS", "Primer producto: $primerProducto")
                }
            }
        }
    }
}