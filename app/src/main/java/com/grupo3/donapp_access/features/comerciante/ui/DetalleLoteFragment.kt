package com.grupo3.donapp_access.features.comerciante.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.databinding.FragmentDetalleLoteBinding
import com.grupo3.donapp_access.features.comerciante.InventarioViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DetalleLoteFragment : Fragment() {

    @Inject
    lateinit var supabase: SupabaseClient

    private var _binding: FragmentDetalleLoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InventarioViewModel by viewModels()

    private var loteIdActual: String = ""
    private var tiendaId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleLoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loteIdActual = arguments?.getString("lote_id") ?: ""
        tiendaId = supabase.auth.currentUserOrNull()?.id ?: "test_seller_id"

        if (loteIdActual.isEmpty()) {
            Toast.makeText(requireContext(), "ID de lote no encontrado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.cargarLoteSeleccionado(loteIdActual)
        observarLoteEnTiempoReal()
        configurarBotones()
    }

    private fun observarLoteEnTiempoReal() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loteActual.collect { lote ->
                lote?.let {
                    binding.tvTituloDetalle.text = if (it.productos_id.length < 20) it.productos_id else "Lote: ${it.numero_lote}"
                    binding.etCantidad.setText(it.cantidad.toString())
                    binding.etPrecioNormal.setText(it.precio_normal.toString())
                    binding.etPrecioOferta.setText(it.precio_oferta?.toString() ?: "")

                    binding.tvEstadoActual.text = it.estado.uppercase()
                    when(it.estado) {
                        "en_oferta" -> binding.tvEstadoActual.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5C518"))
                        "disponible" -> binding.tvEstadoActual.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                        "agotado" -> {
                            binding.tvEstadoActual.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))

                        }
                    }
                }
            }
        }
    }

    private fun configurarBotones() {
        binding.btnGuardar.setOnClickListener {
            val cantidad = binding.etCantidad.text.toString().toIntOrNull() ?: 0
            val pNormal = binding.etPrecioNormal.text.toString().toDoubleOrNull() ?: 0.0
            val pOferta = binding.etPrecioOferta.text.toString().toDoubleOrNull()

            if (tiendaId.isNotEmpty()) {
                viewModel.actualizarDatosLote(loteIdActual, tiendaId, cantidad, pNormal, pOferta)
                Toast.makeText(context, "Lote actualizado exitosamente", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }



        binding.btnEliminar.setOnClickListener {
            viewModel.eliminarLote(loteIdActual)
            Toast.makeText(context, "Lote eliminado correctamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}