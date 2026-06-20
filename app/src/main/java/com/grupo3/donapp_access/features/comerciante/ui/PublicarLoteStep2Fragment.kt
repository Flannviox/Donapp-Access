package com.grupo3.donapp_access.features.comerciante.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.grupo3.donapp_access.databinding.FragmentPublicarLoteStep2Binding
import com.grupo3.donapp_access.features.lotes.PublicarViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PublicarLoteStep2Fragment : Fragment() {

    private var _binding: FragmentPublicarLoteStep2Binding? = null
    private val binding get() = _binding!!

    //se usara el mismo viemodel usando activityviewmodels para que persista entre
    //fragments

    private val viewModel: PublicarViewModel by activityViewModels()

    private var fechaSeleccionada: String? =null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublicarLoteStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSeleccionarFecha.setOnClickListener { mostrarDatePicker() }
        binding.btnPublicarLote.setOnClickListener { publicarLote() }

        observarViewModel()
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is PublicarViewModel.PublicarState.Loading -> {
                            binding.btnPublicarLote.isEnabled = false
                            binding.btnPublicarLote.text = "Publicando..."
                        }
                        is PublicarViewModel.PublicarState.LotePublicado -> {
                            toast("¡Lote publicado!")
                            // Limpia todo el backstack hasta el Dashboard
                            parentFragmentManager.popBackStack(null,
                                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        }
                        is PublicarViewModel.PublicarState.Error -> {
                            binding.btnPublicarLote.isEnabled = true
                            binding.btnPublicarLote.text = "Publicar lote"
                            toast(state.mensaje, long = true)
                            viewModel.resetearEstado()
                        }
                        else -> {
                            binding.btnPublicarLote.isEnabled = true
                            binding.btnPublicarLote.text = "Publicar lote"
                        }
                    }
                }
            }
        }
    }

    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Fecha de vencimiento")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val fechaStr = sdf.format(Date(millis))
            fechaSeleccionada = fechaStr
            binding.btnSeleccionarFecha.text = fechaStr
        }

        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun publicarLote() {
        val cantidad = binding.etCantidad.text?.toString()?.toIntOrNull()
        if (cantidad == null || cantidad <= 0) {
            binding.tilCantidad.error = "Ingresá una cantidad válida"
            return
        }

        binding.tilCantidad.error = null

        val fecha = fechaSeleccionada
        if (fecha == null) {
            toast("Seleccioná una fecha de vencimiento")
            return
        }

        val precioNormal = binding.etPrecioNormal.text?.toString()?.toDoubleOrNull()
        if (precioNormal == null || precioNormal <= 0) {
            binding.tilPrecioNormal.error = "Ingresá un precio válido"
            return
        }
        binding.tilPrecioNormal.error = null

        val precioOfertaStr = binding.etPrecioOferta.text?.toString()?.trim().orEmpty()
        val precioOferta = if (precioOfertaStr.isEmpty()) null else precioOfertaStr.toDoubleOrNull()


        if (precioOferta != null) {
            if (precioOferta <= 0) {
                binding.tilPrecioOferta.error = "El precio oferta debe ser mayor a 0"
                return
            }
            if (precioOferta >= precioNormal) {
                binding.tilPrecioOferta.error = "El precio oferta debe ser menor al normal"
                return
            }
        }
        binding.tilPrecioOferta.error = null

        viewModel.publicarLote(cantidad, fecha, precioNormal, precioOferta)
    }

    private fun toast(msg: String, long: Boolean = false) =
        Toast.makeText(requireContext(), msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}