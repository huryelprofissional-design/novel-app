# Novely — Documento de decisões

App Android para ler, escrever, organizar e **publicar** novels enormes. Funciona sem internet; com conta, sincroniza com a nuvem e publica no catálogo.

## Requisitos aprovados
- Ler e escrever capítulos de 20 mil a 100 mil+ palavras sem travar.
- Funciona sem internet (o banco do aparelho é o principal). Conta obrigatória (Google ou e-mail) para sincronizar e publicar.
- Rolagem contínua **e** modo páginas (por toque nas bordas).
- Editor com Markdown simples: `**negrito**`, `*itálico*`, `# título`, `* * *` (troca de cena).
- Importar **TXT e EPUB**, conferindo os capítulos antes de salvar.
- Busca no capítulo e em todas as obras.
- Leitura em voz alta (motor de voz do Android).
- Capa e banner com imagem própria e ajuste de enquadramento.
- Backup independente de conta online.

## Design (aprovado)
- Só o tema **Noite**: preto puro, destaque âmbar `#E9A65A`, Inter na interface e Newsreader na leitura.
- Abas: **Início · Biblioteca · Escrever · Descobrir · Perfil** (barra some ao rolar). Topo: lupa, sino, foto. Ajustes na engrenagem do Perfil.
- Mockups aprovados: `scratchpad/v3` (8 telas).
- Títulos em serifa, interface em sem-serifa. Leitura em serifa (configurável).
- **Falas de personagens** (linhas começando com `—`, `–`, `-`, `“`, `"`, `«`) ganham travessão âmbar e barra lateral, para o leitor ver que alguém está falando. Pode ser desligado.
- Capas sem imagem recebem arte geométrica gerada a partir do título.
- Mockups: canvas "Novel App — Propostas" (claude.ai).

## Arquitetura
- Kotlin + Jetpack Compose (Material 3), minSdk 26, targetSdk 35.
- **Room** para metadados (obras, volumes, capítulos, progresso). **Texto dos capítulos em arquivos `.md` separados** (`files/textos/<id>.md`), assim listas e consultas nunca carregam texto.
- Gravação **atômica**: escreve `.tmp` → `fsync` → renomeia; a versão anterior fica como `.bak`.
- Ordem dos capítulos em número fracionário: inserir/mover não renumera a obra inteira.
- Leitor: texto dividido em **blocos por parágrafo** (parágrafos gigantes partidos em ~1.500 caracteres) dentro de uma `LazyColumn`: só o que aparece na tela é desenhado. O processamento roda fora da thread principal.
- Editor: o capítulo é dividido em **pedaços de ~3.000 caracteres**, cada um num campo próprio dentro de uma lista preguiçosa. Juntar os pedaços devolve exatamente o texto (testado). Salva 1,5 s depois da última tecla, ao sair da tela e ao minimizar o app.
- Histórico: antes de sobrescrever, guarda uma cópia (no máx. a cada 10 min, até 30 por capítulo).
- Importação em streaming: cada capítulo detectado vai direto para um arquivo temporário; nada do livro inteiro fica na memória. EPUB lido pelo `spine` do OPF.
- Busca: varredura dos arquivos sem índice separado (sem duplicar o texto no disco e sem risco de índice desatualizado). Ignora acentos e maiúsculas, entrega resultados aos poucos.
- Backup `.novely` = ZIP com `manifest.json` + `textos/*.md` + imagens, com CRC de cada capítulo. Restaurar nunca sobrescreve (cada obra entra como nova; duplicatas vêm desmarcadas).
- Build: GitHub Actions compila o APK a cada envio e publica em Releases. Assinatura fixa para que atualizações instalem por cima.

## Estrutura dos dados
- `obras`: título, descrição, status (ESCREVENDO/LENDO/CONCLUIDA), capa/banner + enquadramento (x, y, zoom), cor, último capítulo lido.
- `volumes`: obra, título, ordem.
- `capitulos`: obra, volume (opcional), título, ordem, rascunho, palavras, progresso, posição de leitura.

## Implementado
- [x] A — Projeto, navegação, tema
- [x] B — Banco local, estante, obras, capa/banner, volumes e capítulos
- [x] C — Importar TXT/EPUB com conferência
- [x] D — Leitor: blocos, falas destacadas, voz, busca, modo páginas, ajustes "Aa"
- [x] E — Editor em pedaços, salvamento automático, histórico de versões
- [x] F — Busca global
- [x] G — Backup, restauração, exportar EPUB/TXT, backup automático diário
- [~] H — Visual dinâmico (início com destaque, animações, capas geradas), testes

## Desempenho medido (servidor do GitHub, não no celular)
Testes com texto sintético em `app/src/test`: contar palavras, dividir em blocos e buscar em 5 mil a 100 mil palavras levam de 1 a 30 ms. Medições no aparelho real ainda não foram feitas.

## Problemas conhecidos / limitações
- EPUBs com DRM não podem ser lidos.
- A qualidade da voz depende das vozes instaladas no celular.
- O modo páginas rola uma tela por toque, não diagrama páginas como um livro impresso.
- Uma frase que atravessa dois pedaços do editor não pode ser selecionada de uma vez.

## Próximas tarefas
- Ajustes conforme o uso real no celular.
- Medir abertura de capítulos grandes no aparelho.


## Personagens e lugares no texto
- `@Nome, descrição…` cria/completa a ficha do personagem; `&Lugar, …` do lugar; `@Nome (apelido1, apelido2), …` guarda apelidos.
- Sem vírgula/travessão depois do nome = só menção. No leitor e nas exportações as marcas somem (`Marcas.limpar`).
- Os parágrafos de definição viram trechos da ficha por capítulo: o leitor só vê o que já leu (sem spoiler).

## Conta e nuvem (Firebase, plano grátis)
- Projeto `novely-novelapp-48fe7`. Login Google (Credential Manager) e e-mail/senha.
- `users/{uid}/obras|itens|textos|imagens`. Imagens comprimidas dentro do banco (sem Storage, que exige cartão).
- Gatilhos SQLite marcam `alteradoEm`/`nuvemId` e guardam exclusões; a sincronização grava com `sync_flag=1` para não reenviar.
  (Gatilhos v2: nunca se repetem mesmo com `recursive_triggers` ligado — v1 fechava o app no Android 16.)
- Conflito de texto (mudou nos dois aparelhos): fica o daqui, o outro vai para o Histórico e o sino avisa.

## Publicação
- Catálogo `publicas/{obra}` (id = nuvemId da obra do autor). Capítulo por capítulo, agendamento, republicar manual.
- Leitores baixam a obra inteira (só leitura, offline). Seguir, estrelas, curtidas, comentários, leituras (1 por capítulo por pessoa), ranking semanal.
- Obras de outros autores importadas por EPUB/TXT nunca vão para o catálogo.
- Regras do Firestore: `scratchpad/regras-firestore.txt`.

## Atualização do app
- Repositório público; o app consulta `releases/latest` ao abrir (no máx. a cada 15 min), mostra janela e instala por cima.
