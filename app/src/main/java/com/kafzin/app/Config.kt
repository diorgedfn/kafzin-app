package com.kafzin.app

/**
 * CONFIGURAÇÕES que você edita direto no GitHub (sem mexer no resto do código).
 */
object Config {
    /**
     * Código do widget oficial do Cepea para o Café Arábica.
     * Gere em cepea.org.br ("Cotações do Cepea no seu site" → Café Arábica), copie o código
     * e cole ENTRE as três aspas abaixo. Use endereços https://. Vazio = banner com botão.
     */
    val CEPEA_WIDGET_HTML = """"""

    /** Página do indicador no site do Cepea (usada enquanto o widget não estiver configurado). */
    val CEPEA_CAFE_URL = "https://cepea.org.br/br/indicador/cafe.aspx"

    /** Link do Google Forms de cadastro (profissionais, lojas, bancos). Vazio = cadastro por WhatsApp. */
    val CADASTRO_FORM_URL = ""

    /** Link CSV publicado da aba "publicados" da planilha de parceiros aprovados. */
    val PARCEIROS_CSV_URL = ""

    /** Seu WhatsApp (DDI+DDD+número, só dígitos) para receber pedidos de cadastro. Ex.: 5535999999999 */
    val CONTATO_WHATSAPP = ""
}
