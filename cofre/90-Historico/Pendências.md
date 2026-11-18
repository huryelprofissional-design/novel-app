---
tags: [pendencias]
atualizado: 2026-11-14
---
# Pendências

## Esperando resposta do autor
- [ ] **Telas novas no aparelho** (2026-11-14): as 13 escolhidas foram feitas no PC (visual conferido no Chromium com HTML de exemplo; sem login) e no celular (só compilou no CI). Pedir print, principalmente leitor (abertura, barra com mapa, fim, Ouvir), editor com ✎ e modo foco, e busca.
- [ ] **Vozes no PC lentas**: qual voz usa, se o problema é demorar a começar ou pausas, quantos núcleos o PC tem. Ver [[Vozes]].
- [ ] **Vozes no celular**: retorno de uso real (pausas entre parágrafos, erro ao baixar).
- [ ] **Regras etapa 2**: contadores em lote + ID de semana igual em todos os aparelhos — quer?
- [ ] **Moedas** continuam grátis para sempre?
- [ ] Restringiu a chave `apiKey` no Google Cloud? Ver [[Segurança]].
- [ ] Prints do **V2 no celular**, do **leitor novo (#000)** e dos **35 efeitos** no aparelho (só o PC foi visto em print; o celular só compilou).

## Trabalho começado
- [ ] **Visual Luar ametista** (2026-11-18, só no celular; **PC pausado até o autor pedir**). Feito: cores, fontes Italiana + Spectral, botão central, ícones celestes, capa limpa, fala em itálico, texto e seta, brilho no título, lua do capítulo, fita, abertura com lua, esmaecer, estante do mês, estrelas ✦, ficha de livro na obra (capa pequena + título + números + sinopse em Spectral), retrato no perfil (foto redonda com brilho, nome centrado), folha limpa no editor (contagem e ferramentas só com o lápis aberto). Falta: o autor testar no aparelho. Só compilou; ninguém viu no aparelho. Escolhas: base A5 Ametista, B3 Italiana + Spectral, C4 botão central, D4 capa limpa, E3 fala em itálico, F4 texto e seta, G4 brilho no título; telas H1 capa grande, I4 estante do mês, J3 ficha de livro, K3 lua do capítulo, L1 preto puro, M1 folha limpa, N1 retrato, O3 ícones celestes, P1 lua nascendo, Q1 esmaecer, R3 marcador de fita, R6 estrelas das avaliações. Páginas: https://claude.ai/artifact/UvY13FkPkxKsLY169HMkq8 e https://claude.ai/artifact/Nq2mhF7kwx6g613VvPudxA
- [ ] Auditoria fase **mestre** (código limpo): `ui/TelaEditor.kt` feito em 2026-11-14 (falta o autor conferir no celular). Próximo: `TelaLeitor.kt`, depois `TelaObra.kt`.
- [ ] Telas internas ainda no visual antigo: página da obra, painel e fichas (as outras ganharam as telas novas em 2026-11-14).
- [ ] Código antigo sem rota (`TelaInicioNova`, `TelaEscreverAba`, `TelaPerfilAba`, `TelaDescobrirCatalogo` como aba): avaliar remover.

## Ideias guardadas
- Vozes PC: modelos Piper *x_low*, onnxruntime-web + WebGPU, voz do sistema enquanto a neural aquece.
- XP/missões do PC sincronizarem com o celular.
