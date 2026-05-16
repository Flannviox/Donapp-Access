package com.grupo3.donapp_access.features.comerciante.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.lotes.PublicarViewModel
import com.grupo3.donapp_access.features.lotes.dto.CategoriaDTO
import com.grupo3.donapp_access.features.lotes.dto.ProductoDTO
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class PublicarLoteFragment : Fragment() {

    //ViewModel inyectado por Hilt, scope del Fragment
    private val viewModel: PublicarViewModel by viewModels()

    //Referencias a widgets del layout
    private lateinit var btnBack: ImageButton
    private lateinit var layoutPaso1: View
    private lateinit var layoutPaso2: View

    //Paso 1
    private lateinit var spinnerProductosExistentes: Spinner
    private lateinit var btnUsarProductoExistente: Button
    private lateinit var tilNombreProducto: TextInputLayout
    private lateinit var etNombreProducto: TextInputEditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var ivPreviewImagen: ImageView
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var btnCrearProducto: Button

    //Paso 2
    private lateinit var tilCantidad: TextInputLayout
    private lateinit var etCantidad: TextInputEditText
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var tilPrecioNormal: TextInputLayout
    private lateinit var etPrecioNormal: TextInputEditText
    private lateinit var tilPrecioOferta: TextInputLayout
    private lateinit var etPrecioOferta: TextInputEditText
    private lateinit var btnPublicarLote: Button

    //Estado local del Fragment
    private var imagenUri: Uri? = null
    private var fechaSeleccionada: String? = null

    //Launcher para seleccionar imagen de galería. Se registra en property initializer
    //(patrón canónico) para que la suscripción al ActivityResultRegistry sobreviva
    //rotaciones y re-attachment del Fragment sin lanzar IllegalStateException.
    private val imagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult

        //Rechazar archivos > 5MB
        val size = obtenerTamanoUri(uri)
        if (size > MAX_IMAGEN_BYTES) {
            mostrarToast("La imagen no debe superar 5MB", long = true)
            return@registerForActivityResult
        }

        imagenUri = uri
        Glide.with(this).load(uri).into(ivPreviewImagen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_publicar_lote, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindWidgets(view)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSeleccionarImagen.setOnClickListener {
            imagenLauncher.launch("image/*")
        }

        btnUsarProductoExistente.setOnClickListener {
            usarProductoExistente()
        }

        btnCrearProducto.setOnClickListener {
            crearProductoNuevo()
        }

        btnSeleccionarFecha.setOnClickListener {
            mostrarDatePicker()
        }

        btnPublicarLote.setOnClickListener {
            publicarLote()
        }

        observarViewModel()

        //Disparar la carga inicial (categorías + productos + id_tienda)
        viewModel.cargarDatosIniciales()
    }

    private fun bindWidgets(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        layoutPaso1 = view.findViewById(R.id.layoutPaso1)
        layoutPaso2 = view.findViewById(R.id.layoutPaso2)

        spinnerProductosExistentes = view.findViewById(R.id.spinnerProductosExistentes)
        btnUsarProductoExistente = view.findViewById(R.id.btnUsarProductoExistente)
        tilNombreProducto = view.findViewById(R.id.tilNombreProducto)
        etNombreProducto = view.findViewById(R.id.etNombreProducto)
        spinnerCategoria = view.findViewById(R.id.spinnerCategoria)
        ivPreviewImagen = view.findViewById(R.id.ivPreviewImagen)
        btnSeleccionarImagen = view.findViewById(R.id.btnSeleccionarImagen)
        btnCrearProducto = view.findViewById(R.id.btnCrearProducto)

        tilCantidad = view.findViewById(R.id.tilCantidad)
        etCantidad = view.findViewById(R.id.etCantidad)
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha)
        tilPrecioNormal = view.findViewById(R.id.tilPrecioNormal)
        etPrecioNormal = view.findViewById(R.id.etPrecioNormal)
        tilPrecioOferta = view.findViewById(R.id.tilPrecioOferta)
        etPrecioOferta = view.findViewById(R.id.etPrecioOferta)
        btnPublicarLote = view.findViewById(R.id.btnPublicarLote)
    }

    private fun observarViewModel() {
        //Tres StateFlows colectados en paralelo bajo el mismo repeatOnLifecycle(STARTED)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { onStateChanged(it) }
                }
                launch {
                    viewModel.categorias.collect { poblarSpinnerCategorias(it) }
                }
                launch {
                    viewModel.productos.collect { poblarSpinnerProductos(it) }
                }
            }
        }
    }

    private fun onStateChanged(state: PublicarViewModel.PublicarState) {
        when (state) {
            is PublicarViewModel.PublicarState.Idle -> {
                btnCrearProducto.isEnabled = true
                btnPublicarLote.isEnabled = true
                btnUsarProductoExistente.isEnabled = true
            }
            is PublicarViewModel.PublicarState.Loading -> {
                btnCrearProducto.isEnabled = false
                btnPublicarLote.isEnabled = false
                btnUsarProductoExistente.isEnabled = false
            }
            is PublicarViewModel.PublicarState.ProductoCreado -> {
                layoutPaso1.visibility = View.GONE
                layoutPaso2.visibility = View.VISIBLE
            }
            is PublicarViewModel.PublicarState.LotePublicado -> {
                mostrarToast("¡Lote publicado!")
                parentFragmentManager.popBackStack()
            }
            is PublicarViewModel.PublicarState.Error -> {
                mostrarToast(state.mensaje, long = true)
                viewModel.resetearEstado()
            }
        }
    }

    private fun poblarSpinnerCategorias(lista: List<CategoriaDTO>) {
        spinnerCategoria.adapter = adapterDeNombres(lista) { it.nombre }
    }

    private fun poblarSpinnerProductos(lista: List<ProductoDTO>) {
        spinnerProductosExistentes.adapter = adapterDeNombres(lista) { it.nombre }
    }

    //Adapter genérico que muestra `label(item)` como texto del Spinner
    private fun <T> adapterDeNombres(
        items: List<T>,
        label: (T) -> String
    ): ArrayAdapter<T> {
        val adapter = object : ArrayAdapter<T>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
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
        val seleccionado = spinnerProductosExistentes.selectedItem as? ProductoDTO
        val idProd = seleccionado?.idProducto
        if (idProd == null) {
            mostrarToast("Seleccioná un producto")
            return
        }
        viewModel.seleccionarProductoExistente(idProd)
    }

    private fun crearProductoNuevo() {
        //Validación: nombre
        val nombre = etNombreProducto.text?.toString()?.trim().orEmpty()
        if (nombre.isEmpty()) {
            tilNombreProducto.error = "Ingresá un nombre"
            return
        }
        tilNombreProducto.error = null

        //Validación: categoría seleccionada
        val categoriaSel = spinnerCategoria.selectedItem as? CategoriaDTO
        val catId = categoriaSel?.idCategoria
        if (catId == null) {
            mostrarToast("Seleccioná una categoría")
            return
        }

        //Validación: imagen elegida
        val uri = imagenUri
        if (uri == null) {
            mostrarToast("Seleccioná una imagen")
            return
        }

        //Leer bytes del archivo
        val bytes = try {
            requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
        if (bytes == null) {
            mostrarToast("No se pudo leer la imagen")
            return
        }

        //Derivar extensión a partir del MIME
        val mime = requireContext().contentResolver.getType(uri)
        val ext = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        viewModel.crearProductoNuevo(nombre, catId, bytes, ext)
    }

    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Fecha de vencimiento")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            //La fecha viene en UTC; la formateamos en UTC para evitar saltos por zona horaria
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val fechaStr = sdf.format(Date(millis))
            fechaSeleccionada = fechaStr
            btnSeleccionarFecha.text = fechaStr
        }

        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun publicarLote() {
        //Validación: cantidad
        val cantidad = etCantidad.text?.toString()?.toIntOrNull()
        if (cantidad == null || cantidad <= 0) {
            tilCantidad.error = "Ingresá una cantidad válida"
            return
        }
        tilCantidad.error = null

        //Validación: fecha
        val fecha = fechaSeleccionada
        if (fecha == null) {
            mostrarToast("Seleccioná una fecha de vencimiento")
            return
        }

        //Validación: precio normal
        val precioNormal = etPrecioNormal.text?.toString()?.toDoubleOrNull()
        if (precioNormal == null || precioNormal <= 0) {
            tilPrecioNormal.error = "Ingresá un precio válido"
            return
        }
        tilPrecioNormal.error = null

        //Validación: precio oferta (opcional)
        val precioOfertaStr = etPrecioOferta.text?.toString()?.trim().orEmpty()
        val precioOferta = if (precioOfertaStr.isEmpty()) null else precioOfertaStr.toDoubleOrNull()

        if (precioOferta != null) {
            if (precioOferta <= 0) {
                tilPrecioOferta.error = "El precio oferta debe ser mayor a 0"
                return
            }
            if (precioOferta >= precioNormal) {
                tilPrecioOferta.error = "El precio oferta debe ser menor al normal"
                return
            }
        }
        tilPrecioOferta.error = null

        viewModel.publicarLote(cantidad, fecha, precioNormal, precioOferta)
    }

    private fun obtenerTamanoUri(uri: Uri): Long {
        var size = 0L
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && cursor.moveToFirst()) {
                size = cursor.getLong(idx)
            }
        }
        return size
    }

    private fun mostrarToast(mensaje: String, long: Boolean = false) {
        Toast.makeText(
            requireContext(),
            mensaje,
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val MAX_IMAGEN_BYTES = 5L * 1024 * 1024 //5 MB
    }
}