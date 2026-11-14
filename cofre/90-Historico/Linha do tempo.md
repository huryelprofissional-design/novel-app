---
tags: [historico]
gerado: 2026-11-14
---
# Linha do tempo

Gerada do `git log` (mais novo primeiro). Para ver detalhes: `git log --stat`.

## 2026-11-08
- PC: os 35 efeitos novos (mesmos nomes e campos do celular)
- 35 efeitos novos no celular: 9 em linha e 26 de parágrafo (RPG, terror, fantasia, celular, época, estrutura)
- Leitor: fundo preto puro (#000) e barras de cima e de baixo sólidas no estilo editorial

## 2026-11-07
- Novely V2 no PC: barra lateral editorial e as telas Ler, Descobrir (primeiras linhas), Escrever (mesa) e Eu
- Novely V2 no celular: visual editorial (Ler, Descobrir, Escrever, Eu), fontes Instrument Serif + Plex Mono
- Painel de efeitos: aba Parágrafo com todos os efeitos do Mega Pack, por gênero (celular e PC)
- PC: vozes do Novely em paralelo (até 3 geradores) e primeiro pedaço curto
- PC: vozes do Novely começam mais rápido (motor guardado, aquecimento, frases curtas, aviso de preparo)
- PC: vozes do Novely no navegador (WebAssembly), painel para baixar/testar/narrar
- Vozes: tamanhos reais dos pacotes
- Vozes do Novely no celular: 7 vozes neurais (4 masculinas Piper + 3 Dora Kokoro) baixadas sob demanda, sem internet depois; narrador e elenco podem usar; geração com antecedência; AAR sherpa-onnx baixado no CI com hash conferido
- Vozes neurais: workflow que monta os pacotes (Piper Faber/Cadu/Jeff/Edresson + Kokoro com Dora jovem/expressiva/sussurrada) e publica no release vozes-v1
- PC: painel da voz redesenhado (progresso, play grande, velocidade deslizante, Vozes com todas as vozes do sistema por idioma, voz por personagem, testar); barras centralizadas na coluna do texto e com a mesma largura
- PC: painel da voz enquanto ouve (anterior, pausar, próximo, parar, velocidade, narrador, voz por personagem, ouvir o que está na tela)
- Mestre (Nuvem.kt): texto dividido sem cortar emoji (CC-03), ficha não é adotada por nome se já subiu (CC-07, evitava fundir duas fichas homônimas), sincronização com tempo limite sem internet (CC-12)
- Macaco: seguir/curtir/reagir/avaliar em fila (duplo toque não conta 2x), título/sinopse/nome cortados no limite das regras, convite apagado ao entrar (uso único), comentário aceita relógio adiantado até 1 dia
- Macaco: teste de caos com 3000 textos absurdos + capítulo de 10 MB
- Regras: troca de semana só junto com leitura (teste do emulador pegou)
- Regras do Firestore (etapa 1, compatível com o app atual): contadores só ±1 (FB-02), convites não listáveis + validade 7 dias + uso único (FB-04), sala de colaboração só da própria obra (FB-05), comentários/avaliações com schema e tamanho (FB-06); testes no emulador (regras/teste.mjs) rodando no CI
- SEC-01: assinatura com chave nova + linhagem v3 (via secrets), ensaio automático da rotação; PLAY-01: flavors github/play (play sem atualizador nem REQUEST_INSTALL_PACKAGES); AND-02: FLAG_SECURE com bloqueio ligado; APK publicado como novely.apk (link do site estava quebrado)
- Segurança (SEC-03): teto de tamanho ao ler ZIP (backup, EPUB, DOCX) contra zip bomb
- PC: leitor no padrão Reader HUD (pílula em cima, barra Ouvir/Buscar/Capítulos/Rolar/Leitura embaixo), buscar no capítulo, rolar sozinho e escolher botões da barra
- PC: painel de efeitos com prévia e intensidade, modo Prévia com Remover efeito, salva a cada 5 s, rascunho de emergência, versões com nome, contagem completa, Ctrl+E/Ctrl+P, Lixeira (capítulos, fichas e obras), aparelhos conectados e números da semana/mês
- Ouvir a partir de qualquer parágrafo (menu do parágrafo e 'ouvir o que está na tela'); PC: Ouvir começa do que está na tela; PC: publicar destrava assim que escolhe o gênero
- Ouvir: guarda o parágrafo falado no capítulo a cada parágrafo; se o app for fechado ou a voz cortar, volta exatamente de onde parou (não do começo)
- Atualização: download continua de onde parou e tenta de novo sozinho (até 8x); busca global não trava mais com o termo no começo do capítulo
- Leitor: escolher quais botões aparecem na barra de baixo
- Lixeira para fichas e obras inteiras (com fichas, capa e capítulos), atalhos de teclado físico no editor, lista de aparelhos conectados com desconectar à distância

## 2026-11-06
- Fase 6: alto contraste (celular e PC), painel com semana/mês/média/melhor dia, relatório de revisão por gravidade com filtro e cópia, pasta direto da estante
- Fase 5: fichas com campos próprios, condição por capítulo e segredos revelados no capítulo certo (banco v12); marcadores e grifos presos ao texto; PC edita os campos novos
- Fase 4: salva a cada 5 s digitando, rascunho de emergência com recuperação, versões com nome (não apagadas sozinhas), contagem completa e tela de conflito com escolha
- Testes: formato da intensidade dos efeitos e origem dos blocos
- Editor: verSimbolos depois de irPara (ordem das funções locais)
- Fase 3: painel de efeitos com prévia ao vivo, cor e intensidade (+ forte, - suave); editor com modo Prévia (desenhado pelo leitor) e Remover efeito com desfazer; Reduzir animações para os efeitos; PC entende + e -
- Fase 2: barra de abas E-Paper Ink (Estante/Explorar, pílula branca, ícone preenchido), HUD do leitor no padrão Reader HUD e opção Reduzir animações
- Fase 1: nomes no leitor com confiança — 'Seu', 'Dona' e títulos nunca viram link; parte do nome só sublinhado leve; autor pode marcar 'não é este personagem'

## 2026-11-04
- Notificação [!aviso!] não fica mais presa na tela ao rolar; toque fecha
- Transcrição do áudio sempre visível
- Foto de teste do Mega Pack
- Divide os blocos do Mega Pack em funções (método grande demais); PC: Mega Pack completo (fx.js, estilos, cola do editor)
- Corrige cor nula
- Mega Pack no app: 58 blocos [#efeito:cor ...] (Sistema roxo com cor por texto, murim, terror, mistério, fantasia, sci-fi, celular, época, estrutura) e 18 efeitos {efeito: texto}

## 2026-11-03
- PC: leva 3 de efeitos (rank, telepatia, riscado, missões, item, pergaminho, chat ao vivo, local e hora, conversa, cartão do personagem, epígrafe, nota do autor); nome de constelação com dois-pontos no chat ao vivo
- Efeitos leva 3: [S] rank, {telepatia}, ~~riscado~~, missões no sistema, [+item], {{pergaminho}}, >>>chat ao vivo, :: local e hora, >>Nome: conversa, @@cartão do personagem, > epígrafe, N.A.: nota do autor
- PC: segredo, glitch, voz sombria, barras de status, notificação, carta, título de impacto e pausa
- Efeitos escolhidos: ||segredo||, %%glitch%%, <<voz sombria>>, barras de status no `[[sistema]]`, [!notificação!], == carta, ## título de impacto, ~pausa
- PC: efeitos de escrita (~cena…~fim, !!grito!!, ((sussurro)), ^^brilho^^, `[[sistema]]`, >> mensagem) e botão ✦ Efeitos no editor
- Efeitos de escrita por símbolo: ~chuva…~fim (10 efeitos de cena), !!grito!!, ((sussurro)), ^^brilho^^, `[[janela de sistema]]`, >> mensagem; botão ✦ no editor com a cola
- Clima da cena mais calmo: só palavras de clima de verdade, precisa de várias menções, confirma antes de trocar e entra devagar
- PC: nível, missões e baú no perfil, celebração com confete, fim de capítulo com reações, clima da cena, fogueira no editor, estante e capa grande animada no início
- Foto de teste dos efeitos novos
- Retrospectiva do mês em stories, feed vertical de primeiras linhas, anel girando no avatar e brilho nas insígnias
- Corrige estante
- Estante com prateleiras animadas, fogueira do escritor com som ambiente, clima da cena no leitor e cartão de trecho estilo story
- Fim de capítulo de cinema (anel, confete, ganhos, reações), Início com capa grande animada, anéis girando nos autores e stories de capítulo novo
- Confete: nome sem conflito
- Jogo: XP, nível e título, missões do dia com baú, ranking da semana e comemorações (nível, conquista, baú) com confete
- Máquina de escrever no editor (celular e PC): papel, letra de máquina, som de cada tecla, plim no Enter, batida e a linha sempre no meio
- Bíblia: relações com nomes inteiros e toque para ver mais; nome com epíteto vira apelido; parte do nome não pega outra pessoa (Gwon Jang-ho)
- APK mais leve: só 64 bits e bibliotecas nativas comprimidas

## 2026-11-02
- PC: importar Word (.docx); notas de decisões
- PC: escrever junto ao vivo (mesma sala do celular), salas na colaboração e botão no editor
- PC: moedas (mesma carteira do celular), acesso antecipado no catálogo, ganhar lendo/comentando, autor liga na publicação
- PC: ouvir com voz por personagem, escolher narrador e ler traduzido (tradutor do navegador)
- PC: Bíblia viva (furos, personagens, mapa de relações, relações) e Leitores
- Escrever junto ao vivo: sala por capítulo, parágrafo com trava de quem escreve, presença, cor de quem escreveu, chat e 'Trazer para a obra'
- Moedas grátis e acesso antecipado: leitor ganha lendo/comentando/abrindo o app e libera capítulos agendados antes; autor liga e vê quantos liberaram
- Word: junta pedaços com o mesmo estilo; teste
- Importar Word (.docx) e links (Google Docs, .txt/.epub/.docx), e criar fichas pelos nomes depois de importar
- Ler traduzido: tradução no aparelho (ML Kit, grátis e offline), detecta o idioma da obra, guarda as traduções
- Estatísticas de leitores para o autor (funil, conclusão, onde param, tempo), registro de leitura no app e no PC; regras do Firestore no repositório
- Ouvir com vozes por personagem (elenco com voz e tom), continua com a tela apagada com controles na notificação; ajustes na Bíblia viva
- CI: fotos das telas num ramo próprio
- Teste de telas: comentário
- Mapa de relações (arrasta, zoom, volta no tempo pelos capítulos) e fotos automáticas das telas nos testes
- Bíblia viva: lê os capítulos no aparelho e aponta furos (idade, olhos, cabelo, mortes, ferimentos, nível, nomes errados, personagem sumido), retrato de cada personagem e relações
- Teste no emulador: mediana de 5 aberturas com o perfil aplicado
- Teste no emulador: escolher o build
- Teste automático no emulador depois de cada APK (abre, 600 toques, mede travadas)
- Mais leve e fluido: APK otimizado (R8 + perfis do Compose), imagens do catálogo abertas fora da tela principal com cache, animações que só redesenham, progresso do leitor sem recompor a tela
- Estúdio de IA no PC (mesmos prompts do app, cola a resposta e vira capítulo, fichas ou resumos)
- Estúdio de IA: prompts com todo o contexto da obra (próximo capítulo, cena, revisão, diálogos, gancho, reviravoltas, personagens, mundo, técnicas, furos, resumos, sinopse, títulos) e a resposta volta para o app; ideias de história no gerador
- PC: pacote visual (cor da capa, enfeites, capitular, noite profunda, Anteriormente, comentário no trecho, maratona, conquistas, citações, prévia ao parar o mouse, tempo restante)
- Pacote visual: cor da capa na obra e no leitor, enfeites de cena, noite profunda, 'Anteriormente…', comentário no trecho, maratona, painel do escritor, conquistas, citações favoritas, prévia do pôster, ícones do app e tempo restante

## 2026-11-01
- PC: conteúdo centralizado na tela; personagens só aparecem dentro da história para quem lê
- Gerador: técnicas com nomes que fazem sentido (cada golpe combina com o próprio tipo) e formas
- Gerador de nomes: brasileiros com nome e sobrenome de verdade (celular e PC)
- PC com tudo do celular: obra completa (detalhes, capa, banner, capítulos, publicar/agendar/tirar do ar), fichas, linha do tempo, leitor com ajustes, ouvir, grifos e fichas ao clicar, editor com ferramentas e @, catálogo com gêneros, seguir, avaliar, curtir e comentar, autor, avisos, colaboração, gerador de nomes, importar TXT e painel do dono
- Site do PC: configuração do app web no Firebase
- Site do PC no estilo Pôster com vidro líquido: barra lateral de vidro, pôsteres no Início, obra com título gigante, leitor com cores calmas, perfil com banner
- Vidro: modo Leve (padrão, rápido e legível) e Líquido opcional em Ajustes › Aparência; ficha do personagem com vidro mais fechado para ler bem
- Vidro líquido como no exemplo: refração na borda (Android 13+), saturação e brilho, aro de luz e reflexos; barra de abas de vidro com lente deslizante
- Editor: corrige compilação do vidro
- Vidro líquido de verdade: o texto atrás aparece desfocado (barras do leitor, ficha do personagem, ferramentas do editor)
- Vidro: ficha do personagem no leitor, barra do leitor e barra de ferramentas do editor
- Perfil: banner próprio (sincroniza e aparece no perfil público), foto sem anel; leitor e editor com cores calmas (azul e verde-água)
- Obra: capa inteira aparece no topo (como livro) sobre o fundo desfocado
- Obra: capa do topo não vaza mais sobre o texto; publicar/republicar/tirar do ar pelos 3 pontinhos do capítulo
- Perfil refeito: foto no topo, cartão por cima, nome grande com AUTOR em laranja
- Visual novo: estilo Pôster (tema C)

## 2026-10-31
- Nomes: partes únicas do nome reconhecidas (Seo-jun → Kang Seo-jun) e procurar nomes sem ficha
- Corrige contexto do repositório
- Proteção: outra conta no mesmo aparelho não recebe as obras da anterior
- Revisão: leituras na nuvem muito menores, sincronização sem reler tudo, erros de fundo não fecham o app, puxar para atualizar
- Site: mesmas regras de nomes do app
- Nomes de personagens: hífen como separador, nomes com hífen inteiros, @[nome livre], nomes conhecidos com prioridade, sem apelido automático de primeiro nome; juntar/renomear fichas e reler texto

## 2026-10-29
- Polimento: capas com sombra e toque animado, cabeçalho do Perfil
- Resumo da obra com publicada/publicaId
- Início novo cheio (carrossel, em alta, ranking, gêneros, sua semana...), 52 gêneros com cartões coloridos, telas de gênero
- Widget, atalhos do ícone, bloqueio com digital/senha, tutorial, sessão de escrita com cronômetro, painel de dono
- Fase 4: Novely no PC (site instalável) — mesma conta, biblioteca, leitor, editor sincronizado, catálogo
- Coautor e revisor por código de convite: revisão com notas por parágrafo e capítulos sugeridos
- Fase 3b: notificações com app fechado (capítulos, obras de autores seguidos, versão nova), página e seguir autor, números por capítulo, exportação escolhida pelo autor, aviso +18
- Documento de decisões atualizado (conta, nuvem, publicação, @/&)
- Atualização: procura toda vez que abre o app (máx. a cada 15 min) e mostra janela da versão nova
- Publicar: cria a obra no catálogo antes dos capítulos
- Fase 3a: publicar no catálogo (capítulo por capítulo, agendar, gêneros, tags, classificação), Descobrir com catálogo e ranking, página pública, seguir/baixar offline, estrelas, curtidas, comentários, leituras
- Corrige fechamento: gatilhos de sincronização entravam em recursão (recursive_triggers ligado em alguns Android)
- Fase 2: conta (Google/e-mail) obrigatória e sincronização com a nuvem (Firebase), com conflitos avisados
- Fase 1: abas Início/Biblioteca/Escrever/Descobrir/Perfil, Ajustes por seções, personagens e lugares com @ e & no texto (fichas automáticas sem spoiler)
- Modo livro de verdade (páginas medidas, deslizar/tocar para virar) e painel de opções da obra organizado
- Atualizar de dentro do app; registro de travamentos/fechamentos com relatório; cache de imagens e leitor sem redesenho ao salvar; publicar obra só-leitura (.novely) e Abrir com Novely
- Obras de outras pessoas sem edição; leitor mais visual (abertura com capa, capitular, nomes destacados, retomar, fim de capítulo novo, barra flutuante)
- Biblioteca, linha do tempo, reordenar arrastando, seleção múltipla, etiquetas, coleções, gerador de nomes
- Só tema Noite; Início modelo B (banner atrás, capa na frente); obra clássica; ver capa ampliada; ajustes alinhados
- fix composable call
- Temas completos: Noite, Papel, Neon e Terminal (cores, fontes embutidas, formatos, Início, barra, falas); marcadores, grifos, compartilhar trecho, rolagem automática, luz quente, timer de sono, estatísticas, botão Início

## 2026-10-28
- Estilos Cinema, Revista e Estante (Início e página da obra), escolha em Ajustes
- Editor: botão descer vai sempre ao máximo
- Editor 2.0: capítulo automático corrigido (teclado com corretor), colar com títulos separa sozinho, buscar/substituir (capítulo e obra), mapa, voltar onde parou, ir ao início/fim, modo foco, meta diária, desfazer/refazer, fala rápida, notas do autor, repetições, contador da seleção, mover trecho, juntar capítulos, fichas, comparar versões, lixeira
- import animateFloat
- Leitor: abertura com banner/capa, Sobre a obra com sinopse, resumo por capítulo (Neste capítulo / Anteriormente), fim animado
- Página em branco sem título, prólogo/títulos destacados no editor e no leitor, só vozes pt-BR offline com gênero, troca de voz confiável
- Atalho: Página em branco no botão Nova
- Capítulos automáticos por título, página em branco, prólogo em destaque, falas mais marcadas, escolha de narrador
- Fase H: início com destaque animado, capas geradas, transições, parallax; corrige comentário; DECISOES.md
- Fases F e G: busca global com trechos e salto ao resultado; backup .novely, restauração validada, exportar EPUB/TXT, backup automático
- Fases D e E: leitor com falas destacadas, voz, busca, modo páginas; editor em pedaços para textos gigantes; histórico de versões
- Fase C: importação TXT/EPUB com conferência + testes de texto e desempenho
- Corrige teclado cobrindo o editor (insets duplicados)
- Corrige import rememberSaveable
- CI: mostrar só linhas de erro
- Fase B: código no repositório, build mostra erros
- Add files via upload
