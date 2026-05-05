package com.grupo3.donapp_access

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

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


        var selectRole: String? = null

        fun seleccionUsuario(){
            selectRole = "user"

            cardUsuario.strokeWidth = 4
            cardUsuario.setStrokeColor(resources.getColor(R.color.donapp_primary))
            cardComerciante.strokeWidth= 0

        }

        fun seleccionComerciante(){
            selectRole = "seller"

            cardComerciante.strokeWidth = 4
            cardComerciante.setStrokeColor(resources.getColor(R.color.role_usuario_stroke))
            cardUsuario.strokeWidth= 0
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
                        .replace(R.id.fragmentContainer, RegisterUserFragment())
                        .addToBackStack(null)
                        .commit()
                }

                "seller"->{
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RegisterSellerFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

    }

}