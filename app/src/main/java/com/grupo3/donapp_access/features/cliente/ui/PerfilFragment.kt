package com.grupo3.donapp_access.features.cliente.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.app.MainActivity
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentPerfilBinding
import com.grupo3.donapp_access.features.auth.ui.WelcomeFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import com.grupo3.donapp_access.core.services.GeofenceService

@AndroidEntryPoint
class PerfilFragment : Fragment() {

    private var _binding : FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarDatosUsuario()
        configurarBotones()

        val prefs = requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)

        //función definitiva de control de visibilidad
        fun actualizarVisibilidad() {
            val yaValoro = prefs.getBoolean("valoracion_realizada", false)

            binding.btnValorarApp.visibility = if (yaValoro) View.GONE else View.VISIBLE
            binding.miActividad.visibility = if (yaValoro) View.GONE else View.VISIBLE
        }

        actualizarVisibilidad()

        binding.imgQrValoracion.setOnClickListener {
            abrirFormularioValoracion()
        }

        // 3. Truco de reset (Mantener presionado el nombre de usuario)
        binding.tvNombrePerfil.setOnLongClickListener {
            prefs.edit().remove("valoracion_realizada").apply()
            actualizarVisibilidad()
            android.widget.Toast.makeText(requireContext(), "Modo Test: Valoración reseteada", android.widget.Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnNotificaciones.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(NotificacionesHistorialFragment())
        }
        VoiceAssistantManager.speak("Pantalla de perfil. Aquí puedes revisar tus estadísticas de ahorro, ver tus notificaciones y acceder a los ajustes de accesibilidad.")
    }


    private fun abrirFormularioValoracion(){
        val url = "https://docs.google.com/forms/d/1ppX-ON1dRDirio34-YJm22gd7cVsYJSFzAEwnRjS8pE/viewform"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)

        // marcar como realizada para que la próxima vez no aparezca
        requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("valoracion_realizada", true)
            .apply()

        // Ocultamos el botón inmediatamente
        binding.btnValorarApp.visibility = View.GONE

    }



    private fun cargarDatosUsuario(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?: return@launch

                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter { eq("id_usuarios", userId) }
                    }
                    .decodeSingle<UsuarioPerfilDTO>()

                val nombreCompleto = "${usuario.nombres} ${usuario.apellidos}"
                binding.tvNombrePerfil.text = nombreCompleto
                binding.tvCorreoPerfil.text = usuario.correo
                binding.tvDniPerfil.text = "DNI: ${usuario.dni}"
                binding.tvAvatarPerfil.text = usuario.nombres.first().uppercase()

            }catch (e: Exception){
                android.util.Log.e ("PERFIL", "ERROR: ${e.message}", e )
            }
        }
    }

    private fun configurarBotones(){
        binding.btnAccesibilidad.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(AccesibilidadFragment())
        }
        binding.btnCerrarSesion.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()

                    requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .remove("rol_guardado")
                        .remove("check_accesiblidad_inicial")
                        .remove("valoracion_realizada")
                        .apply()

                    androidx.work.WorkManager.getInstance(requireContext()).cancelAllWork()

                    val intentGeoFence = android.content.Intent(
                        requireContext(),
                        GeofenceService::class.java

                    )

                    requireContext().stopService(intentGeoFence)

                    (requireActivity() as MainActivity).mostrarSinNav(WelcomeFragment())

                } catch (e: Exception) {
                    android.util.Log.e("PERFIL", "Error al cerrar sesión: ${e.message}")
                }
            }
        }
        binding.btnNotificaciones.setOnClickListener { }

        binding.btnEditarPerfil.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(EditarPerfilUsuarioFragment())
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
@Serializable
private data class UsuarioPerfilDTO(
    @SerialName("nombres")   val nombres: String,
    @SerialName("apellidos") val apellidos: String,
    @SerialName("correo")    val correo: String,
    @SerialName("dni")       val dni: String,
    @SerialName("tipo_discapacidad") val tipoDiscapacidad: String? = null
)
