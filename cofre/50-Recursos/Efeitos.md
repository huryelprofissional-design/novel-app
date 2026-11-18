---
tags: [recurso, efeitos]
atualizado: 2026-11-18
---
# Efeitos de escrita (versão 2026-11-18)

Os efeitos antigos foram removidos em 2026-11-18 (ver [[Decisões]]). No mesmo dia o autor escolheu **165 efeitos novos** na página https://claude.ai/artifact/7HFqkVKJzQWRZLRz3urQaq (350 opções em 26 temas).

**Onde funcionam:** só no celular (o PC está pausado). Leitor e prévia do editor.

## Como se escreve
- `{nome: texto}` — dentro da frase vira estilo (cor, itálico, brilho); sozinho na linha (ou `— {nome: texto}`) vira efeito animado.
- `[nome | campo | campo]` — linha sozinha: janelas, fichas, barras, conversas, carimbos.
- `::nome resto` — linha sozinha: som, vibração, partículas, pausa.
- O nome aceita acento ou não. A tabela completa com todos os nomes está no [[Maestro - prompt de escrita]].

## Código
- `data/Fx.kt`: catálogo (`Fx.CATALOGO`: nome → tipo + modo + rótulo), leitura da linha (`Fx.lerLinha`), texto para o Ouvir e buscas (`Fx.plano`), texto sem efeitos para resumos (`Fx.textoSimples`).
- `ui/FxTela.kt`: `FxBloco` desenha cada tipo (37 tipos: letras, frase, janela, ficha, contador, barra, chat, carimbo, partículas…). Toque no efeito repete. Com "reduzir animações" aparece já pronto.
- `ui/TelaLeitor.kt`: `Bloco.Efeito`; `estilizar` faz o `{nome: texto}` no meio da frase; no modo livro cada efeito ocupa 176 dp.
- `data/Limpeza.kt` não apaga nomes que estão no catálogo (as limpezas dos símbolos antigos continuam).
- Teste: `FxTest`.

## Para criar um efeito novo
1. Escolher um tipo existente (ou criar um em `FxTipo` + uma função em `FxTela.kt`).
2. Pôr o nome em `Fx.CATALOGO`.
3. Pôr na tabela do Maestro (`Maestro-prompt.txt` e a nota).

## Limites conhecidos
- Som: só os 5 sons do Ambiente (chuva, lareira, vento, noite, café). `::sino` é só visual e vibração.
- "Voz por personagem" (T6) já existia no Ouvir; não é um efeito novo.
