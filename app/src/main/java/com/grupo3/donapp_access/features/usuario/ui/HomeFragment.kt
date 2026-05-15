package com.grupo3.donapp_access.features.usuario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupo3.donapp_access.databinding.FragmentHomeBinding
import com.grupo3.donapp_access.features.usuario.ClienteRepository
import com.grupo3.donapp_access.model.OfertaLote
import com.grupo3.donapp_access.model.TiendaHome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val ofertaAdapter = HomeOfertaAdapter()
    private val tiendaAdapter = HomeTiendaAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setupRecyclerViews()
        cargarDatos()
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
        Thread {
            try {
                val repository = ClienteRepository()
                val ofertas = repository.obtenerOfertas()
                val tiendas = repository.obtenerTiendas()

                val ofertasFinales = if (ofertas.isEmpty()) datosEjemploOfertas() else ofertas
                val tiendasFinales = if (tiendas.isEmpty()) datosEjemploTiendas() else tiendas

                activity?.runOnUiThread {
                    val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val vencenHoy = ofertasFinales.count { it.fechaVencimiento == hoy }

                    binding.textAlertaOfertas.text = when {
                        vencenHoy > 0 -> "$vencenHoy ofertas vencen hoy!!"
                        ofertasFinales.isNotEmpty() -> "${ofertasFinales.size} ofertas activas cerca de ti"
                        else -> "No hay ofertas activas aún"
                    }

                    ofertaAdapter.submitList(ofertasFinales.take(10))
                    tiendaAdapter.submitList(tiendasFinales.take(10))
                }
            } catch (e: Exception) {
                android.util.Log.e("Donapp", "Error cargando home: ${e.message}", e)
                activity?.runOnUiThread {
                    val ofertasEjemplo = datosEjemploOfertas()
                    val tiendasEjemplo = datosEjemploTiendas()
                    val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val vencenHoy = ofertasEjemplo.count { it.fechaVencimiento == hoy }

                    binding.textAlertaOfertas.text = if (vencenHoy > 0) "$vencenHoy ofertas vencen hoy!!" else "3 ofertas activas cerca de ti"
                    ofertaAdapter.submitList(ofertasEjemplo)
                    tiendaAdapter.submitList(tiendasEjemplo)
                }
            }
        }.start()
    }

    private fun datosEjemploOfertas(): List<OfertaLote> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val hoy = Date()
        val manana = Calendar.getInstance().apply { add(Calendar.DATE, 1) }.time
        val pasado = Calendar.getInstance().apply { add(Calendar.DATE, 2) }.time

        return listOf(
            OfertaLote(
                idLote = "1",
                productoNombre = "Pan blanco Bimbo",
                productoImagen = null,
                productoPresentacion = "Bolsa 500g",
                tiendaNombre = "Bodega El Chamo",
                tiendaDireccion = "0.3 km",
                cantidad = 15,
                fechaVencimiento = sdf.format(hoy),
                precioNormal = 5.00,
                precioOferta = 2.00,
                numeroLote = "L-001",
                ratingTienda = 4.8
            ),
            OfertaLote(
                idLote = "2",
                productoNombre = "Leche Gloria 1L",
                productoImagen = null,
                productoPresentacion = "Caja 1 litro",
                tiendaNombre = "Minimarket El Sol",
                tiendaDireccion = "0.5 km",
                cantidad = 20,
                fechaVencimiento = sdf.format(manana),
                precioNormal = 4.50,
                precioOferta = 2.80,
                numeroLote = "L-002",
                ratingTienda = 4.5
            ),
            OfertaLote(
                idLote = "3",
                productoNombre = "Yogurt Gloria 1kg",
                productoImagen = null,
                productoPresentacion = "Pote 1kg",
                tiendaNombre = "Bodega San José",
                tiendaDireccion = "0.2 km",
                cantidad = 8,
                fechaVencimiento = sdf.format(hoy),
                precioNormal = 6.00,
                precioOferta = 3.50,
                numeroLote = "L-003",
                ratingTienda = 4.2
            ),
            OfertaLote(
                idLote = "4",
                productoNombre = "Galletas Soda Field",
                productoImagen = null,
                productoPresentacion = "Paquete 250g",
                tiendaNombre = "Bodega El Chamo",
                tiendaDireccion = "0.3 km",
                cantidad = 30,
                fechaVencimiento = sdf.format(pasado),
                precioNormal = 2.50,
                precioOferta = 1.20,
                numeroLote = "L-004",
                ratingTienda = 4.8
            ),
            OfertaLote(
                idLote = "5",
                productoNombre = "Aceite Primor 1L",
                productoImagen = null,
                productoPresentacion = "Botella 1 litro",
                tiendaNombre = "Minimarket El Sol",
                tiendaDireccion = "0.5 km",
                cantidad = 12,
                fechaVencimiento = sdf.format(manana),
                precioNormal = 9.50,
                precioOferta = 6.50,
                numeroLote = "L-005",
                ratingTienda = 4.5
            )
        )
    }

    private fun datosEjemploTiendas(): List<TiendaHome> {
        return listOf(
            TiendaHome(nombre = "Bodega El Chamo", direccion = "0.3 km", rating = 4.8),
            TiendaHome(nombre = "Minimarket El Sol", direccion = "0.5 km", rating = 4.5),
            TiendaHome(nombre = "Bodega San José", direccion = "0.2 km", rating = 4.2),
            TiendaHome(nombre = "Bodega La Esquina", direccion = "0.7 km", rating = 4.6)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
