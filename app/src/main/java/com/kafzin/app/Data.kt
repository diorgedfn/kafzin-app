package com.kafzin.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

enum class TipoLancamento(val rotulo: String) {
    PLANTIO("Plantio / mudas"),
    ADUBACAO("Adubação"),
    DEFENSIVOS("Pragas e doenças"),
    PODA("Poda e tratos"),
    MAO_DE_OBRA("Mão de obra"),
    MAQUINAS("Máquinas e combustível"),
    COLHEITA("Colheita"),
    OUTROS("Outros"),
}

data class Lancamento(
    val id: String,
    val data: String, // ISO: yyyy-MM-dd
    val tipo: TipoLancamento,
    val descricao: String,
    val custo: Double,
    val sacas: Double?, // só para colheita
    val receita: Double?, // venda (opcional, só para colheita)
)

data class Lavoura(
    val id: String,
    val nome: String,
    val cidade: String,
    val criadaEm: String,
    val areaM2: Double,
    val variedade: String,
    val pesPlanejados: Int,
    val custoPlanejado: Double,
    val lancamentos: List<Lancamento>,
)

val Lavoura.custoReal: Double get() = lancamentos.sumOf { it.custo }
val Lavoura.sacasTotal: Double get() = lancamentos.sumOf { it.sacas ?: 0.0 }
val Lavoura.receitaTotal: Double get() = lancamentos.sumOf { it.receita ?: 0.0 }

val FMT_DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun dataBr(iso: String): String =
    try { LocalDate.parse(iso).format(FMT_DATA) } catch (e: Exception) { iso }

fun resumoLavoura(l: Lavoura): String = buildString {
    appendLine("Histórico da lavoura – ${l.nome}")
    appendLine("${l.variedade} • ${fmt(l.areaM2 / 10000.0, 2)} ha • ${fmt(l.pesPlanejados.toDouble())} pés planejados")
    appendLine("Gasto total: ${brl(l.custoReal)}")
    if (l.sacasTotal > 0) appendLine("Colhido: ${fmt(l.sacasTotal, 1)} sacas")
    appendLine()
    l.lancamentos.forEach { x ->
        append("${dataBr(x.data)} – ${x.tipo.rotulo}")
        if (x.descricao.isNotBlank()) append(": ${x.descricao}")
        if (x.custo > 0) append(" (${brl(x.custo)})")
        appendLine()
    }
}

/** Guarda as lavouras em um arquivo JSON no próprio celular (sem servidor). */
object Repo {
    val lavouras = mutableStateListOf<Lavoura>()
    private lateinit var arquivo: File

    fun init(ctx: Context) {
        if (::arquivo.isInitialized) return
        arquivo = File(ctx.filesDir, "lavouras.json")
        try {
            if (arquivo.exists()) {
                val arr = JSONArray(arquivo.readText())
                for (i in 0 until arr.length()) lavouras.add(lerLavoura(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // arquivo ilegível: começa vazio
        }
    }

    private fun salvar() {
        try {
            val arr = JSONArray()
            lavouras.forEach { arr.put(paraJson(it)) }
            arquivo.writeText(arr.toString())
        } catch (e: Exception) {
            // falha de gravação: mantém em memória
        }
    }

    fun adicionarLavoura(l: Lavoura) {
        lavouras.add(0, l)
        salvar()
    }

    fun removerLavoura(id: String) {
        lavouras.removeAll { it.id == id }
        salvar()
    }

    fun adicionarLancamento(lavouraId: String, x: Lancamento) {
        val i = lavouras.indexOfFirst { it.id == lavouraId }
        if (i < 0) return
        val atual = lavouras[i]
        lavouras[i] = atual.copy(lancamentos = (atual.lancamentos + x).sortedByDescending { it.data })
        salvar()
    }

    fun removerLancamento(lavouraId: String, id: String) {
        val i = lavouras.indexOfFirst { it.id == lavouraId }
        if (i < 0) return
        val atual = lavouras[i]
        lavouras[i] = atual.copy(lancamentos = atual.lancamentos.filter { it.id != id })
        salvar()
    }

    private fun paraJson(x: Lancamento): JSONObject = JSONObject().apply {
        put("id", x.id)
        put("data", x.data)
        put("tipo", x.tipo.name)
        put("descricao", x.descricao)
        put("custo", x.custo)
        x.sacas?.let { put("sacas", it) }
        x.receita?.let { put("receita", it) }
    }

    private fun paraJson(l: Lavoura): JSONObject = JSONObject().apply {
        put("id", l.id)
        put("nome", l.nome)
        put("cidade", l.cidade)
        put("criadaEm", l.criadaEm)
        put("areaM2", l.areaM2)
        put("variedade", l.variedade)
        put("pesPlanejados", l.pesPlanejados)
        put("custoPlanejado", l.custoPlanejado)
        val arr = JSONArray()
        l.lancamentos.forEach { arr.put(paraJson(it)) }
        put("lancamentos", arr)
    }

    private fun lerLancamento(o: JSONObject): Lancamento {
        val tipo = try {
            TipoLancamento.valueOf(o.getString("tipo"))
        } catch (e: Exception) {
            TipoLancamento.OUTROS
        }
        return Lancamento(
            id = o.getString("id"),
            data = o.getString("data"),
            tipo = tipo,
            descricao = o.optString("descricao", ""),
            custo = o.optDouble("custo", 0.0),
            sacas = if (o.has("sacas")) o.getDouble("sacas") else null,
            receita = if (o.has("receita")) o.getDouble("receita") else null,
        )
    }

    private fun lerLavoura(o: JSONObject): Lavoura {
        val arr = o.optJSONArray("lancamentos") ?: JSONArray()
        val lista = (0 until arr.length()).map { lerLancamento(arr.getJSONObject(it)) }
        return Lavoura(
            id = o.getString("id"),
            nome = o.getString("nome"),
            cidade = o.optString("cidade", ""),
            criadaEm = o.getString("criadaEm"),
            areaM2 = o.getDouble("areaM2"),
            variedade = o.getString("variedade"),
            pesPlanejados = o.getInt("pesPlanejados"),
            custoPlanejado = o.optDouble("custoPlanejado", 0.0),
            lancamentos = lista,
        )
    }
}
