package com.grupo3.donapp_access.features.comerciante.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.databinding.FragmentPublicarLoteBinding
import com.grupo3.donapp_access.features.lotes.PublicarViewModel
import com.grupo3.donapp_access.features.lotes.dto.CategoriaDTO
import com.grupo3.donapp_access.features.lotes.dto.ProductoDTO
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PublicarLoteFragment : Fragment() {

    private var _binding: FragmentPublicarLoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PublicarViewModel by activityViewModels()
    private var imagenUri: Uri? = null





    private val imagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){ uri->
        if(uri == null) return@registerForActivityResult
        val size = obtenerTamanoUri(uri)
        if(size > MAX_IMAGEN_BYTES){
            toast("La imagen no debe superar 5MB", long = true)
        }
        imagenUri = uri
        Glide.with(this).load(uri).into(binding.ivPreviewImagen)



    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublicarLoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val temaActual = requireContext()
            .getSharedPreferences("donapp_prefs", android.content.Context.MODE_PRIVATE)
            .getString("tema_actual","normal")

        val spinnerBackground = when (temaActual) {
            "black_white" -> R.drawable.bg_spinner_contrast_bw
            else -> R.drawable.bg_spinner_contrast
        }


        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSeleccionarImagen.setOnClickListener { imagenLauncher.launch("image/*") }
        binding.btnUsarProductoExistente.setOnClickListener { usarProductoExistente() }
        binding.btnCrearProducto.setOnClickListener { crearProductoNuevo() }
        binding.spinnerProductosExistentes.setBackgroundResource(spinnerBackground)
        binding.spinnerCategoria.setBackgroundResource(spinnerBackground)

        observarViewModel()
        viewModel.cargarDatosIniciales()



    }


    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.categorias.collect { poblarSpinnerCategorias(it) } }
                launch { viewModel.productos.collect { poblarSpinnerProductos(it) } }
                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            is PublicarViewModel.PublicarState.Loading -> {
                                binding.btnCrearProducto.isEnabled = false
                                binding.btnUsarProductoExistente.isEnabled = false
                            }
                            is PublicarViewModel.PublicarState.ProductoCreado -> {
                                toast("Producto listo, ahora completa el lote", long = true)
                                navegarAlStep2(state.idProducto)
                            }
                            is PublicarViewModel.PublicarState.Error -> {
                                binding.btnCrearProducto.isEnabled = true
                                binding.btnUsarProductoExistente.isEnabled = true
                                toast(state.mensaje, long = true)
                                viewModel.resetearEstado()
                            }
                            else -> {
                                binding.btnCrearProducto.isEnabled = true
                                binding.btnUsarProductoExistente.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navegarAlStep2(idProducto: String) {
        val step2 = PublicarLoteStep2Fragment().apply {
            arguments = Bundle().apply {
                putString("producto_id", idProducto)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, step2)
            .addToBackStack(null)
            .commit()
    }

    private fun poblarSpinnerCategorias(lista: List<CategoriaDTO>){
        binding.spinnerCategoria.adapter = adapterDeNombres(lista){it.nombre}
    }

    private fun poblarSpinnerProductos(lista: List<ProductoDTO>) {
        binding.spinnerProductosExistentes.adapter = adapterDeNombres(lista) { it.nombre }
    }

    private fun <T> adapterDeNombres(items: List<T>, label: (T) -> String): ArrayAdapter<T> {
        val adapter = object : ArrayAdapter<T>(
            requireContext(), android.R.layout.simple_spinner_item, items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                getItem(position)?.let { tv.text = label(it) }
                return tv
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                getItem(position)?.let { tv.text = label(it) }
                return tv
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private fun usarProductoExistente() {
        val seleccionado = binding.spinnerProductosExistentes.selectedItem as? ProductoDTO
        val idProd = seleccionado?.idProducto
        if (idProd == null) { toast("Seleccioná un producto"); return }
        viewModel.seleccionarProductoExistente(idProd)
    }

    private fun crearProductoNuevo() {
        val nombre = binding.etNombreProducto.text?.toString()?.trim().orEmpty()
        if (nombre.isEmpty()) { binding.tilNombreProducto.error = "Ingresá un nombre"; return }
        binding.tilNombreProducto.error = null

        val categoriaSel = binding.spinnerCategoria.selectedItem as? CategoriaDTO
        val catId = categoriaSel?.idCategoria
        if (catId == null) { toast("Seleccioná una categoría"); return }

        val uri = imagenUri
        if (uri == null) { toast("Seleccioná una imagen"); return }

        val bytes = try {
            requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) { null }

        if (bytes == null) { toast("No se pudo leer la imagen"); return }

        val imagenBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        val mime = requireContext().contentResolver.getType(uri)
        val ext = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        viewModel.crearProductoNuevo(nombre, catId, imagenBase64, ext)
    }

    private fun obtenerTamanoUri(uri: Uri): Long {
        var size = 0L
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && cursor.moveToFirst()) size = cursor.getLong(idx)
        }
        return size
    }

    private fun toast(msg: String, long: Boolean = false) =
        Toast.makeText(requireContext(), msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_IMAGEN_BYTES = 5L * 1024 * 1024
    }
}