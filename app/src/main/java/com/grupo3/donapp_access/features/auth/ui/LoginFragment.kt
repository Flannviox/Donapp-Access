package com.grupo3.donapp_access.features.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint // Necesario para inyectar el ViewModel con Hilt
class LoginFragment : Fragment() {

    // Inyectamos tu ViewModel
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnVolver = view.findViewById<ImageView>(R.id.btnBack)

        // 1. Vinculamos los elementos de tu XML
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = view.findViewById<TextView>(R.id.tvRegister)

        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Navegación hacia la selección de rol si no tiene cuenta
        tvRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, RoleSelectionFragment())
                .addToBackStack(null)
                .commit()
        }

        // 3. Acción de enviar el formulario a Supabase
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                authViewModel.login(email, pass)
            } else {
                Toast.makeText(requireContext(), "Llene todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Escuchamos las respuestas del ViewModel (Cargando, Éxito, Error)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Idle -> {
                            btnLogin.isEnabled = true
                            btnLogin.text = "Iniciar Sesión"
                        }
                        is AuthViewModel.AuthState.Loading -> {
                            btnLogin.isEnabled = false
                            btnLogin.text = "Cargando..." // Feedback visual
                        }
                        is AuthViewModel.AuthState.Success -> {
                            btnLogin.isEnabled = true
                            btnLogin.text = "Iniciar Sesión"

                            // 5. Redirección final al nav_graph correspondiente usando tu rol
                            val rolUsuario = state.rol.lowercase()
                            if (rolUsuario == "cliente") {
                                findNavController().navigate(R.id.action_global_nav_usuario)
                            } else if (rolUsuario == "comerciante") {
                                findNavController().navigate(R.id.action_global_nav_comerciante)
                            } else {
                                Toast.makeText(requireContext(), "Rol no válido", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is AuthViewModel.AuthState.Error -> {
                            btnLogin.isEnabled = true
                            btnLogin.text = "Iniciar Sesión"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}