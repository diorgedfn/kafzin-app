package com.kafzin.app

import java.util.Locale
import kotlin.math.floor

data class Variedade(val nome: String, val linha: Double, val planta: Double)

val VARIEDADES = listOf(
    Variedade("Bourbon Amarelo", 2.5, 2.5),
    Variedade("Catuaí", 2.0, 2.5),
    Variedade("Mundo Novo", 3.0, 3.0),
    Variedade("Acaiá", 3.5, 3.5),
    Variedade("Icatu", 3.0, 2.5),
    Variedade("Topázio", 2.5, 3.0),
)

/**
 * PREMISSAS: valores de partida. Troque pelas regras reais da calculadora do kafzin.com
 * (Embrapa / IAPAR / Procafé) antes de divulgar o app.
 */
object Premissas {
    const val APROVEITAMENTO_PLANO = 0.85
    const val APROVEITAMENTO_MORRO = 0.75
    const val ADICIONAL_LINHA_MECANICA = 0.5 // m a mais entre linhas na colheita mecânica
    const val ADUBO_KG_PE_ANO = 0.25
}

data class Resultado(
    val pes: Int,
    val pesPor1000m2: Double,
    val aduboKgAno: Double,
    val aduboKgPorPe: Double,
    val linha: Double,
    val planta: Double,
    val aproveitamento: Double,
    val custoMudas: Double?,
    val custoAdubo: Double?,
)

fun calcular(
    areaM2: Double,
    morro: Boolean,
    mecanica: Boolean,
    v: Variedade,
    linhaCustom: Double?,
    plantaCustom: Double?,
    precoMuda: Double?,
    precoAdubo: Double?,
): Resultado? {
    if (areaM2 <= 0) return null
    val extra = if (mecanica) Premissas.ADICIONAL_LINHA_MECANICA else 0.0
    val linha = linhaCustom ?: (v.linha + extra)
    val planta = plantaCustom ?: v.planta
    if (linha <= 0 || planta <= 0) return null
    val aprov = if (morro) Premissas.APROVEITAMENTO_MORRO else Premissas.APROVEITAMENTO_PLANO
    val pes = floor(areaM2 * aprov / (linha * planta)).toInt()
    val aduboAno = pes * Premissas.ADUBO_KG_PE_ANO
    return Resultado(
        pes = pes,
        pesPor1000m2 = pes / areaM2 * 1000.0,
        aduboKgAno = aduboAno,
        aduboKgPorPe = Premissas.ADUBO_KG_PE_ANO,
        linha = linha,
        planta = planta,
        aproveitamento = aprov * 100,
        custoMudas = precoMuda?.let { it * pes },
        custoAdubo = precoAdubo?.let { it * aduboAno },
    )
}

private val PT_BR = Locale("pt", "BR")

fun fmt(x: Double, casas: Int = 0): String = String.format(PT_BR, "%,." + casas + "f", x)

fun brl(x: Double): String = "R$ " + fmt(x, 2)

fun resumo(r: Resultado, variedade: String): String = buildString {
    appendLine("Estimativa de lavoura (Kafzin) – $variedade")
    appendLine("Total de pés: ${fmt(r.pes.toDouble())}")
    appendLine("Espaçamento: ${fmt(r.linha, 2)} m x ${fmt(r.planta, 2)} m")
    appendLine("Adubo anual: ${fmt(r.aduboKgAno)} kg")
    r.custoMudas?.let { appendLine("Mudas: ${brl(it)}") }
    r.custoAdubo?.let { appendLine("Adubo (1º ano): ${brl(it)}") }
    append("Estimativa orientativa. Consulte um engenheiro agrônomo.")
}
