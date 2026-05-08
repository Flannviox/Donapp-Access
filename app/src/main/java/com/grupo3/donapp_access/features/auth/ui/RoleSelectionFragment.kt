package com.grupo3.donapp_access.features.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.RegisterSellerFragment
import com.grupo3.donapp_access.RegisterUserFragment
import com.grupo3.donapp_access.features.auth.RegisterViewModel

class RoleSelectionFragment : Fragment() {
    private val sharedViewModel: RegisterViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_role_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardUsuario = view.findViewById<MaterialCardView>(R.id.cardUsuario)
        val cardComerciante = view.findViewById<MaterialCardView>(R.id.cardComerciante)
        val btnContinuar = view.findViewById<MaterialButton>(R.id.btnContinuar)
        val chipUsuario = view.findViewById<Chip>(R.id.chipUsuarioSelected)
        val chipComerciante = view.findViewById<Chip>(R.id.chipComercianteSelected)
        val btnVolver = view.findViewById<ImageButton>(R.id.btnBack)

        var selectRole: String? = null


        chipUsuario.visibility = View.GONE
        chipComerciante.visibility = View.GONE
        cardUsuario.strokeWidth = 0
        cardComerciante.strokeWidth = 0


        fun seleccionUsuario(){
            selectRole = "user"

            cardUsuario.strokeWidth = 4
            cardUsuario.setStrokeColor(resources.getColor(R.color.donapp_primary))
            cardComerciante.strokeWidth= 0

            chipUsuario.visibility = View.VISIBLE
            chipComerciante.visibility = View.GONE

        }

        fun seleccionComerciante(){
            selectRole = "seller"

            cardComerciante.strokeWidth = 4
            cardComerciante.setStrokeColor(resources.getColor(R.color.role_usuario_stroke))
            cardUsuario.strokeWidth= 0

            chipUsuario.visibility = View.GONE
            chipComerciante.visibility = View.VISIBLE

        }


        cardUsuario.setOnClickListener {
            seleccionUsuario()
        }

        cardComerciante.setOnClickListener {
            seleccionComerciante()
        }

        btnContinuar.setOnClickListener {
            when (selectRole) {
                "user" -> {
                    // 1. Guardamos el rol en el SharedViewModel para la base de datos
                    sharedViewModel.selectedRole = "Cliente"

                    // 2. Ejecutamos tu código original de navegación
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                        .replace(R.id.fragmentContainer, RegisterUserFragment())
                        .addToBackStack(null)
                        .commit()
                }

                "seller" -> {
                    // 1. Guardamos el rol en el SharedViewModel
                    sharedViewModel.selectedRole = "Comerciante"

                    // 2. Ejecutamos tu código original de navegación
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                        .replace(R.id.fragmentContainer, RegisterSellerFragment())
                        .addToBackStack(null)
                        .commit()
                }

                null -> {
                    // Si el usuario le da a continuar sin elegir tarjeta, le avisamos
                    Toast.makeText(requireContext(), "Por favor, selecciona cómo usarás la app", Toast.LENGTH_SHORT).show()
                }
        }


        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()

        }





    }

} }