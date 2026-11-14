---
tags: [pc, referencia]
gerado: 2026-11-14
---
# PC — arquivos

Site em JavaScript puro (módulos ES), sem build. Pasta `web/`.

| Arquivo | Linhas | O que é |
|---|---|---|
| `aovivo.js` | 131 | Escrever junto ao vivo (sala de colaboração em tempo real). |
| `app.js` | 193 | Entrada do site: login, rotas que sobraram (biblioteca, ajustes, importar), redesenhar, importarTXT. Importa `v2.js` primeiro. |
| `biblia.js` | 284 | Mapa de relações em SVG: força de mola (quem divide cena fica perto), pan/zoom e a barra volta no tempo. |
| `catalogo.js` | 237 | Catálogo: descobrir, busca, gêneros, página pública da obra, autor, avisos, colaboração (lado do membro), gerador de nomes e painel do dono. |
| `config.js` | 10 | Configuração do app web no Firebase (Configurações do projeto › Seus apps › Novely PC). |
| `efeitos.js` | 157 | Gamificação (XP, níveis, missões, baú, celebrar) e clima no leitor (partículas). |
| `estilo.css` | 1757 | Todo o visual. No fim: bloco "Novely V2 · editorial" e "efeitos · pacote 2". |
| `extras.js` | 212 | Tira os símbolos de efeito de um trecho (o texto fica). Mesma regra do app. |
| `fx.js` | 225 | Efeitos do Mega Pack: `B` (blocos `[#…]`), `inlineFx` (`{efeito: …}`), `ligarFx` (animações por JS) e `MEGA` (catálogo). |
| `ia.js` | 199 | Estúdio de IA (PC): monta prompts com o contexto da obra e traz a resposta de volta. |
| `index.html` | 18 | Casca HTML + fontes do Google. |
| `leitor.js` | 896 | Leitor: HUD, busca, rolagem automática, Ouvir (voz do sistema + Vozes do Novely), posição. |
| `manifest.webmanifest` | 14 | PWA (instalar no PC). |
| `nucleo.js` | 658 | Núcleo: Firebase, estado `E`, `local` (localStorage), helpers `$`, `esc`, `rota`, `casca()` (barra lateral), catálogo, publicação, perfis. |
| `obra.js` | 474 | Página da obra, capítulos, editor do PC, publicar (genre/tags), fichas. |
| `sw.js` | 25 | Service worker (cache `novely-vNN`, sobe o número a cada mudança). Não guarda `vozes/`; guarda o motor `voz/` à parte. |
| `v2.js` | 247 | Telas V2: Ler (`/`), Descobrir (`/descobrir/:modo`), Escrever (`/escrever`), Eu (`/perfil`) e editar perfil. |
| `vozneural.js` | 148 | Baixa o pacote da voz e guarda no navegador. aoProgresso(0..1). |
| `voz/fonemas-worker.js` | 11 | Pronúncia (espeak-ng em WebAssembly): texto em português → fonemas que as vozes do Novely leem. |
| `voz/sherpa-onnx-tts.js` | 1315 | Biblioteca sherpa-onnx (copiada pelo CI). |
| `voz/sherpa-onnx-wasm-main-tts.js` | 1 | Motor WASM (o CI baixa e corrige o `loadPackage`). |
| `voz/tts-worker.js` | 38 | Monta a configuração padrão da biblioteca e troca só o modelo (kokoro ou vits/piper). |
