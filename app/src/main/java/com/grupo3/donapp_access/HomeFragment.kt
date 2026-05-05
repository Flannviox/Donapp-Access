package com.grupo3.donapp_access

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton


class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val btnCrearCuenta = view.findViewById<MaterialButton>(R.id.btnCrearAccount)
        val btnIniciarSesion = view.findViewById<MaterialButton>(R.id.btnIniciarSesion)

        btnCrearCuenta.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RoleSelectionFragment())
                .addToBackStack(null)
                .commit()
        }

        btnIniciarSesion.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}