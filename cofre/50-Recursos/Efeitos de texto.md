---
tags: [recursos, efeitos]
---
# Efeitos de texto

O texto da obra é Markdown simples + símbolos. O leitor transforma os símbolos em visual; o "Ouvir" e a contagem de palavras ignoram os símbolos (`Marcas.semEfeitos` / `extras.js`). Decisão aprovada: **os símbolos ficam no texto** e o editor mostra prévia.

Há duas famílias:
1. **Símbolos curtos** (mais antigos): `!!grito!!`, `((sussurro))`, `^^brilho^^`, `[[sistema]]`, `~chuva … ~fim` etc. Lista abaixo.
2. **Efeitos 2.0** (nov/2026): `[$nome campo | campo]` e `{$nome: texto}`, 55 efeitos mais detalhados; começam quando aparecem na tela e tocar repete. Lista no fim do [[Catálogo de efeitos]].
3. **Mega Pack**: `{efeito: texto}` e `[#efeito campo | campo]`. Lista completa em [[Catálogo de efeitos]]. Como acrescentar um: [[Como criar um efeito novo]].

## Formato base
- Título do capítulo: linha sozinha `Capítulo 12 — Título` (ou Prólogo/Epílogo/Interlúdio).
- Um parágrafo por linha. Falas começam com `— `. Pensamento em `*itálico*`. Troca de cena: `* * *`.
- Personagem novo: `@Nome, descrição`; lugar: `&Lugar, descrição` (viram fichas). `@[Nome com minúscula]`, `@Nome (Apelido, Outro), descrição`.

## Símbolos curtos (texto do prompt Maestro)

```text
O leitor do Novely transforma estes símbolos em efeitos. Use quando a cena pedir, sem exagero (no máximo 3 ou 4 efeitos de texto por capítulo e 1 ou 2 efeitos de cena; o efeito tem que ter motivo na história):
- Efeito de cena: escreva numa linha sozinha ~chuva, ~neve, ~fogo, ~vento, ~noite, ~tensao, ~sangue, ~flashback, ~sonho ou ~magia. Ele vale daquela linha até ~fim (ou até outro ~efeito). A linha do símbolo não aparece para o leitor. Sempre feche com ~fim quando a cena acabar.
  ~tensao: perseguição, luta decisiva, segredo prestes a ser descoberto. ~flashback: lembranças e passado. ~sonho: sonhos e visões. ~sangue: momentos brutais. ~magia: despertar de poder, técnica, artefato.
- !!texto!! = grito (fica grande e treme). Ex.: — !!SAI DAQUI!! — gritou Kang.
- ((texto)) = sussurro ou pensamento baixo. Ex.: ((Ele não pode saber)), pensou Hana.
- ^^texto^^ = palavra mágica dourada, para nome de técnica, artefato ou poder na primeira vez que aparece. Ex.: a ^^Espada do Vazio^^ acordou.
- [[texto]] num parágrafo sozinho = janela azul de SISTEMA (perfeito para novels de Sistema: missões, status, níveis, recompensas). Ex.: [[Missão nova: Sobreviver à noite. Recompensa: 300 XP]]
- Barras de status dentro do Sistema: NOME número/máximo, separados por |. Ex.: [[HP 30/100 | MP 85/100 | XP 92/100]] (HP vermelho, MP azul, XP dourado, SP verde, outros roxo). Pode misturar texto: [[Status de Seo-jun | HP 30/100 | Força: 14]]
- [!texto!] num parágrafo sozinho = notificação que desliza do topo quando o leitor chega nela. Ex.: [!Você subiu para o nível 12!]
- >> texto num parágrafo sozinho = mensagem de celular. Ex.: >> cadê vc?? to te esperando
- == texto = carta escrita à mão em papel antigo. Cada linha da carta começa com ==, e linhas == seguidas viram uma carta só. Ex.:
  == Hana,
  == se você está lendo isso, eu não voltei.
  == — Y.
- ## TÍTULO num parágrafo sozinho = título de impacto, letras gigantes que se juntam no centro. Para virada de arco, nome de técnica decisiva ou "FIM DA PARTE 1". No máximo um por capítulo.
- ~pausa numa linha sozinha = pausa dramática: aparece "· · ·" e o parágrafo seguinte surge bem devagar. Use antes de uma revelação curta.
- ||texto|| = segredo: fica borrado até o leitor tocar. Para o nome do traidor, uma resposta, uma reviravolta.
- %%texto%% = glitch: letras corrompidas que trocam e tremem. Sistema bugado, IA, possessão, mundo virtual.
- <<texto>> = voz sombria: vermelho com sombra pulsando. Vilão, demônio, entidade, artefato falando.
- [S] = selo de rank colorido no meio do texto (F, E, D, C, B, A, S, SS, SSS). Ex.: uma arma de rank [S].
- {texto} = telepatia, voz mental ou transmissão de qi (azul). Ex.: {Não olhe para trás}, ecoou na mente dele.
- ~~texto~~ = riscado, para diário, carta ou alguém se corrigindo. Ex.: Eu te ~~odeio~~ amo.
- Missões no Sistema: [[Missões | [x] Sobreviver à noite | [ ] Achar o mestre]] ([x] feita, [ ] pendente).
- [+Nome do item | raridade] num parágrafo sozinho = item obtido (raridade: comum, incomum, raro, épico, lendário, mítico). Ex.: [+Espada do Vazio | lendário]
- {{TÍTULO | texto}} num parágrafo sozinho = pergaminho com selo (decreto, édito, técnica secreta). Ex.: {{DECRETO DA SEITA | Que Han Seo-jun seja expulso do Monte Hua.}}
- :: Local | hora num parágrafo sozinho = abertura de cena estilo filme. Ex.: :: Seul, Gangnam | 03:14 da manhã
- > frase — autor num parágrafo sozinho = epígrafe no começo do capítulo.
- @@Nome num parágrafo sozinho = cartão do personagem com os dados da ficha. Use na primeira aparição importante ou quando ele volta depois de muito tempo.
- >>Nome: texto = mensagem de outra pessoa (balão à esquerda, com nome). >> texto sem nome = mensagem do protagonista (à direita).
- >>>Nome: texto em várias linhas seguidas = chat ao vivo / constelações comentando. Nomes entre colchetes ficam dourados. Ex.:
  >>>[Constelação: O Rei sem Nome]: está interessado em você.
  >>>seojun_fã: BORAAA
- N.A.: texto = nota do autor, separada da história (o Ouvir pula). Só escreva se eu pedir.
```
