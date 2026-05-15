package com.grupo3.donapp_access

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al setear este layout, el NavHostFragment inicia automáticamente
        setContentView(R.layout.activity_main)

        MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN
    }
}