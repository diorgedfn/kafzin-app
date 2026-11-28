package com.kafzin.app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Categoria(val rotulo: String) {
    PROFISSIONAL("Agrônomos e profissionais"),
    LOJA("Lojas de insumos"),
    BANCO("Financiamento"),
}

data class Parceiro(
    val categoria: Categoria,
    val nome: String,
    val empresa: String,
    val descricao: String,
    val cidade: String,
    val whatsapp: String,
    val email: String,
)

private val EXEMPLOS = listOf(
    Parceiro(Categoria.PROFISSIONAL, "Agrônomo(a) Exemplo", "", "Nutrição e adubação", "Sul de Minas", "", ""),
    Parceiro(Categoria.LOJA, "Agropecuária Exemplo", "", "Insumos, adubos e defensivos", "Sul de Minas", "", ""),
    Parceiro(Categoria.BANCO, "Gerente Exemplo", "Banco Exemplo", "Crédito rural e custeio", "Sul de Minas", "", ""),
)

/**
 * Diretório de parceiros: lê uma planilha publicada em CSV (colunas: categoria, nome, empresa,
 * descricao, cidade, whatsapp, email; 1ª linha = cabeçalho) e guarda uma cópia no celular.
 */
object Diretorio {
    val itens = mutableStateListOf<Parceiro>()
    var carregando by mutableStateOf(false)
    var usandoExemplo by mutableStateOf(false)
    var erro by mutableStateOf<String?>(null)
    private lateinit var cache: File

    fun init(ctx: Context) {
        if (::cache.isInitialized) return
        cache = File(ctx.filesDir, "parceiros.csv")
        try {
            if (cache.exists()) {
                itens.addAll(paraParceiros(cache.readText()))
                return
            }
        } catch (e: Exception) {
            // cópia ilegível: ignora
        }
        if (Config.PARCEIROS_CSV_URL.isBlank()) {
            itens.addAll(EXEMPLOS)
            usandoExemplo = true
        }
    }

    suspend fun atualizar() {
        if (Config.PARCEIROS_CSV_URL.isBlank()) return
        carregando = true
        erro = null
        try {
            val txt = withContext(Dispatchers.IO) { baixar(Config.PARCEIROS_CSV_URL) }
            val lista = paraParceiros(txt)
            itens.clear()
            itens.addAll(lista)
            usandoExemplo = false
            try { cache.writeText(txt) } catch (e: Exception) { }
        } catch (e: Exception) {
            erro = "Não foi possível atualizar a lista agora. Mostrando a última versão salva."
        } finally {
            carregando = false
        }
    }

    private fun baixar(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.instanceFollowRedirects = true
        try {
            return c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }

    private fun categoriaDe(txt: String): Categoria {
        val t = txt.lowercase()
        return when {
            t.contains("banc") || t.contains("financ") || t.contains("crédito") || t.contains("credito") -> Categoria.BANCO
            t.contains("loja") || t.contains("insumo") || t.contains("agropec") -> Categoria.LOJA
            else -> Categoria.PROFISSIONAL
        }
    }

    private fun paraParceiros(csv: String): List<Parceiro> =
        lerCsv(csv).drop(1).mapNotNull { r ->
            fun c(i: Int) = r.getOrNull(i)?.trim() ?: ""
            if (c(1).isBlank()) null
            else Parceiro(categoriaDe(c(0)), c(1), c(2), c(3), c(4), c(5), c(6))
        }
}

/** Leitor simples de CSV (aspas e vírgulas dentro de campos). */
fun lerCsv(txt: String): List<List<String>> {
    val linhas = mutableListOf<List<String>>()
    var linha = mutableListOf<String>()
    var campo = StringBuilder()
    var aspas = false
    var i = 0
    while (i < txt.length) {
        val ch = txt[i]
        if (aspas) {
            if (ch == '"') {
                if (i + 1 < txt.length && txt[i + 1] == '"') {
                    campo.append('"')
                    i++
                } else {
                    aspas = false
                }
            } else {
                campo.append(ch)
            }
        } else if (ch == '"') {
            aspas = true
        } else if (ch == ',') {
            linha.add(campo.toString())
            campo = StringBuilder()
        } else if (ch == '\n') {
            linha.add(campo.toString())
            campo = StringBuilder()
            linhas.add(linha)
            linha = mutableListOf()
        } else if (ch != '\r') {
            campo.append(ch)
        }
        i++
    }
    if (campo.isNotEmpty() || linha.isNotEmpty()) {
        linha.add(campo.toString())
        linhas.add(linha)
    }
    return linhas
}

fun cadastrar(ctx: Context, cat: Categoria) {
    when {
        Config.CADASTRO_FORM_URL.isNotBlank() -> abrir(ctx, Config.CADASTRO_FORM_URL)
        Config.CONTATO_WHATSAPP.isNotBlank() -> abrir(
            ctx,
            "https://wa.me/" + Config.CONTATO_WHATSAPP + "?text=" +
                Uri.encode("Olá! Quero me cadastrar no app Kafzin: " + cat.rotulo + "."),
        )
        else -> Toast.makeText(ctx, "Cadastro disponível em breve", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ApoioTela(sub: Int, onSub: (Int) -> Unit, cidade: String, onCidade: (String) -> Unit) {
    val categorias = Categoria.values()
    val cat = categorias[sub.coerceIn(0, categorias.size - 1)]
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categorias.forEachIndexed { i, c ->
                FilterChip(selected = sub == i, onClick = { onSub(i) }, label = { Text(c.rotulo) })
            }
        }
        Box(Modifier.weight(1f)) { ParceirosTela(cat, cidade, onCidade) }
    }
}

@Composable
fun ParceirosTela(cat: Categoria, cidade: String, onCidade: (String) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { Diretorio.atualizar() }
    val filtro = cidade.trim()
    val lista = Diretorio.itens.filter {
        it.categoria == cat && (filtro.isBlank() || it.cidade.contains(filtro, ignoreCase = true))
    }
    val apresentacao = when (cat) {
        Categoria.PROFISSIONAL -> "Tire dúvidas com agrônomos, técnicos e outros profissionais do café."
        Categoria.LOJA -> "Encontre lojas de insumos cadastradas e busque agropecuárias perto de você."
        Categoria.BANCO -> "Gerentes e instituições que atendem o crédito rural, para você conversar sobre financiamento."
    }
    val papel = when (cat) {
        Categoria.PROFISSIONAL -> "profissional"
        Categoria.LOJA -> "loja"
        Categoria.BANCO -> "gerente ou instituição financeira"
    }

    Tela {
        Titulo(cat.rotulo)
        Text(apresentacao)
        Campo("Filtrar por cidade", cidade, onCidade, numerico = false)

        if (Diretorio.usandoExemplo) {
            Text(
                "⚠️ Lista de exemplo, ainda sem parceiros reais cadastrados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Diretorio.erro?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (Diretorio.carregando) Text("Atualizando lista…", style = MaterialTheme.typography.bodySmall)

        if (lista.isEmpty() && !Diretorio.carregando) {
            Text("Nenhum cadastro encontrado${if (filtro.isNotBlank()) " para “$filtro”" else ""}. Seja o primeiro a se cadastrar!")
        }
        lista.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.nome, fontWeight = FontWeight.Bold)
                    if (p.empresa.isNotBlank()) Text(p.empresa)
                    if (p.descricao.isNotBlank()) Text(p.descricao, style = MaterialTheme.typography.bodySmall)
                    if (p.cidade.isNotBlank()) Text(p.cidade, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (p.whatsapp.isNotBlank()) {
                            Button(onClick = {
                                val numero = p.whatsapp.filter { it.isDigit() }
                                val msg = Uri.encode("Olá! Vim pelo app Kafzin.")
                                abrir(ctx, "https://wa.me/" + numero + "?text=" + msg)
                            }) { Text("WhatsApp") }
                        }
                        if (p.email.isNotBlank()) {
                            OutlinedButton(onClick = { abrir(ctx, "mailto:" + p.email) }) { Text("E-mail") }
                        }
                    }
                }
            }
        }

        if (cat == Categoria.LOJA) {
            OutlinedButton(
                onClick = { abrir(ctx, "geo:0,0?q=" + Uri.encode("agropecuária insumos agrícolas $filtro".trim())) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Buscar agropecuárias no mapa") }
        }

        Button(onClick = { cadastrar(ctx, cat) }, modifier = Modifier.fillMaxWidth()) {
            Text("Quero me cadastrar como $papel")
        }

        if (cat == Categoria.BANCO) {
            Text(
                "Linhas, taxas e prazos de crédito mudam com frequência. Confirme sempre as condições " +
                    "direto com a instituição. O Kafzin apenas apresenta contatos e não intermedeia crédito.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
