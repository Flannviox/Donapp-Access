package com.grupo3.donapp_access.features.auth.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentRegisterUserBinding
import com.grupo3.donapp_access.features.auth.AuthViewModel
import com.grupo3.donapp_access.features.auth.RegisterViewModel
import com.grupo3.donapp_access.features.auth.VerificacionCorreoFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterUserFragment : Fragment() {

    //se usa viewbinding para acceder de forma segura a los ids del XML
    private var _binding: FragmentRegisterUserBinding? = null
    private val binding get() =_binding!!
    private val sharedViewModel: RegisterViewModel by activityViewModels()

    private val authViewModel: AuthViewModel by viewModels()

    private var tokenTurnstile: String = ""


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

        configurarWebViewCaptcha()



        binding.btnCrearCuenta.setOnClickListener {
            ejecutarRegistro()
        }

        observarRegistro()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarWebViewCaptcha() {
        val webView = binding.webViewCaptcha

        //configuración exhaustiva para permitir la carga de scripts externos
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        // Permite que scripts cargados desde HTTPS accedan a contenido en tu HTML
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        try {
            val htmlContent = requireContext().assets.open("turnstile.html").bufferedReader().use { it.readText() }

            // El BASE_URL debe ser el mismo que agregaste en Cloudflare (uomlyvsrlkvsroowlhqh.supabase.co)
            val baseUrl = "https://uomlyvsrlkvsroowlhqh.supabase.co"

            webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onCaptchaSuccess(token: String) {
            //necesario para que el fragmento pueda actualizar el token
            //cuando el usuario resuelve el captcha, esta función se ejecuta
            requireActivity().runOnUiThread {
                // Asumiendo que tokenTurnstile es una propiedad de tu Fragment
                this@RegisterUserFragment.tokenTurnstile = token
                Toast.makeText(requireContext(), "Captcha resuelto correctamente", Toast.LENGTH_SHORT).show()
            }
        }
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
                captchaToken = tokenTurnstile,
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
                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.fragmentContainer,
                                VerificacionCorreoFragment.newInstance(
                                    binding.etCorreo.text.toString().trim()
                                )
                            )
                            .commit()
                    }

                    is AuthViewModel.AuthState.VerificacionPendiente ->{
                        binding.btnCrearCuenta.isEnabled = true

                    }


                    is AuthViewModel.AuthState.Error -> {

                        binding.btnCrearCuenta.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                        configurarWebViewCaptcha()
                        tokenTurnstile = ""
                    }

                    else -> Unit
                }
            }
        }
    }

}