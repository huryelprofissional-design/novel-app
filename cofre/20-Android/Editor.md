---
tags: [android, pc, editor]
---
# Editor

- Capítulo dividido em **pedaços de ~3.000 caracteres**, cada um num campo dentro de lista preguiçosa. Juntar os pedaços devolve exatamente o texto (testado).
- **Salva a cada 5 s** + ao sair/minimizar. **Rascunho de emergência** se o app morrer.
- **Versões**: cópia antes de sobrescrever (no máx. a cada 10 min, até 30 por capítulo) e versões nomeadas. Tela de **conflito** quando o mesmo capítulo mudou em dois aparelhos (`conflito/{id}/{q}`).
- Contagem de palavras ao vivo; meta diária e `escritaDias` alimentam a Mesa do escritor.
- **Painel de efeitos** (`ui/PainelEfeito.kt`, PC: `web/extras.js`): abas *No trecho* (todos os inline) e *Parágrafo* (todos os blocos do Mega Pack agrupados por gênero), cor, intensidade `+` forte / `-` suave, **Prévia** ao vivo, **Remover efeito**. Efeitos com vários campos entram com o modelo e o trecho selecionado no lugar do campo de texto (`conteudoBloco`).
- Os símbolos ficam no texto (decisão aprovada) e o editor mostra prévia.
- Atalhos de teclado físico.
- Lixeira para fichas e obras.
- Próximo passo da auditoria "mestre": revisar `TelaEditor.kt` (ver [[Pendências]]).
