package com.grupo3.donapp_access.features.lotes

import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.features.lotes.dto.CategoriaDTO
import com.grupo3.donapp_access.features.lotes.dto.LoteDTO
import com.grupo3.donapp_access.features.lotes.dto.ProductoDTO
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject

class LoteRepository @Inject constructor() {

    private val supabase = SupabaseClient.client

    suspend fun fetchCategorias(): List<CategoriaDTO> {
        return try {
            supabase.from("categoria")
                .select()
                .decodeList<CategoriaDTO>()
        } catch (e: Exception) {
            android.util.Log.e("LOTE_REPO", "Error al obtener categorías: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchProductos(): List<ProductoDTO> {
        return try {
            supabase.from("productos")
                .select()
                .decodeList<ProductoDTO>()
        } catch (e: Exception) {
            android.util.Log.e("LOTE_REPO", "Error al obtener productos: ${e.message}", e)
            throw e
        }
    }

    suspend fun getIdTiendaDelUsuario(authUserId: String): String {
        val tienda = try {
            supabase.from("tiendas")
                .select(columns = Columns.list("id_tienda")) {
                    filter { eq("usuarios_id", authUserId) }
                    limit(1)
                }
                .decodeSingleOrNull<TiendaIdRow>()
        } catch (e: Exception) {
            android.util.Log.e("LOTE_REPO", "Error al buscar tienda del usuario: ${e.message}", e)
            throw e
        }

        return tienda?.idTienda
            ?: throw IllegalStateException("El comerciante no tiene tienda registrada")
    }

    suspend fun uploadImagenProducto(
        bytes: ByteArray,
        idTienda: String,
        extension: String
    ): String {
        val path = "$idTienda/${UUID.randomUUID()}.$extension"
        try {
            supabase.storage.from("productos").upload(path, bytes) {
                upsert = false
            }
        } catch (e: Exception) {
            android.util.Log.e("LOTE_REPO", "Error subiendo imagen a Storage: ${e.message}", e)
            throw e
        }
        return supabase.storage.from("productos").publicUrl(path)
    }

    suspend fun insertarProducto(dto: ProductoDTO): String {
        return try {
            val insertado = supabase.from("productos")
                .insert(dto) { select() }
                .decodeSingle<ProductoDTO>()
            insertado.idProducto
                ?: throw IllegalStateException("Supabase no devolvió id_producto tras el insert")
        } catch (e: Exception) {
            android.util.Log.e("LOTE_REPO", "Error al insertar producto: ${e.message}", e)
            throw e
        }
    }

    suspend fun insertarLote(dto: LoteDTO) {
        try {
            supabase.from("lote").insert(dto)
        } catch (e: Exception) {
            android.util.Log.e("LOTE_REPO", "Error al insertar lote: ${e.message}", e)
            throw e
        }
    }

    @Serializable
    private data class TiendaIdRow(
        @SerialName("id_tienda") val idTienda: String
    )
}
