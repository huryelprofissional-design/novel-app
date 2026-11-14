---
tags: [nuvem, seguranca]
---
# Regras do Firestore

Arquivo: `firestore.rules` (raiz do repo). **O autor cola as regras no console do Firebase manualmente**; o repositório é a fonte da verdade.

Princípio: qualquer um cria conta grátis, então "logado" = "qualquer um". Nada confia no app.

## Funções
- `logado()`, `admin()` (e-mail do autor + `email_verified`; o e-mail fica só aqui), `dono(pid)`, `mudou()`.
- `passo(campo, min, max)`: contador só anda dentro de [min, max] por gravação (seguir +1, deixar de seguir −1). Evita alguém gravar `leituras = 999999999`.
- `textoAte(mapa, campo, max)`: limite de tamanho de texto.

## Pontos da auditoria (FB-xx)
- **FB-02** contadores só ±1; zerar a semana (`semanaId`) só junto com uma leitura de verdade; autor não infla os próprios números.
- **FB-04** convites: só `get` pelo código (não dá para listar), vencem em 7 dias, papel não pode ser forjado, apagados depois do uso.
- **FB-05** só cria sala de colaboração para obra própria.
- **FB-06** comentário só com os campos do app, tamanho e data válidos (`em` ≤ agora + 1 dia); avaliação de 1 a 5.
- Privado continua privado (`users/{uid}`).

## Testes
`regras/teste.mjs` com o emulador do Firebase (`@firebase/rules-unit-testing`). Rodam no CI (`.github/workflows/regras.yml`), não localmente (o emulador não baixa no ambiente do Claude). **11 testes, todos passando** na última execução.

## Ideia em aberto (etapa 2)
Contadores em lote + um ID de semana igual em todos os aparelhos. O autor ainda não respondeu se quer. Ver [[Pendências]].
