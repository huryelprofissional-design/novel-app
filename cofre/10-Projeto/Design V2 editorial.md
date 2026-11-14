---
tags: [design]
atualizado: 2026-11-08
---
# Design V2 — editorial

Conceito enviado pelo autor em 2026-11-07 (PDFs/PNGs "Novely-V2-telas" e "Novely-PC-telas"). Aplicado no celular e no PC.

## Tokens
| Nome | Cor | Uso |
|---|---|---|
| fundo | `#12100E` | papel escuro (telas principais) |
| superfície | `#1A1714` | caixas |
| linha | `#2E2924` | fios de 1 px |
| creme | `#EDE6DA` | texto e botão principal |
| suave | `#968D81` | texto secundário, rótulos |
| laranja | `#E5553B` | destaque (ponto da aba, faixa atrás da capa, "+ NOVA OBRA") |
| verde | `#8DBB83` | "salvo na nuvem" |
| **leitor** | `#000000` | fundo do leitor e do editor (OLED, economia de bateria) |

## Tipografia
- **Instrument Serif** (títulos grandes, itálico para subtítulos). Só tem um peso: os pesos fortes apontam para o mesmo arquivo (sem negrito falso).
- **IBM Plex Mono** (rótulos MAIÚSCULOS espaçados: `CONTINUAR · CAPÍTULO 05`).
- **Inter** (interface e botões). **Newsreader** (texto de leitura).
- Efeitos usam também **Caveat** (letra à mão) e **Special Elite** (máquina de escrever).
- Android: `app/src/main/res/font/` (instrument_serif*.ttf, plex_mono*.ttf, caveat.ttf, special_elite.ttf). PC: Google Fonts no `index.html`.

## Peças
- **BotaoCreme**: retângulo creme, texto escuro, seta → à direita. **BotaoLinha**: contorno creme.
- **CapaFaixa**: capa com uma faixa laranja deslocada atrás (direita e embaixo).
- **Mono**: rótulo em Plex Mono, maiúsculo, `letterSpacing 2sp`.
- Barra de baixo (celular): 4 abas só texto em mono, ponto laranja em cima da ativa. PC: barra lateral com o mesmo esquema e o nome do autor + "Capítulo XVI" (nível em romano) no rodapé.
- Sem vidro/blur: superfícies chapadas e fios finos. Botões e chips retos (raio ~0–4 px).

## Telas
- **Ler**: capa grande da obra em leitura + "Continuar · capítulo 05", título gigante, "falta N minutos", barra de progresso, Estante (Lendo/Quero/Lidos + Importar). PC: soma "Saindo agora".
- **Descobrir**: "Primeiras linhas" — feed vertical (celular: deslizar; PC: setas ↑ ↓) com a 1ª frase do capítulo 1, seguir autor, marcar obra. Abas Em alta / Novas / Completas (+ Ranking da semana no PC) e Gêneros.
- **Escrever (Mesa do escritor)**: palavras de hoje gigantes, círculo da meta (34×), calendário do mês (escreveu = creme, hoje = laranja), obra atual com "Abrir rascunho", sumário de ferramentas com pontilhado.
- **Eu (Página do autor)**: banner, foto com anel laranja, nome gigante, bio entre fios, números (seguidores, palavras, lendo), obras, "Sua jornada" (nível como *Capítulo XVI — Lenda do Novely*, trilha de níveis), Missões de hoje, "Seu mês no Novely".
- **Leitor**: barras de cima e de baixo **pretas sólidas** de ponta a ponta; título em serifa, `CAP. 6 DE 6 · FALTAM ~27 MIN` em mono, linha de progresso na cor da obra; botões Ouvir/Buscar/Capítulos/Rolar/Leitura com rótulo mono.

## Onde está no código
- Android: `ui/Editorial.kt` (objeto `Ed`, `Mono`, `Serif`, `BotaoCreme`, `BotaoLinha`, `CapaFaixa`, `BarraEditorial`, `TelaLer`, `TelaDescobrirEd`, `TelaMesa`, `TelaEu`), `ui/theme/Theme.kt` (paleta "Editorial", fontes `InstrumentSerif`, `PlexMono`).
- PC: `web/v2.js` (rotas `/`, `/descobrir`, `/escrever`, `/perfil`), bloco `/* Novely V2 · editorial */` no fim de `web/estilo.css`, `casca()` em `web/nucleo.js`.

## Ainda com visual antigo
Telas internas (obra, editor, ajustes, painel…) só herdaram as cores. No perfil do celular, Ranking/Moedas/Conquistas/Citações ficaram no fim com o visual antigo.
