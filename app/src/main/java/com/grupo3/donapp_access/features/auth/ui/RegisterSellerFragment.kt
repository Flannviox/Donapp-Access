package com.grupo3.donapp_access.features.auth.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.grupo3.donapp_access.BuildConfig
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.auth.AuthViewModel
import com.grupo3.donapp_access.features.auth.VerificacionCorreoFragment
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@AndroidEntryPoint
class RegisterSellerFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    private var latitudSeleccionada: Double = -8.1116
    private var longitudSeleccionada: Double = -79.0287
    private var mapView: MapView? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var tokenTurnstile: String = ""

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val et = view?.findViewById<TextInputEditText>(R.id.etUbicacion) ?: return@registerForActivityResult
            val tv = view?.findViewById<TextView>(R.id.tvMapStatus) ?: return@registerForActivityResult
            obtenerUbicacion(et, tv)
        } else {
            Toast.makeText(requireActivity(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_seller, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val btnVolver = view.findViewById<ImageView>(R.id.btnBack)
        val tvTituloRegistro = view.findViewById<TextView>(R.id.tvTituloRegistro)

        // Elementos de los Pasos
        val layoutPaso1 = view.findViewById<View>(R.id.layoutPaso1)
        val layoutPaso2 = view.findViewById<View>(R.id.layoutPaso2)
        val btnSiguiente = view.findViewById<MaterialButton>(R.id.btnSiguiente)
        val btnAtrasPaso1 = view.findViewById<MaterialButton>(R.id.btnAtrasPaso1)
        val btnCrearCuenta = view.findViewById<MaterialButton>(R.id.btnCrearCuenta)

        // Elementos del Mapa
        val btnUbicacion = view.findViewById<MaterialButton>(R.id.btnUsarUbicacion)
        val etUbicacion = view.findViewById<TextInputEditText>(R.id.etUbicacion)
        val tvStatus = view.findViewById<TextView>(R.id.tvMapStatus)
        mapView = view.findViewById(R.id.mapView)

        mapView?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }

        // --- LÓGICA DE INTERCAMBIO DE PANTALLAS ---
        btnSiguiente.setOnClickListener {
            // Validaciones del Paso 1
            val nombres = view.findViewById<TextInputEditText>(R.id.etNombres).text.toString().trim()
            val apellidos = view.findViewById<TextInputEditText>(R.id.etApellidos).text.toString().trim()
            val dni = view.findViewById<TextInputEditText>(R.id.etDni).text.toString().trim()
            val correo = view.findViewById<TextInputEditText>(R.id.etCorreo).text.toString().trim()
            val telefono = view.findViewById<TextInputEditText>(R.id.etTelefono).text.toString().trim()
            val pass = view.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()
            val confirmPass = view.findViewById<TextInputEditText>(R.id.etConfirmPassword).text.toString().trim()

            if (nombres.isEmpty() || apellidos.isEmpty() || dni.isEmpty() || correo.isEmpty() || telefono.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos tus datos personales", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ocultar Paso 1 y Mostrar Paso 2
            layoutPaso1.visibility = View.GONE
            layoutPaso2.visibility = View.VISIBLE
            tvTituloRegistro.text = "Paso 2: Datos de tu Negocio"
        }

        btnAtrasPaso1.setOnClickListener {
            // Volver al Paso 1
            layoutPaso2.visibility = View.GONE
            layoutPaso1.visibility = View.VISIBLE
            tvTituloRegistro.text = "Paso 1: Datos Personales"
        }

        btnCrearCuenta.setOnClickListener {
            val nombreNegocio = view.findViewById<TextInputEditText>(R.id.etNombreNegocio).text.toString().trim()
            val direccion = etUbicacion.text.toString().trim()
            val termsChecked = view.findViewById<android.widget.CheckBox>(R.id.cbTerminos).isChecked

            if (nombreNegocio.isEmpty() || direccion.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, completa los datos de tu negocio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!termsChecked) {
                Toast.makeText(requireContext(), "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (tokenTurnstile.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, completa el Captcha de seguridad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nombres = view.findViewById<TextInputEditText>(R.id.etNombres).text.toString().trim()
            val apellidos = view.findViewById<TextInputEditText>(R.id.etApellidos).text.toString().trim()
            val dni = view.findViewById<TextInputEditText>(R.id.etDni).text.toString().trim()
            val correo = view.findViewById<TextInputEditText>(R.id.etCorreo).text.toString().trim()
            val telefono = view.findViewById<TextInputEditText>(R.id.etTelefono).text.toString().trim()
            val pass = view.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()

            authViewModel.crearCuentaComerciante(
                email = correo,
                pass = pass,
                nombres = nombres,
                apellidos = apellidos,
                captchaToken = tokenTurnstile,
                dni = dni,
                telefono = telefono,
                nombreTienda = nombreNegocio,
                direccion = direccion,
                lat = latitudSeleccionada,
                lng = longitudSeleccionada,
                horario = "08:00 - 18:00"
            )
        }

        btnUbicacion.setOnClickListener { verificarYPedirPermiso(etUbicacion, tvStatus) }
        configurarMapa(etUbicacion, tvStatus)
        configurarWebViewCaptcha()

        // Observar ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.registerState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Idle -> { }
                        is AuthViewModel.AuthState.Loading -> {
                            btnCrearCuenta.isEnabled = false
                            btnCrearCuenta.text = "Registrando..."
                        }
                        is AuthViewModel.AuthState.Success -> {
                            btnCrearCuenta.isEnabled = true
                            btnCrearCuenta.text = "Crear cuenta"
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragmentContainer,
                                    VerificacionCorreoFragment.newInstance(
                                        view.findViewById<TextInputEditText>(R.id.etCorreo).text.toString().trim()
                                    )
                                )
                                .commit()
                        }
                        is AuthViewModel.AuthState.VerificacionPendiente -> { }
                        is AuthViewModel.AuthState.Error -> {
                            btnCrearCuenta.isEnabled = true
                            btnCrearCuenta.text = "Crear Cuenta"
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            configurarWebViewCaptcha()
                            tokenTurnstile = ""
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarWebViewCaptcha() {
        val webView = view?.findViewById<WebView>(R.id.webViewCaptcha) ?: return
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.webChromeClient = android.webkit.WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        try {
            val htmlString = requireContext().assets.open("turnstile.html").bufferedReader().use { it.readText() }
            webView.loadDataWithBaseURL("https://uomlyvsrlkvsroowlhqh.supabase.co", htmlString, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al cargar la seguridad", Toast.LENGTH_SHORT).show()
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onCaptchaSuccess(token: String) {
            requireActivity().runOnUiThread {
                tokenTurnstile = token
                Toast.makeText(requireContext(), "Validación de seguridad exitosa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarYPedirPermiso(etUbicacion: TextInputEditText, tvStatus: TextView) {
        val finePerm = Manifest.permission.ACCESS_FINE_LOCATION
        val coarsePerm = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), finePerm) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion(etUbicacion, tvStatus)
        } else {
            locationPermissionLauncher.launch(arrayOf(finePerm, coarsePerm))
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion(etUbicacion: TextInputEditText, tvStatus: TextView) {
        tvStatus.text = "Obteniendo ubicacion GPS..."
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null) {
                latitudSeleccionada = location.latitude
                longitudSeleccionada = location.longitude
                mapView?.getMapboxMap()?.setCamera(
                    CameraOptions.Builder().center(Point.fromLngLat(location.longitude, location.latitude)).zoom(16.0).build()
                )
                convertirCoordenadasADireccion(location.latitude, location.longitude, etUbicacion, tvStatus)
            } else {
                tvStatus.text = "No se pudo obtener la ubicación"
                Toast.makeText(requireContext(), "Activa el GPS", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            tvStatus.text = "Error al obtener ubicación"
        }
    }

    private fun configurarMapa(etUbicacion: TextInputEditText, tvStatus: TextView) {
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder().center(Point.fromLngLat(longitudSeleccionada, latitudSeleccionada)).zoom(15.0).build()
        )
        mapView?.getMapboxMap()?.addOnMapIdleListener {
            val centro = mapView?.getMapboxMap()?.cameraState?.center ?: return@addOnMapIdleListener
            latitudSeleccionada = centro.latitude()
            longitudSeleccionada = centro.longitude()
            tvStatus.text = "Cargando dirección..."
            convertirCoordenadasADireccion(latitudSeleccionada, longitudSeleccionada, etUbicacion, tvStatus)
        }
    }

    private fun convertirCoordenadasADireccion(lat: Double, lng: Double, etUbicacion: TextInputEditText, tvStatus: TextView) {
        val token = BuildConfig.MAPBOX_TOKEN
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.mapbox.com/search/geocode/v6/reverse?longitude=$lng&latitude=$lat&access_token=$token&language=es&limit=1"
                val response = URL(url).readText()
                val features = JSONObject(response).getJSONArray("features")
                val direccion = if (features.length() > 0) features.getJSONObject(0).getJSONObject("properties").getString("full_address") else "Lat: $lat, Lng: $lng"
                withContext(Dispatchers.Main) {
                    etUbicacion.setText(direccion)
                    tvStatus.text = "Mueve el mapa para ajustar la ubicación"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    etUbicacion.setText("Lat: $lat, Lng: $lng")
                    tvStatus.text = "Mueve el mapa para ajustar la ubicación"
                }
            }
        }
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onStop() { super.onStop(); mapView?.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
    override fun onDestroyView() { super.onDestroyView(); mapView?.onDestroy(); mapView = null }
}