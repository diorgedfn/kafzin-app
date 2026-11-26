# Kafzin Café – app Android (protótipo)

App para produtores de café: calculadora de lavoura, chuva (Open-Meteo), contato com agrônomos e busca de lojas de insumos.

Stack: Kotlin + Jetpack Compose (Material 3). Sem chave de API.

## Rodar no GitHub (gerar o APK)
1. Crie um repositório e envie estes arquivos para a branch `main`.
2. Aba **Actions** → workflow **Build APK** (roda sozinho a cada push, ou clique em *Run workflow*).
3. Ao terminar, baixe o artefato **kafzin-debug-apk**, extraia e instale `app-debug.apk` no celular
   (permitir instalação de fontes desconhecidas).

## Rodar no Android Studio
Abra a pasta, aguarde o sync e execute no emulador ou celular.

## Para completar
- `Calculator.kt` → objeto `Premissas`: alinhar com as regras reais do site.
- `MainActivity.kt` → lista `AGRONOMOS` (exemplo): cadastrar parceiros reais.
- Conferir os links da biblioteca técnica (`LINKS`).
