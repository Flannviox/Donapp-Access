package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import com.grupo3.donapp_access.model.OfertaLote
import com.grupo3.donapp_access.model.TiendaHome
import java.text.SimpleDateFormat
import java.util.Calendar
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

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeUsuarioBinding? = null
    private val binding get() = _binding!!

    private val ofertaAdapter by lazy{
        HomeOfertaAdapter(requireContext()){
            oferta -> oferta.tiendaId?.let{id ->
            (requireActivity() as MainActivity).navegarA(
                TiendaDetailFragment.newInstance(id, oferta.tiendaNombre)
            )
        }
        }
    }

    private val tiendaAdapter = HomeTiendaAdapter { tienda ->
        tienda.idTienda?.let { id ->
            (requireActivity() as MainActivity).navegarA(
                TiendaDetailFragment.newInstance(id, tienda.nombre)
            )
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
                    ofertas.isNotEmpty() -> "${ofertas.size} ofertas activas cerca de ti"
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





}
