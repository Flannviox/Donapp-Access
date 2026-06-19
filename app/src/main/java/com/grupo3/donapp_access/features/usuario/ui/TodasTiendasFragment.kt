package com.grupo3.donapp_access.features.usuario.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.grupo3.donapp_access.MainActivity
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.core.common.UiState
import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.core.utils.VoiceAssistantManager
import com.grupo3.donapp_access.databinding.FragmentTiendaDetailBinding
import com.grupo3.donapp_access.databinding.FragmentTodasTiendasBinding
import com.grupo3.donapp_access.features.auth.ui.ValoracionesAdapter
import com.grupo3.donapp_access.features.lotes.dto.LoteDTO
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import com.grupo3.donapp_access.features.usuario.TiendaDetailViewModel
import com.grupo3.donapp_access.usuario.ui.TiendaDetailFragment
import com.grupo3.donapp_access.model.OfertaLote
import com.grupo3.donapp_access.usuario.dto.TiendaDTO
import com.grupo3.donapp_access.usuario.dto.ValoracionDTO
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
class TodasTiendasFragment : Fragment() {

    private var _binding: FragmentTodasTiendasBinding? = null
    private val binding get() = _binding!!
    private lateinit var tiendasAdapter: TodasTiendasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodasTiendasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        cargarTodasLasTiendas()
    }

    private fun setupUI() {
        // Configurar botón de retroceso
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        //Configurar RecyclerView en una sola lista vertical
        binding.rvTodasTiendas.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar el adaptador con las tarjetas anchasto
        tiendasAdapter = TodasTiendasAdapter(requireContext()) { tienda ->
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
            }
        }
        binding.rvTodasTiendas.adapter = tiendasAdapter
    }

    private fun cargarTodasLasTiendas() {
        binding.progressBar.visibility = View.VISIBLE

        val repository = ClienteRepository()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tiendas = withContext(Dispatchers.IO) {
                    repository.obtenerTiendas()
                }

                binding.progressBar.visibility = View.GONE
                tiendasAdapter.submitList(tiendas)

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                android.util.Log.e("TodasTiendas", "Error al cargar tiendas: ${e.message}", e)
                android.widget.Toast.makeText(context, "Error al cargar las bodegas", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

