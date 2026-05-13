package com.grupo3.donapp_access.features.usuario

import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.model.Categoria
import com.grupo3.donapp_access.model.OfertaLote
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClienteRepository(
    private val supabaseClient: SupabaseClient = SupabaseClient
) {
    fun obtenerCategorias(): List<Categoria> {
        val response = supabaseClient.get(
            table = "categoria",
            query = mapOf(
                "select" to "id_categoria,nombre,estado",
                "estado" to "eq.ACTIVO",
                "order" to "nombre.asc"
            )
        )

        return JSONArray(response).mapObjects { item ->
            Categoria(
                idCategoria = item.optString("id_categoria"),
                nombre = item.optString("nombre"),
                estado = item.optString("estado", "ACTIVO")
            )
        }
    }

    fun obtenerOfertas(): List<OfertaLote> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val response = try {
            supabaseClient.get(
                table = "lote",
                query = mapOf(
                    "select" to "id_lote,numero_lote,cantidad,fecha_vencimiento,precio_normal,precio_oferta,estado,productos(nombre,imagen,presentacion),tiendas(nombre,direccion,rating_promedio)",
                    "estado" to "eq.en_oferta",
                    "fecha_vencimiento" to "gte.$today",
                    "order" to "fecha_vencimiento.asc"
                )
            )
        } catch (_: Exception) {
            supabaseClient.get(
                table = "lote",
                query = mapOf(
                    "select" to "id_lote,numero_lote,cantidad,fecha_vencimiento,precio_normal,precio_oferta,estado",
                    "estado" to "eq.en_oferta",
                    "fecha_vencimiento" to "gte.$today",
                    "order" to "fecha_vencimiento.asc"
                )
            )
        }

        return JSONArray(response).mapObjects { item ->
            val producto = item.optNestedObject("productos")
            val tienda = item.optNestedObject("tiendas")
            OfertaLote(
                idLote = item.optString("id_lote"),
                productoNombre = producto?.optString("nombre")?.takeIf { it.isNotBlank() }
                    ?: "Producto en oferta",
                productoImagen = producto?.optNullableString("imagen"),
                productoPresentacion = producto?.optNullableString("presentacion"),
                tiendaNombre = tienda?.optString("nombre")?.takeIf { it.isNotBlank() }
                    ?: "Tienda Donapp",
                tiendaDireccion = tienda?.optNullableString("direccion"),
                cantidad = item.optInt("cantidad", 0),
                fechaVencimiento = item.optString("fecha_vencimiento"),
                precioNormal = item.optDouble("precio_normal", 0.0),
                precioOferta = item.optNullableDouble("precio_oferta") ?: 0.0,
                numeroLote = item.optNullableString("numero_lote"),
                ratingTienda = tienda?.optNullableDouble("rating_promedio")
            )
        }
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONObject.optNestedObject(key: String): JSONObject? {
        return when (val value = opt(key)) {
            is JSONObject -> value
            is JSONArray -> if (value.length() > 0) value.optJSONObject(0) else null
            else -> null
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        return if (!has(key) || isNull(key)) null else optDouble(key)
    }
}
