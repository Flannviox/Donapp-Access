package com.grupo3.donapp_access

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.grupo3.donapp_access.features.comerciante.ui.DashboardFragment
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.features.auth.AuthRepository
import com.grupo3.donapp_access.features.auth.ui.WelcomeFragment
import com.grupo3.donapp_access.features.usuario.ui.BuscarFragment
import com.grupo3.donapp_access.features.usuario.ui.HomeFragment
import com.grupo3.donapp_access.features.usuario.ui.OfertasFragment
import com.grupo3.donapp_access.features.usuario.ui.PerfilFragment
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject

    lateinit var authRepository: AuthRepository
    private lateinit var bottomNav : BottomNavigationView
    private var rolActual : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences("donapp_prefs", MODE_PRIVATE)
        val fontScale = prefs.getFloat("font_scale", 1.0f)
        val config = resources.configuration
        config.fontScale = fontScale
        resources.updateConfiguration(config, resources.displayMetrics)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.visibility = View.GONE

        if(savedInstanceState == null){
            verificarSesionActiva()

        }
    }

    private fun verificarSesionActiva() {

        val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

        if (usuarioActual != null) {
            mostrarFragment(HomeFragment())
            lifecycleScope.launch {
                try {
                    val rol = authRepository.obtenerRolUsuario(usuarioActual.id)
                    rolActual = rol
                    configurarNavbar(rol)
                } catch (e: Exception) {
                    mostrarSinNav(WelcomeFragment())
                }
            }
        } else {
            mostrarFragment(WelcomeFragment())
        }
    }

    fun configurarNavbar(rol: String){
        rolActual = rol
        bottomNav.visibility = View.VISIBLE

        if(rol.lowercase()=="cliente"){
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_usuario)
            mostrarFragment(HomeFragment())
            bottomNav.selectedItemId = R.id.nav_inicio

            bottomNav.setOnItemSelectedListener { item->
                when (item.itemId) {
                    R.id.nav_inicio -> mostrarFragment(HomeFragment())
                    R.id.nav_buscar -> mostrarFragment(BuscarFragment())
                    R.id.nav_ofertas -> mostrarFragment(OfertasFragment())
                    R.id.nav_perfil -> mostrarFragment(PerfilFragment())
                }
                true
            }

        }else{
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_comerciante)
            mostrarFragment(DashboardFragment())
            bottomNav.selectedItemId = R.id.nav_negocio

            bottomNav.setOnItemSelectedListener { item->
                when(item.itemId){

                    R.id.nav_negocio -> mostrarFragment(
                        DashboardFragment()
                    )
                    R.id.nav_perfil_com -> mostrarFragment(
                        PerfilComercianteFragment()
                    )
                }
                true
            }
        }


    }

    fun mostrarSinNav (fragment: Fragment){
        bottomNav.visibility = View.GONE
        mostrarFragment(fragment)
    }
    private fun mostrarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}