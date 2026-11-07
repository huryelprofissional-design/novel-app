// Vozes do Novely no PC: as mesmas vozes neurais do celular, rodando no navegador (WebAssembly, sem instalar nada).
// Pronúncia: espeak-ng (voz/fonemas-worker.js). Voz: sherpa-onnx (voz/tts-worker.js).
// Os arquivos ficam no próprio site (vozes/) e são guardados no navegador depois do primeiro download.

export const VOZES = [
  { id: "novely:faber", nome: "Faber", pacote: "piper-faber", sid: 0, feminina: false, descricao: "masculina, clara" },
  { id: "novely:cadu", nome: "Cadu", pacote: "piper-cadu", sid: 0, feminina: false, descricao: "masculina, grave" },
  { id: "novely:jeff", nome: "Jeff", pacote: "piper-jeff", sid: 0, feminina: false, descricao: "masculina, jovem" },
  { id: "novely:edresson", nome: "Edresson", pacote: "piper-edresson", sid: 0, feminina: false, descricao: "masculina, leve" },
  { id: "novely:dora-jovem", nome: "Dora jovem", pacote: "kokoro", sid: 1, feminina: true, descricao: "feminina, jovem" },
  { id: "novely:dora-expressiva", nome: "Dora expressiva", pacote: "kokoro", sid: 2, feminina: true, descricao: "feminina, expressiva" },
  { id: "novely:dora-sussurrada", nome: "Dora sussurrada", pacote: "kokoro", sid: 3, feminina: true, descricao: "feminina, sussurrada" },
];
// arquivos de cada pacote (os grandes vêm em partes de até 90 MB por causa do limite do site)
const ARQUIVOS = {
  "piper-faber": ["model.onnx", "tokens.txt"], "piper-cadu": ["model.onnx", "tokens.txt"],
  "piper-jeff": ["model.onnx", "tokens.txt"], "piper-edresson": ["model.onnx", "tokens.txt"],
  kokoro: ["model.int8.onnx", "tokens.txt", "vozes-novely.bin"],
};
const TAMANHO_MB = { "piper-faber": 63, "piper-cadu": 63, "piper-jeff": 63, "piper-edresson": 61, kokoro: 142 };
const CACHE = "novely-vozes-v1";
const RAIZ = new URL("vozes/", location.href).href;

export const ehNeural = (nome) => typeof nome === "string" && nome.startsWith("novely:");
export const vozNeural = (nome) => VOZES.find((v) => v.id === nome) || null;
const chaveOk = (p) => "vozOk:" + p;
export const instalada = (v) => { try { return localStorage.getItem(chaveOk(v.pacote)) === "1"; } catch (_) { return false; } };
export const faltaMb = (v) => (instalada(v) ? 0 : TAMANHO_MB[v.pacote] || 0);
export const instaladas = () => VOZES.filter(instalada);

// ───────── baixar e guardar no navegador ─────────
async function lerComPartes(url, aoProgresso) {
  const manifesto = await fetch(url + ".partes").then((r) => (r.ok ? r.text() : null)).catch(() => null);
  const partes = manifesto ? manifesto.trim().split("\n").filter(Boolean) : [url.split("/").pop()];
  const base = url.replace(/[^/]*$/, "");
  const blobs = [];
  for (const nome of partes) {
    const r = await fetch(base + nome); if (!r.ok) throw new Error(`${nome}: ${r.status}`);
    const total = +r.headers.get("content-length") || 0; const leitor = r.body.getReader(); const pedacos = []; let feito = 0;
    while (true) { const { done, value } = await leitor.read(); if (done) break; pedacos.push(value); feito += value.length; aoProgresso?.(value.length, total); }
    blobs.push(new Blob(pedacos));
  }
  return new Blob(blobs);
}

/** Baixa o pacote da voz e guarda no navegador. aoProgresso(0..1). */
export async function baixar(v, aoProgresso) {
  const cache = await caches.open(CACHE);
  const arqs = ARQUIVOS[v.pacote]; let feito = 0; const total = (TAMANHO_MB[v.pacote] || 1) * 1024 * 1024;
  for (const a of arqs) {
    const url = RAIZ + v.pacote + "/" + a;
    if (await cache.match(url)) continue;
    const blob = await lerComPartes(url, (n) => { feito += n; aoProgresso?.(Math.min(0.99, feito / total)); });
    await cache.put(url, new Response(blob));
  }
  try { localStorage.setItem(chaveOk(v.pacote), "1"); } catch (_) {}
  aoProgresso?.(1);
}
export async function apagar(pacote) {
  const cache = await caches.open(CACHE);
  for (const a of ARQUIVOS[pacote] || []) await cache.delete(RAIZ + pacote + "/" + a);
  try { localStorage.removeItem(chaveOk(pacote)); } catch (_) {}
}

// ───────── trabalhadores (pronúncia + geradores de voz) ─────────
function canal(w) {
  let n = 0; const pend = new Map();
  w.onmessage = (e) => { const p = pend.get(e.data.id); pend.delete(e.data.id); if (!p) return; e.data.erro ? p[1](new Error(e.data.erro)) : p[0](e.data); };
  w.onerror = (e) => { for (const p of pend.values()) p[1](new Error(e.message || "falha no gerador de voz")); pend.clear(); };
  return (msg, transf) => new Promise((ok, err) => { const id = ++n; pend.set(id, [ok, err]); w.postMessage({ ...msg, id }, transf || []); });
}
let fonemas = null;
const geradores = {}; // pacote → [{ chamar, ocupado }]
function pool(pacote) {
  if (geradores[pacote]) return geradores[pacote];
  // o Kokoro é pesado: vários geradores em paralelo (cada um prepara um trecho) para acompanhar a leitura
  const n = pacote === "kokoro" ? Math.max(1, Math.min(3, (navigator.hardwareConcurrency || 4) - 2)) : 1;
  geradores[pacote] = Array.from({ length: n }, () => ({ chamar: canal(new Worker(new URL("voz/tts-worker.js", location.href))), carregado: null, fila: Promise.resolve() }));
  return geradores[pacote];
}
async function carregar(g, pacote) {
  if (!g.carregado) g.carregado = (async () => {
    const cache = await caches.open(CACHE); const arquivos = {};
    for (const a of ARQUIVOS[pacote]) { const r = await cache.match(RAIZ + pacote + "/" + a); if (!r) throw new Error("voz não baixada"); arquivos[a] = await r.arrayBuffer(); }
    await g.chamar({ tipo: "carregar", pacote, arquivos }, Object.values(arquivos));
  })();
  return g.carregado;
}
let proximo = 0;
/** Gera o áudio de um trecho (Float32Array + taxa). */
export async function gerar(v, texto, velocidade = 1) {
  fonemas ||= canal(new Worker(new URL("voz/fonemas-worker.js", location.href)));
  const f = (await fonemas({ texto, lingua: "pt-br" })).fonemas;
  if (!f.length) return { amostras: new Float32Array(0), taxa: 22050 };
  const gs = pool(v.pacote); const g = gs[proximo++ % gs.length];
  const tarefa = g.fila.then(async () => { await carregar(g, v.pacote); return g.chamar({ tipo: "gerar", pacote: v.pacote, fonemas: f, sid: v.sid, velocidade }); });
  g.fila = tarefa.catch(() => {});
  const r = await tarefa;
  return { amostras: r.amostras, taxa: r.taxa };
}

/** Liga o motor da voz antes de precisar (a primeira vez abre arquivos grandes). */
export function aquecer(v) {
  if (!instalada(v)) return;
  fonemas ||= canal(new Worker(new URL("voz/fonemas-worker.js", location.href)));
  fonemas({ texto: "a", lingua: "pt-br" }).catch(() => {});
  for (const g of pool(v.pacote)) carregar(g, v.pacote).catch(() => {});
}
/** Divide em frases (juntando as curtas até ~140 letras): o primeiro áudio sai bem antes. */
export function frases(t) {
  const fr = t.match(/[^.!?…]+(?:[.!?…]+["”»)]*|$)\s*/g) || [t]; const out = []; let atual = "";
  for (const f of fr) { if (atual && (atual + f).length > 140) { out.push(atual.trim()); atual = ""; } atual += f; }
  if (atual.trim()) out.push(atual.trim());
  return out.length ? out : [t];
}
// gera com antecedência (para não ter pausa entre um trecho e outro); guarda só os últimos 60
const pre = new Map();
export function preparar(v, texto, velocidade = 1) {
  const k = `${v.id}|${velocidade}|${texto}`;
  if (!pre.has(k)) { pre.set(k, gerar(v, texto, velocidade)); pre.get(k).catch(() => pre.delete(k)); while (pre.size > 60) pre.delete(pre.keys().next().value); }
  return pre.get(k);
}
export const esquecer = () => pre.clear();

// ───────── tocar ─────────
let ctx = null, fonte = null, acabou = null, tocou = false;
export const tocouAlgo = () => tocou;
/** Toca até o fim; a promessa resolve quando termina (ou quando parar() é chamado). */
export function tocar(a) {
  ctx ||= new (window.AudioContext || window.webkitAudioContext)();
  if (ctx.state === "suspended") ctx.resume();
  calar();
  if (!a.amostras.length) return Promise.resolve();
  const buf = ctx.createBuffer(1, a.amostras.length, a.taxa); buf.copyToChannel(a.amostras, 0);
  const s = ctx.createBufferSource(); s.buffer = buf; s.connect(ctx.destination); fonte = s;
  return new Promise((ok) => { acabou = ok; s.onended = () => { if (fonte === s) { fonte = null; acabou = null; } ok(); }; s.start(); tocou = true; });
}
export function calar() { const s = fonte; fonte = null; if (s) { s.onended = null; try { s.stop(); } catch (_) {} } const f = acabou; acabou = null; f?.(); }
export function pausar() { if (ctx?.state === "running") ctx.suspend(); }
export function continuar() { if (ctx?.state === "suspended") ctx.resume(); }
export const pausado = () => ctx?.state === "suspended";
export async function testar(v, velocidade = 1) { await tocar(await gerar(v, "Olá! É assim que eu leio a sua história.", velocidade)); }
