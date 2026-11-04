// Efeitos visuais e progresso (XP, missões, baú) no PC — mesmo jeito do app Android.
import { E, $, esc, local, toast, db, doc, getDoc, setDoc, updateDoc, deleteDoc, increment } from "./nucleo.js";

// ───────── confete e celebração ─────────
const CORES = ["#FF6A2B", "#8E2DE2", "#4FA3FF", "#FFC94A", "#F07AAE", "#6FCF97"];
export function confete(n = 120) {
  const c = document.createElement("canvas"); c.className = "confete"; document.body.append(c);
  const w = (c.width = innerWidth), h = (c.height = innerHeight), g = c.getContext("2d");
  const ps = Array.from({ length: n }, () => ({ x: w / 2 + (Math.random() - .5) * 120, y: h * .35, vx: (Math.random() - .5) * 16, vy: -Math.random() * 16 - 6, r: Math.random() * 6.28, vr: (Math.random() - .5) * .4, c: CORES[Math.random() * CORES.length | 0], s: 6 + Math.random() * 6 }));
  const t0 = performance.now();
  (function quadro(t) {
    const k = (t - t0) / 2600; g.clearRect(0, 0, w, h);
    for (const p of ps) { p.vy += .45; p.vx *= .99; p.x += p.vx; p.y += p.vy; p.r += p.vr; g.save(); g.globalAlpha = Math.max(0, 1 - k); g.translate(p.x, p.y); g.rotate(p.r); g.fillStyle = p.c; g.fillRect(-p.s / 2, -p.s / 4, p.s, p.s / 2); g.restore(); }
    if (k < 1) requestAnimationFrame(quadro); else c.remove();
  })(t0);
}
export function celebrar(icone, titulo, sub) {
  const d = document.createElement("div"); d.className = "celebra";
  d.innerHTML = `<div class="raios"></div><div class="selo-c">${esc(icone)}</div><h2>${esc(titulo)}</h2><p>${esc(sub || "")}</p><button class="btn">Continuar</button>`;
  document.body.append(d); confete(160);
  d.querySelector("button").onclick = () => { d.classList.add("sai"); setTimeout(() => d.remove(), 300); };
}

// ───────── nível e XP (local neste navegador) ─────────
export const custoNivel = (n) => 300 + 150 * (n - 1);
export function infoNivel(xp) { let n = 1, r = xp; while (r >= custoNivel(n)) { r -= custoNivel(n); n++; } return { nivel: n, noNivel: r, precisa: custoNivel(n) }; }
const TITULOS = ["Leitor Curioso", "Leitor Voraz", "Devorador de Capítulos", "Guardião de Histórias", "Lenda do Novely", "Mestre das Mil Páginas"];
export const tituloNivel = (n) => TITULOS[Math.min(TITULOS.length - 1, Math.floor((n - 1) / 5))];
const dia = () => new Date().toISOString().slice(0, 10);
export function ganharXp(n) {
  const antes = local.ler("xp", 0), depois = antes + n; local.gravar("xp", depois);
  const a = infoNivel(antes).nivel, b = infoNivel(depois).nivel;
  if (b > a) setTimeout(() => celebrar("◆", `Nível ${b}!`, tituloNivel(b)), 400);
  else flutuar(`+${n} XP`);
}
function flutuar(t) { const s = document.createElement("div"); s.className = "xp-voa"; s.textContent = t; document.body.append(s); setTimeout(() => s.remove(), 1400); }

const POOL = { ler1: ["📖", "Ler 1 capítulo", "ler", 1], ler3: ["📚", "Ler 3 capítulos", "ler", 3], com1: ["💬", "Comentar um capítulo", "comentar", 1], gri1: ["❝", "Grifar um trecho", "grifar", 1], ouv1: ["🎧", "Ouvir um capítulo", "ouvir", 1], esc300: ["✎", "Escrever 300 palavras", "escrever", 300] };
export function missoes() {
  const d = dia(); let m = local.ler("missoes", null);
  if (!m || m.dia !== d) {
    let seed = [...d].reduce((s, c) => s * 31 + c.charCodeAt(0), 7) >>> 0;
    const ids = Object.keys(POOL).sort(() => ((seed = (seed * 1103515245 + 12345) >>> 0) / 4294967296) - .5).slice(0, 3);
    m = { dia: d, ids, cont: {} }; local.gravar("missoes", m);
  }
  return m.ids.map((id) => { const [ic, t, acao, alvo] = POOL[id]; return { id, ic, t, acao, alvo, feito: Math.min(alvo, m.cont[acao] || 0) }; });
}
const XP_ACAO = { ler: 20, comentar: 10, grifar: 5, ouvir: 25 };
export function registrar(acao, qtd = 1) {
  missoes(); const m = local.ler("missoes"); m.cont[acao] = (m.cont[acao] || 0) + qtd; local.gravar("missoes", m);
  ganharXp(acao === "escrever" ? Math.floor(qtd / 10) : (XP_ACAO[acao] || 0) * qtd);
}
export const bauPronto = () => missoes().every((x) => x.feito >= x.alvo) && local.ler("bau", "") !== dia();

export function cartaoNivelHTML() {
  const xp = local.ler("xp", 0), i = infoNivel(xp), f = i.noNivel / i.precisa;
  const ms = missoes(); const pronto = bauPronto(); const aberto = local.ler("bau", "") === dia();
  return `<div class="nivel-card"><div class="anel-xp" style="--f:${f}"><b>${i.nivel}</b></div><div style="flex:1;min-width:0"><div class="rotulo">Nível ${i.nivel}</div><div class="nm-nivel">${tituloNivel(i.nivel)}</div>
      <div class="barra-xp"><i style="width:${f * 100}%"></i></div><div class="suave peq">${i.noNivel} / ${i.precisa} XP para o nível ${i.nivel + 1}</div></div></div>
    <div class="missoes-card"><div style="display:flex;align-items:center;gap:10px"><h3 style="margin:0;flex:1">Missões de hoje</h3>
      <button class="bau ${pronto ? "pronto" : ""} ${aberto ? "aberto" : ""}" id="bau" title="${aberto ? "Baú aberto hoje" : pronto ? "Abrir o baú!" : "Complete as 3 missões"}">${aberto ? "🎁" : "🧰"}</button></div>
      ${ms.map((x) => `<div class="missao ${x.feito >= x.alvo ? "ok" : ""}"><span>${x.ic}</span><div style="flex:1"><div>${esc(x.t)}</div><div class="barra-xp fina"><i style="width:${x.feito / x.alvo * 100}%"></i></div></div><small>${x.feito >= x.alvo ? "✓" : `${x.feito}/${x.alvo}`}</small></div>`).join("")}</div>`;
}
export function ligarBau(redesenhar) {
  const b = $("#bau"); if (!b) return;
  b.onclick = () => { if (!bauPronto()) { toast(local.ler("bau", "") === dia() ? "Você já abriu o baú hoje" : "Complete as 3 missões para abrir"); return; } local.gravar("bau", dia()); celebrar("🎁", "Baú aberto!", "+50 XP de bônus"); local.gravar("xp", local.ler("xp", 0) + 50); redesenhar?.(); };
}

// ───────── reações do fim de capítulo ─────────
export const REACOES = [["chorei", "😭"], ["fogo", "🔥"], ["choque", "😱"], ["risada", "😂"], ["mente", "🤯"]];
export async function reacoesHTML(pid, cid) {
  let mapa = {}, minha = null;
  try { const [c, r] = await Promise.all([getDoc(doc(db, "publicas", pid, "caps", cid)), getDoc(doc(db, "publicas", pid, "caps", cid, "reacoes", E.usuario.uid))]); mapa = c.data()?.reacoes || {}; minha = r.data()?.r || null; } catch (_) {}
  return `<div class="reacoes" data-pid="${pid}" data-cid="${cid}" data-minha="${minha || ""}">${REACOES.map(([k, e]) => `<button class="reacao ${k === minha ? "on" : ""}" data-r="${k}"><span>${e}</span><small>${Math.max(0, mapa[k] || 0)}</small></button>`).join("")}</div>`;
}
export function ligarReacoes() {
  for (const box of document.querySelectorAll(".reacoes")) box.onclick = async (e) => {
    const b = e.target.closest("[data-r]"); if (!b) return;
    const { pid, cid } = box.dataset; const antiga = box.dataset.minha || null; const nova = b.dataset.r === antiga ? null : b.dataset.r;
    for (const x of box.children) { const n = x.querySelector("small"); if (x.dataset.r === antiga) { n.textContent = Math.max(0, +n.textContent - 1); x.classList.remove("on"); } if (x.dataset.r === nova) { n.textContent = +n.textContent + 1; x.classList.add("on", "pula"); setTimeout(() => x.classList.remove("pula"), 500); } }
    box.dataset.minha = nova || "";
    try {
      const cap = doc(db, "publicas", pid, "caps", cid); const mud = {};
      if (antiga) mud["reacoes." + antiga] = increment(-1); if (nova) mud["reacoes." + nova] = increment(1);
      if (nova) await setDoc(doc(cap, "reacoes", E.usuario.uid), { r: nova }); else await deleteDoc(doc(cap, "reacoes", E.usuario.uid));
      await updateDoc(cap, mud);
    } catch (err) { console.warn(err); }
  };
}

// ───────── clima da cena ─────────
export const ROTULO_CLIMA = { chuva: "🌧 Chuva", neve: "❄ Neve", fogo: "🔥 Fogo", vento: "🌬 Vento", noite: "🌙 Noite", tensao: "💓 Tensão", sangue: "🩸 Sangue", flashback: "🎞 Lembrança", sonho: "☁ Sonho", magia: "✨ Magia" };

/** Liga o clima no leitor: olha os parágrafos visíveis e desenha partículas por cima. Devolve uma função para desligar. */
export function ligarClima() {
  if (!local.ler("climaCena", true)) return () => {};
  const c = document.createElement("canvas"); c.className = "clima"; document.body.append(c);
  const pill = document.createElement("div"); pill.className = "pilula-clima"; document.body.append(pill);
  const g = c.getContext("2d"); let tipo = null, alfa = 0, ps = [], vivo = true, t0 = performance.now();
  const tam = () => { c.width = innerWidth; c.height = innerHeight; }; tam(); addEventListener("resize", tam);
  const novo = (k) => { const n = { chuva: 110, neve: 80, fogo: 50, noite: 28, magia: 45, sonho: 9, sangue: 14, flashback: 60 }[k] || 24; ps = Array.from({ length: n }, () => ({ x: Math.random(), y: Math.random(), v: .6 + Math.random() * .8, s: .5 + Math.random(), f: Math.random() * 6.28 })); };
  let alvo = null;
  // o efeito vem do autor: parágrafos entre ~chuva e ~fim têm data-clima
  const olhar = () => {
    const el = [...document.querySelectorAll("#texto [data-b]")].find((p) => p.getBoundingClientRect().bottom > innerHeight * 0.3);
    alvo = el?.dataset.clima || null;
    if (alvo) pill.textContent = ROTULO_CLIMA[alvo]; pill.classList.toggle("on", !!alvo);
  };
  const relogio = setInterval(olhar, 400);
  (function q(agora) {
    if (!vivo) return;
    if (alvo !== tipo) { alfa -= .03; if (alfa <= 0) { alfa = 0; tipo = alvo; if (tipo) novo(tipo); } } else if (tipo && alfa < 1) alfa = Math.min(1, alfa + .015);
    const t = (agora - t0) / 1000, w = c.width, h = c.height; g.clearRect(0, 0, w, h);
    const M = Math.max(w, h);
    const vinheta = (cor, a, r = .72) => { const gr = g.createRadialGradient(w / 2, h / 2, M * r * .35, w / 2, h / 2, M * r); gr.addColorStop(0, "rgba(0,0,0,0)"); gr.addColorStop(1, cor.replace("A", a)); g.fillStyle = gr; g.fillRect(0, 0, w, h); };
    if (tipo === "tensao") { const c = t % 1.1, bat = c < .12 ? c / .12 : c < .3 ? 1 - (c - .12) / .18 : c < .4 ? (c - .3) / .1 * .7 : c < .6 ? .7 * (1 - (c - .4) / .2) : 0; vinheta("rgba(42,0,8,A)", (.6 + .3 * bat) * alfa, .75 - .05 * bat); }
    else if (tipo === "sangue") { vinheta("rgba(138,0,16,A)", (.4 + .1 * Math.sin(t * 2)) * alfa); for (const p of ps) { const y = ((p.y + t * .05 * p.v) % 1) * h * .35, x = p.x * w; g.strokeStyle = `rgba(154,0,18,${.55 * alfa})`; g.lineWidth = 3 * p.s; g.lineCap = "round"; g.beginPath(); g.moveTo(x, 0); g.lineTo(x, y); g.stroke(); g.fillStyle = `rgba(176,0,26,${.7 * alfa})`; g.beginPath(); g.arc(x, y, 3.4 * p.s, 0, 6.28); g.fill(); } }
    else if (tipo === "flashback") { g.fillStyle = `rgba(112,66,20,${.22 * alfa})`; g.fillRect(0, 0, w, h); vinheta("rgba(26,14,4,A)", .65 * alfa); g.fillStyle = `rgba(255,255,255,${.12 * alfa})`; for (let k = 0; k < 60; k++) g.fillRect(Math.random() * w, Math.random() * h, 1.2, 1.2); }
    else if (tipo === "sonho") for (const p of ps) { const x = p.x * w + Math.sin(t * .2 * p.v + p.f) * w * .15, y = p.y * h + Math.cos(t * .17 * p.v + p.f) * h * .1, r = 160 * p.s; const gr = g.createRadialGradient(x, y, 0, x, y, r); gr.addColorStop(0, p.f > 3.14 ? `rgba(183,156,255,${.22 * alfa})` : `rgba(255,199,232,${.22 * alfa})`); gr.addColorStop(1, "rgba(0,0,0,0)"); g.fillStyle = gr; g.fillRect(x - r, y - r, r * 2, r * 2); }
    else if (tipo === "magia") for (const p of ps) { const y = h - ((p.y + t * .08 * p.v) % 1) * h, x = p.x * w + Math.sin(t * 1.3 + p.f) * 18, pi = (Math.sin(t * 4 * p.v + p.f) + 1) / 2, r = 6 * p.s * (.5 + pi); g.strokeStyle = p.f > 3.14 ? `rgba(255,216,107,${.85 * alfa * pi})` : `rgba(197,155,255,${.85 * alfa * pi})`; g.lineWidth = 1.2; g.beginPath(); g.moveTo(x - r, y); g.lineTo(x + r, y); g.moveTo(x, y - r); g.lineTo(x, y + r); g.stroke(); }
    else if (tipo) for (const p of ps) {
      if (tipo === "chuva") { const y = ((p.y + t * 1.3 * p.v) % 1) * (h + 80) - 40, x = ((p.x + t * .12 * p.v) % 1) * w; g.strokeStyle = `rgba(191,216,255,${.35 * alfa * p.s})`; g.lineWidth = 1.2; g.beginPath(); g.moveTo(x, y); g.lineTo(x - 5 * p.v, y + 22 * p.v); g.stroke(); }
      else if (tipo === "neve") { const y = ((p.y + t * .07 * p.v) % 1) * (h + 20) - 10, x = p.x * w + Math.sin(t * .9 + p.f) * 22; g.fillStyle = `rgba(255,255,255,${.7 * alfa * (.4 + p.s / 2)})`; g.beginPath(); g.arc(x, y, 2 * p.s, 0, 6.28); g.fill(); }
      else if (tipo === "fogo") { const k = (p.y + t * .18 * p.v) % 1, y = h - k * h * .75, x = p.x * w + Math.sin(t * 2 + p.f) * 14, b = (1 - k) * (.6 + .4 * Math.sin(t * 9 + p.f)); g.fillStyle = `rgba(255,138,42,${.6 * alfa * b})`; g.beginPath(); g.arc(x, y, 2.4 * p.s, 0, 6.28); g.fill(); }
      else if (tipo === "noite") { const x = (p.x * w + Math.sin(t * .3 * p.v + p.f) * 40 + w) % w, y = (p.y * h + Math.cos(t * .25 * p.v + p.f) * 30 + h) % h, pu = ((Math.sin(t * 1.6 * p.v + p.f) + 1) / 2) ** 2; g.fillStyle = `rgba(232,255,138,${.25 * alfa * pu})`; g.beginPath(); g.arc(x, y, 8, 0, 6.28); g.fill(); g.fillStyle = `rgba(244,255,196,${.9 * alfa * pu})`; g.beginPath(); g.arc(x, y, 1.8, 0, 6.28); g.fill(); }
      else { const x = ((p.x + t * .35 * p.v) % 1) * (w + 200) - 100, y = p.y * h + Math.sin(t * 1.5 + p.f) * 16; g.strokeStyle = `rgba(255,255,255,${.18 * alfa * p.s})`; g.lineWidth = 1; g.beginPath(); g.moveTo(x, y); g.lineTo(x - 40 * p.v, y - 3); g.stroke(); }
    }
    requestAnimationFrame(q);
  })(t0);
  return () => { vivo = false; c.remove(); pill.remove(); removeEventListener("resize", tam); clearInterval(relogio); };
}

/** Cabeçalho do fim de capítulo: anel com quanto já leu e os ganhos. */
export function fimHTML(n, total, lidos) {
  return `<div class="fim-cine"><div class="anel-xp grande" style="--f:${total ? lidos / total : 1}"><b>${n}</b></div>
    <div class="rotulo" style="color:#FF6A2B;margin-top:10px">Fim do capítulo ${n}</div><div class="suave peq">${lidos} de ${total} capítulos lidos</div>
    <div class="ganhos"><span>🪙 +1</span><span>⚡ +20 XP</span></div></div>`;
}
window.efeitos = { confete, celebrar, registrar };
