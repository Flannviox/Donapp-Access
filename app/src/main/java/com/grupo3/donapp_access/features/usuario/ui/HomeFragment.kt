package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.databinding.FragmentHomeUsuarioBinding
import com.grupo3.donapp_access.map.ui.MapFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.usuario.dto.UsuarioNombreDTO
import com.grupo3.donapp_access.usuario.ui.TiendaDetailFragment
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import android.content.Intent
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeUsuarioBinding? = null
    private val binding get() = _binding!!

    // SOLUCIÓN: Ahora enviamos el Nombre del Producto (oferta.productoNombre) para que sea exacto
    private val ofertaAdapter by lazy {
        HomeOfertaAdapter(requireContext()) { oferta ->
            oferta.tiendaId?.let { id ->
                (requireActivity() as MainActivity).navegarA(
                    TiendaDetailFragment.newInstance(id, oferta.tiendaNombre)
                )
            }
        }
    }

    private val tiendaAdapter by lazy {
        HomeTiendaAdapter(requireContext()) { tienda ->
            val id = tienda.idTienda
            if (id != null && id.isNotBlank()) {
                (requireActivity() as MainActivity).navegarA(
                    TiendaDetailFragment.newInstance(id, tienda.nombre)
                )
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error: La tienda no tiene ID",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("HomeFragment", "El ID de la tienda es nulo para: ${tienda.nombre}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeUsuarioBinding.inflate(inflater, container, false)
        setupRecyclerViews()
        cargarDatos()
        cargarNombreUsuario()
        return binding.root
    }

    private fun setupRecyclerViews() {
        binding.recyclerOfertas.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerOfertas.adapter = ofertaAdapter

        binding.recyclerTiendas.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerTiendas.adapter = tiendaAdapter
    }

    private fun cargarDatos() {
        val repository = ClienteRepository()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ofertas = withContext(Dispatchers.IO) { repository.obtenerOfertas() }
                val tiendas = withContext(Dispatchers.IO) { repository.obtenerTiendas() }

                if(_binding ==null) return@launch

                val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val vencenHoy = ofertas.count { it.fechaVencimiento == hoy }

                binding.textAlertaOfertas.text = when {
                    vencenHoy > 0 -> "$vencenHoy ofertas vencen hoy!!"
                    ofertas.isNotEmpty() -> "${ofertas.size} ofertas disponibles"
                    else -> "No hay ofertas activas aún"
                }

                ofertaAdapter.submitList(ofertas.take(10))
                tiendaAdapter.submitList(tiendas.take(10))

            } catch (e: Exception) {
                if (_binding == null) return@launch
                android.util.Log.e("Donapp", "Error cargando home: ${e.message}", e)
                binding.textAlertaOfertas.text = "Error al cargar ofertas"
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVerMapa.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(MapFragment())
        }
        binding.btnVerTodasTiendas.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(TodasTiendasFragment())
        }

        binding.btnVerTodasOfertas.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(TodasOfertasFragment())
        }

        solicitarPermisosYArrancar()
        VoiceAssistantManager.speak("Pantalla de inicio. Arriba tienes el botón para ir al mapa de ofertas, y deslizando hacia abajo encontrarás ofertas relámpago y bodegas populares.")
    }

    private fun solicitarPermisosYArrancar() {
        val permisosNecesarios = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisosNecesarios.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permisosNecesarios.toTypedArray())
    }

    override fun onDestroyView() {
        ofertaAdapter.releaseTTS()
        _binding = null
        super.onDestroyView()
    }

    private fun cargarNombreUsuario() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val usuario = SupabaseClient.client
                    .from("usuarios")
                    .select {
                        filter { eq("id_usuarios", userId) }
                    }
                    .decodeSingle<UsuarioNombreDTO>()

                if (_binding == null) return@launch

                binding.tvNombreUsuario.text = usuario.nombres
                binding.tvAvatarInicial.text = usuario.nombres.first().uppercase()

            }catch (e: Exception){
                android.util.Log.e("HOME_USER", "Error: ${e.message}", e )
                if (_binding == null) return@launch
                binding.tvNombreUsuario.text ="Usuario"
            }
        }
    }

    private fun iniciarServicioGeocercas() {
        val intentService = Intent(requireContext(), com.grupo3.donapp_access.core.services.GeofenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intentService)
        } else {
            requireContext().startService(intentService)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (locationGranted) {
            iniciarServicioGeocercas()
        } else {
            android.widget.Toast.makeText(requireContext(), "Activa la ubicación para recibir alertas de comida", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}