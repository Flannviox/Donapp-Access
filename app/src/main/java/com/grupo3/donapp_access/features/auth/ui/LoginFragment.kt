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
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.comerciante.ui.DashboardFragment
import com.grupo3.donapp_access.features.auth.AuthViewModel
import com.grupo3.donapp_access.features.usuario.ui.HomeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager

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

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = view.findViewById<TextView>(R.id.tvRegister)

        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

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

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                val captchaDialog = CaptchaDialogFragment { captchaToken ->
                    authViewModel.login(email, pass, captchaToken)
                }
                captchaDialog.show(parentFragmentManager, "captcha_login")
            } else {
                Toast.makeText(requireContext(), "Llene todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

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
                            btnLogin.text = "Cargando..."
                        }
                        is AuthViewModel.AuthState.Success -> {
                            btnLogin.isEnabled = true
                            btnLogin.text = "Iniciar Sesión"
                            (requireActivity() as MainActivity).configurarNavbar(state.rol)

                            val rolUsuario = state.rol.lowercase()

                            val destino = when(rolUsuario){
                                "cliente" -> HomeFragment()
                                "comerciante" -> DashboardFragment()
                            else -> {
                                Toast.makeText(requireContext(), "Rol no válido", Toast.LENGTH_SHORT).show()
                                return@collect

                            }

                        }
                            requireActivity().supportFragmentManager.beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                                )
                                .replace(R.id.fragmentContainer, destino)
                                .commit()

                    }
                    is AuthViewModel.AuthState.Error -> {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar Sesión"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()

                    }

                        is AuthViewModel.AuthState.VerificacionPendiente -> Unit

                    }
                }
            }
        }
        VoiceAssistantManager.speak("Pantalla de inicio de sesión. Ingresa tu correo y contraseña. El botón de ingreso está en la parte inferior.")

    }
}
