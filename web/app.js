// Novely para PC/navegador — mesma conta e mesmos dados do app Android.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import {
  getAuth, onAuthStateChanged, signInWithPopup, GoogleAuthProvider, signInWithEmailAndPassword,
  createUserWithEmailAndPassword, sendPasswordResetEmail, signOut, updateProfile,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import {
  initializeFirestore, persistentLocalCache, persistentMultipleTabManager, collection, doc, getDoc, getDocs, setDoc,
  onSnapshot, query, where, orderBy, limit, serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { firebaseConfig } from "./config.js";

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = initializeFirestore(app, { localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }) });

// ───────── utilidades ─────────
const $ = (s, r = document) => r.querySelector(s);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const raiz = $("#app");
const novoId = () => [...crypto.getRandomValues(new Uint8Array(16))].map((b) => b.toString(16).padStart(2, "0")).join("");
const agora = () => Date.now();
const PARTE = 300000;
function toast(t) { const d = document.createElement("div"); d.className = "toast"; d.textContent = t; document.body.appendChild(d); setTimeout(() => d.remove(), 2600); }
function milhar(n) { return n >= 1e6 ? (n / 1e6).toFixed(1).replace(".", ",") + " mi" : n >= 1e3 ? (n / 1e3).toFixed(1).replace(".", ",") + " mil" : String(n || 0); }
function palavrasFmt(n) { return n >= 1000 ? (n / 1000).toFixed(1).replace(".", ",").replace(",0", "") + " mil" : String(n); }

// Mesma assinatura de texto do app Android: CRC32 dos bytes UTF-8 + "-" + tamanho em bytes.
const TABELA = (() => { const t = new Uint32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
function hashTexto(s) {
  const b = new TextEncoder().encode(s); let c = 0xFFFFFFFF;
  for (let i = 0; i < b.length; i++) c = TABELA[(c ^ b[i]) & 0xFF] ^ (c >>> 8);
  return ((c ^ 0xFFFFFFFF) >>> 0).toString(16).padStart(8, "0") + "-" + b.length;
}
function contarPalavras(t) { const m = t.match(/[\p{L}\p{N}][\p{L}\p{N}'’\-]*/gu); return m ? m.length : 0; }
function partir(t) { if (!t) return [""]; const r = []; for (let i = 0; i < t.length; i += PARTE) r.push(t.slice(i, i + PARTE)); return r; }

// Marcas @Personagem e &Lugar somem na leitura (igual ao app).
const RX_MARCA = /(?<![\p{L}\p{N}@&])([@&])(\p{L}[\p{L}\p{N}'’\-]*(?:\s(?:(?:d[aeo]s?|von|van)\s)?\p{Lu}[\p{L}\p{N}'’\-]*){0,4})(\s*\(([^)\n]{1,80})\))?(\s*[,—–:])?/gu;
const limparMarcas = (s) => (s.includes("@") || s.includes("&")) ? s.replace(RX_MARCA, (_m, _s, nome, _a, _b, pont) => nome + (pont || "")) : s;

const ICONES = {
  inicio: '<path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/>',
  biblioteca: '<path d="M4 4h4v16H4zM10 4h4v16h-4zM16 5l3.5-1 2.5 15.5-3.5 1z"/>',
  escrever: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="M13.5 6.5l4 4"/>',
  descobrir: '<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/>',
  perfil: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
};
const svg = (n) => `<svg viewBox="0 0 24 24">${ICONES[n]}</svg>`;

// ───────── estado ─────────
let usuario = null;
let obras = new Map();          // nuvemId -> dados da obra (users/{uid}/obras)
let perfil = {};
let pararObras = null;
const imagens = new Map();       // nome -> url (cache)
let instalar = null;
window.addEventListener("beforeinstallprompt", (e) => { e.preventDefault(); instalar = e; desenhar(); });

const eu = () => doc(db, "users", usuario.uid);
const colObras = () => collection(db, "users", usuario.uid, "obras");
const colItens = () => collection(db, "users", usuario.uid, "itens");

async function imagem(colecao, nome) {
  const chave = colecao.path + "/" + nome;
  if (imagens.has(chave)) return imagens.get(chave);
  let url = null;
  try {
    const d = await getDoc(doc(colecao, nome));
    const b = d.exists() ? d.data().dados : null;
    if (b) url = URL.createObjectURL(new Blob([b.toUint8Array()], { type: "image/jpeg" }));
  } catch (_) {}
  imagens.set(chave, url);
  return url;
}
const imagemMinha = (nome) => imagem(collection(db, "users", usuario.uid, "imagens"), nome);

function capaHTML(o, id, cor) {
  return `<div class="capa" data-capa="${esc(id)}" style="--cor:${cor || "#3B2A4A"}"><div class="sol"></div><div class="t">${esc(o.titulo)}</div></div>`;
}
async function preencherCapas(el = raiz) {
  for (const c of el.querySelectorAll("[data-capa]")) {
    const id = c.dataset.capa; const o = obras.get(id);
    if (!o || !o.capaHash) continue;
    const url = await imagemMinha(id + "-capa");
    if (url) c.innerHTML = `<img src="${url}" alt="">`;
  }
}
const corHex = (n) => n ? "#" + (Number(n) & 0xFFFFFF).toString(16).padStart(6, "0") : "#3B2A4A";

async function capitulos(obraId) {
  const s = await getDocs(query(colItens(), where("obra", "==", obraId)));
  return s.docs.filter((d) => d.data().tipo === "capitulos" && !d.data().apagado).map((d) => ({ id: d.id, ...d.data().dados, alteradoEm: d.data().alteradoEm }))
    .sort((a, b) => (a.ordem || 0) - (b.ordem || 0));
}
async function lerTexto(base, id, partes) {
  let t = "";
  for (let i = 0; i < (partes || 1); i++) {
    const d = await getDoc(doc(base, i === 0 ? id : `${id}~${i}`));
    t += d.exists() ? (d.data().texto || "") : "";
  }
  return t;
}

// ───────── gravação (o celular recebe na próxima sincronização) ─────────
async function salvarObra(id, campos) {
  await setDoc(doc(colObras(), id), { ...campos, apagado: false, alteradoEm: agora(), servidorEm: serverTimestamp() }, { merge: true });
}
async function salvarCapitulo(obraId, capId, dados) {
  await setDoc(doc(colItens(), capId), { tipo: "capitulos", obra: obraId, apagado: false, alteradoEm: agora(), servidorEm: serverTimestamp(), dados }, { merge: true });
}
async function salvarTexto(obraId, cap, texto, titulo) {
  const partes = partir(texto);
  for (let i = 0; i < partes.length; i++) await setDoc(doc(db, "users", usuario.uid, "textos", i === 0 ? cap.id : `${cap.id}~${i}`), { texto: partes[i] });
  await salvarCapitulo(obraId, cap.id, { titulo, palavras: contarPalavras(texto), textoHash: hashTexto(texto), partes: partes.length, atualizadoEm: agora() });
  await salvarObra(obraId, { atualizadaEm: agora() });
}
async function novoCapitulo(obraId) {
  const caps = await capitulos(obraId);
  const id = novoId();
  const ordem = caps.length ? Math.max(...caps.map((c) => c.ordem || 0)) + 1 : 1;
  const especiais = caps.filter((c) => /^(pr[oó]logo|ep[ií]logo)/i.test(c.titulo || "")).length;
  await setDoc(doc(db, "users", usuario.uid, "textos", id), { texto: "" });
  await salvarCapitulo(obraId, id, {
    titulo: `Capítulo ${caps.length - especiais + 1}`, ordem, volume: "", rascunho: true, palavras: 0, progresso: 0, posicaoBloco: 0, posicaoDeslocamento: 0,
    atualizadoEm: agora(), resumo: "", notas: "", tags: "", textoHash: hashTexto(""), partes: 1, publicaId: "", publicadoEm: 0, publicadoHash: "",
  });
  return id;
}
async function novaObra(titulo) {
  const id = novoId();
  const cores = [0xFF3B2A4A, 0xFF4A2420, 0xFF14302E, 0xFF1C2440, 0xFF2B3A22, 0xFF4A1C34];
  await salvarObra(id, {
    titulo: titulo || "Sem título", descricao: "", status: "ESCREVENDO", capaHash: "", bannerHash: "", capaX: .5, capaY: .5, capaZoom: 1,
    bannerX: .5, bannerY: .5, bannerZoom: 1, corBase: cores[Math.floor(Math.random() * cores.length)], criadaEm: agora(), atualizadaEm: agora(), lidaEm: 0,
    colecao: "", minha: true, trancada: false, autor: "", ultimoCap: "", publicada: false, publicaId: "", generos: "", tagsPub: "",
    classificacao: "LIVRE", idioma: "pt", exportavel: true,
  });
  return id;
}

// ───────── roteamento ─────────
const rotas = [];
const rota = (padrao, fn) => rotas.push([new RegExp("^" + padrao + "$"), fn]);
let saindo = null; // função para salvar ao trocar de tela (editor)
async function desenhar() {
  if (saindo) { const s = saindo; saindo = null; await s(); }
  if (!usuario) return telaLogin();
  const h = location.hash.slice(1) || "/";
  for (const [rx, fn] of rotas) { const m = h.match(rx); if (m) { await fn(...m.slice(1).map(decodeURIComponent)); return; } }
  location.hash = "/";
}
window.addEventListener("hashchange", desenhar);

function casca(aba, html) {
  const itens = [["/", "inicio", "Início"], ["/biblioteca", "biblioteca", "Biblioteca"], ["/escrever", "escrever", "Escrever"], ["/descobrir", "descobrir", "Descobrir"], ["/perfil", "perfil", "Perfil"]];
  raiz.innerHTML = `<div class="casca"><nav class="lado"><div class="logo">Novely<i>.</i></div>
    ${itens.map(([h, ic, n]) => `<a class="item ${aba === ic ? "on" : ""}" href="#${h}">${svg(ic)}<span>${n}</span></a>`).join("")}
    <div class="rodape">${instalar ? `<button class="btn2 peq" id="instalar">⬇ Instalar no PC</button>` : ""}
      <div class="suave peq" style="padding:0 12px">${esc(perfil.nome || usuario.email || "")}</div></div></nav>
    <main class="conteudo">${html}</main></div>`;
  const b = $("#instalar"); if (b) b.onclick = async () => { await instalar.prompt(); instalar = null; desenhar(); };
}
const lista = () => [...obras.entries()].map(([id, o]) => ({ id, ...o })).filter((o) => !o.apagado);
function cartao(o, sub) {
  return `<a class="obra-c" href="#/obra/${o.id}">${capaHTML(o, o.id, corHex(o.corBase))}<div class="nm">${esc(o.titulo)}</div>${sub ? `<div class="sb">${esc(sub)}</div>` : ""}</a>`;
}

// ───────── LOGIN ─────────
function telaLogin() {
  raiz.innerHTML = `<div class="login"><div class="caixa">
    <h1>Novely<span style="color:var(--amb)">.</span></h1>
    <p class="suave" style="margin:0 0 18px">Escreva, publique e leia novels gigantes — no celular e no PC.</p>
    <button class="btn google" id="g"><b style="color:#4285F4">G</b>&nbsp; Continuar com Google</button>
    <div class="suave peq" style="text-align:center;margin:8px 0">ou com e-mail</div>
    <input class="campo" id="email" type="email" placeholder="E-mail" autocomplete="email">
    <input class="campo" id="senha" type="password" placeholder="Senha (mínimo 6)" autocomplete="current-password">
    <div style="display:flex;gap:10px"><button class="btn" style="flex:1" id="entrar">Entrar</button><button class="btn2" id="criar">Criar conta</button></div>
    <a class="suave peq" href="javascript:void 0" id="esqueci">Esqueci a senha</a>
    <div class="erro peq" id="msg"></div></div></div>`;
  const msg = (t) => ($("#msg").textContent = t);
  const traduz = (e) => ({ "auth/invalid-credential": "E-mail ou senha incorretos.", "auth/wrong-password": "E-mail ou senha incorretos.", "auth/user-not-found": "Não achei conta com esse e-mail.",
    "auth/email-already-in-use": "Já existe conta com esse e-mail.", "auth/weak-password": "Senha fraca: use pelo menos 6 caracteres.", "auth/popup-closed-by-user": "",
    "auth/unauthorized-domain": "Este endereço ainda não foi liberado no Firebase (Domínios autorizados)." }[e.code] ?? ("Não deu certo: " + e.message));
  $("#g").onclick = () => signInWithPopup(auth, new GoogleAuthProvider()).catch((e) => msg(traduz(e)));
  $("#entrar").onclick = () => signInWithEmailAndPassword(auth, $("#email").value.trim(), $("#senha").value).catch((e) => msg(traduz(e)));
  $("#criar").onclick = () => createUserWithEmailAndPassword(auth, $("#email").value.trim(), $("#senha").value).catch((e) => msg(traduz(e)));
  $("#esqueci").onclick = () => sendPasswordResetEmail(auth, $("#email").value.trim()).then(() => msg("Mandamos um link para o seu e-mail.")).catch((e) => msg(traduz(e)));
}

// ───────── INÍCIO ─────────
rota("/", async () => {
  const l = lista();
  const cont = l.filter((o) => o.lidaEm > 0 && o.ultimoCap).sort((a, b) => b.lidaEm - a.lidaEm)[0];
  const lendo = l.filter((o) => !o.minha && o.status === "LENDO").sort((a, b) => b.lidaEm - a.lidaEm);
  const minhas = l.filter((o) => o.minha && !o.trancada).sort((a, b) => b.atualizadaEm - a.atualizadaEm);
  casca("inicio", `<div class="topo"><h1>Boa leitura${perfil.nome ? ", " + esc(perfil.nome.split(" ")[0]) : ""}</h1></div>
    ${cont ? `<div class="card continuar" onclick="location.hash='#/ler/${cont.id}/${cont.ultimoCap}'">${capaHTML(cont, cont.id, corHex(cont.corBase))}
      <div style="flex:1"><div class="rotulo">Continuar lendo</div><div style="font-weight:800;font-size:18px;margin:4px 0">${esc(cont.titulo)}</div><div class="suave peq">Toque para voltar de onde parou</div></div>
      <span class="btn">▶</span></div>` : ""}
    ${lendo.length ? `<div class="sec"><h2>Na sua estante</h2><a class="suave peq" href="#/biblioteca">Ver tudo ›</a></div><div class="grade">${lendo.slice(0, 12).map((o) => cartao(o, "lendo")).join("")}</div>` : ""}
    ${minhas.length ? `<div class="sec"><h2>Escrevendo</h2><a class="suave peq" href="#/escrever">Ver tudo ›</a></div><div class="grade">${minhas.slice(0, 6).map((o) => cartao(o, "sua obra")).join("")}</div>` : ""}
    ${!l.length ? `<div class="card" style="margin-top:20px">Nenhuma obra ainda. Se você já usa o app no celular, abra ele uma vez para sincronizar — as obras aparecem aqui sozinhas.</div>` : ""}`);
  preencherCapas();
});

// ───────── BIBLIOTECA ─────────
rota("/biblioteca(?:/(\\w+))?", async (aba = "LENDO") => {
  const outros = lista().filter((o) => !o.minha);
  const grupos = { LENDO: outros.filter((o) => o.status === "LENDO"), QUERO: outros.filter((o) => o.status === "QUERO"), LIDOS: outros.filter((o) => o.status === "CONCLUIDA"), ARQUIVOS: outros.filter((o) => !o.trancada) };
  const nomes = { LENDO: "Lendo", QUERO: "Quero ler", LIDOS: "Lidos", ARQUIVOS: "Meus arquivos" };
  const l = (grupos[aba] || []).sort((a, b) => (b.lidaEm || 0) - (a.lidaEm || 0));
  casca("biblioteca", `<div class="topo"><h1>Biblioteca</h1></div>
    <div class="abas">${Object.keys(nomes).map((k) => `<a href="#/biblioteca/${k}" class="${k === aba ? "on" : ""}">${nomes[k]} · ${grupos[k].length}</a>`).join("")}</div>
    ${l.length ? `<div class="grade">${l.map((o) => cartao(o, o.autor ? "por " + o.autor : "")).join("")}</div>` : `<p class="suave">Nada aqui.</p>`}`);
  preencherCapas();
});

// ───────── ESCREVER ─────────
rota("/escrever", async () => {
  const minhas = lista().filter((o) => o.minha && !o.trancada).sort((a, b) => b.atualizadaEm - a.atualizadaEm);
  casca("escrever", `<div class="topo"><h1>Escrever</h1><button class="btn" id="nova">+ Nova obra</button></div>
    ${minhas.length ? `<div class="grade">${minhas.map((o) => cartao(o, o.publicada ? "● publicada" : "em andamento")).join("")}</div>` : `<p class="suave">Crie sua primeira obra.</p>`}
    <div class="card" style="margin-top:28px"><b>Dica:</b> <span class="suave">escreva <b>@Nome, descrição…</b> para criar um personagem e <b>&amp;Lugar, …</b> para um lugar. O app do celular monta as fichas sozinho.</span></div>`);
  $("#nova").onclick = async () => {
    const t = prompt("Título da nova obra:"); if (t === null) return;
    const id = await novaObra(t.trim()); location.hash = `#/obra/${id}`;
  };
  preencherCapas();
});

// ───────── OBRA ─────────
rota("/obra/(\\w+)", async (id) => {
  const o = obras.get(id); if (!o) { location.hash = "/"; return; }
  casca(o.minha ? "escrever" : "biblioteca", `<div class="carregando">◆</div>`);
  const caps = await capitulos(id);
  const banner = o.bannerHash ? await imagemMinha(id + "-banner") : null;
  const pode = o.minha && !o.trancada;
  const alvo = o.ultimoCap && caps.find((c) => c.id === o.ultimoCap) ? o.ultimoCap : caps[0]?.id;
  const palavras = caps.reduce((s, c) => s + (c.palavras || 0), 0);
  const lidos = caps.filter((c) => (c.progresso || 0) >= .98).length;
  let especiais = 0;
  casca(pode ? "escrever" : "biblioteca", `
    <div class="topo"><a class="voltar" href="javascript:history.back()">←</a><div style="flex:1"></div></div>
    <div class="cab-obra"><div class="fundo" style="${banner ? `background-image:url(${banner})` : `background:${corHex(o.corBase)}`}"></div>
      <div class="frente">${capaHTML(o, id, corHex(o.corBase))}<div style="flex:1">
        <div class="rotulo">${pode ? (o.publicada ? "Publicada" : "Escrevendo") : o.trancada ? "Publicada · por " + esc(o.autor) : "Lendo"}</div>
        <h1>${esc(o.titulo)}</h1><div class="suave peq">${caps.length} caps · ${palavrasFmt(palavras)} palavras${caps.length ? ` · ${Math.round(lidos * 100 / caps.length)}% lido` : ""}</div>
        <div style="display:flex;gap:10px;margin-top:14px;flex-wrap:wrap">${alvo ? `<a class="btn" href="#/ler/${id}/${alvo}">▶ Ler</a>` : ""}
          ${pode ? `<button class="btn2" id="maiscap">+ Capítulo</button><button class="btn2" id="editinfo">Editar título e sinopse</button>` : ""}</div>
      </div></div></div>
    ${o.descricao ? `<p class="suave" style="line-height:1.6;max-width:760px">${esc(o.descricao)}</p>` : ""}
    <div class="sec"><h2>Capítulos</h2></div>
    <div class="caps">${caps.map((c) => {
      const esp = /^(pr[oó]logo|ep[ií]logo|interl[uú]dio|extra)/i.test(c.titulo || ""); if (esp) especiais++;
      const n = esp ? "◆" : caps.indexOf(c) + 1 - especiais;
      const lido = (c.progresso || 0) >= .98;
      return `<div class="cap" onclick="location.hash='#/ler/${id}/${c.id}'"><span class="n">${n}</span><span class="tt ${lido ? "lido" : ""}">${esc(c.titulo || "Sem título")}</span>
        <span class="suave peq">${c.rascunho && pode ? "rascunho · " : ""}${palavrasFmt(c.palavras || 0)} palavras</span>
        ${pode ? `<a class="btn2 peq" href="#/editar/${id}/${c.id}" onclick="event.stopPropagation()">✎</a>` : ""}</div>`;
    }).join("")}</div>`);
  preencherCapas();
  if (pode) {
    $("#maiscap").onclick = async () => { const c = await novoCapitulo(id); location.hash = `#/editar/${id}/${c}`; };
    $("#editinfo").onclick = () => {
      const d = document.createElement("dialog");
      d.innerHTML = `<h3 style="margin-top:0">Título e sinopse</h3><input class="campo" id="t" value="${esc(o.titulo)}"><textarea class="campo" id="s" rows="7" style="margin-top:10px">${esc(o.descricao || "")}</textarea>
        <div style="display:flex;gap:10px;justify-content:flex-end;margin-top:14px"><button class="btn2 peq" id="c">Cancelar</button><button class="btn peq" id="ok">Salvar</button></div>`;
      document.body.appendChild(d); d.showModal();
      $("#c", d).onclick = () => d.remove();
      $("#ok", d).onclick = async () => { await salvarObra(id, { titulo: $("#t", d).value.trim() || o.titulo, descricao: $("#s", d).value.trim(), atualizadaEm: agora() }); d.remove(); toast("Salvo"); desenhar(); };
    };
  }
});

// ───────── LEITOR ─────────
function htmlTexto(t, nomes = []) {
  const linhas = limparMarcas(t).split("\n").map((l) => l.trim()).filter(Boolean);
  const rxNomes = nomes.length ? new RegExp(`(?<![\\p{L}\\p{N}])(${nomes.map((n) => n.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|")})(?![\\p{L}\\p{N}])`, "gu") : null;
  return linhas.map((l) => {
    if (/^(\* \* \*|\*\*\*|---|◆)$/.test(l)) return `<div class="sep">◆ ◆ ◆</div>`;
    if (/^#/.test(l)) return `<h3>${esc(l.replace(/^#+\s*/, ""))}</h3>`;
    if (/^(cap[ií]tulo|pr[oó]logo|ep[ií]logo)\b/i.test(l) && l.length < 90) return `<h3>${esc(l)}</h3>`;
    let h = esc(l).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>").replace(/\*(.+?)\*/g, "<i>$1</i>");
    if (rxNomes) h = h.replace(rxNomes, '<span class="nome">$1</span>');
    const fala = /^[—–\-“"«]/.test(l);
    if (fala) h = h.replace(/^([—–\-“"«]\s?)/, '<b class="trav">$1</b>');
    return `<p class="${fala ? "fala" : ""}">${h}</p>`;
  }).join("");
}

rota("/ler/(\\w+)/(\\w+)", async (obraId, capId) => {
  const o = obras.get(obraId); if (!o) { location.hash = "/"; return; }
  casca(o.minha ? "escrever" : "biblioteca", `<div class="carregando">◆</div>`);
  const caps = await capitulos(obraId);
  const i = caps.findIndex((c) => c.id === capId); const c = caps[i]; if (!c) { location.hash = `#/obra/${obraId}`; return; }
  const texto = await lerTexto(collection(db, "users", usuario.uid, "textos"), capId, c.partes);
  let nomes = [];
  try {
    const f = await getDocs(query(colItens(), where("obra", "==", obraId)));
    nomes = f.docs.filter((d) => d.data().tipo === "fichas" && !d.data().apagado).flatMap((d) => [d.data().dados.nome, ...(d.data().dados.apelidos || "").split(",")]).map((s) => (s || "").trim()).filter((s) => s.length >= 3);
  } catch (_) {}
  const ant = caps[i - 1], prox = caps[i + 1];
  const tam = Number(localStorage.getItem("tam") || 20);
  const pode = o.minha && !o.trancada;
  casca(o.minha ? "escrever" : "biblioteca", `<div class="prog" id="prog"></div><div class="leitor" style="--tam:${tam}px">
    <div class="controles"><a class="voltar" href="#/obra/${obraId}">←</a><span class="tt">${esc(o.titulo)} · ${esc(c.titulo)}</span>
      <button class="btn2 peq" id="menos">A−</button><button class="btn2 peq" id="mais">A+</button>${pode ? `<a class="btn2 peq" href="#/editar/${obraId}/${capId}">✎ Editar</a>` : ""}</div>
    <div class="cabec"><div class="rotulo">${esc(o.titulo)}</div><h1>${esc(c.titulo || "Sem título")}</h1><div class="suave peq">${palavrasFmt(c.palavras || 0)} palavras · ◷ ${Math.max(1, Math.round((c.palavras || 0) / 230))} min</div></div>
    <div class="texto" id="texto">${htmlTexto(texto, nomes) || '<p class="suave">Capítulo vazio.</p>'}</div>
    <div class="fim"><div style="color:var(--amb)">◆</div><div class="rotulo" style="color:var(--suave)">Fim do capítulo</div>
      <div style="display:flex;gap:10px">${ant ? `<a class="btn2" href="#/ler/${obraId}/${ant.id}">‹ Anterior</a>` : ""}${prox ? `<a class="btn" href="#/ler/${obraId}/${prox.id}">Próximo: ${esc(prox.titulo)} ›</a>` : `<span class="suave">Último capítulo.</span>`}</div></div></div>`);
  const ajusta = (d) => { const n = Math.min(32, Math.max(14, tam + d)); localStorage.setItem("tam", n); desenhar(); };
  $("#menos").onclick = () => ajusta(-1); $("#mais").onclick = () => ajusta(1);
  // posição: volta onde parou e salva o progresso (o celular recebe)
  const pars = [...$("#texto").children];
  if (c.posicaoBloco > 1 && c.progresso < .98) pars[Math.min(pars.length - 1, c.posicaoBloco - 1)]?.scrollIntoView({ block: "start" }); else scrollTo(0, 0);
  let tempo = null, ultimoProg = c.progresso || 0;
  const aoRolar = () => {
    const alt = document.documentElement.scrollHeight - innerHeight;
    const prog = alt > 0 ? Math.min(1, scrollY / alt) : 1;
    const barra = $("#prog"); if (barra) barra.style.width = prog * 100 + "%";
    clearTimeout(tempo);
    tempo = setTimeout(async () => {
      const bloco = pars.findIndex((p) => p.getBoundingClientRect().bottom > 80);
      const p = Math.max(prog > .97 ? 1 : prog, (c.progresso || 0) >= .98 ? 1 : 0);
      if (Math.abs(p - ultimoProg) < .01 && p < 1) return;
      ultimoProg = p;
      await salvarCapitulo(obraId, capId, { progresso: p, posicaoBloco: Math.max(0, bloco + 1), posicaoDeslocamento: 0 });
      await salvarObra(obraId, { lidaEm: agora(), ultimoCap: capId });
    }, 1200);
  };
  window.onscroll = aoRolar; aoRolar();
  saindo = async () => { window.onscroll = null; };
});

// ───────── EDITOR ─────────
rota("/editar/(\\w+)/(\\w+)", async (obraId, capId) => {
  const o = obras.get(obraId); if (!o || !o.minha || o.trancada) { location.hash = "/"; return; }
  casca("escrever", `<div class="carregando">◆</div>`);
  const caps = await capitulos(obraId);
  const c = caps.find((x) => x.id === capId); if (!c) { location.hash = `#/obra/${obraId}`; return; }
  const original = await lerTexto(collection(db, "users", usuario.uid, "textos"), capId, c.partes);
  casca("escrever", `<div class="editor">
    <div class="barra-ed"><a class="voltar" href="#/obra/${obraId}">←</a><input class="titulo" id="titulo" value="${esc(c.titulo || "")}" placeholder="Título do capítulo">
      <span class="estado suave" id="estado">Salvo</span><a class="btn2 peq" href="#/ler/${obraId}/${capId}">Ler</a></div>
    <textarea id="txt" spellcheck="true" placeholder="Escreva aqui… (@Personagem, descrição · &Lugar, descrição)"></textarea>
    <div class="suave peq" style="margin-top:8px" id="cont"></div></div>`);
  const ta = $("#txt"), ti = $("#titulo"), est = $("#estado"), cont = $("#cont");
  ta.value = original;
  let salvo = original, tituloSalvo = c.titulo || "", tempo = null, gravando = false;
  const contar = () => (cont.textContent = `${contarPalavras(ta.value).toLocaleString("pt-BR")} palavras · salva sozinho e aparece no celular na próxima sincronização`);
  const salvar = async () => {
    if (gravando || (ta.value === salvo && ti.value === tituloSalvo)) return;
    gravando = true; est.textContent = "Salvando…"; est.style.color = "var(--amb)";
    const t = ta.value, tt = ti.value.trim();
    try { await salvarTexto(obraId, c, t, tt); salvo = t; tituloSalvo = ti.value; est.textContent = "Salvo"; est.style.color = "var(--verde)"; }
    catch (e) { est.textContent = "Sem conexão — tentando de novo"; est.style.color = "var(--erro)"; setTimeout(agendar, 4000); }
    gravando = false;
    if (ta.value !== salvo || ti.value !== tituloSalvo) agendar();
  };
  const agendar = () => { clearTimeout(tempo); est.textContent = "Editando…"; est.style.color = "var(--suave)"; tempo = setTimeout(salvar, 1500); };
  ta.oninput = () => { contar(); agendar(); };
  ti.oninput = agendar;
  contar(); ta.focus();
  window.onbeforeunload = (e) => { if (ta.value !== salvo) { salvar(); e.preventDefault(); } };
  saindo = async () => { window.onbeforeunload = null; clearTimeout(tempo); await salvar(); };
});

// ───────── DESCOBRIR (catálogo público) ─────────
rota("/descobrir(?:/(\\w+))?", async (modo = "ALTA") => {
  casca("descobrir", `<div class="topo"><h1>Descobrir</h1></div><div class="carregando" style="height:200px">◆</div>`);
  const col = collection(db, "publicas");
  let q;
  if (modo === "NOVIDADES" || modo === "CONCLUIDAS") q = query(col, orderBy("atualizadaEm", "desc"), limit(60));
  else if (modo === "SEMANA") q = query(col, orderBy("leiturasSemana", "desc"), limit(40));
  else q = query(col, orderBy("leituras", "desc"), limit(40));
  let l = [];
  try { l = (await getDocs(q)).docs.map((d) => ({ id: d.id, ...d.data() })).filter((o) => (o.numCaps || 0) > 0); } catch (e) { console.warn(e); }
  if (modo === "CONCLUIDAS") l = l.filter((o) => o.status === "CONCLUIDA");
  const nomes = { ALTA: "Em alta", SEMANA: "Ranking da semana", NOVIDADES: "Novidades", CONCLUIDAS: "Concluídas" };
  casca("descobrir", `<div class="topo"><h1>Descobrir</h1><input class="campo" id="busca" placeholder="Buscar obra, autor ou tag…" style="max-width:320px"></div>
    <div class="abas">${Object.keys(nomes).map((k) => `<a href="#/descobrir/${k}" class="${k === modo ? "on" : ""}">${nomes[k]}</a>`).join("")}</div>
    <div class="grade" id="grade">${l.map((o, n) => {
      const url = o.capaMini ? URL.createObjectURL(new Blob([o.capaMini.toUint8Array()], { type: "image/jpeg" })) : null;
      const nota = o.numEstrelas ? "★ " + (o.somaEstrelas / o.numEstrelas).toFixed(1).replace(".", ",") : "★ —";
      return `<a class="obra-c" data-busca="${esc((o.titulo + " " + o.autorNome + " " + (o.tags || []).join(" ")).toLowerCase())}" href="#/pub/${o.id}">
        <div class="capa">${url ? `<img src="${url}">` : `<div class="sol"></div><div class="t">${esc(o.titulo)}</div>`}</div>
        <div class="nm">${modo !== "NOVIDADES" ? `<span style="color:var(--amb)">${n + 1}.</span> ` : ""}${esc(o.titulo)}</div><div class="sb">${nota} · ${o.numCaps} caps · por ${esc(o.autorNome)}</div></a>`;
    }).join("") || `<p class="suave">Nada por aqui ainda.</p>`}</div>`);
  $("#busca").oninput = (e) => { const t = e.target.value.toLowerCase().trim(); for (const a of $("#grade").children) a.style.display = !t || (a.dataset.busca || "").includes(t) ? "" : "none"; };
});

rota("/pub/(\\w+)", async (pid) => {
  casca("descobrir", `<div class="carregando">◆</div>`);
  const d = await getDoc(doc(db, "publicas", pid)); if (!d.exists()) { casca("descobrir", `<p class="suave">Obra não encontrada.</p>`); return; }
  const o = d.data();
  const caps = (await getDocs(collection(db, "publicas", pid, "caps"))).docs.map((x) => ({ id: x.id, ...x.data() })).filter((c) => c.publicadoEm <= agora()).sort((a, b) => a.ordem - b.ordem);
  const capa = await imagem(collection(db, "publicas", pid, "arquivos"), "capa");
  const nota = o.numEstrelas ? "★ " + (o.somaEstrelas / o.numEstrelas).toFixed(1).replace(".", ",") : "★ —";
  casca("descobrir", `<div class="topo"><a class="voltar" href="javascript:history.back()">←</a><div style="flex:1"></div></div>
    <div class="cab-obra"><div class="fundo" style="${capa ? `background-image:url(${capa});filter:blur(20px) brightness(.5)` : "background:#3B2A4A"}"></div>
      <div class="frente"><div class="capa">${capa ? `<img src="${capa}">` : ""}</div><div style="flex:1"><div class="rotulo">${esc((o.generos || []).join(" · "))}${o.classificacao && o.classificacao !== "LIVRE" ? " · +" + o.classificacao : ""}</div>
        <h1>${esc(o.titulo)}</h1><div class="suave peq">por <b style="color:var(--tx)">${esc(o.autorNome)}</b> · ${nota} · ${milhar(o.leituras)} leituras · ${milhar(o.seguidores)} seguidores · ${caps.length} caps</div>
        ${caps.length ? `<a class="btn" style="margin-top:14px" href="#/publer/${pid}/${caps[0].id}">▶ Ler do início</a>` : ""}</div></div></div>
    ${o.sinopse ? `<p class="suave" style="line-height:1.6;max-width:760px">${esc(o.sinopse)}</p>` : ""}
    <p class="suave peq">Para seguir, curtir, comentar e baixar para ler offline, use o app no celular.</p>
    <div class="sec"><h2>Capítulos</h2></div>
    <div class="caps">${caps.map((c, n) => `<div class="cap" onclick="location.hash='#/publer/${pid}/${c.id}'"><span class="n">${n + 1}</span><span class="tt">${esc(c.titulo)}</span><span class="suave peq">♥ ${c.curtidas || 0} · 💬 ${c.comentarios || 0}</span></div>`).join("")}</div>`);
});

rota("/publer/(\\w+)/(\\w+)", async (pid, cid) => {
  casca("descobrir", `<div class="carregando">◆</div>`);
  const o = (await getDoc(doc(db, "publicas", pid))).data() || {};
  const caps = (await getDocs(collection(db, "publicas", pid, "caps"))).docs.map((x) => ({ id: x.id, ...x.data() })).filter((c) => c.publicadoEm <= agora()).sort((a, b) => a.ordem - b.ordem);
  const i = caps.findIndex((c) => c.id === cid); const c = caps[i]; if (!c) { location.hash = `#/pub/${pid}`; return; }
  const texto = await lerTexto(collection(db, "publicas", pid, "textos"), cid, c.partes);
  const prox = caps[i + 1], ant = caps[i - 1];
  const tam = Number(localStorage.getItem("tam") || 20);
  casca("descobrir", `<div class="leitor" style="--tam:${tam}px"><div class="controles"><a class="voltar" href="#/pub/${pid}">←</a><span class="tt">${esc(o.titulo)} · ${esc(c.titulo)}</span></div>
    <div class="cabec"><div class="rotulo">${esc(o.titulo)}</div><h1>${esc(c.titulo)}</h1><div class="suave peq">por ${esc(o.autorNome)}</div></div>
    <div class="texto">${htmlTexto(texto)}</div>
    <div class="fim"><div style="color:var(--amb)">◆</div><div style="display:flex;gap:10px">${ant ? `<a class="btn2" href="#/publer/${pid}/${ant.id}">‹ Anterior</a>` : ""}${prox ? `<a class="btn" href="#/publer/${pid}/${prox.id}">Próximo ›</a>` : `<span class="suave">Último capítulo publicado.</span>`}</div></div></div>`);
  scrollTo(0, 0);
});

// ───────── PERFIL ─────────
rota("/perfil", async () => {
  const minhas = lista().filter((o) => o.minha && !o.trancada);
  const palavras = minhas.length;
  casca("perfil", `<div class="topo"><h1>Perfil</h1></div>
    <div class="card" style="display:flex;gap:18px;align-items:center"><div style="width:76px;height:76px;border-radius:50%;background:linear-gradient(135deg,#E9A65A,#7A3B8F);display:grid;place-items:center;font-size:30px;font-weight:900;color:#1A1206">${esc((perfil.nome || "?").slice(0, 1).toUpperCase())}</div>
      <div style="flex:1"><div style="font-size:22px;font-weight:900">${esc(perfil.nome || "Sem nome")}</div><div class="suave">${esc(perfil.bio || "")}</div><div class="suave peq" style="margin-top:6px">${esc(usuario.email || "")} · ${palavras} obra(s) suas</div></div></div>
    <div style="display:flex;gap:10px;margin-top:16px;flex-wrap:wrap"><button class="btn2" id="nome">Editar nome e bio</button><button class="btn2" id="sair">Sair da conta</button></div>
    <div class="card" style="margin-top:24px"><b>Instalar no Windows:</b> <span class="suave">no Chrome ou Edge, clique no ícone de instalar na barra de endereço (ou no botão “Instalar no PC” do menu). O Novely abre como um programa e funciona sem internet com o que já foi aberto.</span></div>`);
  $("#sair").onclick = () => signOut(auth);
  $("#nome").onclick = async () => {
    const n = prompt("Nome de autor:", perfil.nome || ""); if (n === null) return;
    const b = prompt("Bio:", perfil.bio || ""); if (b === null) return;
    await setDoc(eu(), { nome: n.trim(), bio: b.trim(), alteradoEm: agora() }, { merge: true });
    perfil = { ...perfil, nome: n.trim(), bio: b.trim() }; toast("Salvo"); desenhar();
  };
});

// ───────── início ─────────
onAuthStateChanged(auth, async (u) => {
  usuario = u;
  if (pararObras) { pararObras(); pararObras = null; }
  obras = new Map();
  if (!u) return desenhar();
  try { perfil = (await getDoc(eu())).data() || {}; } catch (_) { perfil = {}; }
  if (!perfil.nome && u.displayName) perfil.nome = u.displayName;
  let primeira = true;
  pararObras = onSnapshot(colObras(), (s) => {
    s.docChanges().forEach((ch) => { if (ch.type === "removed") obras.delete(ch.doc.id); else obras.set(ch.doc.id, ch.doc.data()); });
    // redesenha listas quando algo muda (não interrompe leitor/editor)
    const h = location.hash;
    if (primeira || !/^#\/(ler|editar|pub|publer)/.test(h)) desenhar();
    primeira = false;
  }, () => desenhar());
});

if ("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(() => {});
