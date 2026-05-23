package com.grupo3.donapp_access.features.comerciante.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentDashboardBinding
import com.grupo3.donapp_access.features.comerciante.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    @Inject
    lateinit var supabase: SupabaseClient

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private val CHANNEL_ID = "donapp_alertas"
    private var mensajePendiente: String? = null

    //launcher para pedir permiso de notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            mensajePendiente?.let { mostrarNotificacionSistema(it) }
        } else {
            Toast.makeText(requireContext(), "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        //observar datos del ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nombreTienda.collect { nombre ->
                binding.tvNombreTienda.text = nombre
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalLotes.collect { cantidad ->
                binding.tvOfertasActivas.text = cantidad.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clientesAlcanzados.collect { cantidad ->
                binding.tvClientesAlcanzados.text = cantidad.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lotesActivos.collect { lotes ->
                (binding.rvOfertasDashboard.adapter as? InventarioAdapter)?.submitList(lotes)
            }
        }

        // observar la alerta y disparar notificación real del sistema
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mensajeAlerta.collect { mensaje ->
                if (mensaje != null) {
                    gestionarPermisoYNotificar(mensaje)
                }
            }
        }

        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            viewModel.cargarDatosDashboard(userId)
        }

        binding.tvVerTodas.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(InventarioFragment())
        }

        binding.btnNuevaOferta.setOnClickListener {
            (requireActivity() as MainActivity).navegarA(PublicarLoteFragment())
        }
    }

    private fun gestionarPermisoYNotificar(mensaje: String) {
        mensajePendiente = mensaje
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        mostrarNotificacionSistema(mensaje)
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Vencimiento"
            val descriptionText = "Notificaciones sobre lotes próximos a vencer"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun mostrarNotificacionSistema(mensaje: String) {
        crearCanalNotificacion()

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.mipmap.logo) // Asegúrate de que el icono logo.png funciona bien aquí
            .setContentTitle("Donapp: ¡Atención Comerciante!")
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireContext())) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(1001, builder.build())
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvOfertasDashboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOfertasDashboard.adapter = InventarioAdapter { lote ->
            val fragment = DetalleLoteFragment()
            val bundle = Bundle().apply {
                putString("lote_id", lote.id_lote)
            }
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}