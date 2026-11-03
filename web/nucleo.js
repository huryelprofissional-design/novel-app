// Núcleo do Novely no PC: Firebase, estado, utilidades e todas as operações de dados
// (mesmos formatos que o app Android usa na sincronização e no catálogo).
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import {
  getAuth, onAuthStateChanged, signInWithPopup, GoogleAuthProvider, signInWithEmailAndPassword,
  createUserWithEmailAndPassword, sendPasswordResetEmail, signOut,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import {
  initializeFirestore, persistentLocalCache, persistentMultipleTabManager, collection, doc, getDoc, getDocs, setDoc, addDoc, deleteDoc, updateDoc,
  onSnapshot, query, where, orderBy, limit, serverTimestamp, increment, runTransaction, Bytes,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { firebaseConfig } from "./config.js";

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = initializeFirestore(app, { localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }) });
export { onAuthStateChanged, signInWithPopup, GoogleAuthProvider, signInWithEmailAndPassword, createUserWithEmailAndPassword, sendPasswordResetEmail, signOut };
export { collection, doc, getDoc, getDocs, setDoc, onSnapshot, query, where, orderBy, limit, updateDoc, addDoc, deleteDoc, increment };

export const EMAIL_DONO = "huryelprofissional@gmail.com";

// ───────── estado compartilhado entre as telas ─────────
export const E = { usuario: null, obras: new Map(), perfil: {}, instalar: null, saindo: null };

// ───────── utilidades ─────────
export const $ = (s, r = document) => r.querySelector(s);
export const $$ = (s, r = document) => [...r.querySelectorAll(s)];
export const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
export const raiz = document.getElementById("app");
export const novoId = () => [...crypto.getRandomValues(new Uint8Array(16))].map((b) => b.toString(16).padStart(2, "0")).join("");
export const agora = () => Date.now();
export const PARTE = 300000;
export function toast(t) { const d = document.createElement("div"); d.className = "toast"; d.textContent = t; document.body.appendChild(d); setTimeout(() => d.remove(), 2800); }
export function milhar(n) { n = n || 0; return n >= 1e6 ? (n / 1e6).toFixed(1).replace(".", ",") + " mi" : n >= 1e3 ? (n / 1e3).toFixed(1).replace(".", ",") + " mil" : String(n); }
export function palavrasFmt(n) { n = n || 0; return n >= 1000 ? (n / 1000).toFixed(1).replace(".", ",").replace(",0", "") + " mil" : String(n); }
export function haQuanto(ms) { const m = Math.max(0, (agora() - (ms || 0)) / 60000); return m < 1 ? "agora" : m < 60 ? Math.round(m) + " min" : m < 1440 ? Math.round(m / 60) + " h" : m < 10080 ? Math.round(m / 1440) + " d" : Math.round(m / 10080) + " sem"; }
export const dataFmt = (ms) => new Date(ms).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
export const recente = (ms) => agora() - (ms || 0) < 3 * 86400000;
export const corHex = (n) => n ? "#" + (Number(n) & 0xFFFFFF).toString(16).padStart(6, "0") : "#3B2A4A";
export const ehEspecial = (t) => /^(pr[oó]logo|ep[ií]logo|interl[uú]dio|prologue|epilogue|interlude|extra)/i.test((t || "").trim());
export const local = { ler: (k, d) => { try { const v = localStorage.getItem(k); return v === null ? d : JSON.parse(v); } catch (_) { return d; } }, gravar: (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)); } catch (_) {} } };

// CRC32 igual ao do Android ("%08x-%d" com o tamanho em bytes)
const TABELA = (() => { const t = new Uint32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
export function crc(bytes) { let c = 0xFFFFFFFF; for (let i = 0; i < bytes.length; i++) c = TABELA[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8); return ((c ^ 0xFFFFFFFF) >>> 0).toString(16).padStart(8, "0") + "-" + bytes.length; }
export const hashTexto = (s) => crc(new TextEncoder().encode(s || ""));
export function contarPalavras(t) { const m = (t || "").match(/[\p{L}\p{N}][\p{L}\p{N}'’\-]*/gu); return m ? m.length : 0; }
export function partir(t) { if (!t) return [""]; const r = []; for (let i = 0; i < t.length; i += PARTE) r.push(t.slice(i, i + PARTE)); return r; }

// ───────── @Personagem e &Lugar ─────────
const PAL = "[\\p{L}\\p{N}'’]*(?:-[\\p{L}\\p{N}'’]+)*";
export const RX_MARCA = new RegExp(`(?<![\\p{L}\\p{N}@&])([@&])(?:\\[([^\\]\\n]{1,60})\\]|(\\p{L}${PAL}(?:\\s(?:(?:d[aeo]s?|de|di|del|della|du|la|le|van|von|der|den|bin|ibn|al)\\s)?\\p{Lu}${PAL}){0,4}))(\\s*\\(([^)\\n]{1,80})\\))?(\\s*[,—–:;]|\\s+-(?=\\s))?`, "gu");
export const limparMarcas = (s) => (s.includes("@") || s.includes("&")) ? s.replace(RX_MARCA, (_m, _s, col, nome, _a, _b, sep) => (col || nome) + (sep || "")) : s;
/** Partes de nome que só uma ficha tem ("Kang Seo-jun" → "Seo-jun"). */
export function partesUnicas(fichas) {
  const cont = new Map();
  for (const f of fichas) { const p = (f.nome || "").trim().split(/\s+/); if (p.length < 2) continue; for (const x of p) if (x.length >= 3 && !/^(d[aeo]s?|de|di|del|della|du|la|le|van|von|der|den|bin|ibn|al)$/i.test(x)) { const k = x.toLowerCase(); if (!cont.has(k)) cont.set(k, new Set()); cont.get(k).add(f.id); } }
  const saida = new Map();
  for (const f of fichas) for (const x of (f.nome || "").trim().split(/\s+/)) { const ids = cont.get(x.toLowerCase()); if (ids && ids.size === 1 && !fichas.some((o) => o.nome.toLowerCase() === x.toLowerCase())) saida.set(x, f.id); }
  return saida;
}
/** nome visível → id da ficha (nomes, apelidos e partes únicas). */
export function mapaNomes(fichas) {
  const m = new Map();
  for (const f of fichas) { m.set(f.nome.trim(), f.id); for (const a of (f.apelidos || "").split(",")) if (a.trim().length >= 2) m.set(a.trim(), f.id); }
  for (const [p, id] of partesUnicas(fichas)) if (!m.has(p)) m.set(p, id);
  return m;
}

// ───────── ícones ─────────
export const ICONES = {
  inicio: '<path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/>',
  biblioteca: '<path d="M4 4h4v16H4zM10 4h4v16h-4zM16 5l3.5-1 2.5 15.5-3.5 1z"/>',
  escrever: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="M13.5 6.5l4 4"/>',
  descobrir: '<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/>',
  perfil: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
  sino: '<path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4"/>',
  lupa: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
  pessoas: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 4.5a3.5 3.5 0 0 1 0 7M18 14a6 6 0 0 1 4 6"/>',
};
export const svg = (n) => `<svg viewBox="0 0 24 24">${ICONES[n]}</svg>`;

// ───────── diálogos ─────────
/** Abre uma janela de vidro com o html; devolve o elemento <dialog>. */
export function dialogo(html, largo = false) {
  const d = document.createElement("dialog");
  if (largo) d.style.maxWidth = "760px";
  d.innerHTML = html;
  document.body.appendChild(d); d.showModal();
  d.addEventListener("close", () => d.remove());
  d.addEventListener("click", (e) => { if (e.target === d) d.close(); });
  return d;
}
export function confirmar(titulo, texto, botao = "Confirmar", perigo = false) {
  return new Promise((ok) => {
    const d = dialogo(`<h3 style="margin-top:0">${esc(titulo)}</h3><p class="suave">${esc(texto)}</p>
      <div class="linha-botoes"><button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" data-s style="${perigo ? "background:var(--erro);color:#fff" : ""}">${esc(botao)}</button></div>`);
    $("[data-n]", d).onclick = () => { d.close(); ok(false); };
    $("[data-s]", d).onclick = () => { d.close(); ok(true); };
  });
}
export function perguntar(titulo, valor = "", dica = "") {
  return new Promise((ok) => {
    const d = dialogo(`<h3 style="margin-top:0">${esc(titulo)}</h3><input class="campo" id="v" value="${esc(valor)}" placeholder="${esc(dica)}">
      <div class="linha-botoes"><button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" data-s>OK</button></div>`);
    const i = $("#v", d); i.focus(); i.select();
    const sim = () => { d.close(); ok(i.value); };
    i.onkeydown = (e) => { if (e.key === "Enter") sim(); };
    $("[data-n]", d).onclick = () => { d.close(); ok(null); };
    $("[data-s]", d).onclick = sim;
  });
}

// ───────── imagens ─────────
const imagens = new Map();
export function urlBlob(b) { return b ? URL.createObjectURL(new Blob([b.toUint8Array ? b.toUint8Array() : b], { type: "image/jpeg" })) : null; }
export async function imagem(colecao, nome) {
  const chave = colecao.path + "/" + nome;
  if (imagens.has(chave)) return imagens.get(chave);
  let url = null;
  try { const d = await getDoc(doc(colecao, nome)); const b = d.exists() ? d.data().dados : null; if (b) url = urlBlob(b); } catch (_) {}
  imagens.set(chave, url);
  return url;
}
export const esquecerImagem = (colecao, nome) => imagens.delete(colecao.path + "/" + nome);
export const colImagens = () => collection(db, "users", E.usuario.uid, "imagens");
export const imagemMinha = (nome) => imagem(colImagens(), nome);
/** Lê um arquivo de imagem, reduz para ladoMax e devolve os bytes JPEG. */
export async function comprimirArquivo(arquivo, ladoMax, qualidade = 0.82) {
  const bmp = await createImageBitmap(arquivo);
  const k = Math.min(1, ladoMax / Math.max(bmp.width, bmp.height));
  const c = document.createElement("canvas"); c.width = Math.round(bmp.width * k); c.height = Math.round(bmp.height * k);
  c.getContext("2d").drawImage(bmp, 0, 0, c.width, c.height);
  const blob = await new Promise((r) => c.toBlob(r, "image/jpeg", qualidade));
  return new Uint8Array(await blob.arrayBuffer());
}
export function escolherArquivo(aceita = "image/*") {
  return new Promise((ok) => { const i = document.createElement("input"); i.type = "file"; i.accept = aceita; i.onchange = () => ok(i.files[0] || null); i.click(); });
}
/** Envia uma imagem minha (capa, banner, foto de ficha, perfil). Devolve o hash para gravar no documento. */
export async function enviarImagemMinha(nome, bytes) {
  await setDoc(doc(colImagens(), nome), { dados: Bytes.fromUint8Array(bytes) });
  esquecerImagem(colImagens(), nome);
  return crc(bytes);
}

export function capaHTML(o, id, cor) {
  return `<div class="capa" data-capa="${esc(id)}" style="--cor:${cor || "#3B2A4A"}"><div class="sol"></div><div class="t">${esc(o.titulo)}</div></div>`;
}
export async function preencherCapas(el = raiz) {
  for (const c of el.querySelectorAll("[data-capa]")) {
    const id = c.dataset.capa; const o = E.obras.get(id);
    if (!o || !o.capaHash) continue;
    const url = await imagemMinha(id + "-capa");
    if (url) c.innerHTML = `<img src="${url}" alt="">`;
  }
}

// ───────── dados do usuário (users/{uid}/…) ─────────
export const eu = () => doc(db, "users", E.usuario.uid);
export const colObras = () => collection(db, "users", E.usuario.uid, "obras");
export const colItens = () => collection(db, "users", E.usuario.uid, "itens");
export const colTextos = () => collection(db, "users", E.usuario.uid, "textos");
export const lista = () => [...E.obras.entries()].map(([id, o]) => ({ id, ...o })).filter((o) => !o.apagado);

const cacheItens = new Map(); // obra -> {em, itens}
export async function itensObra(obraId, forcar = false) {
  const c = cacheItens.get(obraId);
  if (!forcar && c && agora() - c.em < 4000) return c.itens;
  const s = await getDocs(query(colItens(), where("obra", "==", obraId)));
  // _tipo = coleção do item (capitulos, fichas…); "tipo" dentro dos dados é o da ficha (PERSONAGEM, LUGAR…)
  const itens = s.docs.filter((d) => !d.data().apagado).map((d) => ({ ...d.data().dados, id: d.id, _tipo: d.data().tipo, alteradoEm: d.data().alteradoEm }));
  cacheItens.set(obraId, { em: agora(), itens });
  return itens;
}
export const esquecerItens = (obraId) => cacheItens.delete(obraId);
export async function capitulos(obraId, forcar) { return (await itensObra(obraId, forcar)).filter((i) => i._tipo === "capitulos").sort((a, b) => (a.ordem || 0) - (b.ordem || 0)); }
export async function fichas(obraId, forcar) { return (await itensObra(obraId, forcar)).filter((i) => i._tipo === "fichas").sort((a, b) => a.nome.localeCompare(b.nome)); }
export async function volumes(obraId, forcar) { return (await itensObra(obraId, forcar)).filter((i) => i._tipo === "volumes").sort((a, b) => (a.ordem || 0) - (b.ordem || 0)); }
export async function eventos(obraId, forcar) { return (await itensObra(obraId, forcar)).filter((i) => i._tipo === "eventos").sort((a, b) => (a.ordem || 0) - (b.ordem || 0)); }
export async function marcadores(obraId, forcar) { return (await itensObra(obraId, forcar)).filter((i) => i._tipo === "marcadores"); }

export async function lerTexto(base, id, partes) {
  let t = "";
  for (let i = 0; i < (partes || 1); i++) { const d = await getDoc(doc(base, i === 0 ? id : `${id}~${i}`)); t += d.exists() ? (d.data().texto || "") : ""; }
  return t;
}

// gravação (o celular recebe na próxima sincronização)
export async function salvarObra(id, campos) {
  await setDoc(doc(colObras(), id), { ...campos, apagado: false, alteradoEm: agora(), servidorEm: serverTimestamp() }, { merge: true });
  const o = E.obras.get(id); if (o) E.obras.set(id, { ...o, ...campos });
}
export async function salvarItem(tipo, obraId, id, dados) {
  esquecerItens(obraId);
  dados = { ...dados }; delete dados.id; delete dados._tipo; delete dados.alteradoEm;
  await setDoc(doc(colItens(), id), { tipo, obra: obraId, apagado: false, alteradoEm: agora(), servidorEm: serverTimestamp(), dados }, { merge: true });
}
export async function apagarItem(tipo, obraId, id) {
  esquecerItens(obraId);
  await setDoc(doc(colItens(), id), { tipo, apagado: true, alteradoEm: agora(), servidorEm: serverTimestamp() }, { merge: true });
}
export const salvarCapitulo = (obraId, capId, dados) => salvarItem("capitulos", obraId, capId, dados);
export async function salvarTexto(obraId, cap, texto, titulo) {
  const partes = partir(texto);
  for (let i = 0; i < partes.length; i++) await setDoc(doc(colTextos(), i === 0 ? cap.id : `${cap.id}~${i}`), { texto: partes[i] });
  await salvarCapitulo(obraId, cap.id, { titulo, palavras: contarPalavras(texto), textoHash: hashTexto(texto), partes: partes.length, atualizadoEm: agora() });
  await salvarObra(obraId, { atualizadaEm: agora() });
}
export async function novoCapitulo(obraId, titulo, texto = "") {
  const caps = await capitulos(obraId, true);
  const id = novoId();
  const ordem = caps.length ? Math.max(...caps.map((c) => c.ordem || 0)) + 1 : 1;
  const especiais = caps.filter((c) => ehEspecial(c.titulo)).length;
  const partes = partir(texto);
  for (let i = 0; i < partes.length; i++) await setDoc(doc(colTextos(), i === 0 ? id : `${id}~${i}`), { texto: partes[i] });
  await salvarCapitulo(obraId, id, {
    titulo: titulo || `Capítulo ${caps.length - especiais + 1}`, ordem, volume: "", rascunho: true, palavras: contarPalavras(texto), progresso: 0, posicaoBloco: 0, posicaoDeslocamento: 0,
    atualizadoEm: agora(), resumo: "", notas: "", tags: "", textoHash: hashTexto(texto), partes: partes.length, publicaId: "", publicadoEm: 0, publicadoHash: "",
  });
  await salvarObra(obraId, { atualizadaEm: agora() });
  return id;
}
export async function novaObra(titulo, status = "ESCREVENDO") {
  const id = novoId();
  const cores = [0xFF3B2A4A, 0xFF4A2420, 0xFF14302E, 0xFF1C2440, 0xFF2B3A22, 0xFF4A1C34];
  const o = {
    titulo: titulo || "Sem título", descricao: "", status, capaHash: "", bannerHash: "", capaX: .5, capaY: .5, capaZoom: 1,
    bannerX: .5, bannerY: .5, bannerZoom: 1, corBase: cores[Math.floor(Math.random() * cores.length)], criadaEm: agora(), atualizadaEm: agora(), lidaEm: 0,
    colecao: "", minha: true, trancada: false, autor: "", ultimoCap: "", publicada: false, publicaId: "", generos: "", tagsPub: "",
    classificacao: "LIVRE", idioma: "pt", exportavel: true,
  };
  await salvarObra(id, o);
  E.obras.set(id, o);
  return id;
}
export async function apagarObra(id) {
  await setDoc(doc(colObras(), id), { apagado: true, alteradoEm: agora(), servidorEm: serverTimestamp() }, { merge: true });
  E.obras.delete(id);
}
export const FICHA_VAZIA = { tipo: "PERSONAGEM", nome: "", descricao: "", apelidos: "", idade: "", aparencia: "", poder: "", faccao: "", relacoes: "", imagemHash: "" };
export async function salvarFicha(obraId, id, f) {
  const novo = !id; id = id || novoId();
  await salvarItem("fichas", obraId, id, { ...FICHA_VAZIA, ...(novo ? { criadaEm: agora() } : {}), ...f });
  return id;
}

// ───────── perfil ─────────
export const nomeAutor = () => E.perfil.nome || E.usuario?.displayName || "Autor";
export async function salvarPerfil(campos) {
  await setDoc(eu(), { ...campos, alteradoEm: agora() }, { merge: true });
  E.perfil = { ...E.perfil, ...campos };
  await publicarPerfil();
}
/** Perfil público (perfis/{uid}): nome, bio, foto pequena e banner. */
export async function publicarPerfil() {
  try {
    const dados = { nome: nomeAutor(), bio: E.perfil.bio || "", alteradoEm: agora() };
    const foto = E.perfil.fotoHash ? await bytesDe(colImagens(), "perfil") : null;
    if (foto) dados.foto = Bytes.fromUint8Array(await reduzirBytes(foto, 200, .7));
    const ban = E.perfil.bannerHash ? await bytesDe(colImagens(), "perfilBanner") : null;
    dados.banner = ban ? Bytes.fromUint8Array(await reduzirBytes(ban, 900, .65)) : null;
    await setDoc(doc(db, "perfis", E.usuario.uid), dados, { merge: true });
  } catch (e) { console.warn(e); }
}
async function bytesDe(col, nome) { const d = await getDoc(doc(col, nome)); return d.exists() && d.data().dados ? d.data().dados.toUint8Array() : null; }
async function reduzirBytes(bytes, lado, q) { return comprimirArquivo(new Blob([bytes], { type: "image/jpeg" }), lado, q); }

// ───────── catálogo público ─────────
export const colPub = () => collection(db, "publicas");
export function semanaAtual() {
  // igual ao Calendar.WEEK_OF_YEAR do Android (semana começa no domingo, 1ª semana tem 1º de janeiro)
  const d = new Date(); const ini = new Date(d.getFullYear(), 0, 1);
  const dias = Math.floor((new Date(d.getFullYear(), d.getMonth(), d.getDate()) - ini) / 86400000);
  return `${d.getFullYear()}-${Math.floor((dias + ini.getDay()) / 7) + 1}`;
}
export const pubDe = (d) => ({ id: d.id, ...d.data() });
export const notaTxt = (o) => o.numEstrelas ? "★ " + (o.somaEstrelas / o.numEstrelas).toFixed(1).replace(".", ",") : "NOVO";
const cacheCat = new Map();
export async function catalogo(modo, forcar) {
  const c = cacheCat.get(modo); if (!forcar && c && agora() - c.em < 5 * 60000) return c.l;
  let q;
  if (modo === "ALTA") q = query(colPub(), orderBy("leituras", "desc"), limit(40));
  else if (modo === "SEMANA") q = query(colPub(), orderBy("leiturasSemana", "desc"), limit(40));
  else if (modo.startsWith("genero:")) q = query(colPub(), where("generos", "array-contains", modo.slice(7)), limit(60));
  else q = query(colPub(), orderBy("atualizadaEm", "desc"), limit(60));
  let l = (await getDocs(q)).docs.map(pubDe).filter((o) => (o.numCaps || 0) > 0);
  if (modo === "SEMANA") l = l.filter((o) => o.semanaId === semanaAtual() && o.leiturasSemana > 0);
  if (modo === "CONCLUIDAS") l = l.filter((o) => o.status === "CONCLUIDA");
  if (modo.startsWith("genero:")) l.sort((a, b) => (b.leituras || 0) - (a.leituras || 0));
  cacheCat.set(modo, { em: agora(), l });
  return l;
}
export async function obraPub(pid) { const d = await getDoc(doc(colPub(), pid)); return d.exists() ? pubDe(d) : null; }
export async function capsPub(pid, autor = false) {
  return (await getDocs(collection(db, "publicas", pid, "caps"))).docs.map(pubDe).filter((c) => autor || (c.publicadoEm || 0) <= agora()).sort((a, b) => (a.ordem || 0) - (b.ordem || 0));
}
export const imagemPub = (pid, qual) => imagem(collection(db, "publicas", pid, "arquivos"), qual);
export async function fichasPub(pid) { try { return (await getDocs(collection(db, "publicas", pid, "fichas"))).docs.map(pubDe); } catch (_) { return []; } }

/** O que falta para publicar. */
export function pendencias(o) {
  const l = [];
  if (!o.capaHash) l.push("Coloque uma capa");
  if (!o.bannerHash) l.push("Coloque um banner");
  if (!(o.descricao || "").trim()) l.push("Escreva a sinopse");
  if (!(o.generos || "").trim()) l.push("Escolha pelo menos um gênero");
  return l;
}
/** Publica/atualiza os dados da obra no catálogo (igual ao Publicador do Android). */
export async function publicarObra(id) {
  const o = E.obras.get(id);
  const ref = doc(colPub(), id);
  const caps = (await capitulos(id, true)).filter((c) => (c.publicadoEm || 0) > 0);
  const capaBytes = o.capaHash ? await bytesDe(colImagens(), id + "-capa") : null;
  const dados = {
    autorUid: E.usuario.uid, autorNome: nomeAutor(), titulo: o.titulo, sinopse: o.descricao || "",
    generos: (o.generos || "").split(",").map((s) => s.trim()).filter(Boolean),
    tags: (o.tagsPub || "").split(",").map((s) => s.trim().replace(/^#/, "").toLowerCase()).filter(Boolean),
    classificacao: o.classificacao || "LIVRE", idioma: o.idioma || "pt", status: o.status, exportavel: o.exportavel !== false, enfeite: o.enfeite || "",
    capaMini: capaBytes ? Bytes.fromUint8Array(await reduzirBytes(capaBytes, 260, .7)) : null,
    numCaps: caps.length, palavras: caps.reduce((s, c) => s + (c.palavras || 0), 0), atualizadaEm: agora(),
  };
  const existe = (await getDoc(ref)).exists();
  if (!existe) Object.assign(dados, { publicadaEm: agora(), leituras: 0, leiturasSemana: 0, semanaId: semanaAtual(), seguidores: 0, somaEstrelas: 0, numEstrelas: 0 });
  await setDoc(ref, dados, { merge: true });
  if (capaBytes) await setDoc(doc(db, "publicas", id, "arquivos", "capa"), { dados: Bytes.fromUint8Array(capaBytes) });
  const ban = o.bannerHash ? await bytesDe(colImagens(), id + "-banner") : null;
  if (ban) await setDoc(doc(db, "publicas", id, "arquivos", "banner"), { dados: Bytes.fromUint8Array(ban) });
  const fs = await fichas(id, true);
  const remotas = new Set((await getDocs(collection(db, "publicas", id, "fichas"))).docs.map((d) => d.id));
  for (const f of fs) {
    await setDoc(doc(db, "publicas", id, "fichas", f.id), { tipo: f.tipo, nome: f.nome, apelidos: f.apelidos || "", idade: f.idade || "", aparencia: f.aparencia || "", poder: f.poder || "", faccao: f.faccao || "", relacoes: f.relacoes || "", descricao: f.descricao || "" });
    remotas.delete(f.id);
  }
  for (const r of remotas) await deleteDoc(doc(db, "publicas", id, "fichas", r));
  if (!o.publicada) await salvarObra(id, { publicada: true });
}
export async function publicarCapitulos(id, caps, quando = agora()) {
  if (!(await getDoc(doc(colPub(), id))).exists()) await publicarObra(id);
  for (const c of caps) {
    const texto = await lerTexto(colTextos(), c.id, c.partes);
    const pedacos = partir(texto);
    for (let i = 0; i < pedacos.length; i++) await setDoc(doc(db, "publicas", id, "textos", i === 0 ? c.id : `${c.id}~${i}`), { texto: pedacos[i] });
    const ja = (c.publicadoEm || 0) > 0;
    const em = ja && c.publicadoEm <= agora() ? c.publicadoEm : quando;
    await setDoc(doc(db, "publicas", id, "caps", c.id), { titulo: c.titulo, ordem: c.ordem, palavras: c.palavras || 0, publicadoEm: em, atualizadoEm: agora(), partes: pedacos.length, resumo: c.resumo || "" }, { merge: true });
    await salvarCapitulo(id, c.id, { publicadoEm: em, publicadoHash: c.textoHash || hashTexto(texto), rascunho: false });
  }
  await publicarObra(id);
}
export async function despublicarCapitulo(id, c) {
  await deleteDoc(doc(db, "publicas", id, "caps", c.id));
  for (let i = 0; i < Math.max(1, c.partes || 1) + 2; i++) { try { await deleteDoc(doc(db, "publicas", id, "textos", i === 0 ? c.id : `${c.id}~${i}`)); } catch (_) {} }
  await salvarCapitulo(id, c.id, { publicadoEm: 0, publicadoHash: "" });
  await publicarObra(id);
}
export async function despublicarObra(id) {
  for (const sub of ["caps", "textos", "arquivos", "fichas"]) for (const d of (await getDocs(collection(db, "publicas", id, sub))).docs) await deleteDoc(d.ref);
  await deleteDoc(doc(colPub(), id));
  await salvarObra(id, { publicada: false });
  for (const c of (await capitulos(id, true)).filter((c) => (c.publicadoEm || 0) > 0)) await salvarCapitulo(id, c.id, { publicadoEm: 0, publicadoHash: "" });
}

// ───────── leitor do catálogo: seguir, avaliar, curtir, comentar ─────────
export async function seguindo(pid) { try { return (await getDoc(doc(db, "publicas", pid, "seguidores", E.usuario.uid))).exists(); } catch (_) { return false; } }
/** Seguir põe a obra na biblioteca (o celular baixa os capítulos para ler offline). */
export async function seguir(pid, sim, p) {
  const ref = doc(db, "publicas", pid, "seguidores", E.usuario.uid);
  const ja = (await getDoc(ref)).exists();
  if (sim && !ja) { await setDoc(ref, { em: agora() }); await updateDoc(doc(colPub(), pid), { seguidores: increment(1) }); }
  if (!sim && ja) { await deleteDoc(ref); await updateDoc(doc(colPub(), pid), { seguidores: increment(-1) }); }
  const naBib = lista().find((o) => o.publicaId === pid);
  if (sim && !naBib && p) {
    const id = novoId();
    const o = { titulo: p.titulo, descricao: p.sinopse || "", status: "LENDO", minha: false, trancada: true, autor: p.autorNome || "", publicaId: pid,
      capaHash: "", bannerHash: "", capaX: .5, capaY: .5, capaZoom: 1, bannerX: .5, bannerY: .5, bannerZoom: 1, corBase: 0xFF3B2A4A, criadaEm: agora(),
      atualizadaEm: agora(), lidaEm: 0, colecao: "", ultimoCap: "", publicada: false, generos: (p.generos || []).join(","), tagsPub: (p.tags || []).join(","),
      classificacao: p.classificacao || "LIVRE", idioma: p.idioma || "pt", exportavel: p.exportavel !== false };
    // capa vai junto para a biblioteca
    const capa = await bytesDe(collection(db, "publicas", pid, "arquivos"), "capa");
    if (capa) o.capaHash = await enviarImagemMinha(id + "-capa", capa);
    await salvarObra(id, o); E.obras.set(id, o);
  }
}
export async function minhaNota(pid) { try { return (await getDoc(doc(db, "publicas", pid, "avaliacoes", E.usuario.uid))).data()?.estrelas || 0; } catch (_) { return 0; } }
export async function avaliar(pid, estrelas) {
  const antes = await minhaNota(pid);
  await setDoc(doc(db, "publicas", pid, "avaliacoes", E.usuario.uid), { estrelas, em: agora() });
  await updateDoc(doc(colPub(), pid), { somaEstrelas: increment(estrelas - antes), numEstrelas: increment(antes === 0 ? 1 : 0) });
}
// ───────── moedas (grátis) — a mesma carteira do celular, guardada na conta ─────────
const hojeId = () => new Date().toISOString().slice(0, 10);
export async function carteira(forcar) {
  if (E.carteira && !forcar) return E.carteira;
  let c = {}; try { c = (await getDoc(doc(db, "users", E.usuario.uid))).data()?.carteira || {}; } catch (_) {}
  E.carteira = { saldo: c.saldo || 0, liberados: (c.liberados || "").split("\n").filter(Boolean) };
  return E.carteira;
}
async function gravarCarteira() {
  const c = E.carteira; try { await setDoc(doc(db, "users", E.usuario.uid), { carteira: { saldo: c.saldo, liberados: c.liberados.slice(-2000).join("\n"), em: agora() } }, { merge: true }); } catch (_) {}
  document.querySelectorAll("[data-saldo]").forEach((el) => (el.textContent = c.saldo));
}
/** Ganha moedas com limite por dia por motivo; [unico] evita ganhar duas vezes pela mesma coisa. */
export async function ganharMoedas(qtd, motivo, limite, unico) {
  const c = await carteira(); const reg = local.ler("moedasDia", {}); const k = hojeId() + "|" + motivo;
  if (unico) { const u = local.ler("moedasUnicas", {}); if (u[unico]) return 0; u[unico] = 1; local.gravar("moedasUnicas", u); }
  const dar = Math.max(0, Math.min(qtd, limite - (reg[k] || 0))); if (!dar) return 0;
  for (const x of Object.keys(reg)) if (!x.startsWith(hojeId())) delete reg[x];
  reg[k] = (reg[k] || 0) + dar; local.gravar("moedasDia", reg);
  c.saldo += dar; await gravarCarteira(); return dar;
}
export async function diarioMoedas() { if (local.ler("moedaDiariaPC", "") === hojeId()) return 0; local.gravar("moedaDiariaPC", hojeId()); return ganharMoedas(3, "diario", 3); }
export const liberado = (pid, cid) => !!E.carteira?.liberados.includes(pid + "/" + cid);
export async function gastarMoedas(qtd, pid, cid) {
  const c = await carteira(true); if (c.liberados.includes(pid + "/" + cid)) return true; if (c.saldo < qtd) return false;
  c.saldo -= qtd; c.liberados.push(pid + "/" + cid); await gravarCarteira();
  try { await updateDoc(doc(colPub(), pid), { desbloqueios: increment(1) }); } catch (_) {}
  return true;
}
export async function definirAntecipado(pid, custo) { await updateDoc(doc(colPub(), pid), { antecipado: custo }); }

/** Estatística de leitura para o autor (sem texto, sem nome): até onde leu e quanto tempo. Uma por pessoa por capítulo. */
export async function registrarLeitura(pid, cid, bloco, total, segundos) {
  if (!E.usuario || total <= 0 || bloco < 0) return;
  const k = "lmax/" + pid + "/" + cid; const mx = local.ler("lmax", {});
  const max = Math.max(mx[k] ?? -1, Math.min(bloco, total - 1)); mx[k] = max; local.gravar("lmax", mx);
  try {
    await setDoc(doc(db, "publicas", pid, "leituras", `${cid}_${E.usuario.uid}`), {
      uid: E.usuario.uid, cap: cid, bloco: max, total, segundos: increment(segundos), sessoes: increment(1), em: agora(), plataforma: "pc",
    }, { merge: true });
  } catch (_) {}
}
export async function leiturasObra(pid) {
  const r = await getDocs(query(collection(db, "publicas", pid, "leituras"), limit(8000)));
  return r.docs.map((d) => d.data()).filter((x) => x.uid && x.cap);
}
export async function contarLeitura(pid, cid) {
  const vistos = local.ler("contados", {}); const k = pid + "/" + cid;
  if (vistos[k]) return;
  try {
    const ref = doc(colPub(), pid);
    await runTransaction(db, async (t) => {
      const d = await t.get(ref); const sem = semanaAtual();
      if (d.data()?.semanaId === sem) t.update(ref, { leituras: increment(1), leiturasSemana: increment(1) });
      else t.update(ref, { leituras: increment(1), leiturasSemana: 1, semanaId: sem });
    });
    try { await updateDoc(doc(db, "publicas", pid, "caps", cid), { leituras: increment(1) }); } catch (_) {}
    vistos[k] = 1; local.gravar("contados", vistos);
  } catch (_) {}
}
export async function curtiu(pid, cid) { try { return (await getDoc(doc(db, "publicas", pid, "caps", cid, "curtidas", E.usuario.uid))).exists(); } catch (_) { return false; } }
export async function curtir(pid, cid, sim) {
  const cap = doc(db, "publicas", pid, "caps", cid); const ref = doc(db, "publicas", pid, "caps", cid, "curtidas", E.usuario.uid);
  const ja = (await getDoc(ref)).exists();
  if (sim && !ja) { await setDoc(ref, { em: agora() }); await updateDoc(cap, { curtidas: increment(1) }); }
  if (!sim && ja) { await deleteDoc(ref); await updateDoc(cap, { curtidas: increment(-1) }); }
}
export async function comentarios(pid, cid) {
  return (await getDocs(query(collection(db, "publicas", pid, "caps", cid, "comentarios"), orderBy("em", "desc"), limit(200)))).docs.map(pubDe);
}
export async function comentar(pid, cid, texto, bloco = -1, trecho = "") {
  await addDoc(collection(db, "publicas", pid, "caps", cid, "comentarios"), { uid: E.usuario.uid, nome: nomeAutor(), texto: texto.trim().slice(0, 2000), em: agora(), bloco, trecho: trecho.slice(0, 300) });
  await updateDoc(doc(db, "publicas", pid, "caps", cid), { comentarios: increment(1) });
  ganharMoedas(1, "comentario", 5);
}
export async function apagarComentario(pid, cid, id) {
  await deleteDoc(doc(db, "publicas", pid, "caps", cid, "comentarios", id));
  await updateDoc(doc(db, "publicas", pid, "caps", cid), { comentarios: increment(-1) });
}

// ───────── autores ─────────
const cachePerfis = new Map();
export async function perfilPub(uid) {
  if (cachePerfis.has(uid)) return cachePerfis.get(uid);
  let p = null; try { const d = await getDoc(doc(db, "perfis", uid)); if (d.exists()) p = { uid, ...d.data() }; } catch (_) {}
  cachePerfis.set(uid, p); return p;
}
export async function obrasDoAutor(uid) { return (await getDocs(query(colPub(), where("autorUid", "==", uid)))).docs.map(pubDe).filter((o) => (o.numCaps || 0) > 0 || uid === E.usuario.uid).sort((a, b) => (b.atualizadaEm || 0) - (a.atualizadaEm || 0)); }
export const segueAutor = (uid) => local.ler("autoresSeguidos", []).includes(uid);
export async function seguirAutor(uid, sim) {
  const ref = doc(db, "perfis", uid, "seguidores", E.usuario.uid);
  const ja = (await getDoc(ref)).exists();
  if (sim && !ja) { await setDoc(ref, { em: agora() }); try { await updateDoc(doc(db, "perfis", uid), { seguidores: increment(1) }); } catch (_) {} }
  if (!sim && ja) { await deleteDoc(ref); try { await updateDoc(doc(db, "perfis", uid), { seguidores: increment(-1) }); } catch (_) {} }
  const l = new Set(local.ler("autoresSeguidos", [])); sim ? l.add(uid) : l.delete(uid); local.gravar("autoresSeguidos", [...l]);
  cachePerfis.delete(uid);
}

// ───────── colaboração (coautor / revisor) ─────────
const LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
export const colColabs = () => collection(db, "colabs");
export async function garantirColab(id) { const o = E.obras.get(id); await setDoc(doc(colColabs(), id), { autorUid: E.usuario.uid, titulo: o.titulo, autorNome: nomeAutor() }, { merge: true }); }
export async function convidar(id, papel) {
  await garantirColab(id);
  const codigo = Array.from({ length: 6 }, () => LETRAS[Math.floor(Math.random() * LETRAS.length)]).join("");
  await setDoc(doc(db, "convites", codigo), { pid: id, papel, autorUid: E.usuario.uid, titulo: E.obras.get(id).titulo, autorNome: nomeAutor(), criadoEm: agora() });
  return codigo;
}
export async function membros(pid) { try { return (await getDocs(collection(db, "colabs", pid, "membros"))).docs.map(pubDe); } catch (_) { return []; } }
export const removerMembro = (pid, uid) => deleteDoc(doc(db, "colabs", pid, "membros", uid));
export const minhasColabs = () => local.ler("colabs", []);
export async function entrarColab(codigo) {
  codigo = codigo.trim().toUpperCase();
  const c = await getDoc(doc(db, "convites", codigo));
  if (!c.exists()) return "Código não encontrado. Confira as letras.";
  const x = c.data();
  if (x.autorUid === E.usuario.uid) return "Esse convite é seu — mande para outra pessoa.";
  await setDoc(doc(db, "colabs", x.pid, "membros", E.usuario.uid), { papel: x.papel || "REVISOR", codigo, nome: nomeAutor(), em: agora() });
  local.gravar("colabs", [...minhasColabs().filter((m) => m.pid !== x.pid), { pid: x.pid, titulo: x.titulo || "", papel: x.papel || "REVISOR", autor: x.autorNome || "" }]);
  return null;
}
export async function sairColab(pid) { try { await deleteDoc(doc(db, "colabs", pid, "membros", E.usuario.uid)); } catch (_) {} local.gravar("colabs", minhasColabs().filter((m) => m.pid !== pid)); }
export async function enviarRevisao(id, caps) {
  await garantirColab(id);
  for (const c of caps) {
    const texto = await lerTexto(colTextos(), c.id, c.partes); const ped = partir(texto);
    for (let i = 0; i < ped.length; i++) await setDoc(doc(db, "colabs", id, "textos", i === 0 ? c.id : `${c.id}~${i}`), { texto: ped[i] });
    await setDoc(doc(db, "colabs", id, "revisao", c.id), { titulo: c.titulo, ordem: c.ordem, enviadoEm: agora(), partes: ped.length }, { merge: true });
  }
}
export const tirarRevisao = (pid, cid) => deleteDoc(doc(db, "colabs", pid, "revisao", cid));
export async function capsRevisao(pid) {
  const l = (await getDocs(collection(db, "colabs", pid, "revisao"))).docs.map(pubDe);
  for (const c of l) { try { c.notas = (await getDocs(collection(db, "colabs", pid, "revisao", c.id, "notas"))).size; } catch (_) { c.notas = 0; } }
  return l.sort((a, b) => (a.ordem || 0) - (b.ordem || 0));
}
export const textoRevisao = (pid, c) => lerTexto(collection(db, "colabs", pid, "textos"), c.id, c.partes);
export async function notas(pid, cid) { return (await getDocs(query(collection(db, "colabs", pid, "revisao", cid, "notas"), orderBy("em", "asc")))).docs.map(pubDe); }
export const anotar = (pid, cid, trecho, texto) => addDoc(collection(db, "colabs", pid, "revisao", cid, "notas"), { uid: E.usuario.uid, nome: nomeAutor(), trecho: trecho.slice(0, 600), texto: texto.trim().slice(0, 3000), em: agora() });
export const apagarNota = (pid, cid, id) => deleteDoc(doc(db, "colabs", pid, "revisao", cid, "notas", id));
export const sugerir = (pid, titulo, texto) => addDoc(collection(db, "colabs", pid, "sugestoes"), { uid: E.usuario.uid, nome: nomeAutor(), titulo: titulo.trim(), texto: texto.slice(0, PARTE), em: agora() });
export async function sugestoes(pid) { try { return (await getDocs(query(collection(db, "colabs", pid, "sugestoes"), orderBy("em", "desc")))).docs.map(pubDe); } catch (_) { return []; } }
export async function aceitarSugestao(id, s) { const c = await novoCapitulo(id, s.titulo || `Sugestão de ${s.nome}`, s.texto || ""); await deleteDoc(doc(db, "colabs", id, "sugestoes", s.id)); return c; }
export const descartarSugestao = (pid, sid) => deleteDoc(doc(db, "colabs", pid, "sugestoes", sid));

// ───────── gêneros (iguais aos do app) ─────────
export const GENEROS_GRUPOS = {
  "Fantasia": ["Fantasia", "Alta fantasia", "Fantasia sombria", "Fantasia urbana", "Romantasy", "Contos de fadas", "Mitologia"],
  "Oriental": ["Murim", "Wuxia", "Xianxia", "Cultivo", "Artes marciais", "Dorama"],
  "Poder e progressão": ["Sistema", "LitRPG", "Torre / Masmorra", "Regressão", "Reencarnação", "Isekai", "Protagonista OP", "Construção de reino"],
  "Romance": ["Romance", "Romance de época", "Slow burn", "Inimigos para amantes", "Vilã", "BL", "GL", "Harém"],
  "Ação e aventura": ["Ação", "Aventura", "Militar / Guerra", "Sobrevivência", "Pós-apocalíptico", "Esportes"],
  "Mistério e medo": ["Mistério", "Suspense", "Thriller", "Policial", "Terror", "Horror cósmico", "Sobrenatural", "Psicológico"],
  "Ficção e mundo real": ["Ficção científica", "Cyberpunk", "Distopia", "Histórico", "Político", "Drama", "Tragédia"],
  "Leve": ["Comédia", "Slice of life", "Escolar / Academia", "Culinária", "Música", "Conto"],
};
export const GENEROS = Object.values(GENEROS_GRUPOS).flat();
export const CLASSIFICACOES = ["LIVRE", "12", "14", "16", "18"];
const CORES_G = [["#5B2A86", "#1A0E2B"], ["#8A2C2C", "#2A0D0D"], ["#1F5E6B", "#0A2126"], ["#8A5A1C", "#2B1A07"], ["#2D4F8A", "#0C1729"], ["#6B2A5E", "#240C1F"], ["#2F6B3A", "#0C2410"], ["#7A3B1F", "#2A130A"], ["#4A3D8F", "#14102E"], ["#8A2C5A", "#2B0C1C"], ["#3D6B6B", "#0F2222"], ["#6B5A1F", "#241D08"]];
export function corGenero(g) { let h = 0; for (const c of g) h = (h * 31 + c.charCodeAt(0)) | 0; return CORES_G[Math.abs(h) % CORES_G.length]; }
export const STATUS_NOME = { ESCREVENDO: "Escrevendo", LENDO: "Lendo", CONCLUIDA: "Concluída", QUERO: "Quero ler" };

// ───────── cor da capa ─────────
const coresCapa = new Map();
/** Cor viva que mais aparece na imagem, clareada para ler bem sobre o preto (igual ao app). */
export async function corDaImagem(url) {
  if (!url) return null;
  if (coresCapa.has(url)) return coresCapa.get(url);
  let cor = null;
  try {
    const img = new Image(); img.src = url; await img.decode();
    const c = document.createElement("canvas"); c.width = 48; c.height = Math.max(1, Math.round(48 * img.height / img.width));
    const g = c.getContext("2d", { willReadFrequently: true }); g.drawImage(img, 0, 0, c.width, c.height);
    const d = g.getImageData(0, 0, c.width, c.height).data;
    const pesos = new Array(24).fill(0), sr = new Array(24).fill(0), sg = new Array(24).fill(0), sb = new Array(24).fill(0);
    for (let i = 0; i < d.length; i += 4) {
      const [h, s, v] = hsv(d[i], d[i + 1], d[i + 2]);
      if (s > .22 && v > .18) { const k = Math.min(23, Math.floor(h / 15)); const w = (s * v) ** 2; pesos[k] += w; sr[k] += d[i] * w; sg[k] += d[i + 1] * w; sb[k] += d[i + 2] * w; }
    }
    const m = pesos.indexOf(Math.max(...pesos));
    if (pesos[m] > .5) cor = realcar(sr[m] / pesos[m], sg[m] / pesos[m], sb[m] / pesos[m]);
  } catch (_) {}
  coresCapa.set(url, cor);
  return cor;
}
function hsv(r, g, b) { r /= 255; g /= 255; b /= 255; const mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn; let h = 0; if (d) h = mx === r ? ((g - b) / d) % 6 : mx === g ? (b - r) / d + 2 : (r - g) / d + 4; return [(h * 60 + 360) % 360, mx ? d / mx : 0, mx]; }
function realcar(r, g, b) {
  let [h, s, v] = hsv(r, g, b); s = Math.min(.78, Math.max(.45, s)); v = Math.min(1, Math.max(.88, v));
  const f = (n) => { const k = (n + h / 60) % 6; return v - v * s * Math.max(0, Math.min(k, 4 - k, 1)); };
  return "#" + [f(5), f(3), f(1)].map((x) => Math.round(x * 255).toString(16).padStart(2, "0")).join("");
}
export const corDaBase = (n) => { const x = Number(n) & 0xFFFFFF; return realcar(x >> 16 & 255, x >> 8 & 255, x & 255); };
export const usarCorDaObra = () => local.ler("corDaObra", true);

// ───────── enfeites de cena ─────────
export const ENFEITES = { LOSANGO: "Losango", ESTRELAS: "Estrelas", LOTUS: "Flor de lótus", ESPADAS: "Espadas", ONDA: "Onda" };
export function enfeiteHTML(k) {
  if (k === "ESTRELAS") return `<span class="enf-estrelas">✦ ✦ ✦</span>`;
  if (k === "LOTUS") return `<i class="enf-linha"></i><span class="enf-simb">❀</span><i class="enf-linha inv"></i>`;
  if (k === "ESPADAS") return `<span class="enf-simb">⚔</span><i class="enf-tracos"></i><span class="enf-simb">⚔</span>`;
  if (k === "ONDA") return `<svg width="160" height="16" viewBox="0 0 160 16"><path d="M0 8 Q10 0 20 8 T40 8 T60 8 T80 8 T100 8 T120 8 T140 8 T160 8" fill="none" stroke="currentColor" stroke-width="2"/></svg>`;
  return `<i class="enf-linha"></i><i class="enf-losango"></i><i class="enf-linha inv"></i>`;
}

// ───────── conquistas (calculadas no celular, guardadas no perfil) ─────────
export const CONQUISTAS = [
  ["p1k", "✎", "Primeiras 1.000 palavras", "#FF9A5A"], ["p10k", "✎", "10 mil palavras", "#FF7A3A"], ["p50k", "✎", "50 mil palavras", "#FF6A2B"], ["p100k", "✒", "100 mil palavras", "#E0452B"],
  ["s7", "🔥", "7 dias seguidos", "#FF8A2A"], ["s30", "🔥", "30 dias seguidos", "#E85A1F"], ["l10", "👁", "10 capítulos lidos", "#5AB0FF"], ["l100", "👁", "100 capítulos lidos", "#3A8BFF"],
  ["h10", "☾", "10 horas lendo", "#7A8BFF"], ["fim", "✓", "Leu uma obra inteira", "#6FC2B4"], ["pub", "★", "Primeira obra publicada", "#FFD54A"], ["com", "💬", "Primeiro comentário", "#C13DFF"],
  ["g10", "❝", "10 trechos grifados", "#E88AA8"], ["c20", "▤", "Obra com 20 capítulos", "#4FD1B8"],
];

// ───────── roteamento ─────────
export const rotas = [];
export const rota = (padrao, fn) => rotas.push([new RegExp("^" + padrao + "$"), fn]);
export const ir = (h) => { location.hash = h; };

// ───────── casca (menu lateral) ─────────
export function casca(aba, html) {
  const itens = [["/", "inicio", "Início"], ["/biblioteca", "biblioteca", "Biblioteca"], ["/escrever", "escrever", "Escrever"], ["/descobrir", "descobrir", "Descobrir"], ["/perfil", "perfil", "Perfil"]];
  const avisos = contarAvisos();
  raiz.innerHTML = `<div class="casca"><nav class="lado vidro"><div class="logo"><span class="losango"></span>Novely</div>
    ${itens.map(([h, ic, n]) => `<a class="item ${aba === ic ? "on" : ""}" href="#${h}">${svg(ic)}<span>${n}</span></a>`).join("")}
    <div class="sep-lado"></div>
    <a class="item extra ${aba === "buscar" ? "on" : ""}" href="#/buscar">${svg("lupa")}<span>Buscar</span></a>
    <a class="item extra ${aba === "avisos" ? "on" : ""}" href="#/avisos">${svg("sino")}<span>Avisos${avisos ? ` <b class="bolinha">${avisos}</b>` : ""}</span></a>
    <a class="item extra ${aba === "colab" ? "on" : ""}" href="#/colaborar">${svg("pessoas")}<span>Colaboração</span></a>
    <div class="rodape">${E.instalar ? `<button class="btn2 peq" id="instalar">⬇ Instalar no PC</button>` : ""}
      <a class="suave peq" href="#/perfil" style="padding:0 8px">${esc(nomeAutor())}</a></div></nav>
    <main class="conteudo">${html}</main></div>`;
  const b = $("#instalar"); if (b) b.onclick = async () => { await E.instalar.prompt(); E.instalar = null; };
}
export function contarAvisos() { return (local.ler("novidades", []) || []).length; }
export const carregando = (aba) => casca(aba, `<div class="carregando" style="height:60vh">◆</div>`);
