---
tags: [android, pc, leitor]
---
# Leitor

## Como desenha
- Texto dividido em **blocos por parágrafo** (parágrafos gigantes partidos em ~1.500 caracteres) dentro de `LazyColumn`: só o visível é desenhado. Processamento fora da thread principal.
- Falas (linhas com `—`) ganham barra lateral e caixa (estilo de fala configurável).
- Efeitos: `lerBlocoFx` / `BlocoFx` (parágrafo) e `lerInlineFx` / `animarInlineFx` (trecho). Animação por quadro de 110 ms só nos blocos com efeito animado (`INLINE_ANIMADOS`). Ver [[Efeitos de texto]].
- **Fundo preto puro `#000`** no leitor e no editor (OLED). Feito em `CoresDeLeitura` (`ui/Poster.kt`). Caixas `#0D0D0D`, bordas `#1F1F1F`.
- "Reduzir animações" (`LocalSemMovimento`) congela os efeitos.

## HUD (barras)
- Cima: barra preta sólida, voltar, título em Instrument Serif, `CAP. X DE Y · FALTAM ~N MIN` em Plex Mono, ícones (tradução, sobre, editar se for minha) e linha de progresso na cor da obra com %.
- Baixo: Ouvir · Buscar · Capítulos · Rolar · Leitura. O usuário escolhe quais botões aparecem.
- Toque no texto mostra/esconde o HUD. Atalhos de teclado físico.

## Ouvir (voz)
- `data/Narrador.kt`: divide parágrafos em narração e falas e descobre quem fala (elenco: voz por personagem).
- Pode misturar voz do sistema (TTS do Android) e [[Vozes]] neurais por trecho.
- **Posição salva por parágrafo** (`guardarPosicao`): se cortar, continua de onde parou. "Ouvir a partir daqui" num parágrafo e botão "da tela".
- PC (`web/leitor.js`): começa do parágrafo visível (`blocoNaTela`), barra de voz com ⏮⏯⏭■, velocidade, ⚙ Vozes (narrador, voz por personagem, Vozes do Novely).

## Outros
Marcadores ancorados, busca no capítulo, rolagem automática, modo páginas (toque nas bordas), clima de cena (`~chuva` etc.), som ambiente gerado (`data/Ambiente.kt`), tradução.
