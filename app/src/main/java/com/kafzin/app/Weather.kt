package com.kafzin.app

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Cidade(val nome: String, val lat: Double, val lon: Double)

data class Clima(
    val cidade: String,
    val mensalMm: List<Pair<String, Double>>, // "2026-03" -> mm
    val totalAnoMm: Double,
    val previsao16DiasMm: Double,
)

/** Dados via Open-Meteo (gratuito, sem chave): geocoding, ERA5 histórico e previsão. */
object ClimaApi {
    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        try {
            return JSONObject(c.inputStream.bufferedReader().readText())
        } finally {
            c.disconnect()
        }
    }

    suspend fun buscarCidade(nome: String): Cidade? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(nome, "UTF-8")
        val j = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=pt&format=json")
        val r = j.optJSONArray("results") ?: return@withContext null
        val o = r.getJSONObject(0)
        val uf = o.optString("admin1", "")
        val nomeCompleto = if (uf.isBlank()) o.getString("name") else o.getString("name") + " – " + uf
        Cidade(nomeCompleto, o.getDouble("latitude"), o.getDouble("longitude"))
    }

    suspend fun carregar(c: Cidade): Clima = withContext(Dispatchers.IO) {
        // 12 meses completos (o arquivo ERA5 tem ~5 dias de atraso)
        val ref = LocalDate.now().minusDays(7)
        val fim = ref.withDayOfMonth(1).minusDays(1)
        val ini = fim.withDayOfMonth(1).minusMonths(11)

        val h = get(
            "https://archive-api.open-meteo.com/v1/archive?latitude=${c.lat}&longitude=${c.lon}" +
                "&start_date=$ini&end_date=$fim&daily=precipitation_sum&timezone=auto"
        )
        val d = h.getJSONObject("daily")
        val datas = d.getJSONArray("time")
        val vals = d.getJSONArray("precipitation_sum")
        val porMes = linkedMapOf<String, Double>()
        for (i in 0 until datas.length()) {
            val mes = datas.getString(i).substring(0, 7)
            val mm = if (vals.isNull(i)) 0.0 else vals.getDouble(i)
            porMes[mes] = (porMes[mes] ?: 0.0) + mm
        }

        val f = get(
            "https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lon}" +
                "&daily=precipitation_sum&forecast_days=16&timezone=auto"
        )
        val fv = f.getJSONObject("daily").getJSONArray("precipitation_sum")
        var prev = 0.0
        for (i in 0 until fv.length()) if (!fv.isNull(i)) prev += fv.getDouble(i)

        Clima(c.nome, porMes.map { it.key to it.value }, porMes.values.sum(), prev)
    }
}
