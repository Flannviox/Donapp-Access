package com.grupo3.donapp_access

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.grupo3.donapp_access.features.auth.ui.HomeFragment
import com.mapbox.common.MapboxOptions

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
    }
}