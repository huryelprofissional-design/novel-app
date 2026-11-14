---
tags: [nuvem]
---
# Firebase e Firestore

Projeto Firebase: `novely-novelapp-48fe7`. Login com Google ou e-mail (Firebase Auth). Celular e PC usam a mesma base.

## Coleções
| Caminho | O que guarda | Quem lê / escreve |
|---|---|---|
| `users/{uid}/**` | obras, capítulos, fichas, progresso e aparelhos da pessoa (sincronização) | só o dono |
| `perfis/{uid}` (+ `seguidores/{quem}`) | perfil público: nome, bio, fotoHash, bannerHash, conquistas, contadores | todos leem; dono edita; contadores só ±1 |
| `publicas/{pid}` | obra publicada: título, sinopse, autorUid, autorNome, capaMini, gêneros, tags, classificação, contadores, semanaId | logados leem; dono edita; contadores com `passo()` |
| `publicas/{pid}/caps/{cid}` | capítulo publicado (+ `curtidas/{uid}`, `reacoes/{uid}` com chorei/fogo/choque/risada/mente, `comentarios/{id}`) | ver [[Regras do Firestore]] |
| `publicas/{pid}/textos/{t}` | texto dos capítulos publicados (partido em partes) | logados leem; dono escreve |
| `publicas/{pid}/arquivos`, `fichas`, `seguidores/{uid}`, `avaliacoes/{uid}` (1 a 5), `leituras/{id}` | anexos, fichas públicas, seguidores da obra, notas, leituras | — |
| `convites/{codigo}` | convite de colaboração (só `get`, sem `list`; vale 7 dias; uso único) | — |
| `colabs/{pid}` (+ `membros`, `aovivo/{cid}`, `revisao/{cid}/notas`, `textos`, `sugestoes`) | colaboração e sala ao vivo | dono e membros |

## Sincronização (celular)
`data/Nuvem.kt`: texto partido em pedaços (`partirTexto`, seguro com emoji/surrogates), timeout de 180 s, ficha só é "adotada" pelo nome se nunca foi sincronizada. `data/Aparelhos.kt` lista aparelhos conectados.

## Publicação
`data/Publicacao.kt` (celular) e `web/nucleo.js` (PC). Ações sociais protegidas contra toque duplo (Mutex no celular, fila `emFila` no PC). Título, sinopse e nome são cortados no tamanho máximo antes de gravar. Catálogo em cache (`CacheCatalogo`, modos `ALTA`, `NOVIDADES`, `CONCLUIDAS`, `SEMANA`). Primeira frase de uma obra: `pub.inicioDaObra(pid)` (celular) / `capsPub` + `lerTexto` (PC).
