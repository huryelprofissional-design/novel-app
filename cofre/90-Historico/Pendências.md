---
tags: [pendencias]
atualizado: 2026-11-14
---
# Pendências

## Esperando resposta do autor
- [ ] **Vozes no PC lentas**: qual voz usa, se o problema é demorar a começar ou pausas, quantos núcleos o PC tem. Ver [[Vozes]].
- [ ] **Vozes no celular**: retorno de uso real (pausas entre parágrafos, erro ao baixar).
- [ ] **Regras etapa 2**: contadores em lote + ID de semana igual em todos os aparelhos — quer?
- [ ] **Moedas** continuam grátis para sempre?
- [ ] Restringiu a chave `apiKey` no Google Cloud? Ver [[Segurança]].
- [ ] Prints do **V2 no celular**, do **leitor novo (#000)** e dos **35 efeitos** no aparelho (só o PC foi visto em print; o celular só compilou).

## Trabalho começado
- [ ] **Efeitos 2.0 — fazer os 55 escolhidos** (celular e PC, mesmos nomes). Sintaxe nova: `[$nome campo | campo]` e `{$nome: texto}`; os antigos ficam intactos. Visual de referência: prévias na página https://claude.ai/artifact/9dTn3j9ZddXKzVNv9hvaEk (o HTML/CSS de cada cartão serve de base para `web/fx2.js` + CSS `n2-`). Plano: PC primeiro (parser `[$` em `leitor.js` ao lado de `blocoFx`, Ouvir/contagem em `extras.js semEfeito`), teste Playwright, depois Kotlin (`ui/EfeitosNovos2.kt`), painel, Maestro e Catálogo. Melhorias M1 (começar ao entrar na tela + tocar repete) e M3 (vibração no celular). Nada de código feito ainda.
- [ ] Auditoria fase **mestre** (código limpo): `ui/TelaEditor.kt` feito em 2026-11-14 (falta o autor conferir no celular). Próximo: `TelaLeitor.kt`, depois `TelaObra.kt`.
- [ ] Telas internas ainda no visual antigo (só herdaram cores): obra, ajustes, painel, estatísticas; no perfil do celular, Ranking/Moedas/Conquistas/Citações.
- [ ] Código antigo sem rota (`TelaInicioNova`, `TelaEscreverAba`, `TelaPerfilAba`, `TelaDescobrirCatalogo` como aba): avaliar remover.

## Ideias guardadas
- Vozes PC: modelos Piper *x_low*, onnxruntime-web + WebGPU, voz do sistema enquanto a neural aquece.
- 33 efeitos recusados (ver [[Decisões]]) — não oferecer de novo sem o autor pedir.
- XP/missões do PC sincronizarem com o celular.
