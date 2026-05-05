package com.grupo3.donapp_access

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

class RoleSelectionFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_role_selection, container, false)
    }

    override fun onViewCreated( view: View, savedInstanceState: Bundle?) {
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
            when(selectRole){
                "user"->{
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right, //entra
                            R.anim.slide_out_left, //sale
                            R.anim.slide_in_left, //vuelve a entrar
                            R.anim.slide_out_right //vuelve a salir
                        )
                        .replace(R.id.fragmentContainer, RegisterUserFragment())
                        .addToBackStack(null)
                        .commit()
                }

                "seller"->{
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right, //entra
                            R.anim.slide_out_left, //sale
                            R.anim.slide_in_left, //vuelve a entrar
                            R.anim.slide_out_right //vuelve a salir
                        )
                        .replace(R.id.fragmentContainer, RegisterSellerFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }


        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()

        }



    }

}