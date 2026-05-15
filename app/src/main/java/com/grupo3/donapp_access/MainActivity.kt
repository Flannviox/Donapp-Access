package com.grupo3.donapp_access

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.features.auth.ui.HomeUsuarioFragment
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al setear este layout, el NavHostFragment inicia automáticamente
        setContentView(R.layout.activity_main)

        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN

        if(savedInstanceState ==null){
            verificarSesionActiva()
            //ya tiene sesion activa = ir directo sin pasar login

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeUsuarioFragment())
                .commit()
        }else{
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeUsuarioFragment())
                .commit()
        }
    }

    private fun verificarSesionActiva(){
        val sesionactiva = SupabaseClient.client.auth.currentUserOrNull()

        if(sesionactiva != null){

        }
    }
}