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

# Efeitos 2.0

Gerado de `MEGA2` em `web/fx2.js` (55 entradas). No celular: `MEGA_PACK_V2` em `ui/EfeitosV2.kt`. Sintaxe com `$`; os de cima continuam valendo.

## Sistema e rpg

| Efeito | Como escrever |
|---|---|
| Janela do Sistema 2.0 | `[$sistema MISSÃO DE EMERGÊNCIA \| Sobreviva até o amanhecer \| Recompensa: ???]` |
| Pilha de avisos | `[$avisos Você derrotou o Lobo das Cinzas \| +300 XP \| Novo título disponível]` |
| Recompensa girando | `[$recompensa lendário \| Espada do Vazio \| +40 ATK]` |
| Despertar | `[$despertar S \| Classe: Necromante]` |
| Penalidade | `[$penalidade Missão diária não cumprida \| 00:00:10]` |
| Patrocínio de constelação | `[$patrocinio O Rei sem Nome \| 1000]` |
| Portal aberto | `[$portal A \| Gangnam]` |
| Você morreu | `[$morte Você morreu \| 3]` |
| Recarga de habilidade | `[$recarga Corte Fantasma \| 12s]` |
| Fusão de habilidades | `[$fusao Chama + Vento = Tornado de Fogo]` |
| Carregando do Sistema | `[$carregando Sincronizando com o usuário \| 100]` |

## Murim

| Efeito | Como escrever |
|---|---|
| Pincel de caligrafia | `[$pincel 天魔神功 \| Arte Divina do Demônio Celestial]` |
| Placa de jade | `[$jade Ordem do Líder \| Monte Hua \| Quem portar esta placa fala pela seita.]` |
| Flores de ameixa | `[$ameixa Vinte e Quatro Ameixas Florescentes \| 7ª forma]` |
| Os Dez Grandes | `[$dez Os Dez Grandes Mestres \| Espada Celestial \| Demônio Sangrento \| Punho de Ferro do Norte]` |

## Fantasia

| Efeito | Como escrever |
|---|---|
| Constelação | `[$constelacao Órion Caído]` |
| Vitral | `[$vitral A Luz Não Perdoa]` |
| Brasão de casa | `[$brasao Casa Valerian \| Ferro e Cinza]` |
| Livro que se abre | `[$livro Capítulo Perdido \| No princípio, a torre não tinha portas.]` |
| Poção | `[$pocao Elixir da Lua \| Cura 50% \| raro]` |
| Ovo chocando (toque) | `[$eclosao Filhote de Wyvern]` |
| Juramento de sangue | `[$juramento Pelo meu sangue, eu juro.]` |

## Terror

| Efeito | Como escrever |
|---|---|
| Frase que se repete | `[$repete eu não estou sozinho \| 7]` |
| Foto que muda (toque) | `[$foto-muda Acampamento, 1987]` |

## Mistério

| Efeito | Como escrever |
|---|---|
| Digital | `[$digital Han Seo-jun \| 98%]` |
| Cofre | `[$cofre 4-1-7 \| Dentro: uma carta e uma chave.]` |
| Relógio parado | `[$relogio 03:14 \| Hora da morte]` |
| Bilhete de recortes | `[$recortes VOCÊ É O PRÓXIMO]` |

## Ficção científica

| Efeito | Como escrever |
|---|---|
| HUD do capacete | `[$hud Alerta: oxigênio baixo \| O₂ 34% \| ALT 880 m \| PRESSÃO OK \| TEMP -41°]` |
| Autodestruição | `[$autodestruicao 00:59]` |
| Salto no espaço | `[$dobra Saltando para Kepler-442]` |
| Exame de DNA | `[$dna Compatível 99,7% \| Humano?]` |
| Olho da IA | `[$olho-ia MÃE \| Bom dia, tripulação. Faltam 3 de vocês.]` |
| Radar | `[$radar 3 contatos \| 400 m]` |

## Celular e redes

| Efeito | Como escrever |
|---|---|
| Chamada de vídeo | `[$video Hana \| 03:12]` |
| Live | `[$live yuna.k \| 12,4 mil \| jae_03: ela tá chorando?? \| min.s: alguém liga pra polícia]` |
| Pix recebido | `[$pix 5000 \| Desconhecido]` |
| Visto e sem resposta | `[$visto você tá bem? me responde \| 03:14]` |
| Post de fórum | `[$forum torre_norte \| Alguém mais viu a luz no 13º andar? \| 342 \| 87]` |
| Tradutor | `[$traduz 살려줘 \| me ajude \| coreano]` |
| Despertador | `[$alarme 06:00 \| Primeiro dia na Academia]` |

## Romance

| Efeito | Como escrever |
|---|---|
| Coração acelerado | `[$batimento 72 > 140 \| Ele sorriu.]` |
| Fio vermelho | `[$fio Hana \| Seo-jun]` |
| Bilhete dobrado (toque) | `[$bilhete para: você \| Me espera depois da aula.]` |
| Polaroid | `[$polaroid Nosso último verão]` |

## Ação

| Efeito | Como escrever |
|---|---|
| Tempo parado | `[$tempo-parado Tudo parou. \| Nem a chuva caía.]` |

## Estrutura do capítulo

| Efeito | Como escrever |
|---|---|
| Abertura 2.0 | `[$abertura 12 \| O Rei sem Nome]` |
| Começo de arco | `[$arco II \| A Torre Caída]` |
| Contador de dias | `[$dias 1 > 100 \| Dias desde o apocalipse]` |
| Coordenadas | `[$coord 37.5665° N · 126.9780° E \| Seul]` |

## No trecho (em linha)

| Efeito | Como escrever |
|---|---|
| Palavra cortada | `{$corte: acabou}` |
| Corando | `{$corar: texto}` |
| Aura | `{$aura:roxo: texto}` |
| Cristal | `{$cristal: texto}` |
| Assinatura | `{$assinatura: Nome}` |
