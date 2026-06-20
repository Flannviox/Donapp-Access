package com.grupo3.donapp_access

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.grupo3.donapp_access.features.comerciante.ui.InventarioFragment
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.core.services.GeofenceService
import com.grupo3.donapp_access.databinding.FragmentPerfilComercianteBinding
import com.grupo3.donapp_access.features.auth.ui.WelcomeFragment
import com.grupo3.donapp_access.features.comerciante.ui.EditarTiendaFragment
import com.grupo3.donapp_access.features.usuario.ui.AccesibilidadFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager

@AndroidEntryPoint
class PerfilComercianteFragment : Fragment() {
    private var _binding : FragmentPerfilComercianteBinding? = null

    private val binding get()= _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilComercianteBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarDatos()
        configurarBotones()
        val prefs = requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)

        fun actualizarVisibilidad() {
            val yaValoro = prefs.getBoolean("valoracion_realizada", false)

            // si valoró ocultamos el contenedor completo y el título
            // si no valoró
            binding.btnValorarApp.visibility = if (yaValoro) View.GONE else View.VISIBLE
            binding.miActividad.visibility = if (yaValoro) View.GONE else View.VISIBLE
        }

        // ejecutamos la visibilidad al iniciar
        actualizarVisibilidad()

        binding.imgQrValoracion.setOnClickListener {
            abrirFormularioValoracion()
        }

        //Truco de reset
        binding.tvNombreTiendaPerfil.setOnLongClickListener {
            prefs.edit().remove("valoracion_realizada").apply()
            actualizarVisibilidad()
            android.widget.Toast.makeText(requireContext(), "Modo Test: Valoración reseteada", android.widget.Toast.LENGTH_SHORT).show()
            true
        }
        binding.btnEditarTienda.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(EditarTiendaFragment())
        }
        VoiceAssistantManager.speak("Perfil de tu negocio. Desde aquí puedes gestionar tus ofertas, editar la información de tu tienda y entrar a la configuración de accesibilidad.")
    }

    private fun abrirFormularioValoracion(){
        val url = "https://docs.google.com/forms/d/1ppX-ON1dRDirio34-YJm22gd7cVsYJSFzAEwnRjS8pE/viewform"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)

        // Marcamos como realizada para que la próxima vez no aparezca
        requireContext().getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("valoracion_realizada", true)
            .apply()

        // Ocultamos el botón inmediatamente
        binding.btnValorarApp.visibility = View.GONE

    }

    private fun cargarDatos(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?:return@launch


                //cargar datos del usuario
                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select { filter{eq("id_usuarios", userId)} }
                    .decodeSingle<UsuarioComercianteDTO>()

                val tienda = SupabaseClient.client
                    .from("tiendas")
                    .select { filter { eq("usuarios_id", userId) } }
                    .decodeSingle<TiendaComercianteDTO>()

                // Tus datos de texto
                binding.tvNombreTiendaPerfil.text = tienda.nombre
                binding.tvCorreoPerfil.text = usuario.correo
                binding.tvHorarioPerfil.text = tienda.horaAtencion ?: "08:00 - 18:00"
                binding.tvDireccionPerfil.text = tienda.direccion

                // foto de perfil
                if (!tienda.imagenReferencia.isNullOrBlank()) {
                    // Si tiene imagen: Ocultamos la letra y mostramos la foto
                    binding.tvAvatarPerfil.visibility = View.GONE
                    binding.ivAvatarPerfil.visibility = View.VISIBLE

                    Glide.with(requireContext())
                        .load(tienda.imagenReferencia)
                        .centerCrop()
                        .into(binding.ivAvatarPerfil)
                } else {
                    // Si no tiene imagen: Mostramos la letra y ocultamos la foto
                    binding.tvAvatarPerfil.visibility = View.VISIBLE
                    binding.ivAvatarPerfil.visibility = View.GONE

                    binding.tvAvatarPerfil.text = tienda.nombre.first().uppercase()
                }
            }catch (e : Exception){
                android.util.Log.e("PERFIL_COM", "ERROR: ${e.message}", e)
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

        binding.btnMisOfertas.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer,
                    InventarioFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}

@kotlinx.serialization.Serializable
private data class UsuarioComercianteDTO(
    @SerialName("correo") val correo: String
)

@Serializable
private data class TiendaComercianteDTO(
    @SerialName("nombre")        val nombre: String,
    @SerialName("direccion")     val direccion: String,
    @SerialName("hora_atencion") val horaAtencion: String? = null,
    // 👇 Añade esta línea nueva:
    @SerialName("imagen_referencia") val imagenReferencia: String? = null
)