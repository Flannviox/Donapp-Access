package com.grupo3.donapp_access.features.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.auth.ui.LoginFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VerificacionCorreoFragment : Fragment() {


    private val authViewModel : AuthViewModel by viewModels()
    private var email: String =""

    companion object{
        private const val ARG_EMAIL = "arg_email"

        fun newInstance(email: String) = VerificacionCorreoFragment().apply {
            arguments = Bundle().apply { putString(ARG_EMAIL, email) }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL) ?: ""
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_verificacion_correo, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvCorreoEnviado).text =
            "Enviamos un correo de verificacion a: \n$email"


        view.findViewById<Button>(R.id.btnYaVerifique).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }

        observarEstado(view)

    }


    private fun observarEstado(view: View){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                authViewModel.registerState.collect{state ->
                    when(state){
                        is AuthViewModel.AuthState.Loading ->{
                            view.findViewById<Button>(R.id.btnReenviarCorreo).isEnabled = false
                        }

                        is AuthViewModel.AuthState.VerificacionPendiente->{
                            view.findViewById<Button>(R.id.btnReenviarCorreo).isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                "Correo enviado a $email",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                        is AuthViewModel.AuthState.Error->{
                            view.findViewById<Button>(R.id.btnReenviarCorreo).isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> Unit

                    }


                }
            }
        }
    }



}