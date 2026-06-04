package com.grupo3.donapp_access.features.auth.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.savedstate.serialization.saved
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.auth.ui.RegisterSellerFragment
import com.grupo3.donapp_access.features.auth.ui.RegisterUserFragment
import com.grupo3.donapp_access.features.auth.RegisterViewModel

class RoleSelectionFragment : Fragment() {
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private var selectRole: String? = null

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


        val prefs = requireContext().getSharedPreferences("donapp_prefs", Context.MODE_PRIVATE)
        val temaActual = prefs.getString("tema_actual", "normal")

        val(bgUsuario, strokeUsuario, bgComerciante, strokeComerciante) =
            when(temaActual){
                "black_white" -> listOf(
                    R.color.bw_surface,
                    R.color.bw_primary,
                    R.color.bw_surface,
                    R.color.bw_primary
                )
                "high_contrast" -> listOf(
                    R.color.role_usuario_bg_contrast,
                    R.color.role_usuario_stroke_contrast,
                    R.color.role_comerciante_bg_contrast,
                    R.color.role_comerciante_stroke_contrast
                )
                else -> listOf(
                    R.color.role_usuario_bg,
                    R.color.role_usuario_stroke,
                    R.color.role_comerciante_bg,
                    R.color.role_comerciante_stroke
                )
        }

        chipUsuario.visibility = View.GONE
        chipComerciante.visibility = View.GONE
        cardUsuario.strokeWidth = 0
        cardComerciante.strokeWidth = 0

        cardUsuario.setCardBackgroundColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), bgUsuario)
        )
        cardUsuario.strokeColor =
            androidx.core.content.ContextCompat.getColor(requireContext(), strokeUsuario)

        cardComerciante.setCardBackgroundColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), bgComerciante)
        )
        cardComerciante.strokeColor =
            androidx.core.content.ContextCompat.getColor(requireContext(), strokeComerciante)


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

        selectRole = savedInstanceState?.getString("select_role")
        when(selectRole){
            "user" -> seleccionUsuario()
            "seller" -> seleccionComerciante()
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
                    sharedViewModel.selectedRole = "Cliente"

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
                    sharedViewModel.selectedRole = "Comerciante"

                    //Ejecutamos tu código original de navegación
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







    }
        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()

        }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectRole?.let { outState.putString("select_role", it) }
    }
}