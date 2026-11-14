---
tags: [seguranca]
---
# Segurança

## Auditoria (nov/2026) — ordem "hacker, macaco e mestre"
1. **Hacker** (red team): [[Regras do Firestore]] reescritas (FB-02…FB-06), limites de ZIP (`data/Limites.kt`: texto 64 MB, imagem 25 MB por entrada; proteção contra zip slip), `FLAG_SECURE` aprovado, flavor `play` sem instalar pacotes, rotação da chave de assinatura ([[Assinatura do APK]]).
2. **Macaco** (monkey test): `MacacoTest` com 3.000 textos caóticos + capítulo de 10 MB; ações sociais em fila/Mutex.
3. **Mestre** (código limpo): feito em `Nuvem.kt`, `Publicacao.kt`, `Colaboracao.kt`, `web/nucleo.js`. **Falta: `TelaEditor.kt` e o resto** (ver [[Pendências]]).

Decisões do autor: 1A (rotação de chave com linhagem), 2 sim (flavors), 3 sim (FLAG_SECURE).

## Segredos
- GitHub *secrets*: `KEYSTORE_B64` (keystore em base64) e `KEYSTORE_SENHA`. Nunca no código, nunca repetir na conversa.
- A chave `apiKey` em `web/config.js` (termina em `…uXSk`) **é pública por natureza** (config do Firebase web). Em 2026-11-08 chegou um e-mail de "Croesus Audit" dizendo que ela estava exposta: é aviso automático de venda — **não clicar nos links nem rodar `git filter-repo`**. A proteção certa: no Google Cloud Console, restringir a chave a referenciadores HTTP (`https://huryelprofissional-design.github.io/*`, `https://novely-novelapp-48fe7.firebaseapp.com/*`, `http://localhost/*`) e às APIs Identity Toolkit, Token Service, Cloud Firestore e Firebase Installations. Não apagar a chave (o site para). *Não confirmado se o autor já fez.*
- O e-mail do autor aparece só na regra `admin()`.

## Privacidade
Dados privados ficam em `users/{uid}` (só o dono). Perfis e obras publicadas são públicos para logados.
