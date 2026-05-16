package com.grupo3.donapp_access

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.features.comerciante.ui.InventarioFragment
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentPerfilComercianteBinding
import com.grupo3.donapp_access.features.auth.ui.WelcomeFragment
import com.grupo3.donapp_access.features.usuario.ui.AccesibilidadFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@AndroidEntryPoint
class PerfilComercianteFragment : Fragment() {
    private var _binding : FragmentPerfilComercianteBinding? = null

    private val binding get()= _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentPerfilComercianteBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarDatos()
        configurarBotones()
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

                binding.tvNombreTiendaPerfil.text = tienda.nombre
                binding.tvCorreoPerfil.text = usuario.correo
                binding.tvAvatarPerfil.text = tienda.nombre.first().uppercase()
                binding.tvHorarioPerfil.text = tienda.horaAtencion ?: "08:00 - 18:00"
                binding.tvDireccionPerfil.text = tienda.direccion
            }catch (e : Exception){
                android.util.Log.e("PERFIL_COM", "ERROR: ${e.message}", e)
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


                }catch (e: Exception){
                    android.util.Log.e("PERFIL_COM", "Error al cerrar sesión: ${e.message}")
                }
            }
        }

        binding.btnInformacionPersonal.setOnClickListener { }
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
    @SerialName("hora_atencion") val horaAtencion: String? = null
)