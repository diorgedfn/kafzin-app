package com.kafzin.app

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Banner do preço do café arábica (Indicador CEPEA/ESALQ), via widget oficial do Cepea. */
@Composable
fun BannerCafe() {
    val ctx = LocalContext.current
    val widget = Config.CEPEA_WIDGET_HTML
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("☕ Café arábica hoje", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (widget.isBlank()) {
                Text(
                    "Indicador CEPEA/ESALQ, saca de 60 kg. Toque para ver a cotação de hoje.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = { abrir(ctx, Config.CEPEA_CAFE_URL) }) { Text("Ver cotação de hoje") }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    factory = { c ->
                        WebView(c).apply {
                            settings.javaScriptEnabled = true
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            loadDataWithBaseURL(
                                "https://cepea.org.br",
                                "<html><body style='margin:0'>" + widget + "</body></html>",
                                "text/html",
                                "UTF-8",
                                null,
                            )
                        }
                    },
                )
            }
            Text(
                "Fonte: CEPEA/ESALQ – café arábica tipo 6, bebida dura para melhor, posto São Paulo. Valores de referência, atualizados em dias úteis.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
