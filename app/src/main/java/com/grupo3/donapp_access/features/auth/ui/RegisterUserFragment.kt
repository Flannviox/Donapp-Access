package com.grupo3.donapp_access.features.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.comerciante.ui.DashboardFragment
import com.grupo3.donapp_access.databinding.FragmentRegisterUserBinding
import com.grupo3.donapp_access.features.auth.AuthViewModel
import com.grupo3.donapp_access.features.auth.RegisterViewModel
import com.grupo3.donapp_access.features.usuario.ui.HomeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterUserFragment : Fragment() {

    //se usa viewbinding para acceder de forma segura a los ids del XML
    private var _binding: FragmentRegisterUserBinding? = null
    private val binding get() =_binding!!
    private val sharedViewModel: RegisterViewModel by activityViewModels()

    private val authViewModel: AuthViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnVolver = view.findViewById<ImageView>(R.id.btnBack)

        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        configurarSelectorDiscapacidad()

        binding.btnCrearCuenta.setOnClickListener {
            ejecutarRegistro()
        }

        observarRegistro()
    }


    private fun configurarSelectorDiscapacidad(){
        val opciones = arrayOf("NINGUNA", "MOTRIZ","VISUAL")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones)
        binding.actvDiscapacidad.setAdapter(adapter)

        binding.actvDiscapacidad.setOnClickListener {
            binding.actvDiscapacidad.showDropDown()
        }

    }


    private fun ejecutarRegistro(){
        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val dni =binding.etDni.text.toString().trim()
        val discapacidad = binding.actvDiscapacidad.text.toString()
        val correoApoderado = binding.etCorreoApoderado.text.toString().trim().ifEmpty { null }
        val telefono = binding.etTelefono.text.toString().trim()

        if (validarCampos(nombres, apellidos, correo, pass, dni, telefono)) {
            authViewModel.crearCuentaCompleta(
                email = correo,
                pass = pass,
                nombres = nombres,
                apellidos = apellidos,
                dni = dni,
                telefono = telefono,
                discapacidad = discapacidad,
                rol = sharedViewModel.selectedRole,
                correoApoderado = correoApoderado
            )
        }
    }

    private fun validarCampos(nom: String, ape: String, mail: String, pw: String, dni: String, tel: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return when {

            nom.isEmpty() -> {
                mostrarToast("Por favor, ingresa tu nombre")
                false
            }
            ape.isEmpty() -> {
                mostrarToast("Por favor, ingresa tu apellido")
                false
            }
            dni.length != 8 -> {
                mostrarToast("El DNI debe tener exactamente 8 dígitos")
                false
            }
            tel.length != 9 -> {
                mostrarToast("El teléfono debe tener 9 dígitos")
                false
            }
            mail.isEmpty() || !mail.matches(emailPattern.toRegex()) -> {
                mostrarToast("Ingresa un correo electrónico válido")
                false
            }
            pw.length < 6 -> {
                mostrarToast("La contraseña debe tener al menos 6 caracteres")
                false
            }
            !binding.cbTerminos.isChecked -> {
                mostrarToast("Debes aceptar los términos y condiciones")
                false
            }
            else -> true
        }



    }


    private fun mostrarToast(mensaje: String){
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }



    private fun observarRegistro() {

        viewLifecycleOwner.lifecycleScope.launch {

            authViewModel.registerState.collect { state ->

                when(state) {

                    is AuthViewModel.AuthState.Loading -> {

                        binding.btnCrearCuenta.isEnabled = false
                    }

                    is AuthViewModel.AuthState.Success -> {

                        binding.btnCrearCuenta.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            "Cuenta creada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        val destino: Fragment = when(state.rol.lowercase()) {

                            "cliente" -> HomeFragment()

                            "comerciante" -> DashboardFragment()

                            else -> WelcomeFragment()
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, destino)
                            .commit()
                    }

                    is AuthViewModel.AuthState.Error -> {

                        binding.btnCrearCuenta.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> Unit
                }
            }
        }
    }

}