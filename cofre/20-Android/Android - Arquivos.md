---
tags: [android, referencia]
gerado: 2026-11-14
---
# Android — arquivos

Lista gerada do código (linhas e primeira descrição do arquivo). Pacote `com.huryel.novely`.

## `data/`

| Arquivo | Linhas | O que é |
|---|---|---|
| `Ambiente.kt` | 110 | Som ambiente gerado no próprio app (sem arquivo e sem internet): chuva, lareira, vento, noite e café. |
| `AoVivo.kt` | 128 | Escrever junto ao vivo (autor + coautores), parágrafo por parágrafo: |
| `Aparelhos.kt` | 42 | Aparelhos conectados à conta (users/{uid}/aparelhos/{id}): cada um avisa que está vivo na sincronização. |
| `Arquivos.kt` | 171 | Texto de cada capítulo = um arquivo próprio. |
| `Atualizador.kt` | 169 | Nova versão encontrada nos Releases do GitHub. |
| `Backup.kt` | 363 | Backup portátil: um arquivo .novely (ZIP comum) com manifest.json + textos/(id).md + imagens. |
| `Banco.kt` | 522 | O banco guarda só os DADOS DE ORGANIZAÇÃO (títulos, ordem, progresso). |
| `Biblia.kt` | 321 | Bíblia viva: lê os capítulos no próprio aparelho (sem IA, sem internet) e monta |
| `Buscador.kt` | 68 | Busca local sem índice separado: lê um capítulo por vez do armazenamento, compara sem acentos |
| `CamposFicha.kt` | 51 | Campos de texto livre da ficha, uma linha por item: |
| `Colaboracao.kt` | 156 | Coautores e revisores (por convite com código): |
| `Diagnostico.kt` | 83 | Guarda o que aconteceu quando o app fecha sozinho ou trava, |
| `Importador.kt` | 363 | Importação em duas etapas: (1) lê o arquivo em modo streaming e grava cada capítulo detectado |
| `Limites.kt` | 32 | Leitura com teto de tamanho para entradas de ZIP (backup .novely, EPUB, DOCX). |
| `Maquina.kt` | 84 | Sons da máquina de escrever, gerados no próprio app (sem arquivo de áudio): |
| `Marcas.kt` | 230 | Personagens e lugares escritos direto no texto: |
| `Moedas.kt` | 87 | Moedas do Novely (de graça, sem dinheiro): o leitor ganha lendo e gasta para ler capítulos |
| `Narrador.kt` | 397 | Elenco: divide cada parágrafo em narração e falas e descobre quem fala |
| `Nuvem.kt` | 569 | Cada função devolve null quando deu certo, ou a mensagem de erro em português. |
| `Progresso.kt` | 133 | Progresso de jogo do Novely: XP, nível, título, missões do dia com baú e os momentos de comemorar |
| `Prompts.kt` | 151 | Estúdio de IA: monta prompts completos com o contexto da obra (sinopse, estilo do autor, fichas, |
| `Publicacao.kt` | 514 | Obra no catálogo público. |
| `Repositorio.kt` | 685 | Chamado com as palavras novas escritas (para a meta diária). |
| `Texto.kt` | 178 | Funções puras de texto (sem Android) — testadas em app/src/test com textos sintéticos. |
| `Tradutor.kt` | 65 | Tradução no próprio celular (ML Kit do Google): grátis, sem conta, funciona offline depois de baixar |
| `Vigia.kt` | 75 | Roda de hora em hora mesmo com o app fechado (só com internet): |
| `VozNeural.kt` | 214 | Vozes neurais do próprio Novely (sem internet depois de baixar), pela biblioteca sherpa-onnx. |
| `WidgetNovely.kt` | 64 | Widget da tela inicial: continuar lendo (capa, capítulo, progresso) e meta do dia. Toque abre o capítulo. |

## `ui/`

| Arquivo | Linhas | O que é |
|---|---|---|
| `Abas.kt` | 1030 | Troca de aba sem empilhar telas. |
| `AppNavegacao.kt` | 311 |  |
| `Classico.kt` | 485 | Banner da obra (ou arte gerada) com degradê que se funde ao fundo preto. |
| `Componentes.kt` | 185 | Imagens já abertas ficam na memória (até ~1/8 da RAM do app): trocar de tela não decodifica de novo. |
| `CorObra.kt` | 103 | Cor de destaque tirada da capa: a cor viva que mais aparece, clareada para ler bem sobre o preto. |
| `Editorial.kt` | 793 | Novely V2 · visual editorial: papel escuro, creme, vermelho-laranja; serifa grande, rótulos em mono, botões retos. |
| `Efeitos.kt` | 406 | Reflexo de luz passando por cima (botões, insígnias, lombadas douradas). Só redesenha. |
| `EfeitosBloco.kt` | 1394 | Cores que o autor escreve depois do nome do efeito. |
| `EfeitosNovos.kt` | 522 | Valor 0..1 que vai e volta (ou fica no meio com "Reduzir animações"). |
| `Enfeites.kt` | 65 | Enfeites de troca de cena que o autor escolhe para a obra. |
| `ExtrasLeitor.kt` | 328 | Folha que abre ao segurar um parágrafo no leitor. |
| `IconesApp.kt` | 28 | Troca o ícone do app ligando um activity-alias e desligando os outros. |
| `InicioNovo.kt` | 696 | Capas grandes (900 px) e perfis de autores, guardados enquanto o app está aberto. |
| `MapaRelacoes.kt` | 157 | Distribui os personagens: quem divide mais cenas fica perto (força de mola + repulsão), sempre igual para a mesma obra. |
| `Painel.kt` | 242 | Calcula as conquistas com o que o app já sabe (palavras, leitura, sequência…) e guarda as ganhas. |
| `PainelEfeito.kt` | 171 | Efeitos que o painel oferece: em um trecho (dentro da frase) e no parágrafo inteiro. |
| `Poster.kt` | 162 | Perfil no estilo Pôster: fundo cinza com a foto em preto e branco e um cartão escuro "fosco" por cima, |
| `Retro.kt` | 231 | Banner no Perfil que abre a retrospectiva. |
| `Stories.kt` | 121 | Stories de capítulo novo: um por obra atualizada nos últimos dias, começando pelo autor tocado. |
| `TelaAjusteImagem.kt` | 105 | ───────────────────────── Ajuste de capa / banner ───────────────────────── |
| `TelaAoVivo.kt` | 260 | Sala de escrita ao vivo: cada um escreve num parágrafo, todo mundo vê na hora, com chat ao lado. |
| `TelaAtualizar.kt` | 270 | Aviso de versão nova (no Início), relatório depois de travar/fechar e tratamento de "Abrir com Novely". |
| `TelaBackup.kt` | 140 |  |
| `TelaBiblia.kt` | 313 | Bíblia viva: furos, retrato de cada personagem e relações — tudo lido dos capítulos, no aparelho. |
| `TelaBusca.kt` | 131 |  |
| `TelaColaboracao.kt` | 333 | ───────────────────────── AUTOR: colaboração da obra ───────────────────────── |
| `TelaEditor.kt` | 1327 | Editor para capítulos gigantes: o texto é dividido em pedaços de ~3 mil caracteres, cada um num |
| `TelaEstatisticas.kt` | 182 | Números de um capítulo publicado, montados das leituras. |
| `TelaFichas.kt` | 272 | Fichas de personagens, lugares e outros elementos de uma obra. |
| `TelaIA.kt` | 220 | Estúdio de IA: escolhe o que pedir, monta o prompt com o contexto da obra, copia/abre no app de IA e traz a resposta de volta. |
| `TelaImportar.kt` | 188 | destino = id da obra para acrescentar capítulos, ou null para criar obra nova. |
| `TelaLeitor.kt` | 2131 | impacto: "## TÍTULO" (letras gigantes que se juntam). |
| `TelaLogin.kt` | 139 | Entrada obrigatória: Google ou e-mail. Fundo com capas inclinadas, como no mockup. |
| `TelaObra.kt` | 595 | Entrada da Bíblia viva, com a contagem de alertas (lida em segundo plano). |
| `TelasExtras.kt` | 270 | ───────────────────────── LINHA DO TEMPO ───────────────────────── |
| `TelasFinais.kt` | 171 | ───────────────────────── Bloqueio ───────────────────────── |
| `TelasPublicas.kt` | 789 | Imagens do catálogo já abertas (chave = o próprio array de bytes, que fica guardado no cache de capas). |
| `Vidro.kt` | 184 | Vidro líquido (igual ao exemplo em HTML): o que está ATRÁS do painel aparece através dele com |
| `VozesNovely.kt` | 76 | Vozes do Novely: vozes neurais que vêm do próprio app (baixadas uma vez, funcionam sem internet). |

## `ui/theme/`

| Arquivo | Linhas | O que é |
|---|---|---|
| `Theme.kt` | 162 | Serifa editorial (títulos grandes). Só tem um peso: os pesos fortes usam o mesmo arquivo, sem negrito falso. |

## Raiz do pacote

| Arquivo | Linhas | O que é |
|---|---|---|
| `MainActivity.kt` | 137 | Arquivo aberto com o Novely (WhatsApp, gerenciador de arquivos…), esperando ser tratado. |

## Testes (`app/src/test`)

- `com/huryel/novely/data/BibliaTest.kt`
- `com/huryel/novely/data/CamposFichaTest.kt`
- `com/huryel/novely/data/LimitesTest.kt`
- `com/huryel/novely/data/MarcasTest.kt`
- `com/huryel/novely/data/TextoTest.kt`
- `com/huryel/novely/ui/EfeitosEscritaTest.kt`
- `com/huryel/novely/ui/MacacoTest.kt`
- `com/huryel/novely/ui/PainelEfeitoTest.kt`
- `com/huryel/novely/ui/TelasTest.kt`
