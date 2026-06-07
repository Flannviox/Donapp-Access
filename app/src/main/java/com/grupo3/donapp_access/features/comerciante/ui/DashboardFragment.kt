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
import com.bumptech.glide.Glide
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentDashboardBinding
import com.grupo3.donapp_access.features.comerciante.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    @Inject
    lateinit var supabase: SupabaseClient

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()


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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nombreTienda.collect { nombre ->
                binding.tvNombreTienda.text = nombre

                // Actualizamos la letra del círculo con la inicial real de la tienda
                if (nombre.isNotBlank() && nombre != "Cargando..." && nombre != "Error al cargar") {
                    binding.tvAvatarDashboard.text = nombre.first().uppercase()
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.imagenTienda.collect { url ->
                if (!url.isNullOrBlank() && url.startsWith("http")) {
                    // Si hay foto: Ocultamos la inicial y mostramos la imagen
                    binding.tvAvatarDashboard.visibility = View.GONE
                    binding.ivAvatarDashboard.visibility = View.VISIBLE

                    Glide.with(requireContext())
                        .load(url)
                        .centerCrop()
                        .into(binding.ivAvatarDashboard)
                } else {
                    // Si no hay foto: Dejamos todo como estaba (la inicial amarilla)
                    binding.tvAvatarDashboard.visibility = View.VISIBLE
                    binding.ivAvatarDashboard.visibility = View.GONE
                }
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
                if (lotes.isEmpty()) {
                    binding.rvOfertasDashboard.visibility = View.GONE
                    binding.tvSinOfertasDashboard.visibility = View.VISIBLE
                } else {
                    binding.rvOfertasDashboard.visibility = View.VISIBLE
                    binding.tvSinOfertasDashboard.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mensajeAlerta.collect { mensaje ->
                if (mensaje != null) {
                    binding.tvAlertaVencimiento.visibility = View.VISIBLE
                    binding.tvAlertaVencimiento.text = mensaje
                } else {
                    binding.tvAlertaVencimiento.visibility = View.GONE
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
        VoiceAssistantManager.speak("Panel de control de tu tienda. Usa el menú inferior para navegar entre tus reservas, inventario y publicar nuevos lotes.")
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