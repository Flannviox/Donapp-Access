package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentAccesibilidadBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager //////////
class AccesibilidadFragment : Fragment() {

    private var _binding: FragmentAccesibilidadBinding? = null
    private val binding get() = _binding!!
    private var soundPool: android.media.SoundPool? = null
    private var soundId: Int = 0

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

        inicilizarSonidos()
        cargarPreferencias()
        setupListeners()
    }

    private fun inicilizarSonidos() {
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = android.media.SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()

        soundId = 0
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)

        // cargar temas
        val temaActual = prefs.getString("tema_actual", "normal")
        binding.switchHighContrast.isChecked = temaActual == "high_contrast"
        binding.switchBlackWhite.isChecked = temaActual == "black_white"

        val fontScale = prefs.getFloat("font_scale", 1f)
        binding.sliderFontSize.value = fontScale * 18f

        //cargar estados de interruptores
        binding.switchTalkback.isChecked = prefs.getBoolean("talkback_activo", false)
        binding.switchSounds.isChecked = prefs.getBoolean("sonidos_activos", false)
        binding.switchVibration.isChecked = prefs.getBoolean("vibracion_activa", false)

        actualizarEstadoTalkback(binding.switchTalkback.isChecked)
        //cargar estado de nuestro Asistente de Voz
        val voicePrefs = requireContext().getSharedPreferences("AccesibilidadPrefs", android.content.Context.MODE_PRIVATE)
        binding.switchVoiceAssistant.isChecked = voicePrefs.getBoolean("voice_assistant_enabled", false)
        binding.sliderVoiceVolume.value = voicePrefs.getFloat("voice_volume", 1.0f)
    }

    private fun aplicarTema(tema: String) {
        val prefs = requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("tema_actual", tema)
            .putBoolean("alto_contraste", tema == "high_contrast")
            .putString("rol_guardado", (requireActivity() as MainActivity).obtenerRol())
            .apply()

        viewLifecycleOwner.lifecycleScope.launch {
            guardarPreferenciasSuspend()
            activity?.recreate()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.switchHighContrast.setOnClickListener {
            val isChecked = binding.switchHighContrast.isChecked
            if (isChecked) {
                binding.switchBlackWhite.isChecked = false
                aplicarTema("high_contrast")
            } else {
                aplicarTema("normal")
            }
        }

        binding.switchBlackWhite.setOnClickListener {
            val isChecked = binding.switchBlackWhite.isChecked
            if (isChecked) {
                binding.switchHighContrast.isChecked = false
                aplicarTema("black_white")
            } else {
                aplicarTema("normal")
            }
        }

        binding.switchTalkback.setOnCheckedChangeListener { _, isChecked ->
            // Guardamos localmente para que sea independiente
            requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("talkback_activo", isChecked).apply()

            actualizarEstadoTalkback(isChecked)
            if (isChecked) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
                startActivity(intent)
            }
            viewLifecycleOwner.lifecycleScope.launch { guardarPreferenciasSuspend() }
        }

        binding.switchSounds.setOnCheckedChangeListener { _, isChecked ->
            requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("sonidos_activos", isChecked).apply()

            if (isChecked) {
                android.media.RingtoneManager.getRingtone(
                    requireContext(),
                    android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_NOTIFICATION
                    )
                ).play()
            }
            viewLifecycleOwner.lifecycleScope.launch { guardarPreferenciasSuspend() }
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("vibracion_activa", isChecked).apply()

            viewLifecycleOwner.lifecycleScope.launch { guardarPreferenciasSuspend() }
        }

        binding.sliderFontSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("font_scale", value / 18f)
                    .apply()
                viewLifecycleOwner.lifecycleScope.launch {
                    guardarPreferenciasSuspend()
                    activity?.let { activity ->
                        val intent = activity.intent
                        activity.finish()
                        activity.startActivity(intent)
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            }
        }
        //listeners del asstente de voz
        binding.switchVoiceAssistant.setOnCheckedChangeListener { _, isChecked ->
            //guardamos en sus preferencias exclusivas
            requireContext().getSharedPreferences("AccesibilidadPrefs", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("voice_assistant_enabled", isChecked).apply()

            if (isChecked) {
                // Pequeña prueba de sonido para que el usuario sepa que funciona
                VoiceAssistantManager.speak("Asistente de voz activado")
            } else {
                VoiceAssistantManager.stop()
            }
        }

        binding.sliderVoiceVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                requireContext().getSharedPreferences("AccesibilidadPrefs", android.content.Context.MODE_PRIVATE)
                    .edit().putFloat("voice_volume", value).apply()

                // se lee en voz alta el porcentaje para que pruebe el volumen
                val porcentaje = (value * 100).toInt()
                VoiceAssistantManager.speak("Volumen al $porcentaje por ciento")
            }
        }
    }

    private fun actualizarEstadoTalkback(activo: Boolean) {
        if (activo) {
            binding.tvTalkbackStatus.text = "TALKBACK ACTIVO"
            binding.tvTalkbackStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.donapp_success)
            )
        } else {
            binding.tvTalkbackStatus.text = "TALK BACK INACTIVO"
            binding.tvTalkbackStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.donapp_error)
            )
        }
    }

    private suspend fun guardarPreferenciasSuspend() {
        try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return

            val nuevoTipo = when {
                binding.switchTalkback.isChecked || binding.switchHighContrast.isChecked -> "VISUAL"
                binding.switchSounds.isChecked || binding.switchVibration.isChecked -> "MOTRIZ"
                else -> "NINGUNA"
            }

            SupabaseClient.client.from("usuarios").update(
                { set("tipo_discapacidad", nuevoTipo) }
            ) {
                filter { eq("id_usuarios", userId) }
            }
        } catch (e: Exception) {
            android.util.Log.e("ACCESIBILIDAD", "ERROR AL GUARDAR EN BD: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Serializable
private data class AccesibilidadDTO(
    @SerialName("tipo_discapacidad") val tipoDiscapacidad: String? = null
)