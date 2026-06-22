package com.grupo3.donapp_access.features.mapa.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentMapBinding
import com.grupo3.donapp_access.data.model.Tienda
import com.grupo3.donapp_access.features.map.TiendaConLotes
import com.grupo3.donapp_access.features.mapa.MapViewModel
import com.grupo3.donapp_access.features.cliente.ui.TiendaDetailFragment
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation


//anotacion de hilt para habilitar la inyeccion de dependencias automatica
@AndroidEntryPoint
class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var mapView: MapView? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if(granted){
            viewModel.obtenerUbicacionYTiendas()
        }else{
            mostrarError("Necesitamos tu ubicación para mostrarte tiendas cercanas")

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    //CICLO DE VIDA

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapView
        inicializarMapa()
        observarEstados()

        binding.fabMiUbicacion.setOnClickListener {
            verificarYPedirPermiso()
        }
        VoiceAssistantManager.speak("Vista de mapa activada. Te recomendamos usar la vista de lista para mayor comodidad visual.")
    }


    private var ubicacionUsuario: Point? = null

    //MAPA
    private fun inicializarMapa(){
        mapView?.getMapboxMap()?.setCamera(//mueve la vista del usuario
            CameraOptions.Builder()//entrar a las configuraciones y construirlas paso a paso
                .center(Point.fromLngLat(-79.0287, -8.1116)) // Trujillo por defecto
                .zoom(13.0)
                .build()
            /*1  → planeta entero
              5  → país
              10 → ciudad
              15 → calles
              20 → edificios*/

        )
        verificarYPedirPermiso()
    }

    private fun verificarYPedirPermiso(){
        val tienePermiso = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )== PackageManager.PERMISSION_GRANTED

        if(tienePermiso){
            viewModel.obtenerUbicacionYTiendas()
        }else{
            locationPermissionLauncher.launch(//inicia la peticion de permisos
                arrayOf(//array porque puede pedir varios permisos al mismo tiempo
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }



    //OBSERVAR ESTADOS
    private fun observarEstados(){
        //usar el ciclo de vida de la View
        //lifecyclescop es una coroutinescop ligado al lifecycle,
        //"esta courutine vive mientras el lifecycle viva, osea, mientras la UI exista
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){//ejecuta este bloque solo cuando este al menos en started(visible ylisto)

                //ESTADO DE UBICACION
                launch {
                    viewModel.ubicacionState.collect { state ->
                        when(state){
                            is MapViewModel.UbicacionState.Loading->{
                                mostarLoading("Obteniendo ubicación")
                            }
                            is MapViewModel.UbicacionState.Success->{
                                centrarMapaEnUsuario(
                                    state.location.latitude,
                                    state.location.longitude
                                )
                            }
                            is MapViewModel.UbicacionState.Error->{
                                ocultarLaoding()
                                mostrarError(state.message)
                            }
                            else -> Unit
                        }
                    }
                }

                //ESTADO DE TIENDAS
                launch {
                    viewModel.tiendasState.collect { state ->
                        when(state){
                            is MapViewModel.TiendasState.Loading ->{
                                mostarLoading("Buscando tiendas cercanas...")
                            }
                            is MapViewModel.TiendasState.Success ->{
                                ocultarLaoding()
                                pintarMarcadores(state.tiendas)
                            }
                            is MapViewModel.TiendasState.Error ->{
                                ocultarLaoding()
                                mostrarError(state.message)
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    //MARCADORES
    private fun centrarMapaEnUsuario(lat: Double, lng: Double){

        ubicacionUsuario = Point.fromLngLat(lng,lat)
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(14.0)
                .build()
        )
    }

    private fun pintarMarcadores(tiendas: List<TiendaConLotes>){
        val mapboxMap = mapView?.getMapboxMap() ?: return
        val annotationApi = mapView?.annotations ?: return


        mapboxMap.getStyle{
            style ->
            style.addImage("marker_verde", crearIconoCirculo("#4CAF50"))
            style.addImage("marker_amarillo", crearIconoCirculo("#FFC107"))
            style.addImage("marker_rojo", crearIconoCirculo("#F44336"))

            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val origen = ubicacionUsuario

            tiendas.forEach { tienda ->
                val punto = Point.fromLngLat(tienda.longitud, tienda.latitud)

                //calculamos la distancia entre el usuario y la tienda en metros

                val iconoId = if (origen != null){
                    val resultado = FloatArray(1)
                    Location.distanceBetween(
                        origen.latitude(), origen.longitude(),
                        tienda.latitud, tienda.longitud,
                        resultado
                    )
                    val distanciaMetros = resultado[0]

                    when{
                        distanciaMetros <= 1000 -> "marker_verde"
                        distanciaMetros <= 3000 -> "marker_amarillo"
                        else-> "marker_rojo"
                    }
                }else{
                    "marker_amarillo"
                }
                val opciones = PointAnnotationOptions()
                    .withPoint(punto)
                    .withIconImage(iconoId)
                    .withIconSize(1.0)
                    .withTextField("${tienda.nombre}\n${tienda.lotesActivos} ofertas")
                    .withTextSize(12.0)
                    .withTextColor("#FFFFFF")
                    .withTextHaloColor("#000000")
                    .withTextHaloWidth(1.5)
                    .withTextOffset(listOf(0.0, 1.8))

                pointAnnotationManager.create(opciones)

            }
            // Tap en marcador para ir a TiendaDetailFragment
            pointAnnotationManager.addClickListener { annotation ->
                val tienda = tiendas.firstOrNull { tienda ->
                    tienda.longitud == annotation.point.longitude() &&
                            tienda.latitud == annotation.point.latitude()
                }
                tienda?.let { navegarAdetalle(it) }
                true
            }
        }
    }


    //NAVEGACION
    private fun navegarAdetalle(tienda: TiendaConLotes){
        val fragment = TiendaDetailFragment.newInstance(
            tiendaId = tienda.id,
            tiendaNombre = tienda.nombre
        )
        parentFragmentManager.beginTransaction()
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


    //UI HELPERS
    private fun mostarLoading(mensaje: String){
        binding.loadingContainer.visibility = View.VISIBLE
        binding.tvLoadingMsg.text = mensaje
        binding.tvError.visibility = View.GONE
    }

    private fun ocultarLaoding(){
        binding.loadingContainer.visibility = View.GONE
    }

    private fun mostrarError(mensaje: String){
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text= mensaje
    }


    //CIRCULO DE COLOR COMO BITMAP

    private fun crearIconoCirculo (colorHex: String, diametroDp: Int = 36): Bitmap{
        val diametroPx = (diametroDp * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(diametroPx, diametroPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radio = diametroPx / 2f

        //CIRCULO DE COLOR
        val paintRelleno = Paint(Paint.ANTI_ALIAS_FLAG)
        paintRelleno.color = Color.parseColor(colorHex)
        canvas.drawCircle(radio, radio, radio - 2f, paintRelleno)

        //BORRDE BLANCO PARA QUE RESALTE

        val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG)
        paintBorde.color = Color.WHITE
        paintBorde.style = Paint.Style.STROKE
        paintBorde.strokeWidth = 4f
        canvas.drawCircle(radio, radio,radio -2f, paintBorde)

        return bitmap
    }


    //CICLO DE VIDA MAPVIEW
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
        _binding = null
    }



}