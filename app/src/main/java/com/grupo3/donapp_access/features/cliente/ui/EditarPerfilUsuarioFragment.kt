package com.grupo3.donapp_access.features.cliente.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentEditarPerfilUsuarioBinding
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@AndroidEntryPoint
class EditarPerfilUsuarioFragment : Fragment() {

    private var _binding : FragmentEditarPerfilUsuarioBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarPerfilUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarDatos()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun cargarDatos(){
        viewLifecycleOwner.lifecycleScope.launch{
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?:return@launch

                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter { eq("id_usuarios", userId) }
                    }
                    .decodeSingle<UsuarioEditDTO>()

                with(binding) {
                    etNombres.setText(usuario.nombres)
                    etApellidos.setText(usuario.apellidos)
                    etTelefono.setText(usuario.telefono ?: "")
                    etDni.setText(usuario.dni)
                    etCorreo.setText(usuario.correo)
                    tvAvatarEditar.text = usuario.nombres.first().uppercase()
                }

            }catch (e: Exception){
                Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun guardarCambios() {
        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()

        if (nombres.isBlank()) {
            binding.tilNombres.error = "Ingresa tus nombres"
            return
        } else {
            binding.tilNombres.error = null
        }

        if (apellidos.isBlank()) {
            binding.tilApellidos.error = "Ingresa tus apellidos"
            return
        } else {
            binding.tilApellidos.error = null
        }

        if (telefono.isBlank()) {
            binding.tilTelefono.error = "Ingresa tu teléfono"
            return
        } else if (telefono.length < 9) {
            binding.tilTelefono.error = "El teléfono debe tener 9 dígitos"
            return
        } else {
            binding.tilTelefono.error = null
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false


        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?:return@launch

                SupabaseClient.client
                    .from("usuarios")
                    .update (
                        mapOf(
                            "nombres" to nombres,
                            "apellidos" to apellidos,
                            "telefono" to telefono
                        )
                    ){
                        filter { eq("id_usuarios", userId) }
                    }

                Toast.makeText(requireContext(), "Perfil actualizado ✓", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()


            }catch (e: Exception){
                val mensaje = if (e.message?.contains("telefono", ignoreCase = true) == true)
                    "Ese número de teléfono ya está en uso"
                else
                    "Error al guardar cambios"
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

@Serializable
private data class UsuarioEditDTO(
    @SerialName("nombres")   val nombres: String,
    @SerialName("apellidos") val apellidos: String,
    @SerialName("correo")    val correo: String,
    @SerialName("dni")       val dni: String,
    @SerialName("telefono")  val telefono: String? = null
)