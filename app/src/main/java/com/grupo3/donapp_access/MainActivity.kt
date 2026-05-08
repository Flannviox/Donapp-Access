package com.grupo3.donapp_access

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.grupo3.donapp_access.features.auth.ui.HomeFragment
import com.grupo3.donapp_access.features.auth.ui.RoleSelectionFragment
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN

        if (savedInstanceState == null) {
// Usamos post para que se ejecute en el siguiente ciclo del loop de la UI
                window.decorView.post {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RoleSelectionFragment())
                        .commit()

                }

        }
    }
}