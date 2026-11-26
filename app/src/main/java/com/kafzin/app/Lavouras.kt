package com.kafzin.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.UUID

@Composable
fun LavourasTela(abertaId: String?, onAbrir: (String?) -> Unit, onIrCalculadora: () -> Unit) {
    val aberta = Repo.lavouras.firstOrNull { it.id == abertaId }
    if (aberta == null) {
        ListaLavouras(onAbrir = { onAbrir(it) }, onIrCalculadora = onIrCalculadora)
    } else {
        DetalheLavoura(aberta, onVoltar = { onAbrir(null) })
    }
}

@Composable
fun ListaLavouras(onAbrir: (String) -> Unit, onIrCalculadora: () -> Unit) {
    Tela {
        Titulo("Minhas lavouras")
        if (Repo.lavouras.isEmpty()) {
            Text(
                "Você ainda não tem lavouras. Faça uma simulação na Calculadora e toque em " +
                    "“Salvar como lavoura” para começar o histórico: plantio, adubações, custos e colheitas."
            )
            Button(onClick = onIrCalculadora, modifier = Modifier.fillMaxWidth()) {
                Text("Ir para a Calculadora")
            }
        } else {
            Repo.lavouras.forEach { l ->
                Card(Modifier.fillMaxWidth().clickable { onAbrir(l.id) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(l.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${l.variedade} • ${fmt(l.areaM2 / 10000.0, 2)} ha")
                        Text("Gasto até agora: ${brl(l.custoReal)}", style = MaterialTheme.typography.bodySmall)
                        Text("${l.lancamentos.size} lançamentos", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DetalheLavoura(l: Lavoura, onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    var mostrarNovo by remember { mutableStateOf(false) }
    var excluirLavoura by remember { mutableStateOf(false) }
    var excluirLanc by remember { mutableStateOf<Lancamento?>(null) }
    val ha = l.areaM2 / 10000.0
    val porTipo = l.lancamentos
        .filter { it.custo > 0 }
        .groupBy { it.tipo }
        .map { (t, lista) -> t to lista.sumOf { x -> x.custo } }
        .sortedByDescending { it.second }

    Tela {
        TextButton(onClick = onVoltar) { Text("← Voltar") }
        Titulo(l.nome)
        val local = if (l.cidade.isNotBlank()) " • " + l.cidade else ""
        Text("${l.variedade} • ${fmt(ha, 2)} ha$local")

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Resumo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Linha("Pés planejados", fmt(l.pesPlanejados.toDouble()))
                if (l.custoPlanejado > 0) Linha("Custo planejado (simulação)", brl(l.custoPlanejado))
                Linha("Gasto real até agora", brl(l.custoReal))
                if (l.sacasTotal > 0) {
                    HorizontalDivider()
                    Linha("Colhido", "${fmt(l.sacasTotal, 1)} sacas")
                    if (ha > 0) Linha("Produtividade", "${fmt(l.sacasTotal / ha, 1)} sacas/ha")
                    Linha("Custo por saca", brl(l.custoReal / l.sacasTotal))
                }
                if (l.receitaTotal > 0) {
                    Linha("Receita", brl(l.receitaTotal))
                    Linha("Resultado", brl(l.receitaTotal - l.custoReal))
                }
            }
        }

        if (porTipo.isNotEmpty()) {
            Text("Gastos por categoria", fontWeight = FontWeight.Bold)
            porTipo.forEach { (t, v) -> Linha(t.rotulo, brl(v)) }
        }

        Button(onClick = { mostrarNovo = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Adicionar lançamento")
        }

        Text("Histórico", fontWeight = FontWeight.Bold)
        if (l.lancamentos.isEmpty()) {
            Text("Nenhum lançamento ainda. Registre a compra de mudas, adubações, mão de obra, colheitas…")
        }
        l.lancamentos.forEach { x ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${dataBr(x.data)} • ${x.tipo.rotulo}", fontWeight = FontWeight.Bold)
                        if (x.descricao.isNotBlank()) Text(x.descricao)
                        if (x.custo > 0) Text("Custo: ${brl(x.custo)}", style = MaterialTheme.typography.bodySmall)
                        x.sacas?.let { Text("Colhido: ${fmt(it, 1)} sacas", style = MaterialTheme.typography.bodySmall) }
                        x.receita?.let { Text("Venda: ${brl(it)}", style = MaterialTheme.typography.bodySmall) }
                    }
                    TextButton(onClick = { excluirLanc = x }) { Text("Excluir") }
                }
            }
        }

        OutlinedButton(onClick = { compartilhar(ctx, resumoLavoura(l)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Compartilhar histórico")
        }
        TextButton(onClick = { excluirLavoura = true }) {
            Text("Excluir esta lavoura", color = MaterialTheme.colorScheme.error)
        }
    }

    if (mostrarNovo) {
        NovoLancamentoDialog(
            onSalvar = { x ->
                Repo.adicionarLancamento(l.id, x)
                mostrarNovo = false
            },
            onFechar = { mostrarNovo = false },
        )
    }

    excluirLanc?.let { x ->
        AlertDialog(
            onDismissRequest = { excluirLanc = null },
            title = { Text("Excluir lançamento?") },
            text = { Text("${dataBr(x.data)} – ${x.tipo.rotulo}") },
            confirmButton = {
                TextButton(onClick = {
                    Repo.removerLancamento(l.id, x.id)
                    excluirLanc = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluirLanc = null }) { Text("Cancelar") } },
        )
    }

    if (excluirLavoura) {
        AlertDialog(
            onDismissRequest = { excluirLavoura = false },
            title = { Text("Excluir lavoura?") },
            text = { Text("Todo o histórico de “${l.nome}” será apagado deste celular.") },
            confirmButton = {
                TextButton(onClick = {
                    excluirLavoura = false
                    onVoltar()
                    Repo.removerLavoura(l.id)
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluirLavoura = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun NovoLancamentoDialog(onSalvar: (Lancamento) -> Unit, onFechar: () -> Unit) {
    var tipo by remember { mutableStateOf(TipoLancamento.ADUBACAO) }
    var data by remember { mutableStateOf(LocalDate.now().format(FMT_DATA)) }
    var desc by remember { mutableStateOf("") }
    var custo by remember { mutableStateOf("") }
    var sacas by remember { mutableStateOf("") }
    var receita by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Novo lançamento") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Tipo", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoLancamento.values().forEach { t ->
                        FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t.rotulo) })
                    }
                }
                Campo("Data (dd/mm/aaaa)", data, { data = it }, numerico = false)
                Campo("Descrição / quantidade (ex.: 2.000 mudas)", desc, { desc = it }, numerico = false)
                Campo("Custo (R$)", custo, { custo = it })
                if (tipo == TipoLancamento.COLHEITA) {
                    Campo("Sacas colhidas (60 kg)", sacas, { sacas = it })
                    Campo("Valor da venda (R$, opcional)", receita, { receita = it })
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val d = try { LocalDate.parse(data.trim(), FMT_DATA) } catch (e: Exception) { null }
                val c = custo.toNum() ?: 0.0
                val colheita = tipo == TipoLancamento.COLHEITA
                val s = if (colheita) sacas.toNum() else null
                val r = if (colheita) receita.toNum() else null
                if (d == null) {
                    erro = "Data inválida. Use dd/mm/aaaa."
                } else if (c <= 0.0 && (s ?: 0.0) <= 0.0) {
                    erro = "Informe o custo ou as sacas colhidas."
                } else {
                    onSalvar(
                        Lancamento(
                            id = UUID.randomUUID().toString(),
                            data = d.toString(),
                            tipo = tipo,
                            descricao = desc.trim(),
                            custo = c,
                            sacas = s,
                            receita = r,
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

@Composable
fun SalvarLavouraDialog(nomeInicial: String, onSalvar: (String) -> Unit, onFechar: () -> Unit) {
    var nome by remember { mutableStateOf(nomeInicial) }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Salvar como lavoura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A simulação vira o ponto de partida do histórico. Depois você registra custos, tratos e colheitas.")
                Campo("Nome da lavoura (ex.: Talhão da encosta)", nome, { nome = it }, numerico = false)
            }
        },
        confirmButton = {
            Button(
                enabled = nome.isNotBlank(),
                onClick = { onSalvar(nome.trim()) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
