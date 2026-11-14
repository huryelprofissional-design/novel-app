---
tags: [recursos, efeitos, receita]
---
# Como criar um efeito novo

Todo efeito existe **nos dois lados** (celular e PC) com o **mesmo nome e os mesmos campos**. Checklist:

## Efeitos 2.0 (`$`)
Efeito novo vai na família 2.0, não no Mega Pack antigo:
- PC: `web/fx2.js` (bloco em `B`, apelido em `APELIDO`, catálogo `MEGA2`; trecho em `INLINE2` + `inlineFx2`) e CSS em `web/fx2.css` (classes `v2-*`, keyframes `v2*`). `fx.js` repassa `[$`/`{$` para cá. Números usam `data-conta="de|até|ms|formato|atraso"`, partículas `data-part="tipo|quantidade"`.
- Celular: `ui/EfeitosV2.kt` (`BLOCOS_V2`, `APELIDOS_V2`, ramo em `ConteudoV2`, catálogo `MEGA_PACK_V2`; trecho em `INLINE_V2` + `estiloInlineV2`/`animarInlineV2`). Nomes guardados com `$` na frente (`"$sistema"`) para não colidir com os antigos.
- Tocar repete a animação (`BlocoV2`/`ligarFx2`); os de toque ficam em `TOQUE_V2`/`TOQUE`. Vibração em `VIBRA_V2`/`VIBRA`.
- Teste: `PainelEfeitoTest.efeitos2LidosSemMexerNosAntigos`.

## 1. Celular (Kotlin)
- **Bloco `[#nome …]`**: em `ui/EfeitosNovos.kt` acrescente o nome em `BLOCOS_NOVOS` (e apelidos com acento em `APELIDOS_NOVOS`) e um ramo `"nome" -> { … }` em `BlocoNovo(nome, corNome, c, estilo)`. `c` = lista de campos; `corFx(corNome, padrão)` dá a cor. Use `ciclo(ms)` (vai e volta) e `entrada(ms)` (aparece uma vez): os dois já respeitam "Reduzir animações".
  - Os blocos antigos ficam em `ui/EfeitosBloco.kt` (`BLOCOS_FX`, `BlocoFx`, `fx56`…); `BlocoFx` cai em `BlocoNovo` no `else`.
- **Em linha `{nome: …}`**: em `EfeitosNovos.kt` acrescente em `INLINE_NOVOS`, estilo em `estiloInlineNovo` e, se mexer, em `INLINE_ANIMADOS` + ramo em `animarInlineNovos` (quadro de 110 ms; **não mude o tamanho do texto** — a contagem troca dígitos com zeros à esquerda). Texto transformado na leitura (como `corrompido`, `runa`) vai no `mostrado` de `TelaLeitor.kt`.
- **Catálogo/painel**: `MEGA_PACK` em `EfeitosBloco.kt` (`Triple(nome, exemplo, categoria)`) — a aba Parágrafo do painel lê daqui. Em linha: `EFEITOS_TRECHO` em `ui/PainelEfeito.kt`. Se o efeito tem um campo só, ponha em `FX_SIMPLES`; se pisca, em `PISCAM`.
- Fontes novas: `app/src/main/res/font/` + `FontFamily` em `EfeitosNovos.kt`.

## 2. PC (JavaScript)
- `web/fx.js`: bloco em `B` (`nome: (c) => html`), apelidos em `AP`; em linha no `INLINE` + `case` em `inlineUm`; comportamento por JS (toque, contagem) em `ligarFx`; catálogo em `MEGA`.
- CSS: fim de `web/estilo.css`, classes com prefixo `n-` dentro de `.fx`. Respeitar `prefers-reduced-motion` (já há regra geral para `.fx *`).
- Fonte nova: link do Google Fonts em `web/index.html`.
- Subir o cache em `web/sw.js` (`novely-vNN`).

## 3. Testes e entrega
- PC: página de teste que importa `fx.js`, desenha `MEGA.slice(-N)` com `blocoFx`/`inlineFx` + `ligarFx` e tira print com Playwright ([[Como testar]]). Conferir que nenhum devolve vazio.
- Celular: CI compila e roda os testes (`PainelEfeitoTest` confere que todos os de parágrafo estão no painel).
- Atualizar [[Maestro - prompt de escrita]] e regenerar [[Catálogo de efeitos]].

## Para propor efeitos ao autor
Ele gosta de escolher numa página HTML: cartões com prévia animada, sintaxe, descrição, abas por gênero e botão "Copiar escolhidos". Ele responde "Quero estes efeitos: 7 Brilho…, 11 …".
