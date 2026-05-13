package com.grupo3.donapp_access

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.grupo3.donapp_access.databinding.ActivityMainBinding
import com.grupo3.donapp_access.features.usuario.ui.BuscarFragment
import com.grupo3.donapp_access.features.usuario.ui.OfertasFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBottomNavigation()
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_buscar
        }
    }

    private fun configurarBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_buscar -> {
                    mostrarFragment(BuscarFragment())
                    true
                }
                R.id.nav_ofertas -> {
                    mostrarFragment(OfertasFragment())
                    true
                }
                R.id.nav_home -> {
                    mostrarFragment(OfertasFragment())
                    true
                }
                R.id.nav_perfil -> {
                    mostrarFragment(BuscarFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun mostrarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
