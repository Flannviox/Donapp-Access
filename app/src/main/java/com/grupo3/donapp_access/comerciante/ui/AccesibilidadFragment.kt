package com.grupo3.donapp_access.comerciante.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentAccesibilidadBinding

class AccesibilidadFragment : Fragment() {

    private var _binding: FragmentAccesibilidadBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "donapp_accessibility_prefs"
    private val KEY_TALKBACK = "talkback_enabled"
    private val KEY_SOUNDS = "sounds_enabled"
    private val KEY_VIBRATION = "vibration_enabled"
    private val KEY_FONT_SIZE = "font_size"

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

        loadPreferences()
        setupListeners()
        setupNavigation()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Switch TalkBack
        binding.switchTalkback.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_TALKBACK, isChecked)
            updateTalkbackStatus(isChecked)
        }

        // Switch Sonidos
        binding.switchSounds.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_SOUNDS, isChecked)
            val msg = if (isChecked) "Sonidos activados" else "Sonidos desactivados"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Switch Vibración
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_VIBRATION, isChecked)
            val msg = if (isChecked) "Vibración activada" else "Vibración desactivada"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Slider de Tamaño de Letra
        binding.sliderFontSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                savePreference(KEY_FONT_SIZE, value.toInt())
                // Aquí podrías aplicar el cambio de fuente globalmente si tuvieras un BaseActivity
            }
        }
    }

    private fun updateTalkbackStatus(isActive: Boolean) {
        if (isActive) {
            binding.tvTalkbackStatus.text = "✓ TalkBack está activo"
            binding.tvTalkbackStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.donapp_success))
        } else {
            binding.tvTalkbackStatus.text = "✗ TalkBack está inactivo"
            binding.tvTalkbackStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.donapp_error))
        }
    }

    private fun setupNavigation() {
        binding.btnNavInicio.setOnClickListener {
            Toast.makeText(requireContext(), "Navegando a Inicio...", Toast.LENGTH_SHORT).show()
            // Simular navegación: parentFragmentManager.beginTransaction().replace(...).commit()
        }
        
        binding.btnNavNegocio.setOnClickListener {
            Toast.makeText(requireContext(), "Ya estás en la sección de Negocio", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnNavPerfil.setOnClickListener {
            Toast.makeText(requireContext(), "Navegando a Perfil...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePreference(key: String, value: Any) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
        }
        editor.apply()
    }

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Cargar TalkBack
        val talkbackEnabled = prefs.getBoolean(KEY_TALKBACK, true)
        binding.switchTalkback.isChecked = talkbackEnabled
        updateTalkbackStatus(talkbackEnabled)

        // Cargar Sonidos y Vibración
        binding.switchSounds.isChecked = prefs.getBoolean(KEY_SOUNDS, true)
        binding.switchVibration.isChecked = prefs.getBoolean(KEY_VIBRATION, true)

        // Cargar Tamaño de letra
        val fontSize = prefs.getInt(KEY_FONT_SIZE, 18)
        binding.sliderFontSize.value = fontSize.toFloat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
