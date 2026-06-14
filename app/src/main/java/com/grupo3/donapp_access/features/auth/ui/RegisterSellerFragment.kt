package com.grupo3.donapp_access.features.auth.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
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
import com.google.android.material.textfield.TextInputLayout
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

@AndroidEntryPoint
class RegisterSellerFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private var latitudSeleccionada: Double = -8.1116
    private var longitudSeleccionada: Double = -79.0287
    private var mapView: MapView? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var tokenTurnstile: String = ""
    private var busquedaJob: Job? = null
    private var mapaActualizandose = false

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

        val btnVolver      = view.findViewById<ImageView>(R.id.btnBack)
        val btnCrearCuenta = view.findViewById<MaterialButton>(R.id.btnCrearCuenta)
        val btnUbicacion   = view.findViewById<MaterialButton>(R.id.btnUsarUbicacion)
        val etUbicacion    = view.findViewById<TextInputEditText>(R.id.etUbicacion)
        val tvStatus       = view.findViewById<TextView>(R.id.tvMapStatus)
        val etBuscar       = view.findViewById<TextInputEditText>(R.id.etBuscarDireccion)
        val lvSugerencias  = view.findViewById<ListView>(R.id.lvSugerencias)
        mapView = view.findViewById(R.id.mapView)

        mapView?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE -> { v.parent?.requestDisallowInterceptTouchEvent(true) }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {v.parent?.requestDisallowInterceptTouchEvent(false) }
            }
            false
        }

        btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }
        btnUbicacion.setOnClickListener { verificarYPedirPermiso(etUbicacion, tvStatus) }

        configurarBusqueda(etBuscar, lvSugerencias, etUbicacion, tvStatus)
        configurarMapa(etUbicacion, tvStatus)
        configurarWebViewCaptcha()


        btnCrearCuenta.setOnClickListener {
            if (!validarCampos()) return@setOnClickListener

            val nombres       = view.findViewById<TextInputEditText>(R.id.etNombres).text.toString().trim()
            val apellidos     = view.findViewById<TextInputEditText>(R.id.etApellidos).text.toString().trim()
            val dni           = view.findViewById<TextInputEditText>(R.id.etDni).text.toString().trim()
            val correo        = view.findViewById<TextInputEditText>(R.id.etCorreo).text.toString().trim()
            val telefono      = view.findViewById<TextInputEditText>(R.id.etTelefono).text.toString().trim()
            val pass          = view.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()
            val nombreNegocio = view.findViewById<TextInputEditText>(R.id.etNombreNegocio).text.toString().trim()
            val direccion     = etUbicacion.text.toString().trim()

            authViewModel.crearCuentaComerciante(
                email        = correo,
                pass         = pass,
                nombres      = nombres,
                apellidos    = apellidos,
                captchaToken = tokenTurnstile,
                dni          = dni,
                telefono     = telefono,
                nombreTienda = nombreNegocio,
                direccion    = direccion,
                lat          = latitudSeleccionada,
                lng          = longitudSeleccionada,
                horario      = "08:00 - 18:00"
            )
        }

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

    private fun validarCampos(): Boolean {
        val nombres       = view?.findViewById<TextInputEditText>(R.id.etNombres)?.text.toString().trim()
        val apellidos     = view?.findViewById<TextInputEditText>(R.id.etApellidos)?.text.toString().trim()
        val dni           = view?.findViewById<TextInputEditText>(R.id.etDni)?.text.toString().trim()
        val correo        = view?.findViewById<TextInputEditText>(R.id.etCorreo)?.text.toString().trim()
        val telefono      = view?.findViewById<TextInputEditText>(R.id.etTelefono)?.text.toString().trim()
        val pass          = view?.findViewById<TextInputEditText>(R.id.etPassword)?.text.toString().trim()
        val confirmPass   = view?.findViewById<TextInputEditText>(R.id.etConfirmPassword)?.text.toString().trim()
        val nombreNegocio = view?.findViewById<TextInputEditText>(R.id.etNombreNegocio)?.text.toString().trim()
        val direccion     = view?.findViewById<TextInputEditText>(R.id.etUbicacion)?.text.toString().trim()
        val terms         = view?.findViewById<android.widget.CheckBox>(R.id.cbTerminos)?.isChecked ?: false

        if (nombres.isNullOrEmpty() || apellidos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Ingresa tu nombre y apellidos", Toast.LENGTH_SHORT).show()
            return false
        }
        if (dni?.length != 8) {
            Toast.makeText(requireContext(), "El DNI debe tener 8 dígitos", Toast.LENGTH_SHORT).show()
            return false
        }
        if (correo.isNullOrEmpty() || !correo.contains("@")) {
            Toast.makeText(requireContext(), "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (telefono?.length != 9) {
            Toast.makeText(requireContext(), "El teléfono debe tener 9 dígitos", Toast.LENGTH_SHORT).show()
            return false
        }
        if (pass.isNullOrEmpty() || pass.length < 6) {
            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        if (pass != confirmPass) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return false
        }
        if (nombreNegocio.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el nombre de tu negocio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (direccion.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Selecciona la ubicación de tu negocio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!terms) {
            Toast.makeText(requireContext(), "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
            return false
        }
        if (tokenTurnstile.isEmpty()) {
            Toast.makeText(requireContext(), "Completa el Captcha de seguridad", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun configurarBusqueda(
        etBuscar: TextInputEditText,
        lvSugerencias: ListView,
        etUbicacion: TextInputEditText,
        tvStatus: TextView

        ){

        etBuscar.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                busquedaJob?.cancel()
                if (query.length < 3) { lvSugerencias.visibility = View.GONE; return }
                busquedaJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    buscarLugares(query, lvSugerencias, etBuscar, etUbicacion, tvStatus)
                }
            }
        })

        etBuscar.setOnEditorActionListener { _, _, _ ->
            val query = etBuscar.text.toString().trim()
            if (query.isNotEmpty()) {
                busquedaJob?.cancel()
                busquedaJob = viewLifecycleOwner.lifecycleScope.launch {
                    buscarLugares(query, lvSugerencias, etBuscar, etUbicacion, tvStatus)
                }
            }
            true
        }
    }
    private suspend fun buscarLugares(
        query: String,
        lvSugerencias: ListView,
        etBuscar: TextInputEditText,
        etUbicacion: TextInputEditText,
        tvStatus: TextView
    ) {
        withContext(Dispatchers.IO) {
            try {
                val token = BuildConfig.MAPBOX_TOKEN
                val queryEncoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.mapbox.com/search/searchbox/v1/suggest" +
                        "?q=$queryEncoded" +
                        "&access_token=$token" +
                        "&language=es" +
                        "&country=PE" +
                        "&limit=5" +
                        "&proximity=$longitudSeleccionada,$latitudSeleccionada" +
                        "&session_token=donapp-${System.currentTimeMillis()}"

                val response = URL(url).readText()
                val suggestions = JSONObject(response).getJSONArray("suggestions")

                data class Sugerencia(
                    val nombre: String,
                    val direccion: String,
                    val mapboxId: String
                )
                val lista = mutableListOf<Sugerencia>()

                for (i in 0 until suggestions.length()) {
                    val item      = suggestions.getJSONObject(i)
                    val nombre    = item.optString("name", "")
                    val direccion = item.optString("full_address",
                        item.optString("place_formatted", ""))
                    val mapboxId  = item.optString("mapbox_id", "")
                    if (nombre.isNotEmpty() && mapboxId.isNotEmpty()) {
                        lista.add(Sugerencia(nombre, direccion, mapboxId))
                    }
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    if (lista.isEmpty()) { lvSugerencias.visibility = View.GONE; return@withContext }

                    lvSugerencias.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_2,
                        android.R.id.text1,
                        lista.map { it.nombre }
                    ).also { adapter ->
                        lvSugerencias.adapter = object : ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_list_item_2,
                            lista.map { it.nombre }
                        ) {
                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val v = super.getView(position, convertView, parent)
                                v.findViewById<TextView>(android.R.id.text1).text = lista[position].nombre
                                v.findViewById<TextView>(android.R.id.text2).text = lista[position].direccion
                                v.findViewById<TextView>(android.R.id.text2).textSize = 11f
                                return v
                            }
                        }
                    }
                    lvSugerencias.visibility = View.VISIBLE

                    lvSugerencias.setOnItemClickListener { _, _, position, _ ->
                        val seleccionado = lista[position]

                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val retrieveUrl = "https://api.mapbox.com/search/searchbox/v1/retrieve/${seleccionado.mapboxId}" +
                                        "?access_token=$token" +
                                        "&session_token=donapp-${System.currentTimeMillis()}"

                                val detalle   = JSONObject(URL(retrieveUrl).readText())
                                val features  = detalle.getJSONArray("features")
                                val coords    = features.getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONArray("coordinates")
                                val lng = coords.getDouble(0)
                                val lat = coords.getDouble(1)
                                val direccionCompleta = features.getJSONObject(0)
                                    .getJSONObject("properties")
                                    .optString("full_address", seleccionado.direccion)

                                withContext(Dispatchers.Main) {
                                    if (!isAdded) return@withContext
                                    latitudSeleccionada  = lat
                                    longitudSeleccionada = lng

                                    mapaActualizandose = true
                                    mapView?.getMapboxMap()?.setCamera(
                                        CameraOptions.Builder()
                                            .center(Point.fromLngLat(lng, lat))
                                            .zoom(16.0).build()
                                    )
                                    etUbicacion.setText(direccionCompleta)
                                    tvStatus.text = "Mueve el mapa para ajustar si es necesario"
                                    etBuscar.setText("")
                                    lvSugerencias.visibility = View.GONE

                                    Handler(Looper.getMainLooper()).postDelayed(
                                        { mapaActualizandose = false }, 1500
                                    )
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    if (isAdded) Toast.makeText(requireContext(), "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { if (isAdded) lvSugerencias.visibility = View.GONE }
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




