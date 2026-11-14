---
tags: [operacao, seguranca]
---
# Assinatura do APK

- Chave antiga: `app/novel-debug.keystore` (no repo, senha no `build.gradle.kts`) — usada desde o começo para as atualizações instalarem por cima.
- Chave nova (nov/2026, decisão 1A): **rotação APK Signature Scheme v3 com linhagem** (`apksigner rotate` + `sign --lineage --rotation-min-sdk-version 28`). Aparelhos com Android 9+ passam a confiar na chave nova; os antigos continuam com a antiga. Certificado novo: SHA-256 começa com `8329…` e termina com `…029f` (conferido no build 131).
- A keystore nova e a senha estão nos *secrets* `KEYSTORE_B64` e `KEYSTORE_SENHA`. **O autor guarda o arquivo e a senha; nunca commitar nem repetir.**
- Sem os secrets, o CI assina só com a antiga e faz um "ensaio" de rotação com chave descartável.
