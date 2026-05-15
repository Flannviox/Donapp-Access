package com.grupo3.donapp_access.map.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentMapBinding
import com.grupo3.donapp_access.features.auth.models.Tienda
import com.grupo3.donapp_access.map.MapViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


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
        arguments?.let {

        }
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
    }


    //MAPA
    private fun inicializarMapa(){
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-79.0287, -8.1116)) // Trujillo por defecto
                .zoom(13.0)
                .build()

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
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }



    //OBSERVAR ESTADOS
    private fun observarEstados(){
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){

                //ESTADO DE UBICACION
                launch {
                    viewModel.ubicacionState.collect { state ->
                        when(state){
                            is MapViewModel.UbicacionState.Loading->{
                                mostrarLoading("Obteniendo ubicación")
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
                                ocutarLaoding()
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
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(14.0)
                .build()
        )
    }

    private fun pintarMarcadores(tiendas: List<Tienda>){
        val annotationApi = mapView?.annotations ?: return
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()

        tiendas.forEach{ tienda ->
            val punto = Point.fromLngLat(tienda.longitud, tienda.latitud)

            val opciones = PointAnnotationOptions()
                .withPoint(punto)
                .withTextField("${tienda.nombre}\n${tienda.lotesActivos} ofertas")
                .withTextSize(12.0)
                .withTextColor("FFCA28")
            pointAnnotationManager.create(opciones)
        }

        // Tap en marcador → TiendaDetailFragment
        pointAnnotationManager.addClickListener{annotation->
            val tienda = tiendas.firstOrNull {tienda ->
                tienda.longitud == annotation.point.longitude() &&
                        tienda.latitud == annotation.point.latitude()
            }
            tienda?.let { navegarAdetalle(it) }
            true
        }
    }


    //NAVEGACION
    private fun navegarAdetalle(tienda: Tienda){
        val fragment = TiendaDetailFragment.newInstance(
            tiendaID = tienda.id,
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