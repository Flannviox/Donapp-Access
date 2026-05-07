package com.grupo3.donapp_access

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL


class RegisterSellerFragment : Fragment() {

    private var latitudSeleccionada: Double = -8.1116
    private var longitudSeleccionada: Double = -79.0287
    private var mapView : MapView? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register_seller, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val btnVolver        = view.findViewById<ImageView>(R.id.btnBack)
        val etUbicacion      = view.findViewById<TextInputEditText>(R.id.etUbicacion)
        val tvStatus         = view.findViewById<TextView>(R.id.tvMapStatus)
        mapView              = view.findViewById(R.id.mapView)

        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        configurarMapa(etUbicacion, tvStatus)
    }

    private fun configurarMapa(
        etUbicacion: TextInputEditText,
        tvStatus: TextView
    ){
        mapView?.getMapboxMap()?.apply {

            //centra el mapa en Lima al inicio
            setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(longitudSeleccionada, latitudSeleccionada))
                    .zoom(15.0)
                    .build()
            )

            //cuando el usuario deja de mover el mapa -> captura el centro
            addOnMapIdleListener {
                val centro = cameraState.center
                latitudSeleccionada = centro.latitude()
                longitudSeleccionada = centro.longitude()
                tvStatus.text ="Cargando dirección"
            }

            convertirCoordenadasADireccion(
                latitudSeleccionada,
                longitudSeleccionada,
                etUbicacion,
                tvStatus
            )


        }
    }
    private fun convertirCoordenadasADireccion(
        lat: Double,
        lng: Double,
        etUbicacion: TextInputEditText,
        tvStatus: TextView
    ) {
        val token = BuildConfig.MAPBOX_TOKEN

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/" +
                        "$lng,$lat.json?access_token=$token&language=es&limit=1"

                val response = URL(url).readText()
                val json     = JSONObject(response)
                val features = json.getJSONArray("features")

                val direccion = if (features.length() > 0) {
                    features.getJSONObject(0).getString("place_name")
                } else {
                    "Lat: $lat, Lng: $lng"
                }

                withContext(Dispatchers.Main) {
                    etUbicacion.setText(direccion)
                    tvStatus.text = "Mueve el mapa para ajustar la ubicación"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Si falla la API, muestra las coordenadas directamente
                    etUbicacion.setText("Lat: $lat, Lng: $lng")
                    tvStatus.text ="Mueve el mapa para ajustar la ubicación"
                }
            }
        }
    }

    //ciclo de vida del mapview

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
        mapView = null
    }

    fun getLatitud()   = latitudSeleccionada
    fun getLongitud()  = longitudSeleccionada








}