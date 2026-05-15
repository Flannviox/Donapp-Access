package com.grupo3.donapp_access.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentAccesibilidadBinding

class AccesibilidadFragment : Fragment() {

    private var _binding: FragmentAccesibilidadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccesibilidadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // Regresar
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Lógica del Switch de TalkBack
        binding.switchTalkback.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvTalkbackStatus.text = "✓ TalkBack está activo"
                binding.tvTalkbackStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.donapp_success))
            } else {
                binding.tvTalkbackStatus.text = "✕ TalkBack está inactivo"
                binding.tvTalkbackStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.donapp_text_secondary))
            }
        }

        // Slider de tamaño de letra (Opcional: podrías aplicar el cambio de fuente aquí)
        binding.sliderFontSize.addOnChangeListener { _, value, _ ->
            // Aquí podrías implementar la lógica para cambiar el tamaño de fuente de la app
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
