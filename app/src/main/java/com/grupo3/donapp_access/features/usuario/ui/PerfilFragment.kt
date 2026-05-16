package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentPerfilBinding
import com.grupo3.donapp_access.features.auth.ui.WelcomeFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


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
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, AccesibilidadFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnCerrarSesion.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                    (requireActivity() as MainActivity).mostrarSinNav(WelcomeFragment())
                } catch (e: Exception) {
                    android.util.Log.e("PERFIL", "Error al cerrar sesión: ${e.message}")
                }
            }
        }
        binding.btnInformacionPersonal.setOnClickListener { }
        binding.btnNotificaciones.setOnClickListener { }
        binding.btnMisValoraciones.setOnClickListener { }


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
