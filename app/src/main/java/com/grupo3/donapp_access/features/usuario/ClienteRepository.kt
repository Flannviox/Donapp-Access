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

import com.grupo3.donapp_access.features.lotes.dto.OfertaViewDTO
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
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




    suspend fun buscarOfertas(query: String): List<OfertaLote> {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val lotes = SupabaseClient.client.from("vw_ofertas_busqueda")
            .select {
                filter {
                    isIn("estado", listOf("disponible", "en_oferta"))
                    gte("fecha_vencimiento", hoy)

                    // Búsqueda simultánea usando el bloque 'or' de Kotlin
                    or {
                        ilike("producto_nombre", "%$query%")
                        ilike("categoria_nombre", "%$query%")
                    }
                }
            }.decodeList<OfertaViewDTO>()

        return lotes.map { it.toOfertaLote() }
    }




    suspend fun obtenerTiendas(): List<TiendaHome> {
        return SupabaseClient.client.from("tiendas")
            // Agrega latitud y longitud a la consulta raw
            .select(Columns.raw("id_tienda,nombre,direccion,rating_promedio,latitud,longitud"))
            .decodeList<TiendaHomeDTO>()
            .map {
                TiendaHome(
                    idTienda = it.idTienda,
                    nombre = it.nombre,
                    direccion = it.direccion,
                    rating = it.rating,
                    latitud = it.latitud,
                    longitud = it.longitud
                )
            }
    }

    suspend fun obtenerCategorias(): List<Categoria> {
        return SupabaseClient.client.from("categoria")
            .select(Columns.raw("id_categoria,nombre,estado")) {
                filter { eq("estado", "ACTIVO") }
            }.decodeList<Categoria>()
    }
    suspend fun obtenerTiendasCercanas(latUsuario: Double, lngUsuario: Double, radioMetros: Int = 10000): List<TiendaHome> {
        // Empaquetamos las coordenadas para enviarlas a Supabase
        val parametros = CoordenadasParam(lat = latUsuario, lng = lngUsuario, radio = radioMetros)

        // Llamamos a tu función SQL "tiendas_cercanas" usando .rpc()
        return SupabaseClient.client.postgrest.rpc("tiendas_cercanas", parametros)
            .decodeList<TiendaHomeDTO>()
            .map {
                TiendaHome(
                    idTienda = it.idTienda,
                    nombre = it.nombre,
                    direccion = it.direccion,
                    rating = it.rating,
                    latitud = it.latitud,
                    longitud = it.longitud
                )
            }
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
    @SerialName("id_tienda") val idTienda: String? = null,
    val nombre: String,
    val direccion: String? = null,
    @SerialName("rating_promedio") val rating: Double? = null,

    val latitud: Double? = null,
    val longitud: Double? = null
)




@Serializable
data class CoordenadasParam(
    val lat: Double,
    val lng: Double,
    val radio: Int
)