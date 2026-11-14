# Novely — instruções para o Claude Code

App de ler/escrever/publicar webnovels. **Android** (Kotlin + Compose, pasta `app/`) e **PC** (JavaScript puro, pasta `web/`, GitHub Pages), mesma nuvem Firebase.

Todo o conhecimento do projeto está no cofre do Obsidian em **`cofre/`**. Comece por `cofre/00-Comece-aqui/Início.md` e leia a nota da área antes de mexer nela.

@cofre/00-Comece-aqui/Como trabalhar com o autor.md
@cofre/90-Historico/Pendências.md

## Regras rápidas
- Responder ao autor em **português, curto**. Mostrar mockup antes de mudança visual grande. **Nunca dizer que funciona sem testar**; dizer o que foi testado.
- Mudança de recurso vai para **celular e PC** com os mesmos nomes (ex.: efeitos: `ui/EfeitosNovos.kt` + `web/fx.js`). Receita: `cofre/50-Recursos/Como criar um efeito novo.md`.
- Não há Android SDK local: compilar = `git push` e esperar o CI (`bash cofre/60-Operacao/scripts/ci-apk.sh`; site: `ci-site.sh`). Ver `cofre/60-Operacao/Build e CI.md`.
- PC: subir `CACHE` em `web/sw.js` a cada mudança em `web/`.
- Nunca commitar nem repetir segredos (`KEYSTORE_B64`, `KEYSTORE_SENHA`). O e-mail do autor só existe em `firestore.rules`.
- Ao mudar efeitos ou formato do texto, atualizar `cofre/50-Recursos/Maestro-prompt.txt` (e a nota) e regenerar `Catálogo de efeitos.md`.
- **Mantenha o cofre em dia**: ao terminar uma tarefa, atualize `Pendências.md`, `Decisões.md` e a nota da área.
