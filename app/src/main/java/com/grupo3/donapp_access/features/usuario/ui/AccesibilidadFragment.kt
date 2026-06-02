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

        cargarPreferencias()
        setupListeners()
        inicilizarSonidos()
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
        val temaActual = prefs.getString("tema_actual", "normal")

        binding.switchHighContrast.isChecked = temaActual == "high_contrast"
        binding.switchBlackWhite.isChecked = temaActual == "black_white"

        val fontScale = prefs.getFloat("font_scale", 1f)
        binding.sliderFontSize.value = fontScale * 18f

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?: return@launch

                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select { filter { eq("id_usuarios", userId) } }
                    .decodeSingle<AccesibilidadDTO>()

                when (usuario.tipoDiscapacidad?.uppercase()) {
                    "VISUAL" -> {
                        binding.switchTalkback.isChecked = true
                        binding.switchSounds.isChecked = true
                        binding.switchVibration.isChecked = true
                        actualizarEstadoTalkback(true)
                    }
                    "MOTRIZ" -> {
                        binding.switchTalkback.isChecked = false
                        binding.switchSounds.isChecked = true
                        binding.switchVibration.isChecked = true
                        actualizarEstadoTalkback(false)
                    }
                    else -> {
                        binding.switchTalkback.isChecked = false
                        binding.switchSounds.isChecked = false
                        binding.switchVibration.isChecked = false
                        actualizarEstadoTalkback(false)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ACCESIBILIDAD", "Error al cargar: ${e.message}", e)
            }
        }
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
                // desactivar blanco y negro si se activa alto contraste
                binding.switchBlackWhite.isChecked = false
                aplicarTema("high_contrast")
            } else {
                aplicarTema("normal")
            }
        }

        binding.switchBlackWhite.setOnClickListener {
            val isChecked = binding.switchBlackWhite.isChecked
            if (isChecked) {
                // desactivar alto contraste si se activa blanco y negro
                binding.switchHighContrast.isChecked = false
                aplicarTema("black_white")
            } else {
                aplicarTema("normal")
            }
        }

        binding.switchTalkback.setOnCheckedChangeListener { _, isChecked ->
            actualizarEstadoTalkback(isChecked)
            if (isChecked) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
                startActivity(intent)
            }
            //lanzamos corrutina porque la función ahora sí suspende el hilo
            viewLifecycleOwner.lifecycleScope.launch { guardarPreferenciasSuspend() }
        }

        binding.switchSounds.setOnCheckedChangeListener { _, isChecked ->
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

        binding.switchVibration.setOnCheckedChangeListener { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch { guardarPreferenciasSuspend() }
        }

        binding.sliderFontSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("font_scale", value / 18f)
                    .apply()
                viewLifecycleOwner.lifecycleScope.launch {
                    //guardamos en la base de datos
                    guardarPreferenciasSuspend()

                    // Reiniciamos la actividad para aplicar el nuevo tema/escala
                    activity?.let { activity ->
                        val intent = activity.intent
                        activity.finish()
                        activity.startActivity(intent)
                        //añadir una pequeña transición para que no sea tan brusco
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
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

    //función de suspensión real que bloquea secuencialmente su propio ámbito
    private suspend fun guardarPreferenciasSuspend() {
        try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                ?: return

            val nuevoTipo = when {
                binding.switchTalkback.isChecked -> "VISUAL"
                binding.switchHighContrast.isChecked -> "VISUAL"
                binding.switchSounds.isChecked || binding.switchVibration.isChecked -> "MOTRIZ"
                else -> "NINGUNA"
            }

            SupabaseClient.client.from("usuarios").update(
                {
                    set("tipo_discapacidad", nuevoTipo)
                }
            ) {
                filter { eq("id_usuarios", userId) }
            }
            android.util.Log.d("ACCESIBILIDAD", "PREFERENCIAS GUARDADAS EN BD: $nuevoTipo")

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