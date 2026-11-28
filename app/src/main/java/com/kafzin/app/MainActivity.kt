package com.kafzin.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        Diretorio.init(applicationContext)
        setContent { KafzinTheme { App() } }
    }
}

// ---------- Tema ----------

@Composable
fun KafzinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2E5E3E),
            secondary = Color(0xFF6F4E37),
            primaryContainer = Color(0xFFD7EBD9),
            onPrimaryContainer = Color(0xFF0E2A18),
        ),
        content = content,
    )
}

// ---------- Utilidades ----------

fun abrir(ctx: Context, uri: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Não foi possível abrir", Toast.LENGTH_SHORT).show()
    }
}

fun compartilhar(ctx: Context, texto: String) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    ctx.startActivity(Intent.createChooser(i, "Compartilhar estimativa"))
}

fun String.toNum(): Double? = trim().replace(',', '.').toDoubleOrNull()

@Composable
fun Tela(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun Titulo(t: String) {
    Text(t, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
fun Linha(a: String, b: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(a)
        Text(b, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Campo(label: String, valor: String, onChange: (String) -> Unit, numerico: Boolean = true) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numerico) KeyboardType.Decimal else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------- App ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var cidade by rememberSaveable { mutableStateOf("") }
    var lavouraAberta by rememberSaveable { mutableStateOf<String?>(null) }
    var apoioSub by rememberSaveable { mutableIntStateOf(0) }
    val itens = listOf(
        "Início" to Icons.Default.Home,
        "Calculadora" to Icons.Default.Build,
        "Lavouras" to Icons.Default.Create,
        "Clima" to Icons.Default.DateRange,
        "Apoio" to Icons.Default.Person,
    )
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kafzin • Café", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                itens.forEachIndexed { i, (nome, icone) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icone, nome) },
                        label = { Text(nome, maxLines = 1, fontSize = 10.sp) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> InicioTela(onIr = { tab = it }, onApoio = { apoioSub = it; tab = 4 })
                1 -> CalculadoraTela(cidade) { id -> lavouraAberta = id; tab = 2 }
                2 -> LavourasTela(lavouraAberta, { lavouraAberta = it }) { tab = 1 }
                3 -> ClimaTela(cidade) { cidade = it }
                else -> ApoioTela(apoioSub, { apoioSub = it }, cidade) { cidade = it }
            }
        }
    }
}

// ---------- Início ----------

private val LINKS = listOf(
    "Embrapa Café" to "https://www.embrapa.br/cafe",
    "Fundação Procafé" to "https://fundacaoprocafe.com.br",
    "Indicador de preço – CEPEA/ESALQ" to "https://www.cepea.esalq.usp.br",
    "Safra de café – Conab" to "https://www.conab.gov.br",
)

@Composable
fun Atalho(titulo: String, sub: String, icone: ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icone, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(titulo, fontWeight = FontWeight.Bold)
                Text(sub, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun InicioTela(onIr: (Int) -> Unit, onApoio: (Int) -> Unit) {
    val ctx = LocalContext.current
    Tela {
        Titulo("Olá, produtor! ☕")
        Text("Simule sua lavoura, registre custos e colheitas, acompanhe a chuva, fale com agrônomos e encontre insumos, tudo em um só lugar.")
        BannerCafe()
        Atalho("Calculadora de lavoura", "Pés, adubo e investimento", Icons.Default.Build) { onIr(1) }
        Atalho("Minhas lavouras", "Histórico e custos, do plantio à colheita", Icons.Default.Create) { onIr(2) }
        Atalho("Chuva da minha lavoura", "Histórico de 12 meses e previsão", Icons.Default.DateRange) { onIr(3) }
        Atalho("Agrônomos e profissionais", "Orientação técnica por WhatsApp", Icons.Default.Person) { onApoio(0) }
        Atalho("Lojas de insumos", "Lojas cadastradas e agropecuárias perto de você", Icons.Default.ShoppingCart) { onApoio(1) }
        Atalho("Financiamento", "Contatos de gerentes de crédito rural", Icons.Default.Info) { onApoio(2) }
        Titulo("Biblioteca técnica")
        LINKS.forEach { (t, u) ->
            OutlinedButton(onClick = { abrir(ctx, u) }, modifier = Modifier.fillMaxWidth()) { Text(t) }
        }
    }
}

// ---------- Calculadora ----------

@Composable
fun CalculadoraTela(cidade: String, onSalva: (String) -> Unit) {
    val ctx = LocalContext.current
    var area by rememberSaveable { mutableStateOf("10000") }
    var morro by rememberSaveable { mutableStateOf(false) }
    var mecanica by rememberSaveable { mutableStateOf(false) }
    var varIdx by rememberSaveable { mutableIntStateOf(0) }
    var personalizado by rememberSaveable { mutableStateOf(false) }
    var linhaTxt by rememberSaveable { mutableStateOf("") }
    var plantaTxt by rememberSaveable { mutableStateOf("") }
    var muda by rememberSaveable { mutableStateOf("") }
    var adubo by rememberSaveable { mutableStateOf("") }
    var resultado by remember { mutableStateOf<Resultado?>(null) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val v = VARIEDADES[varIdx]

    Tela {
        Titulo("Calculadora de lavoura")
        Campo("Área disponível (m²) – 1 ha = 10.000 m²", area, { area = it })

        Text("Terreno", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !morro, onClick = { morro = false }, label = { Text("🌾 Plano") })
            FilterChip(selected = morro, onClick = { morro = true }, label = { Text("⛰️ Morro") })
        }

        Text("Colheita", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !mecanica, onClick = { mecanica = false }, label = { Text("🤲 Manual") })
            FilterChip(selected = mecanica, onClick = { mecanica = true }, label = { Text("🚜 Mecânica") })
        }

        Text("Variedade", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VARIEDADES.forEachIndexed { i, x ->
                FilterChip(selected = varIdx == i, onClick = { varIdx = i }, label = { Text(x.nome) })
            }
        }
        Text(
            "Sugerido: ${fmt(v.linha, 1)} m × ${fmt(v.planta, 1)} m",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = personalizado, onCheckedChange = { personalizado = it })
            Text("Espaçamento personalizado")
        }
        if (personalizado) {
            Campo("Entre linhas (m)", linhaTxt, { linhaTxt = it })
            Campo("Entre plantas (m)", plantaTxt, { plantaTxt = it })
        }

        Text("Custos (opcional)", fontWeight = FontWeight.Bold)
        Campo("Preço por muda (R$)", muda, { muda = it })
        Campo("Preço do adubo (R$/kg)", adubo, { adubo = it })

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val r = calcular(
                    areaM2 = area.toNum() ?: 0.0,
                    morro = morro,
                    mecanica = mecanica,
                    v = v,
                    linhaCustom = if (personalizado) linhaTxt.toNum() else null,
                    plantaCustom = if (personalizado) plantaTxt.toNum() else null,
                    precoMuda = muda.toNum(),
                    precoAdubo = adubo.toNum(),
                )
                erro = if (r == null) "Confira a área e o espaçamento informados." else null
                resultado = r
            },
        ) { Text("Calcular minha lavoura") }

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        resultado?.let { r ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resultado estimado", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Linha("Total de pés", "${fmt(r.pes.toDouble())} mudas")
                    Linha("Pés por 1.000 m²", fmt(r.pesPor1000m2))
                    Linha("Espaçamento", "${fmt(r.linha, 2)} × ${fmt(r.planta, 2)} m")
                    Linha("Aproveitamento da área", "${fmt(r.aproveitamento)}%")
                    Linha("Adubo anual", "${fmt(r.aduboKgAno)} kg")
                    Linha("Adubo por pé", "${fmt(r.aduboKgPorPe, 2)} kg")
                    r.custoMudas?.let { Linha("Mudas", brl(it)) }
                    r.custoAdubo?.let { Linha("Adubo (1º ano)", brl(it)) }
                    if (r.custoMudas != null || r.custoAdubo != null) {
                        HorizontalDivider()
                        Linha("Total", brl((r.custoMudas ?: 0.0) + (r.custoAdubo ?: 0.0)))
                    }
                }
            }
            OutlinedButton(
                onClick = { compartilhar(ctx, resumo(r, v.nome)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Compartilhar") }
            Button(
                onClick = { salvando = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Salvar como lavoura (iniciar histórico)") }
        }

        Text(
            "⚠️ Estimativas orientativas. Espaçamento, densidade e adubação variam conforme análise de solo, " +
                "altitude e manejo. Consulte sempre um Engenheiro Agrônomo habilitado.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    val rAtual = resultado
    if (salvando && rAtual != null) {
        SalvarLavouraDialog(
            nomeInicial = "${v.nome} ${Repo.lavouras.size + 1}",
            onFechar = { salvando = false },
            onSalvar = { nome ->
                val nova = Lavoura(
                    id = UUID.randomUUID().toString(),
                    nome = nome,
                    cidade = cidade,
                    criadaEm = LocalDate.now().toString(),
                    areaM2 = area.toNum() ?: 0.0,
                    variedade = v.nome,
                    pesPlanejados = rAtual.pes,
                    custoPlanejado = (rAtual.custoMudas ?: 0.0) + (rAtual.custoAdubo ?: 0.0),
                    lancamentos = emptyList(),
                )
                Repo.adicionarLavoura(nova)
                salvando = false
                onSalva(nova.id)
            },
        )
    }
}

// ---------- Clima ----------

private val MESES = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

private fun nomeMes(chave: String): String = MESES[chave.substring(5, 7).toInt() - 1]

@Composable
fun ClimaTela(cidade: String, onCidade: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var clima by remember { mutableStateOf<Clima?>(null) }

    Tela {
        Titulo("Chuva da minha lavoura")
        Campo("Cidade (ex.: Machado)", cidade, onCidade, numerico = false)
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !carregando && cidade.isNotBlank(),
            onClick = {
                scope.launch {
                    carregando = true
                    erro = null
                    try {
                        val c = ClimaApi.buscarCidade(cidade) ?: throw IllegalStateException("Cidade não encontrada.")
                        clima = ClimaApi.carregar(c)
                    } catch (e: Exception) {
                        clima = null
                        erro = if (e is IllegalStateException) e.message else "Sem conexão ou serviço indisponível."
                    } finally {
                        carregando = false
                    }
                }
            },
        ) { Text(if (carregando) "Carregando…" else "Ver chuvas") }

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        clima?.let { c ->
            val seco = c.mensalMm.minByOrNull { it.second }
            val umido = c.mensalMm.maxByOrNull { it.second }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(c.cidade, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Linha("Chuva nos últimos 12 meses", "${fmt(c.totalAnoMm)} mm")
                    seco?.let { Linha("Mês mais seco", "${nomeMes(it.first)} (${fmt(it.second)} mm)") }
                    umido?.let { Linha("Mês mais chuvoso", "${nomeMes(it.first)} (${fmt(it.second)} mm)") }
                    Linha("Próximos 16 dias (previsão)", "${fmt(c.previsao16DiasMm)} mm")
                }
            }
            val max = c.mensalMm.maxOf { it.second }.coerceAtLeast(1.0)
            Text("Chuva por mês (mm)", fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                c.mensalMm.forEach { (mes, mm) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(16.dp)
                                .height((100.0 * mm / max).dp.coerceAtLeast(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(nomeMes(mes), fontSize = 9.sp)
                    }
                }
            }
            Text("Fonte: Open-Meteo (reanálise ERA5 e modelo de previsão).", style = MaterialTheme.typography.bodySmall)
        }
    }
}
