package com.grupo3.donapp_access.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// --- Imports locales de tu proyecto Donapp ---
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.BuildConfig
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.core.services.AlertasWorker
import com.grupo3.donapp_access.features.auth.AuthRepository
import com.grupo3.donapp_access.features.auth.ui.LoginFragment
import com.grupo3.donapp_access.features.auth.ui.WelcomeFragment
import com.grupo3.donapp_access.features.cliente.ui.TiendaDetailFragment
import com.grupo3.donapp_access.features.comerciante.ui.DashboardFragment
import com.grupo3.donapp_access.features.comerciante.ui.PerfilComercianteFragment
import com.grupo3.donapp_access.features.comerciante.ui.ReservasComercianteFragment
import com.grupo3.donapp_access.features.mapa.ui.MapFragment
import com.grupo3.donapp_access.features.usuario.ui.BuscarFragment
import com.grupo3.donapp_access.features.usuario.ui.HomeFragment
import com.grupo3.donapp_access.features.usuario.ui.MisReservasFragment
import com.grupo3.donapp_access.features.usuario.ui.OfertasFragment
import com.grupo3.donapp_access.features.usuario.ui.PerfilFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    private lateinit var bottomNav : BottomNavigationView
    private var rolActual : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("donapp_prefs", MODE_PRIVATE)
        when (prefs.getString("tema_actual", "normal")) {
            "high_contrast" -> setTheme(R.style.Theme_DonappAccess_HighContrast)
            "black_white" -> setTheme(R.style.Theme_DonappAccess_BlackWhite)
            else -> setTheme(R.style.Theme_DonappAccess)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.visibility = View.GONE

        val rolRestaurado = savedInstanceState?.getString("rol_actual")
        if (rolRestaurado != null) {
            configurarNavbar(rolRestaurado, navegarAlInicio = false)
        } else if (savedInstanceState == null) {
            if (intent?.data?.scheme == "donapp") {
                manejarDeepLink()
            } else {
                verificarSesionActiva()
            }
        }

        iniciarWorkerDeAlertas()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentNavegacion(intent)
    }

    private fun manejarIntentNavegacion(intent: Intent){
        if (intent.getStringExtra("navegar_a") == "mapa"){
            intent.removeExtra("navegar_a")

            if(bottomNav.visibility == View.VISIBLE && rolActual.lowercase() == "cliente"){
                bottomNav.selectedItemId = R.id.nav_inicio
                navegarA(MapFragment())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (rolActual.isNotEmpty() && bottomNav.visibility == View.VISIBLE){
            outState.putString("rol_actual", rolActual)
        }
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
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun obtenerRol(): String = rolActual

    private fun verificarSesionActiva() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, Fragment())
            .commit()

        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

            if (usuarioActual != null) {
                try {
                    val rol = authRepository.obtenerRolUsuario(usuarioActual.id)
                    configurarNavbar(rol)
                } catch (e: Exception) {
                    mostrarSinNav(WelcomeFragment())
                }
            } else {
                mostrarSinNav(WelcomeFragment())
            }
        }
    }

    private fun manejarDeepLink() {
        findViewById<View>(R.id.fragmentContainer).post {
            lifecycleScope.launch {
                try {
                    kotlinx.coroutines.delay(1500)
                    mostrarSinNav(LoginFragment())
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "✅ Correo verificado. Ya puedes iniciar sesión.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    android.util.Log.e("DEEPLINK", "Error: ${e.message}")
                    mostrarSinNav(WelcomeFragment())
                }
            }
        }
    }

    fun configurarNavbar(rol: String, navegarAlInicio: Boolean = true){
        rolActual = rol
        bottomNav.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val prefs = getSharedPreferences("donapp_prefs", MODE_PRIVATE)
                    val checkHecho = prefs.getBoolean("check_accesibilidad_inicial", false)
                    val currentScale = prefs.getFloat("font_scale", 1f)

                    if (!checkHecho) {
                        val usuario = SupabaseClient.client.from("usuarios")
                            .select { filter { eq("id_usuarios", userId) } }
                            .decodeSingle<CheckDiscapacidadDTO>()

                        if (usuario.tipoDiscapacidad?.uppercase() == "VISUAL" && currentScale == 1f) {
                            prefs.edit()
                                .putFloat("font_scale", 1.5f)
                                .putBoolean("check_accesibilidad_inicial", true)
                                .apply()

                            val currentIntent = intent
                            finish()
                            startActivity(currentIntent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            return@launch
                        } else {
                            prefs.edit().putBoolean("check_accesibilidad_inicial", true).apply()
                        }
                    }
                }
            } catch(e: Exception) {
                android.util.Log.e("MAIN_ACCESIBILIDAD", "Error verificando accesibilidad: ${e.message}")
            }
        }

        if(rol.lowercase() == "cliente"){
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_usuario)
            if (navegarAlInicio) {
                mostrarFragment(HomeFragment())
                bottomNav.selectedItemId = R.id.nav_inicio
            }
            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio -> mostrarFragment(HomeFragment())
                    R.id.nav_buscar -> mostrarFragment(BuscarFragment())
                    R.id.nav_ofertas -> mostrarFragment(OfertasFragment())
                    R.id.nav_reservas -> mostrarFragment(MisReservasFragment())
                    R.id.nav_perfil -> mostrarFragment(PerfilFragment())
                }
                true
            }

        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_comerciante)
            if (navegarAlInicio) {
                mostrarFragment(DashboardFragment())
                bottomNav.selectedItemId = R.id.nav_negocio
            }

            bottomNav.setOnItemSelectedListener { item ->
                when(item.itemId){
                    R.id.nav_negocio -> mostrarFragment(DashboardFragment())
                    R.id.nav_reservas_com -> mostrarFragment(ReservasComercianteFragment())
                    R.id.nav_perfil_com -> mostrarFragment(PerfilComercianteFragment())
                }
                true
            }
        }

        val accionNotificacion = intent.getStringExtra("EXTRA_ACCION_NOTIFICACION")
        val idTiendaRecibido = intent.getStringExtra("EXTRA_TIENDA_ID")

        if (accionNotificacion == "RESERVAR_OFERTA" && idTiendaRecibido != null) {
            intent.removeExtra("EXTRA_ACCION_NOTIFICACION")
            intent.removeExtra("EXTRA_TIENDA_ID")

            val bundle = Bundle().apply {
                putString("id_tienda", idTiendaRecibido)
            }
            val fragmentDestino = TiendaDetailFragment().apply {
                arguments = bundle
            }

            navegarA(fragmentDestino)
        }
        manejarIntentNavegacion(intent)
    }

    fun mostrarSinNav(fragment: Fragment){
        rolActual = ""
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