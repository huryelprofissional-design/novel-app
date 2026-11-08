// Novely para PC/navegador — mesma conta e mesmos dados do app Android.
import {
  E, $, $$, esc, raiz, agora, toast, milhar, haQuanto, recente, corHex, local, lista, capaHTML, preencherCapas, casca, carregando, rotas, rota, ir,
  auth, onAuthStateChanged, signInWithPopup, GoogleAuthProvider, signInWithEmailAndPassword, createUserWithEmailAndPassword, sendPasswordResetEmail, signOut,
  eu, colObras, getDoc, onSnapshot, doc, db, catalogo, urlBlob, notaTxt, imagemPub, imagemMinha, nomeAutor, novaObra, perguntar, dialogo,
  salvarPerfil, escolherArquivo, comprimirArquivo, enviarImagemMinha, publicarPerfil, STATUS_NOME, salvarObra, confirmar, apagarObra, perfilPub,
  segueAutor, CONQUISTAS, colItens, getDocs, query, where, obraPub, esc as escapar, carteira, diarioMoedas,
} from "./nucleo.js";
import { cartaoNivelHTML, ligarBau } from "./efeitos.js";
import "./v2.js";
import "./obra.js";
import * as X from "./extras.js";
import "./leitor.js";
import "./catalogo.js";
import "./ia.js";
import "./biblia.js";
import "./aovivo.js";

window.addEventListener("beforeinstallprompt", (e) => { e.preventDefault(); E.instalar = e; });

async function desenhar() {
  if (E.saindo) { const s = E.saindo; E.saindo = null; try { await s(); } catch (_) {} }
  window.onscroll = null; window.onkeydown = null;
  if (!E.usuario) return telaLogin();
  const h = location.hash.slice(1) || "/";
  for (const [rx, fn] of rotas) { const m = h.match(rx); if (m) { await fn(...m.slice(1).map((x) => x === undefined ? undefined : decodeURIComponent(x))); return; } }
  ir("/");
}
window.addEventListener("hashchange", desenhar);
window.redesenhar = desenhar;

export function cartao(o, sub) {
  return `<a class="obra-c" href="#/obra/${o.id}">${capaHTML(o, o.id, corHex(o.corBase))}<div class="nm">${esc(o.titulo)}</div>${sub ? `<div class="sb">${esc(sub)}</div>` : ""}</a>`;
}
window.cartaoLocal = cartao;

// ───────── LOGIN ─────────
function telaLogin() {
  raiz.innerHTML = `<div class="login"><div class="caixa vidro">
    <h1><span class="losango" style="width:30px;height:30px"></span>Novely</h1>
    <p class="suave" style="margin:0 0 18px">Escreva, publique e leia novels gigantes — no celular e no PC.</p>
    <button class="btn google" id="g"><b style="color:#4285F4">G</b>&nbsp; Continuar com Google</button>
    <div class="suave peq" style="text-align:center;margin:8px 0">ou com e-mail</div>
    <input class="campo" id="email" type="email" placeholder="E-mail" autocomplete="email">
    <input class="campo" id="senha" type="password" placeholder="Senha (mínimo 6)" autocomplete="current-password">
    <div style="display:flex;gap:10px"><button class="btn" style="flex:1" id="entrar">Entrar</button><button class="btn2" id="criar">Criar conta</button></div>
    <a class="suave peq" href="javascript:void 0" id="esqueci">Esqueci a senha</a>
    <div class="erro peq" id="msg"></div>
    <div class="baixar-android"><span class="suave peq">Tem Android?</span> <a class="btn2 peq" href="https://github.com/huryelprofissional-design/novel-app/releases/latest/download/novely.apk">⬇ Baixar o app</a></div></div></div>`;
  const msg = (t) => ($("#msg").textContent = t);
  const traduz = (e) => ({ "auth/invalid-credential": "E-mail ou senha incorretos.", "auth/wrong-password": "E-mail ou senha incorretos.", "auth/user-not-found": "Não achei conta com esse e-mail.",
    "auth/email-already-in-use": "Já existe conta com esse e-mail.", "auth/weak-password": "Senha fraca: use pelo menos 6 caracteres.", "auth/popup-closed-by-user": "",
    "auth/unauthorized-domain": "Este endereço ainda não foi liberado no Firebase (Domínios autorizados)." }[e.code] ?? ("Não deu certo: " + e.message));
  $("#g").onclick = () => signInWithPopup(auth, new GoogleAuthProvider()).catch((e) => msg(traduz(e)));
  $("#entrar").onclick = () => signInWithEmailAndPassword(auth, $("#email").value.trim(), $("#senha").value).catch((e) => msg(traduz(e)));
  $("#senha").onkeydown = (e) => { if (e.key === "Enter") $("#entrar").click(); };
  $("#criar").onclick = () => createUserWithEmailAndPassword(auth, $("#email").value.trim(), $("#senha").value).catch((e) => msg(traduz(e)));
  $("#esqueci").onclick = () => sendPasswordResetEmail(auth, $("#email").value.trim()).then(() => msg("Mandamos um link para o seu e-mail.")).catch((e) => msg(traduz(e)));
}

// ───────── peças do catálogo ─────────
export function posterHTML(o) {
  const url = urlBlob(o.capaMini);
  return `<a class="poster" href="#/pub/${o.id}" data-poster="${o.id}">${url ? `<img src="${url}" alt="">` : ""}
    <div class="autor vidro" data-autor="${esc(o.autorUid)}"><span class="av" data-av="${esc(o.autorUid)}">${esc((o.autorNome || "?").slice(0, 1).toUpperCase())}</span><div style="flex:1;min-width:0"><b>${esc(o.autorNome || "")}</b><small>${milhar(o.seguidores)} SEGUIDORES DA OBRA</small></div></div>
    <div class="baixo"><h3>${esc(o.titulo)}</h3><div class="info">${recente(o.atualizadaEm) ? `<span class="selo">NOVO CAP</span>` : ""}<span>● ${haQuanto(o.atualizadaEm)}</span><span>▶ ${milhar(o.leituras)}</span></div></div></a>`;
}
export function cartaoPub(o, pos) {
  const url = urlBlob(o.capaMini);
  return `<a class="obra-c" data-busca="${esc((o.titulo + " " + o.autorNome + " " + (o.tags || []).join(" ") + " " + (o.generos || []).join(" ")).toLowerCase())}" href="#/pub/${o.id}">
    <div class="capa">${pos ? `<span class="pos">${pos}</span>` : `<span class="nota">${notaTxt(o)}</span>`}${o.classificacao === "18" ? `<span class="mais18">+18</span>` : ""}${url ? `<img src="${url}">` : `<div class="sol"></div><div class="t">${esc(o.titulo)}</div>`}</div>
    <div class="nm">${esc(o.titulo)}</div><div class="sb">${esc(o.autorNome || "")} · ${o.numCaps || 0} caps</div></a>`;
}
window.posterHTML = posterHTML; window.cartaoPub = cartaoPub;
/** Troca a capa pequena dos pôsteres pela grande e põe a foto dos autores. */
export async function enfeitarPosteres() {
  for (const el of raiz.querySelectorAll("[data-poster]")) {
    const url = await imagemPub(el.dataset.poster, "capa");
    if (url) { let img = el.querySelector(":scope > img"); if (!img) { img = document.createElement("img"); el.prepend(img); } img.src = url; }
  }
  for (const a of raiz.querySelectorAll("[data-av]")) { const p = await perfilPub(a.dataset.av); if (p?.foto) a.innerHTML = `<img src="${urlBlob(p.foto)}">`; }
}
window.enfeitarPosteres = enfeitarPosteres;
document.addEventListener("click", (e) => { const a = e.target.closest("[data-autor]"); if (a && a.closest(".poster")) { e.preventDefault(); ir("/autor/" + a.dataset.autor); } });

// ───────── BIBLIOTECA ─────────
rota("/biblioteca(?:/(\\w+))?", async (aba = "LENDO") => {
  const outros = lista().filter((o) => !o.minha);
  const grupos = { LENDO: outros.filter((o) => o.status === "LENDO"), QUERO: outros.filter((o) => o.status === "QUERO"), LIDOS: outros.filter((o) => o.status === "CONCLUIDA"), ARQUIVOS: outros.filter((o) => !o.trancada) };
  const nomes = { LENDO: "Lendo", QUERO: "Quero ler", LIDOS: "Lidos", ARQUIVOS: "Meus arquivos" };
  const ord = local.ler("ordBib", "RECENTE");
  const colecoes = [...new Set(outros.map((o) => o.colecao).filter(Boolean))];
  const col = local.ler("colBib", "");
  let l = (grupos[aba] || []).filter((o) => !col || o.colecao === col);
  l.sort(ord === "TITULO" ? (a, b) => a.titulo.localeCompare(b.titulo) : ord === "ADICIONADA" ? (a, b) => (b.criadaEm || 0) - (a.criadaEm || 0) : (a, b) => (b.lidaEm || 0) - (a.lidaEm || 0));
  casca("biblioteca", `<div class="topo"><h1>Estante</h1><input class="campo" id="filtro" placeholder="Filtrar…" style="max-width:240px;border-radius:24px">
      <button class="btn2 peq" id="modoEst" title="Estante ou grade">${local.ler("estante", true) ? "📚 Estante" : "▦ Grade"}</button><select class="campo" id="ord" style="max-width:190px;border-radius:24px"><option value="RECENTE">Lidas recentemente</option><option value="TITULO">Título (A–Z)</option><option value="ADICIONADA">Adicionadas</option></select></div>
    <div class="abas">${Object.keys(nomes).map((k) => `<a href="#/biblioteca/${k}" class="${k === aba ? "on" : ""}">${nomes[k]} · ${grupos[k].length}</a>`).join("")}</div>
    ${colecoes.length ? `<div class="chips" style="margin-bottom:18px"><span class="chip ${!col ? "on" : ""}" data-col="">Todas</span>${colecoes.map((c) => `<span class="chip ${c === col ? "on" : ""}" data-col="${esc(c)}">${esc(c)}</span>`).join("")}</div>` : ""}
    ${l.length ? `<div class="grade ${local.ler("estante", true) ? "estante" : ""}" id="grade">${l.map((o, i) => cartao(o, o.autor ? "por " + o.autor : STATUS_NOME[o.status] || "").replace('class="obra-c"', `class="obra-c ${o.status === "CONCLUIDA" ? "lida" : ""}" style="--i:${i % 18}"`)).join("")}</div>`
      : `<div class="card vazio"><b>Nada aqui ainda.</b><span class="suave">Encontre obras em Explorar e toque em ♥ Seguir para elas aparecerem aqui.</span><a class="btn peq" href="#/descobrir">Explorar</a></div>`}`);
  $("#modoEst").onclick = () => { local.gravar("estante", !local.ler("estante", true)); desenhar(); };
  $("#ord").value = ord; $("#ord").onchange = (e) => { local.gravar("ordBib", e.target.value); desenhar(); };
  raiz.querySelectorAll("[data-col]").forEach((c) => (c.onclick = () => { local.gravar("colBib", c.dataset.col); desenhar(); }));
  $("#filtro").oninput = (e) => { const t = e.target.value.toLowerCase(); for (const a of ($("#grade")?.children || [])) a.style.display = a.textContent.toLowerCase().includes(t) ? "" : "none"; };
  preencherCapas();
});

// ───────── AJUSTES ─────────
rota("/ajustes", async () => {
  casca("perfil", `<div class="topo"><a class="voltar vidro" href="#/perfil">←</a><h1>Ajustes</h1></div>
    <div class="lista-ajustes">
      <div class="card"><b>Conta</b><div class="suave peq" style="margin:4px 0 12px">${esc(E.usuario.email || "")} — tudo fica salvo na nuvem e aparece no celular.</div><button class="btn2 peq" id="sair">Sair da conta</button></div>
      <div class="card"><b>📱 Aparelhos conectados</b><div id="aparelhos" class="suave peq" style="margin-top:8px">Carregando…</div><div class="suave peq" style="margin-top:8px">Perdeu um celular? Desconecte aqui: ele sai da conta na próxima vez que ligar a internet.</div></div>
      <a class="card" href="#/lixeira"><b>🗑 Lixeira</b><div class="suave peq" style="margin-top:4px">Capítulos, fichas e obras apagados nos últimos 30 dias</div></a>
      <div class="card"><b>Conteúdo adulto</b><div class="suave peq" style="margin:4px 0 12px">Obras +18 pedem confirmação antes de abrir.</div>
        <label class="troca"><input type="checkbox" id="m18" ${local.ler("maior18", false) ? "checked" : ""}> Tenho 18 anos ou mais</label></div>
      <div class="card"><b>Instalar no Windows</b><div class="suave peq" style="margin-top:4px">No Chrome ou Edge: menu ⋯ › Aplicativos › Instalar este site como um aplicativo. O Novely abre como um programa e funciona sem internet com o que já foi aberto.</div></div>
      <div class="card"><b>App para Android</b><div class="suave peq" style="margin:4px 0 12px">Mesma conta, tudo sincronizado.</div><a class="btn2 peq" href="https://github.com/huryelprofissional-design/novel-app/releases/latest/download/novely.apk">⬇ Baixar APK</a></div>
      ${(E.usuario.email || "").toLowerCase() === "huryelprofissional@gmail.com" ? `<a class="card" href="#/dono"><b>Painel de dono</b><div class="suave peq" style="margin-top:4px">Obras do catálogo e pessoas</div></a>` : ""}
    </div>`);
  $("#sair").onclick = async () => { if (await confirmar("Sair da conta?", "Suas obras continuam salvas na nuvem.", "Sair")) { await X.sairDesteAparelho(); signOut(auth); } };
  const pintarAp = async () => {
    const caixa = $("#aparelhos"); if (!caixa) return;
    try { caixa.innerHTML = X.aparelhosHTML(await X.listaAparelhos()); } catch (_) { caixa.textContent = "Não deu para carregar agora."; }
    $$("[data-desc-ap]", caixa).forEach((b) => (b.onclick = async () => { if (!(await confirmar("Desconectar este aparelho?", "Ele sai da conta na próxima vez que tiver internet. As obras continuam na nuvem.", "Desconectar", true))) return; await X.desconectarAparelho(b.dataset.descAp); pintarAp(); }));
  };
  pintarAp();
  $("#m18").onchange = (e) => local.gravar("maior18", e.target.checked);
});

// prévia: parar o mouse em cima de um pôster ou capa do catálogo mostra sinopse e primeira frase
let tempoPrevia = null, previaAberta = null;
document.addEventListener("mouseover", (e) => {
  const a = e.target.closest?.('[data-poster], a.obra-c[href^="#/pub/"]');
  if (!a || a === previaAberta?.alvo) return;
  clearTimeout(tempoPrevia);
  tempoPrevia = setTimeout(async () => {
    const pid = a.dataset.poster || a.getAttribute("href").slice(6);
    const o = await obraPub(pid).catch(() => null); if (!o || !a.matches(":hover")) return;
    document.querySelectorAll(".previa").forEach((x) => x.remove());
    const el = document.createElement("div"); el.className = "previa vidro";
    el.innerHTML = `<b>${esc(o.titulo)}</b><div class="suave peq">${esc((o.generos || []).slice(0, 2).join(" · "))} · ${o.numCaps || 0} caps · ${esc(o.autorNome)}</div><p>${esc((o.sinopse || "Sem sinopse.").slice(0, 320))}</p><div class="suave peq" id="pv-inicio"></div>`;
    document.body.appendChild(el);
    const r = a.getBoundingClientRect(); const w = 320;
    el.style.left = Math.min(innerWidth - w - 12, r.right + 12 > innerWidth - w ? Math.max(12, r.left - w - 12) : r.right + 12) + "px";
    el.style.top = Math.max(12, Math.min(innerHeight - el.offsetHeight - 12, r.top)) + "px";
    previaAberta = { alvo: a, el };
    try {
      const { capsPub, lerTexto, collection, db, limparMarcas } = await import("./nucleo.js");
      const c = (await capsPub(pid))[0];
      if (c) { const t = await lerTexto(collection(db, "publicas", pid, "textos"), c.id, 1); const l = limparMarcas(t).split("\n").map((x) => x.trim()).find((x) => x.length > 30 && !x.startsWith("#")); if (l && el.isConnected) el.querySelector("#pv-inicio").textContent = "“" + l.slice(0, 260) + "…”"; }
    } catch (_) {}
  }, 650);
});
document.addEventListener("mouseout", (e) => {
  const a = e.target.closest?.('[data-poster], a.obra-c[href^="#/pub/"]'); if (!a || a.contains(e.relatedTarget)) return;
  clearTimeout(tempoPrevia);
  if (previaAberta?.alvo === a) { previaAberta.el.remove(); previaAberta = null; }
});
window.addEventListener("hashchange", () => { document.querySelectorAll(".previa").forEach((x) => x.remove()); previaAberta = null; });

// brilho do vidro acompanha o mouse
document.addEventListener("pointermove", (e) => {
  const v = e.target.closest?.(".vidro"); if (!v) return;
  const b = v.getBoundingClientRect();
  v.style.setProperty("--mx", ((e.clientX - b.left) / b.width * 100).toFixed(1) + "%");
  v.style.setProperty("--my", ((e.clientY - b.top) / b.height * 100).toFixed(1) + "%");
});

// ───────── início do app ─────────
let pararObras = null;
onAuthStateChanged(auth, async (u) => {
  E.usuario = u;
  if (pararObras) { pararObras(); pararObras = null; }
  E.obras = new Map();
  if (!u) return desenhar();
  try { E.perfil = (await getDoc(eu())).data() || {}; } catch (_) { E.perfil = {}; }
  if (!E.perfil.nome && u.displayName) E.perfil.nome = u.displayName;
  X.registrarAparelho(); clearInterval(window.__vidaAp); window.__vidaAp = setInterval(() => X.registrarAparelho(), 180000);
  carteira(true).then(() => diarioMoedas()).then((n) => n && toast(`🪙 +${n} moedas de hoje`)).catch(() => {});
  let primeira = true;
  pararObras = onSnapshot(colObras(), (s) => {
    s.docChanges().forEach((ch) => { if (ch.type === "removed") E.obras.delete(ch.doc.id); else E.obras.set(ch.doc.id, ch.doc.data()); });
    const h = location.hash;
    // redesenha listas quando algo muda (não interrompe leitor, editor nem janelas abertas)
    if (primeira || (!/^#\/(ler|editar|pub|publer|obra|fichas|tempo|publicar|revisao|colab)/.test(h) && !document.querySelector("dialog[open]"))) desenhar();
    primeira = false;
    window.verificarNovidades?.();
  }, () => desenhar());
});

if ("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(() => {});
