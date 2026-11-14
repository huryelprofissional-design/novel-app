---
tags: [recursos, voz]
---
# Vozes

## Vozes do sistema
Celular: TTS do Android (todas as vozes instaladas). PC: `speechSynthesis` (todas as vozes do sistema; narrador e voz por personagem, salvas em `narradorPC` e `vozPers:<id>`).

## Vozes do Novely (neurais, offline)
Biblioteca **sherpa-onnx 1.13.8**. Ids `novely:<nome>`:

| Id | Pacote | Tipo |
|---|---|---|
| `novely:faber` | piper-faber | masculina, clara (Piper pt_BR medium) |
| `novely:cadu` | piper-cadu | masculina, grave |
| `novely:jeff` | piper-jeff | masculina, jovem |
| `novely:edresson` | piper-edresson | masculina, leve (modelo *low*, o mais rápido) |
| `novely:dora-jovem` | kokoro, sid 1 | feminina (Dora 60% + Heart 40%) |
| `novely:dora-expressiva` | kokoro, sid 2 | feminina (Dora + Bella) |
| `novely:dora-sussurrada` | kokoro, sid 3 | feminina (Dora + Nicole) |

- Pacotes no release **`vozes-v1`** do GitHub (prerelease, não é "latest"): `espeak.zip`, `piper-*.zip`, `kokoro.zip` (com `vozes-novely.bin`: linhas 0–3 do voices.bin trocadas pelas misturas; Kokoro com `lang pt-br`). Montados por `.github/workflows/vozes.yml`.
- **Celular** (`data/VozNeural.kt`, `ui/VozesNovely.kt`): baixa com retomada para `files/vozes/<pacote>`, proteção zip slip, marcador `.ok`. AAR baixado no CI e conferido por sha256 `633c2432…bd96`. `Narrador.falarMisto` toca trecho a trecho misturando neural e sistema; prepara o parágrafo atual e o próximo.
- **PC** (`web/vozneural.js`, `web/voz/tts-worker.js`, `web/voz/fonemas-worker.js`): WebAssembly. O workflow `site.yml` baixa o wasm do k2-fsa, corrige `loadPackage({files:[],remote_package_size:0})`, cria `.data` vazio, copia `piper-phonemize`, e copia os pacotes para `web/vozes/<pacote>/` partindo arquivos > 90 MB em partes com manifesto `.partes`. Fonemas via espeak-ng WASM → `generateWithConfig("", {phonemeCodepoints, sid, speed})`. Guardado em Cache Storage `novely-vozes-v1`.
  - Até 3 geradores em paralelo (núcleos − 1 no Piper, − 2 no Kokoro), frases de até ~140 letras, primeiro pedaço até a primeira vírgula, aquecimento ao apertar Ouvir.
  - Medido em navegador sem tela: Piper ~3,3 s para 8,9 s de áudio; Kokoro ~31 s para 9,4 s (muito pesado no PC).

## Situação
- O autor achou o PC **"muito lento"** mesmo depois das melhorias; perguntamos qual voz, se o problema é começar ou pausas, e quantos núcleos o PC tem — **sem resposta**. Ideias se voltar: modelos Piper *x_low/low*, onnxruntime-web com WebGPU, usar a voz do sistema enquanto a neural aquece.
- Vozes no celular: implementadas e compiladas, **sem retorno do autor** num aparelho real.
