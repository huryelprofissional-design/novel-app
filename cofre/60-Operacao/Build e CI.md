---
tags: [operacao]
---
# Build e CI

**Não há Android SDK no ambiente do Claude**: o APK é compilado só no GitHub Actions. Fluxo padrão:

```bash
git add -A && git commit -m "mensagem em português" && git push origin main
bash cofre/60-Operacao/scripts/ci-apk.sh    # espera o "Gerar APK" do último commit e mostra erros
bash cofre/60-Operacao/scripts/ci-site.sh   # idem para o "Site (PC)" (só roda se mudou web/)
```
Os scripts leem a API pública do GitHub (repo público) e mostram `RESULTADO: success|failure (run N)` + as anotações de erro do Kotlin. Rodar a partir da raiz do repo (ou com `REPO=/caminho`). Um build leva ~6–9 min.

## Workflows (`.github/workflows/`)
| Arquivo | Nome | Quando | O que faz |
|---|---|---|---|
| `build.yml` | Gerar APK | push na main | baixa o AAR do sherpa-onnx (sha256), `gradle assembleGithubRelease testGithubReleaseUnitTest processPlayReleaseMainManifest`, assina com rotação v3 ([[Assinatura do APK]]), publica release `build-N` com `novely.apk` (o atualizador do app lê daqui) |
| `site.yml` | Site (PC) | push em `web/**` | monta `web/voz/` e `web/vozes/` ([[Vozes]]) e publica no GitHub Pages |
| `regras.yml` | Regras do Firestore | mudança nas regras | roda `regras/teste.mjs` no emulador do Firebase |
| `vozes.yml` | Pacotes de voz | manual / mudança no próprio arquivo | monta os pacotes de voz e publica no release `vozes-v1` |
| `emulador.yml` | Teste no emulador | manual ou depois do build | instala o APK num emulador Android (KVM), abre e "cutuca", guarda prints no artefato `teste-emulador` |

## Commits
Mensagens em português, curtas, dizendo o que mudou para o usuário. Terminar com as linhas de atribuição que a sessão pedir.

## Limites do ambiente do Claude (nuvem)
- A rede passa por um proxy com lista de sites: `github.com`, `raw.githubusercontent.com`, npm e pip funcionam; o site no GitHub Pages, o console do Firebase e *assets* de release às vezes **não** (401/403). Conferir via logs do CI.
- `gh secret` não funciona pelo proxy: o autor cadastra os *secrets* à mão.
- O emulador do Firebase não baixa localmente: testes de regras só no CI.
