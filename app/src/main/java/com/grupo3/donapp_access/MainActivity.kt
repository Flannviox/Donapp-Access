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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.grupo3.donapp_access.worker.AlertasWorker
import java.util.concurrent.TimeUnit

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
        iniciarWorkerDeAlertas()
    }

    private fun iniciarWorkerDeAlertas() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AlertasWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DonappAlertasWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun verificarSesionActiva() {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, androidx.fragment.app.Fragment())
            .commit()

            lifecycleScope.launch {
                kotlinx.coroutines.delay(500)

                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                if(usuarioActual!=null){
                    try {
                        val rol = authRepository.obtenerRolUsuario(usuarioActual.id)
                        configurarNavbar(rol)
                    }catch (e: Exception){
                        mostrarSinNav(WelcomeFragment())

                    }

                }else{
                    mostrarSinNav(WelcomeFragment())
                }
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
        supportFragmentManager.popBackStack(null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)


        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navegarA(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}