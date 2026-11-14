---
tags: [pc]
---
# PC — visão geral

- Pasta `web/`, **JavaScript puro em módulos ES, sem build e sem framework**. Publicado no GitHub Pages pelo workflow `site.yml`. Lista de arquivos: [[PC - Arquivos]].
- Firebase pelo CDN (`gstatic`), configuração pública em `web/config.js` (ver [[Segurança]] sobre a chave).
- Roteamento por hash: `rota(regex, fn)` em `nucleo.js`; a primeira rota registrada que casa ganha. `app.js` importa `v2.js` **antes** das outras para as telas V2 valerem.
- `casca(aba, html)` desenha a barra lateral + conteúdo. `window.redesenhar()` refaz a tela atual.
- Estado global `E` (usuário, perfil, obras). `local.ler/gravar` = localStorage com JSON.
- PWA: `sw.js` guarda o app (cache `novely-vNN` — **subir o número a cada mudança** e incluir arquivos novos na lista `BASE`). Não guarda `vozes/` (ficam em `novely-vozes-v1`, ver [[Vozes]]); o motor `voz/` fica em `novely-motor-sherpa-1.13.8`.
- Celular estreito (< 760 px): a barra lateral vira 4 abas embaixo.

## Rotas
`/` Ler · `/descobrir/:modo` (LINHAS, ALTA, SEMANA, NOVIDADES, CONCLUIDAS) · `/escrever` · `/perfil` · `/biblioteca/:aba` · `/ajustes` · `/obra/:id/:aba` · `/ler/:obra/:cap` · `/editar/:obra/:cap` · `/publer/:pid/:cid` · `/pub/:pid` · `/autor/:uid` · `/publicar/:id` · `/buscar` · `/generos` · `/genero/:g` · `/avisos` · `/colaborar` · `/colab/:pid` · `/colaboracao/:id` · `/aovivo/:p/:c` · `/revisao/:p/:c` · `/biblia/:id/:aba` · `/leitores/:id` · `/ia/:id/:cap` · `/nomes` · `/dono` · `/lixeira`.

## Diferenças em relação ao celular
- XP, missões e escritaDias do PC ficam no `localStorage` (não sincronizam com o celular).
- "Lendo" no perfil do PC é a contagem de obras em leitura (o navegador não guarda horas).
- Ações sociais (seguir, curtir, avaliar) passam por uma fila (`emFila`) para toque duplo não contar duas vezes.
