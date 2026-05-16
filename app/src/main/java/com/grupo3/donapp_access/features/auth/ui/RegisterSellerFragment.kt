package com.grupo3.donapp_access.features.auth.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.grupo3.donapp_access.BuildConfig
import com.grupo3.donapp_access.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

import androidx.fragment.app.viewModels
import com.grupo3.donapp_access.features.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint



import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlin.math.ln

@AndroidEntryPoint // importante para inyectar el ViewModel
class RegisterSellerFragment : Fragment() {
    // 3. Declarar el ViewModel a nivel de clase (fuera de las funciones)
    private val authViewModel: AuthViewModel by viewModels()


    private var latitudSeleccionada: Double = -8.1116
    private var longitudSeleccionada: Double = -79.0287
    private var mapView : MapView? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissions ->
        val granted = permissions [Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if(granted){
            val et = view?.findViewById<TextInputEditText>(R.id.etUbicacion) ?: return@registerForActivityResult
            val tv = view?.findViewById<TextView>(R.id.tvMapStatus) ?: return@registerForActivityResult
            obtenerUbicacion(et, tv)
        }else{
            Toast.makeText(
                requireActivity(),
                "Permiso de ubicación denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


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


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val btnUbicacion     = view.findViewById<MaterialButton>(R.id.btnUsarUbicacion)
        val btnVolver        = view.findViewById<ImageView>(R.id.btnBack)
        val etUbicacion      = view.findViewById<TextInputEditText>(R.id.etUbicacion)
        val tvStatus         = view.findViewById<TextView>(R.id.tvMapStatus)
        mapView              = view.findViewById(R.id.mapView)


        mapView?.setOnTouchListener { v, event ->
            when (event.action){
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE ->{
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL->{
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
            }
            false
        }


        btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }




        // Opción 1 — GPS automático

        btnUbicacion.setOnClickListener {
            verificarYPedirPermiso(etUbicacion, tvStatus)
        }


        // Opción 2 — Mapa interactivo
        configurarMapa(etUbicacion, tvStatus)


        val btnCrearCuenta = view.findViewById<MaterialButton>(R.id.btnCrearCuenta)

        btnCrearCuenta.setOnClickListener {
            val email = view.findViewById<TextInputEditText>(R.id.etCorreo).text.toString().trim()
            val password = view.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()
            val nombreTienda = view.findViewById<TextInputEditText>(R.id.etNombreNegocio).text.toString().trim()
            val direccion = view.findViewById<TextInputEditText>(R.id.etUbicacion).text.toString().trim()
            val telefono = view.findViewById<TextInputEditText>(R.id.etTelefono).text.toString().trim()

            // Verificamos que la función crearCuentaComerciante esté en tu AuthViewModel
            authViewModel.crearCuentaComerciante(
                email = email,
                pass = password,
                nombres = "Nombre_Pendiente", // En un MVP de comerciante podemos pedir estos datos después o agregar los campos
                apellidos = "Apellido_Pendiente",
                dni = "00000000",
                telefono = telefono,
                nombreTienda = nombreTienda,
                direccion = direccion,
                lat = latitudSeleccionada,
                lng = longitudSeleccionada,
                horario = "08:00 - 18:00"
            )
        }

        // Observar los estados del ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.registerState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Idle -> {
                            // Estado inicial, no hacemos nada
                        }
                        is AuthViewModel.AuthState.Loading -> {
                            // Deshabilitamos el botón para evitar múltiples clics
                            btnCrearCuenta.isEnabled = false
                            btnCrearCuenta.text = "Registrando..."
                        }
                        is AuthViewModel.AuthState.Success -> {
                            Toast.makeText(requireContext(), "¡Tienda registrada con éxito!", Toast.LENGTH_SHORT).show()

                            // Restauramos el botón por si el usuario presiona "Atrás" luego
                            btnCrearCuenta.isEnabled = true
                            btnCrearCuenta.text = "Crear Cuenta"

                            // Navegamos de vuelta al Login
                            // Usamos popBackStack para limpiar la pila de fragmentos de registro
                            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, LoginFragment())
                                .commit()
                        }
                        is AuthViewModel.AuthState.Error -> {
                            // Ocurrió un error (ej. correo ya existe, sin internet)
                            btnCrearCuenta.isEnabled = true
                            btnCrearCuenta.text = "Crear Cuenta"
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }



    }


    // ── Opción 1: GPS
    private fun verificarYPedirPermiso(etUbicacion: TextInputEditText, tvStatus: TextView){
        val finePerm = Manifest.permission.ACCESS_FINE_LOCATION
        val coarsePerm = Manifest.permission.ACCESS_COARSE_LOCATION

        val tienePermiso = ContextCompat.checkSelfPermission(
            requireContext(), finePerm
        ) == PackageManager.PERMISSION_GRANTED

        if(tienePermiso){
            obtenerUbicacion(etUbicacion, tvStatus)
        }else{
            locationPermissionLauncher.launch(
                arrayOf(finePerm, coarsePerm))
        }

    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion(
        etUbicacion: TextInputEditText,
        tvStatus: TextView
    ){
        tvStatus.text ="Obteniendo ubicacion GPS..."

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener { location ->
            if(location != null){
                latitudSeleccionada = location.latitude
                longitudSeleccionada = location.longitude

                mapView?.getMapboxMap()?.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.latitude))
                        .zoom(16.0)
                        .build()
                )

                convertirCoordenadasADireccion(
                    location.latitude,
                    location.longitude,
                    etUbicacion,
                    tvStatus
                )
            }else{
                tvStatus.text = "No se pudo obtener la ubicación"
                Toast.makeText(
                    requireContext(),
                    "No se pudo obtener la ubicación. Activa el GPS",
                    Toast.LENGTH_SHORT
                    ).show()

            }
        }.addOnFailureListener {
            tvStatus.text = "Error al obtener ubicación"
            Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }



    // ── Opción 2: Mapa interactivo

    private fun configurarMapa(
        etUbicacion: TextInputEditText,
        tvStatus: TextView
    ) {
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(longitudSeleccionada, latitudSeleccionada))
                .zoom(15.0)
                .build()
        )

        mapView?.getMapboxMap()?.addOnMapIdleListener {
            val centro = mapView?.getMapboxMap()?.cameraState?.center ?: return@addOnMapIdleListener
            latitudSeleccionada = centro.latitude()
            longitudSeleccionada = centro.longitude()
            tvStatus.text = "Cargando dirección..."

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
        android.util.Log.d("GEOCODING", "Iniciando lat=$lat lng=$lng")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("GEOCODING", "Haciendo request...")
                val url = "https://api.mapbox.com/search/geocode/v6/reverse?" +
                        "longitude=$lng&latitude=$lat&access_token=$token&language=es&limit=1"
                val response = URL(url).readText()
                android.util.Log.d("GEOCODING", "Response: ${response.take(200)}")

                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                val direccion = if (features.length() > 0) {
                    val properties = features.getJSONObject(0).getJSONObject("properties")
                    properties.getString("full_address")
                } else {
                    "Lat: $lat, Lng: $lng"
                }

                withContext(Dispatchers.Main) {
                    etUbicacion.setText(direccion)
                    tvStatus.text = "Mueve el mapa para ajustar la ubicación"
                }
            } catch (e: Exception) {
                android.util.Log.e("GEOCODING", "ERROR: ${e.javaClass.simpleName}: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    etUbicacion.setText("Lat: $lat, Lng: $lng")
                    tvStatus.text = "Mueve el mapa para ajustar la ubicación"
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