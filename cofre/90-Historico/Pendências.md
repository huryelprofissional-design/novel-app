---
tags: [pendencias]
atualizado: 2026-11-14
---
# Pendências

## Esperando resposta do autor
- [ ] **Efeitos 2.0 no aparelho** (2026-11-14): os 55 foram feitos no PC (testados no Chromium) e no celular (só compilou no CI; ninguém viu a tela). Pedir print/retorno, principalmente de vibração, ovo/bilhete/foto (toque) e do HUD. No celular os títulos usam Instrument Serif no lugar da Cinzel e a fonte de pincel do sistema (Ma Shan Zheng ficaria pesada no APK).
- [ ] **Vozes no PC lentas**: qual voz usa, se o problema é demorar a começar ou pausas, quantos núcleos o PC tem. Ver [[Vozes]].
- [ ] **Vozes no celular**: retorno de uso real (pausas entre parágrafos, erro ao baixar).
- [ ] **Regras etapa 2**: contadores em lote + ID de semana igual em todos os aparelhos — quer?
- [ ] **Moedas** continuam grátis para sempre?
- [ ] Restringiu a chave `apiKey` no Google Cloud? Ver [[Segurança]].
- [ ] Prints do **V2 no celular**, do **leitor novo (#000)** e dos **35 efeitos** no aparelho (só o PC foi visto em print; o celular só compilou).

## Trabalho começado
- [ ] Auditoria fase **mestre** (código limpo): `ui/TelaEditor.kt` feito em 2026-11-14 (falta o autor conferir no celular). Próximo: `TelaLeitor.kt`, depois `TelaObra.kt`.
- [ ] Telas internas ainda no visual antigo (só herdaram cores): obra, ajustes, painel, estatísticas; no perfil do celular, Ranking/Moedas/Conquistas/Citações.
- [ ] Código antigo sem rota (`TelaInicioNova`, `TelaEscreverAba`, `TelaPerfilAba`, `TelaDescobrirCatalogo` como aba): avaliar remover.

## Ideias guardadas
- Vozes PC: modelos Piper *x_low*, onnxruntime-web + WebGPU, voz do sistema enquanto a neural aquece.
- 33 efeitos recusados (ver [[Decisões]]) — não oferecer de novo sem o autor pedir.
- XP/missões do PC sincronizarem com o celular.
