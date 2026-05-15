package com.grupo3.donapp_access.features.auth

import com.grupo3.donapp_access.core.network.SupabaseClient
import com.grupo3.donapp_access.features.auth.dto.LoteDTO
import io.github.jan.supabase.postgrest.from // IMPORTANTE PARA EL .from
import io.github.jan.supabase.postgrest.query.Columns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class LoteRepository @Inject constructor() {

    suspend fun getLotesVencenHoy(): List<LoteDTO> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        // Usamos directamente el cliente de tu core.network
        return SupabaseClient.client.from("lote").select(columns = Columns.raw("*, productos(*)")) {
            filter {
                // gt = Greater Than (Mayor que)
                gt("cantidad", 0)
                // Comenta la fecha si quieres ver lotes de otros días (como el del día 17 que creaste)
                // eq("fecha_vencimiento", today)
            }
        }.decodeList<LoteDTO>()
    }
}