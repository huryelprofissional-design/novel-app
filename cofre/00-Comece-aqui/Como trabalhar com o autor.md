---
tags: [regras]
---
# Como trabalhar com o autor

O autor (dono do projeto) é brasileiro e não é programador. Ele testa no celular (Android, tela OLED) e no PC.

## Conversa
- **Responder em português, curto e direto.** Sem jargão técnico; quando precisar, explicar em palavras simples.
- **Mostrar mockup antes de mudança grande de visual** (HTML com prévia, ou print). Quando ele mesmo manda o conceito (PDF/PNG), segue o conceito.
- **Nunca dizer que algo funciona sem ter testado.** Dizer exatamente o que foi testado e o que não foi (ex.: "compilou e os testes passaram, mas não vi a tela no celular").
- Quando ele pede para escolher entre opções, mandar uma página HTML com cartões selecionáveis e um botão "Copiar escolhidos" (foi assim com os 68 efeitos).
- Mensagens curtas dele ("Sim", "Prox", "faz pc") significam: siga com o próximo passo combinado.

## Entrega
- Toda mudança vai para **celular e PC** quando fizer sentido ("PC atualiza também").
- Depois de mudar: commit, push, esperar o CI ([[Build e CI]]) e só então avisar.
- No fim: o que mudou, o que foi testado, o que ele precisa conferir. Sem recapitular passo a passo.

## Segurança e privacidade
- Nunca repetir segredos (senhas, keystore, tokens). Se precisar citar, mascarar: `AIzaSyDP••••uXSk`.
- O e-mail do autor só aparece na regra de admin do `firestore.rules`. Não mandar para nenhum serviço externo.
- A keystore e a senha ficam **só** nos *secrets* do GitHub (`KEYSTORE_B64`, `KEYSTORE_SENHA`). Nunca commitar.
- Ver [[Segurança]].
