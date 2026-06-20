package com.grupo3.donapp_access.features.comerciante.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.grupo3.donapp_access.BuildConfig
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.databinding.FragmentEditarTiendaBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import com.bumptech.glide.Glide
import io.github.jan.supabase.storage.storage
import java.util.UUID

class EditarTiendaFragment : Fragment() {

    private var _binding: FragmentEditarTiendaBinding? = null
    private val binding get() = _binding!!

    // Variables de datos
    private var imagenBase64: String? = null
    private var idTiendaActual: String? = null
    private var latitudSeleccionada: Double = 0.0
    private var longitudSeleccionada: Double = 0.0

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Lanzador para permisos de GPS
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            obtenerUbicacionGPS()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para galería de imágenes
    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivImagenTienda.setImageURI(it)
            imagenBase64 = convertirUriABase64(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarTiendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupBotones()
        setupMapaInteractividad()
        cargarDatosActuales()
    }

    private fun setupBotones() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSeleccionarImagen.setOnClickListener { selectorDeImagen.launch("image/*") }
        binding.btnGuardarCambios.setOnClickListener { guardarCambios() }

        binding.btnUsarUbicacionActual.setOnClickListener {
            verificarYPedirPermisoGPS()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapaInteractividad() {
        // Truco del Scroll
        binding.mapView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    binding.scrollViewEditar.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.scrollViewEditar.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        // Listener de Mapbox para cuando el usuario termina de mover el mapa
        binding.mapView.getMapboxMap().addOnMapIdleListener {
            val centro = binding.mapView.getMapboxMap().cameraState.center
            latitudSeleccionada = centro.latitude()
            longitudSeleccionada = centro.longitude()

            binding.tvDireccionMapa.text = "Cargando dirección..."
            convertirCoordenadasADireccion(latitudSeleccionada, longitudSeleccionada)
        }
    }

    private fun cargarDatosActuales() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val tienda = SupabaseClient.client
                    .from("tiendas")
                    .select { filter { eq("usuarios_id", userId) } }
                    .decodeSingle<TiendaEditarDTO>()

                idTiendaActual = tienda.idTienda

                binding.etNombreTienda.setText(tienda.nombre)
                binding.etDireccion.setText(tienda.direccion)
                binding.etReferencia.setText(tienda.referencia ?: "")
                binding.etHorario.setText(tienda.horaAtencion ?: "")

                // Mostrar la imagen usando Glide si es URL, o decodificando si era Base64 antiguo
                if (!tienda.imagenReferencia.isNullOrBlank()) {
                    if (tienda.imagenReferencia.startsWith("http")) {
                        // Es una URL, usamos Glide
                        Glide.with(requireContext())
                            .load(tienda.imagenReferencia)
                            .into(binding.ivImagenTienda)
                    } else {
                        // Código de respaldo por si tienes fotos viejas guardadas como Base64 en tu BD
                        try {
                            val imageBytes = Base64.decode(tienda.imagenReferencia, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            binding.ivImagenTienda.setImageBitmap(decodedImage)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    // Guardamos la referencia actual (URL o Base64 viejo)
                    imagenBase64 = tienda.imagenReferencia
                }

                // Cargar coordenadas en el mapa
                if (tienda.latitud != null && tienda.longitud != null) {
                    latitudSeleccionada = tienda.latitud
                    longitudSeleccionada = tienda.longitud
                    actualizarCamaraMapa(tienda.latitud, tienda.longitud)
                }

            } catch (e: Exception) {
                android.util.Log.e("EditarTienda", "Error al cargar datos: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cargar tu tienda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- LÓGICA DE MAPBOX Y GPS ---

    private fun actualizarCamaraMapa(lat: Double, lng: Double) {
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(16.0)
                .build()
        )
    }

    private fun verificarYPedirPermisoGPS() {
        val finePerm = Manifest.permission.ACCESS_FINE_LOCATION
        val coarsePerm = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), finePerm) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionGPS()
        } else {
            locationPermissionLauncher.launch(arrayOf(finePerm, coarsePerm))
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionGPS() {
        binding.tvDireccionMapa.text = "Obteniendo ubicacion GPS..."
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null) {
                latitudSeleccionada = location.latitude
                longitudSeleccionada = location.longitude
                actualizarCamaraMapa(location.latitude, location.longitude)
                convertirCoordenadasADireccion(location.latitude, location.longitude)
            } else {
                binding.tvDireccionMapa.text = "No se pudo obtener la ubicación"
                Toast.makeText(requireContext(), "Activa el GPS", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            binding.tvDireccionMapa.text = "Error al obtener ubicación"
        }
    }

    private fun convertirCoordenadasADireccion(lat: Double, lng: Double) {
        val token = BuildConfig.MAPBOX_TOKEN
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.mapbox.com/search/geocode/v6/reverse?longitude=$lng&latitude=$lat&access_token=$token&language=es&limit=1"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")

                val direccion = if (features.length() > 0) {
                    features.getJSONObject(0).getJSONObject("properties").getString("full_address")
                } else {
                    "Lat: $lat, Lng: $lng"
                }

                withContext(Dispatchers.Main) {
                    binding.tvDireccionMapa.text = direccion
                    // Opcional: También rellenar el campo de texto de Dirección con lo que dice el GPS
                    binding.etDireccion.setText(direccion)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvDireccionMapa.text = "Lat: $lat, Lng: $lng"
                }
            }
        }
    }

    // --- LÓGICA DE GUARDADO E IMAGEN ---

    private fun guardarCambios() {
        val nombreNuevo = binding.etNombreTienda.text.toString().trim()
        val direccionNueva = binding.etDireccion.text.toString().trim()
        val referenciaNueva = binding.etReferencia.text.toString().trim()
        val horarioNuevo = binding.etHorario.text.toString().trim()

        if (nombreNuevo.isEmpty() || direccionNueva.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre y la dirección son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (idTiendaActual == null) return

        binding.btnGuardarCambios.isEnabled = false
        binding.btnGuardarCambios.text = "GUARDANDO..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var urlFinalParaBD = imagenBase64

                //verificamos si hay una imagen nueva seleccionada (Base64 puro, no URL)
                if (imagenBase64 != null && !imagenBase64!!.startsWith("http")) {
                    binding.btnGuardarCambios.text = "SUBIENDO FOTO..."

                    // decodificamos a base 64
                    val imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT)

                    // Creamos una ruta única
                    val imagePath = "$idTiendaActual/${UUID.randomUUID()}.jpg"

                    // Subimos la imagen al bucket "tiendas" de Supabase Storage
                    SupabaseClient.client.storage.from("productos").upload(imagePath, imageBytes) {
                        upsert = false
                    }

                    // Obtenemos la URL pública generada
                    urlFinalParaBD = SupabaseClient.client.storage.from("productos").publicUrl(imagePath)
                }

                binding.btnGuardarCambios.text = "ACTUALIZANDO DATOS..."

                // 2. Preparamos el paquete de datos con la URL final
                val updateData = TiendaUpdateDTO(
                    nombre = nombreNuevo,
                    direccion = direccionNueva,
                    referencia = referenciaNueva.ifEmpty { null },
                    horaAtencion = horarioNuevo.ifEmpty { null },
                    latitud = latitudSeleccionada,
                    longitud = longitudSeleccionada,
                    imagenReferencia = urlFinalParaBD // <-- Aquí enviamos la URL, no el Base64
                )

                // ejeuctamos update
                SupabaseClient.client.from("tiendas").update(updateData) {
                    filter { eq("id_tienda", idTiendaActual!!) }
                }

                Toast.makeText(requireContext(), "¡Tienda actualizada con éxito!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                android.util.Log.e("EditarTienda", "Error al guardar: ${e.message}", e)
                Toast.makeText(requireContext(), "Ocurrió un error al guardar", Toast.LENGTH_SHORT).show()
                binding.btnGuardarCambios.isEnabled = true
                binding.btnGuardarCambios.text = "GUARDAR CAMBIOS"
            }
        }
    }

    private fun convertirUriABase64(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    // --- CICLO DE VIDA DE MAPBOX ---
    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onDestroyView() { super.onDestroyView(); binding.mapView.onDestroy(); _binding = null }
}

@kotlinx.serialization.Serializable
private data class TiendaEditarDTO(
    @kotlinx.serialization.SerialName("id_tienda") val idTienda: String,
    @kotlinx.serialization.SerialName("nombre") val nombre: String,
    @kotlinx.serialization.SerialName("direccion") val direccion: String,
    @kotlinx.serialization.SerialName("referencia") val referencia: String? = null,
    @kotlinx.serialization.SerialName("latitud") val latitud: Double? = null,
    @kotlinx.serialization.SerialName("longitud") val longitud: Double? = null,
    @kotlinx.serialization.SerialName("hora_atencion") val horaAtencion: String? = null,
    @kotlinx.serialization.SerialName("imagen_referencia") val imagenReferencia: String? = null
)

@kotlinx.serialization.Serializable
private data class TiendaUpdateDTO(
    @kotlinx.serialization.SerialName("nombre") val nombre: String,
    @kotlinx.serialization.SerialName("direccion") val direccion: String,
    @kotlinx.serialization.SerialName("referencia") val referencia: String?,
    @kotlinx.serialization.SerialName("hora_atencion") val horaAtencion: String?,
    @kotlinx.serialization.SerialName("latitud") val latitud: Double,
    @kotlinx.serialization.SerialName("longitud") val longitud: Double,
    @kotlinx.serialization.SerialName("imagen_referencia") val imagenReferencia: String?
)