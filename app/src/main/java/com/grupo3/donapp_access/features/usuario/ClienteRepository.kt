package com.grupo3.donapp_access.features.usuario

import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.model.Categoria
import com.grupo3.donapp_access.model.OfertaLote
import com.grupo3.donapp_access.model.TiendaHome
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClienteRepository @Inject constructor() {

    suspend fun obtenerOfertas(): List<OfertaLote> {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val lotes = SupabaseClient.client.from("lote")
            .select(Columns.raw(
                "id_lote,numero_lote,cantidad,fecha_vencimiento," +
                        "precio_normal,precio_oferta,estado," +
                        "productos(nombre,imagen,presentacion)," +
                        "tiendas(id_tienda,nombre,direccion,rating_promedio)"
            )) {
                filter {
                    eq("estado", "en_oferta")
                    gte("fecha_vencimiento", hoy)
                }
            }.decodeList<LoteConRelaciones>()

        return lotes.map { it.toOfertaLote() }
    }

    suspend fun obtenerTiendas(): List<TiendaHome> {
        return SupabaseClient.client.from("tiendas")
            .select(Columns.raw("nombre,direccion,rating_promedio"))
            .decodeList<TiendaHomeDTO>()
            .map { TiendaHome(nombre = it.nombre, direccion = it.direccion, rating = it.rating) }
    }

    suspend fun obtenerCategorias(): List<Categoria> {
        return SupabaseClient.client.from("categoria")
            .select(Columns.raw("id_categoria,nombre,estado")) {
                filter { eq("estado", "ACTIVO") }
            }.decodeList<Categoria>()
    }
}

// DTOs internos para deserializar los joins
@Serializable
private data class LoteConRelaciones(
    @SerialName("id_lote")           val idLote: String,
    @SerialName("id_tienda")         val idTienda: String? = null,
    @SerialName("numero_lote")       val numeroLote: String? = null,
    @SerialName("cantidad")          val cantidad: Int,
    @SerialName("fecha_vencimiento") val fechaVencimiento: String,
    @SerialName("precio_normal")     val precioNormal: Double,
    @SerialName("precio_oferta")     val precioOferta: Double? = null,
    @SerialName("productos")         val producto: ProductoRef? = null,
    @SerialName("tiendas")           val tienda: TiendaRef? = null
) {
    fun toOfertaLote() = OfertaLote(
        idLote = idLote,
        productoNombre = producto?.nombre ?: "Producto en oferta",
        tiendaId = tienda?.idTienda,
        productoImagen = producto?.imagen,
        productoPresentacion = producto?.presentacion,
        tiendaNombre = tienda?.nombre ?: "Tienda Donapp",
        tiendaDireccion = tienda?.direccion,
        cantidad = cantidad,
        fechaVencimiento = fechaVencimiento,
        precioNormal = precioNormal,
        precioOferta = precioOferta ?: 0.0,
        numeroLote = numeroLote,
        ratingTienda = tienda?.rating
    )
}

@Serializable
private data class ProductoRef(
    val nombre: String,
    val imagen: String? = null,
    val presentacion: String? = null
)

@Serializable
private data class TiendaRef(
    @SerialName("id_tienda") val idTienda: String? = null,
    val nombre: String,
    val direccion: String? = null,
    @SerialName("rating_promedio") val rating: Double? = null
)

@Serializable
private data class TiendaHomeDTO(
    val nombre: String,
    val direccion: String? = null,
    @SerialName("rating_promedio") val rating: Double? = null
)