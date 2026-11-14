---
tags: [android]
---
# Android — visão geral

- Kotlin + Jetpack Compose (Material 3). `applicationId com.huryel.novely`, minSdk 26, targetSdk 35.
- Versão: `versionName = "2.3 · build N"`, `versionCode = N` (número do run do CI).
- Pacote: `app/src/main/java/com/huryel/novely/` → `data/` (lógica, banco, nuvem) e `ui/` (telas). Lista completa: [[Android - Arquivos]].
- `NovelyApp.instancia` dá acesso a `repo` (Repositorio), `prefs`, `pub` (Publicacao), `sinc` (Nuvem), `atualizador`.

## Armazenamento
- **Room v12** (`data/Banco.kt`, `exportSchema = true`): `obras`, `volumes`, `capitulos`, `fichas`, `ficha_trechos`, `marcadores`, `eventos`, `lixeira`.
- **Texto dos capítulos em arquivos** `files/textos/<id>.md` (`data/Arquivos.kt`), nunca no banco. Gravação atômica: `.tmp` → fsync → rename, `.bak` da versão anterior.
- Preferências em `prefs` (`Repositorio.kt`): `autorNome`, `bio`, `fotoPerfil`, `bannerPerfil`, `xp`, `sequencia`, `palavrasHoje`, `metaDiaria`, `missoesDia`, `bauDia`, `escritaDias()` (mapa `yyyyMMdd → palavras`), `estatisticas()` (tempo de leitura) etc.
- Ordem dos capítulos com número fracionário (`ordem: Double`): mover não renumera tudo.

## Navegação (`ui/AppNavegacao.kt`)
Abas da barra: `inicio` (Ler) · `descobrir` · `escrever` · `perfil` (Eu). `irAba(nav, rota)` troca de aba.
Rotas: `biblioteca`, `catalogo`, `buscar`, `ajustes`, `ajustes/{s}`, `avisos`, `dono`, `generos`, `genero/{g}`, `colaboracao/{id}`, `colaborar`, `colab/{p}`, `stories/{u}`, `retro`, `feed`, `vivo/{id}`, `aovivo/{p}/{c}`, `revisao/{p}/{c}`, `autor/{u}`, `pub/{p}`, `publicar/{id}`, `lixeira/{id}`, `obra/{id}`, `marcadores/{id}`, `estatisticas`, `painel`, `biblia/{id}`, `leitores/{id}`, `ia/{id}?cap={cap}`, `editor/{id}`, `fichas/{id}`, `lixeira`, `nomes`, `tempo/{id}`, `reordenar/{id}`, `historico/{id}`, `conflito/{id}/{q}`, `importar`, `importar/{id}`, `backup`, `atualizar`.

## Telas principais
- Abas V2: `ui/Editorial.kt` ([[Design V2 editorial]]). As antigas (`TelaInicioNova`, `TelaEscreverAba`, `TelaPerfilAba` em `Abas.kt`/`InicioNovo.kt`) continuam no código mas sem rota de aba.
- [[Leitor]]: `ui/TelaLeitor.kt` (+ `ExtrasLeitor.kt`, `Efeitos*.kt`).
- [[Editor]]: `ui/TelaEditor.kt` + `ui/PainelEfeito.kt`.
- Obra: `ui/TelaObra.kt`. Fichas: `ui/TelaFichas.kt`. Bíblia/relações: `TelaBiblia.kt`, `MapaRelacoes.kt`.

## Flavors
`github` (com atualizador que baixa o APK do Releases e `REQUEST_INSTALL_PACKAGES`) e `play` (sem isso; manifesto em `app/src/play/AndroidManifest.xml`). `BuildConfig.ATUALIZADOR`.

## Bibliotecas nativas
`app/libs/sherpa-onnx.aar` (vozes neurais) é baixado pelo CI e conferido por sha256; está no `.gitignore`. Ver [[Vozes]].
