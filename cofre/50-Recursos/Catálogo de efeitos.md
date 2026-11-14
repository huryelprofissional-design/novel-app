---
tags: [recursos, efeitos, referencia]
gerado: 2026-11-14
---
# Catálogo de efeitos (Mega Pack)

Gerado de `MEGA` em `web/fx.js` (112 entradas). O celular tem a mesma lista em `MEGA_PACK` (`ui/EfeitosBloco.kt`). Os 36 últimos (desde "Brilho estelar") são o **pacote 2** de 2026-11-08.

Forma geral: `{efeito: texto}` · `{efeito:cor: texto}` · `[#efeito campo | campo]` · `[#efeito:cor …]` · intensidade `+`/`-` colada no nome (`{fogo+: …}`). Cores: roxo, vermelho, azul, verde, dourado, rosa, branco, sombrio, laranja.

## No trecho (em linha)

| Efeito | Como escrever |
|---|---|
| Texto em chamas | `{fogo: texto}` |
| Texto congelado | `{gelo: texto}` |
| Neon | `{neon: texto}` |
| Tremendo de medo | `{medo: texto}` |
| Eco | `{eco: texto}` |
| Runas antigas | `{runa: texto}` |
| Outro idioma | `{idioma: tradução}` |
| Voz divina | `{divino: texto}` |
| Feitiço | `{feitico: texto}` |
| Texto colorido | `{cor:vermelho: texto}` |
| Sangue escorrendo | `{sangue: texto}` |
| Veneno | `{veneno: texto}` |
| Raio | `{raio: texto}` |
| Sombra | `{sombra: texto}` |
| Texto quebrando | `{quebra: texto}` |
| Levado pelo vento | `{vento: texto}` |
| Ouro | `{ouro: texto}` |
| Coração batendo | `{coracao: texto}` |
| Brilho estelar | `{estrela: a última estrela}` |
| Fantasma | `{fantasma: estou atrás de você}` |
| Luz sagrada | `{sagrado: Que a luz te guie}` |
| Virando cinzas | `{cinzas: tudo o que restou}` |
| Letra à mão | `{mao: me encontre às 3h}` |
| Contagem regressiva | `{contagem: 10}` |
| Telepatia | `{mente: consegue me ouvir?}` |
| Corrompido | `{corrompido: não era humano}` |
| Número de dano | `{dano: -9999}` |

## RPG / Sistema

| Efeito | Como escrever |
|---|---|
| Painel de evolução | `[#evolucao Nv 11 > 12 \| Força 14 > 17 \| Agilidade 9 > 11]` |
| Atributos | `[#atributos FOR 14 \| AGI 9 \| VIT 12 \| INT 7 \| SAB 10 \| SOR 5]` |
| Habilidade | `[#habilidade Corte Fantasma \| ativa \| rank A \| 30 MP \| 12s \| descrição]` |
| Missão concluída | `[#missao-ok Sobreviver à primeira noite \| +300 XP \| +1 Baú de bronze]` |
| Missão falhou | `[#missao-falhou Proteger a vila \| −200 XP \| Reputação −5]` |
| Alerta de perigo | `[#perigo Presença hostil detectada \| Recomendação: FUGIR]` |
| Conquista desbloqueada | `[#conquista Primeiro Sangue \| +50 G]` |
| Classe obtida | `[#classe Espadachim Sombrio \| passiva 1 \| passiva 2]` |
| Título obtido | `[#titulo Aquele que Sobreviveu ao Abismo \| +10% resistência]` |
| Chefe à vista | `[#chefe Rei Goblin Gorok \| Nv 45 \| 3 fases]` |
| Andar da masmorra | `[#andar 47 \| Câmara dos Espelhos Quebrados]` |
| Janela do Sistema (qualquer cor) | `[#sistema:vermelho Título \| texto]` |
| Baú de recompensa | `[#bau Baú de Ouro \| 300 G \| Espada Rúnica \| Poção x3]` |
| Ranking | `[#ranking Torre \| 1 Kang 9820 ↑2 \| 2 Hana 9400 ↓1 \| 3 Você 8990 novo]` |
| Tempo da missão | `[#tempo 00:00:15 \| Fuja da masmorra]` |
| Ficha de inimigo | `[#inimigo Lobo das Cinzas \| Nv 18 \| fraco: gelo \| forte: fogo \| descrição]` |
| Subiu de nível | `[#levelup 12 \| +5 Força \| +3 Agilidade]` |
| Loja do Sistema | `[#loja 1240 PTS \| Poção de cura 50 \| Pergaminho de fuga 300 \| Ovo de dragão 9999]` |
| Buffs e debuffs | `[#status +Fúria 3t \| +Escudo \| -Veneno \| -Lentidão]` |
| Drop raro | `[#drop lendário \| Coração do Abismo]` |
| Medidor de ameaça | `[#ameaca Rei Demônio \| 9/10]` |

## Murim e cultivo

| Efeito | Como escrever |
|---|---|
| Avanço de reino | `[#reino Condensação > Fundação > Núcleo Dourado > Alma Nascente \| 3]` |
| Técnica marcial | `[#tecnica Palma do Dragão Celestial \| grau \| forma 1 \| forma 2 \| ???]` |
| Seita ou clã | `[#seita Monte Hua \| Mestre Cheong \| lema]` |
| Pílula | `[#pilula Pílula da Alma Pura \| 9ª classe \| efeito 1 \| efeito 2]` |
| Duelo | `[#duelo Kang Min-ho 70% vs Ryu Seong 45%]` |
| Dantian (Qi acumulado) | `[#dantian 72% \| Fundação, estágio médio]` |
| Tribulação celestial | `[#tribulacao 4/9 \| A quarta onda caiu]` |
| Pergaminho de técnica | `[#pergaminho Palma do Dragão Celeste \| Terceira forma · Trovão que rasga o céu]` |

## Terror

| Efeito | Como escrever |
|---|---|
| Página rasgada | `[#rasgada texto]` |
| Fita VHS | `[#vhs 03 NOV 1998 02:14 \| texto]` |
| Regras | `[#regras Regras do turno da noite \| 1. Não atenda o telefone \| 2. … \| 3. …]` |
| Foto antiga | `[#foto legenda]` |
| Vozes ao redor | `[#vozes volta… \| fica \| ele mente \| abre a porta]` |
| Monitor cardíaco | `[#ecg 142]` |

## Mistério

| Efeito | Como escrever |
|---|---|
| Arquivo confidencial | `[#arquivo Caso 0417 \| campo \| campo]` |
| Depoimento | `[#depoimento DET. PARK: pergunta \| SUSPEITO: resposta]` |
| Telegrama | `[#telegrama CHEGO AMANHÃ STOP ...]` |
| Linha do tempo | `[#linha 22:00 Saiu do bar \| 23:14 ??? \| 00:30 Corpo encontrado]` |
| Saco de prova | `[#prova Nº 7 \| Luva de couro, mão esquerda \| Margem do rio]` |
| Carta lacrada | `[#lacre R \| Abra só depois que eu morrer.]` |

## Fantasia

| Efeito | Como escrever |
|---|---|
| Profecia | `[#profecia verso 1 \| verso 2 \| verso 3]` |
| Contrato mágico | `[#contrato Termos \| assinatura]` |
| Grimório | `[#grimorio Aquele que ler estas palavras em voz alta abrirá a porta.]` |
| Círculo mágico | `[#circulo Invocação \| texto]` |
| Bênção | `[#bencao Bênção da Lua \| +10% de sorte]` |
| Maldição | `[#maldicao Marca do Corvo \| não pode mentir]` |
| Carta de tarô | `[#taro XVI \| A Torre \| ruína e verdade]` |

## Ficção científica

| Efeito | Como escrever |
|---|---|
| Terminal | `[#terminal $ comando \| saída \| [OK] resultado]` |
| Holograma | `[#holo Projeção ativa \| dado \| dado]` |
| Transmissão cortada | `[#corte texto \| trecho perdido \| texto]` |
| Scanner de pessoa | `[#scan Kang Min-ho \| 17 \| rank F \| anomalia]` |
| Chuva de código | `[#codigo ACESSO CONCEDIDO]` |
| Alerta vermelho | `[#alerta-vermelho Violação de casco no setor 7]` |
| Voz de IA | `[#ia ÁRTEMIS \| texto]` |
| Mira travando | `[#mira Alvo \| 340 m \| TRAVADO]` |

## Celular e redes

| Efeito | Como escrever |
|---|---|
| Post de rede social | `[#post kang_minho \| texto \| curtidas \| comentários]` |
| Manchete de jornal | `[#jornal MANCHETE \| subtítulo \| texto]` |
| E-mail | `[#email Dra. Han \| Re: o que você viu na torre? \| corpo]` |
| Ligação recebida | `[#ligacao Número desconhecido \| +82 02 ••• 0417]` |
| Pesquisa | `[#busca como sair de uma masmorra sem morrer]` |
| Mensagem de voz | `[#voz 0:23 \| transcrição]` |
| Tela de bloqueio | `[#bloqueio 03:14 \| notificação \| notificação]` |
| Mensagem apagada | `[#apagada texto]` |
| Digitando… | `[#digitando Kang \| mensagem]` |
| Grupo de mensagens | `[#grupo Família 🏠 \| Mãe: texto \| Pai: texto \| eu: texto]` |
| Localização ao vivo | `[#localizacao Hana \| Torre Norte, 13º andar \| ao vivo]` |
| Notificação no topo | `[#notificacao WhatsApp \| Mãe \| Onde você está?]` |
| Chamadas perdidas | `[#perdidas Hana x7 03:12 \| Número privado 03:09]` |
| Story | `[#story yuna.k \| ninguém vai sentir minha falta \| 2 h]` |
| Bateria acabando | `[#bateria 2% \| O celular vai desligar]` |
| Música tocando | `[#musica Last Breath \| Night Echo]` |

## Época

| Efeito | Como escrever |
|---|---|
| Diário com data | `[#diario 12 de março de 1894 \| texto]` |
| Cartaz de procurado | `[#procurado Jack "Cinzas" Moreau \| $ 5.000 \| crimes]` |
| Rádio antigo | `[#radio Atenção, cidadãos: toque de recolher.]` |
| Página datilografada | `[#datilografado Relatório nº 7 \| texto]` |

## Estrutura do capítulo

| Efeito | Como escrever |
|---|---|
| Aviso de conteúdo | `[#aviso violência, morte de personagem, linguagem forte]` |
| Salto no tempo | `[#salto 3 anos depois]` |
| Abertura de capítulo | `[#capitulo 12 \| O Rei sem Nome]` |
| No próximo capítulo | `[#proximo texto]` |
| Fim de arco | `[#fimarco II \| A Torre Caída \| O próximo arco começa no capítulo 41]` |
| Epígrafe | `[#epigrafe Todo monstro já foi alguém que ninguém ouviu. \| Diário de Han]` |
| Enquanto isso… | `[#enquanto Enquanto isso, em Seul]` |
| Relógio da cena | `[#hora 03:17 \| Madrugada]` |
| Lembrança borrada | `[#lembranca …e ela sorriu.]` |
