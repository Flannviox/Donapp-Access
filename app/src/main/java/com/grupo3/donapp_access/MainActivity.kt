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
import com.grupo3.donapp_access.features.usuario.ui.MisReservasFragment
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.grupo3.donapp_access.features.comerciante.ui.ReservasComercianteFragment
import com.grupo3.donapp_access.worker.AlertasWorker
import java.util.concurrent.TimeUnit
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject

    lateinit var authRepository: AuthRepository
    private lateinit var bottomNav : BottomNavigationView
    private var rolActual : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("donapp_prefs", MODE_PRIVATE)
        val altoContrasteActivo = prefs.getBoolean("alto_contraste", false)
        if (altoContrasteActivo) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            )
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.visibility = View.GONE

        if (savedInstanceState == null) {
            if (intent?.data?.scheme == "donapp") {
                findViewById<View>(R.id.fragmentContainer).post {
                    manejarDeepLink()
                }
            } else {
                verificarSesionActiva()
            }
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

    private fun manejarDeepLink() {
        findViewById<View>(R.id.fragmentContainer).post {
            lifecycleScope.launch {
                try {
                    // Supabase necesita un momento para procesar el token
                    kotlinx.coroutines.delay(1500)
                    mostrarSinNav(
                        com.grupo3.donapp_access.features.auth.ui.LoginFragment()
                    )
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "✅ Correo verificado. Ya puedes iniciar sesión.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    android.util.Log.e("DEEPLINK", "Error: ${e.message}")
                    mostrarSinNav(
                        com.grupo3.donapp_access.features.auth.ui.WelcomeFragment()
                    )
                }
            }
        }
    }

    fun configurarNavbar(rol: String){
        rolActual = rol

        // NUEVO: Validar si el usuario tiene discapacidad visual en su primer inicio de sesión
        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val prefs = getSharedPreferences("donapp_prefs", MODE_PRIVATE)
                    val checkHecho = prefs.getBoolean("check_accesibilidad_inicial", false)
                    val currentScale = prefs.getFloat("font_scale", 1f)

                    if (!checkHecho) {
                        // Usamos un DTO privado para evitar crashes de serialización
                        val usuario = SupabaseClient.client.from("usuarios")
                            .select { filter { eq("id_usuarios", userId) } }
                            .decodeSingle<CheckDiscapacidadDTO>()

                        if (usuario.tipoDiscapacidad?.uppercase() == "VISUAL" && currentScale == 1f) {
                            prefs.edit()
                                .putFloat("font_scale", 1.5f) // Aplica la letra grande
                                .putBoolean("check_accesibilidad_inicial", true) // Marca que ya se revisó
                                .apply()

                            // Reiniciamos la MainActivity para que el attachBaseContext aplique la fuente al instante
                            val currentIntent = intent
                            finish()
                            startActivity(currentIntent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            return@launch // Evitamos cargar fragmentos porque la actividad se reiniciará
                        } else {
                            // Si no es visual o ya cambió la letra, igual marcamos como revisado
                            prefs.edit().putBoolean("check_accesibilidad_inicial", true).apply()
                        }
                    }
                }
            } catch(e: Exception) {
                android.util.Log.e("MAIN_ACCESIBILIDAD", "Error verificando accesibilidad: ${e.message}")
            }
        }

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
                    R.id.nav_reservas -> mostrarFragment(MisReservasFragment())
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
                    R.id.nav_negocio -> mostrarFragment(DashboardFragment())
                    R.id.nav_reservas_com -> mostrarFragment(ReservasComercianteFragment())
                    R.id.nav_perfil_com -> mostrarFragment(com.grupo3.donapp_access.PerfilComercianteFragment())
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

    override fun onStop() {
        super.onStop()
        ejecutarWorkerInmediato()
    }

    override fun onStart() {
        super.onStart()
        WorkManager.getInstance(this).cancelUniqueWork("DonappAlertaInmediata")
    }

    private fun ejecutarWorkerInmediato() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AlertasWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "DonappAlertaInmediata",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("donapp_prefs", MODE_PRIVATE)
        val fontScale = prefs.getFloat("font_scale", 1.0f)

        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.fontScale = fontScale

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }



}
@Serializable
private data class CheckDiscapacidadDTO(
    @SerialName("tipo_discapacidad") val tipoDiscapacidad: String? = null
)