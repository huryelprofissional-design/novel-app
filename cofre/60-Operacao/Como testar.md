---
tags: [operacao, testes]
---
# Como testar

Regra do autor: **nunca dizer que funciona sem testar** — e dizer o que foi e o que não foi testado.

## Android
- Testes de unidade em `app/src/test` (rodam no CI): desempenho com 50 mil e 100 mil palavras (contar, dividir em blocos, buscar — imprime tempos no log), `MacacoTest` (3.000 textos caóticos + 10 MB), `PainelEfeitoTest`, editor em pedaços etc.
- Visual: não há emulador local. Opções: workflow `emulador.yml` (prints num emulador do Actions) ou pedir print ao autor.

## PC (Playwright no ambiente do Claude)
Chromium + Playwright já vêm instalados (`require('playwright')`, não reinstalar). Servidor: `cd web && python3 -m http.server 8766`.
- **Fontes**: o Google Fonts não carrega pelo proxy. Interceptar `fonts.googleapis.com` com `page.route` e servir `@font-face` apontando para os `.ttf` locais (`app/src/main/res/font/`).
- **Telas que precisam de login/Firestore**: interceptar `nucleo.js` e `efeitos.js` com módulos falsos (stub) que exportam os mesmos nomes com dados de exemplo, e montar uma página que importa `v2.js` e chama as rotas. Foi assim que as telas V2 foram conferidas em 1440 px e 390 px.
- **Efeitos**: página que importa `fx.js`, desenha `MEGA` com `blocoFx`/`inlineFx`, chama `ligarFx` e tira print de página inteira.
- **Vozes neurais**: página que importa `vozneural.js`, chama `baixar` e `preparar` e mede o tempo; troca `AudioBufferSourceNode.prototype.start` para registrar quando o áudio começa.
- Print para o autor: `deviceScaleFactor: 2`. Print só para conferir: 1.

## Regras do Firestore
Só no CI (`regras.yml`). 11 testes.
