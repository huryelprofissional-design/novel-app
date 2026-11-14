// Efeitos 2.0 no PC: blocos [$efeito:cor campo | campo] e trechos {$efeito: texto}.
// Os antigos ([#…], {fogo: …}) continuam em fx.js. Mesmos nomes e campos do app (ui/EfeitosV2.kt).
const e = (s) => String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const campos = (s) => s.split(/(?<!\|)\|(?!\|)/).map((x) => x.trim()).filter(Boolean);
const num = (s, d = 0) => { const m = String(s ?? "").replace(/\./g, "").replace(",", ".").match(/-?\d+(\.\d+)?/); return m ? +m[0] : d; };
const seg = (s) => { const p = String(s || "").split(":").map((x) => +x || 0); return p.reduce((a, b) => a * 60 + b, 0) || num(s, 0); };
const hash = (s) => [...String(s)].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7);
const rnd = (semente) => { let x = semente || 1; return () => ((x = (x * 1103515245 + 12345) >>> 0) / 4294967296); };
const COR = { roxo: "#A47CFF", vermelho: "#FF3B4E", azul: "#5FB0FF", verde: "#3DDC84", dourado: "#FFC94A", rosa: "#FF5FB0", branco: "#E9E7F2", sombrio: "#7A6F8F", laranja: "#FF6A2B" };
const RARIDADE = (r) => { r = (r || "").toLowerCase(); return /^lend/.test(r) ? "#F5C04A" : /^m[ií]t/.test(r) ? "#FF4458" : /^[ée]p/.test(r) ? "#B57BFF" : /^incom/.test(r) ? "#6FCF97" : /^rar/.test(r) ? "#5CC8FF" : "#B0B0B8"; };
const RANK = (r) => ({ S: "#F5A623", SS: "#FF4458", SSS: "#FF2AD4", A: "#B57BFF", B: "#5CC8FF", C: "#6FCF97", D: "#C9C9C9", E: "#A0A0A0", F: "#808080" }[(r || "").toUpperCase()] || "#B57BFF");
const NUM_CN = "一二三四五六七八九十";
const letras = (s) => [...s].map((c, i) => `<i style="--i:${i}">${c === " " ? "&nbsp;" : e(c)}</i>`).join("");
const conta = (de, ate, ms, f, atraso = 0) => `data-conta="${de}|${ate}|${ms}|${f}|${atraso}"`;

// ───── blocos ─────
const B = {
  sistema: (c, t) => { const tit = c.length > 1 ? c[0] : "SISTEMA"; const ls = c.length > 1 ? c.slice(1) : c;
    return `<div class="v2-sis"><i class="v2-cn tl"></i><i class="v2-cn tr"></i><i class="v2-cn bl"></i><i class="v2-cn br"></i><b>◆ ${e(tit.toUpperCase())}</b>${ls.map((l, k) => `<p style="--d:${(0.75 + k * 0.3).toFixed(2)}s">${t(l).replace(/\?\?\?/g, '<span class="q">???</span>')}</p>`).join("")}</div>`; },
  avisos: (c, t) => `<div class="v2-pilha">${c.map((l, k) => `<div class="v2-nt" style="--d:${k * 0.5}s"><i>${/^[+\d]/.test(l) || /xp|moeda|ouro|\bg\b/i.test(l) ? "+" : /t[ií]tulo|conquista|novo/i.test(l) ? "★" : "!"}</i><span>${t(l)}</span></div>`).join("")}</div>`,
  recompensa: (c, t) => { const cor = RARIDADE(c[0]); return `<div class="v2-rec" style="--c:${cor}"><div class="v2-rcard"><span class="rr">${e((c[0] || "comum").toUpperCase())}</span><b>${e(c[1] || "")}</b>${c.slice(2).map((x) => `<small>${t(x)}</small>`).join("")}</div></div>`; },
  despertar: (c) => `<div class="v2-desp"><div class="fl"></div><div class="rk" style="--c:${RANK(c[0])}">${e((c[0] || "S").toUpperCase())}</div>${c.slice(1).map((x) => { const k = x.indexOf(":"); return `<p>${k > 0 ? `${e(x.slice(0, k + 1))} <b>${e(x.slice(k + 1).trim())}</b>` : e(x)}</p>`; }).join("")}</div>`,
  penalidade: (c, t) => { const s = seg(c[1] || "10"); return `<div class="v2-pen"><b>PENALIDADE</b><p>${t(c[0] || "")}</p><span ${conta(s, 0, s * 1000, "tempo3")}>00:00:00</span></div>`; },
  patrocinio: (c, t) => `<div class="v2-cheio" data-part="moeda|16"></div><div class="v2-pat"><small>✦ CONSTELAÇÃO</small><b>${e(c[0] || "")}</b><p>patrocinou você com <em ${conta(0, num(c[1], 100), 1600, "int")}>0</em> ${e(c[2] || "moedas")}.</p></div>`,
  portal: (c) => `<div class="v2-portal" style="--c:${RANK(c[0])}"><div class="anel"></div><div class="anel a2"></div><b>${e((c[0] || "A").toUpperCase())}</b></div><p class="v2-leg">PORTAL RANK ${e((c[0] || "A").toUpperCase())}${c[1] ? " · " + e(c[1].toUpperCase()) : ""}</p>`,
  morte: (c) => { const s = Math.max(0, num(c[1], 3)); return `<div class="v2-morte"><b class="v2-let">${letras((c[0] || "Você morreu").toUpperCase())}</b>${s ? `<p>Renascendo em <span ${conta(s, 0, s * 1000, "int", 1600)}>${s}</span>…</p>` : ""}</div>`; },
  recarga: (c) => { const s = Math.max(1, num(c[1], 10)); return `<div class="v2-cd" style="--t:${s}s"><svg viewBox="0 0 100 100" width="96" height="96"><circle cx="50" cy="50" r="42" fill="none" stroke="#1d2a3f" stroke-width="8"/><circle class="cda" cx="50" cy="50" r="42" fill="none" stroke="#5fb0ff" stroke-width="8" stroke-linecap="round"/></svg><b ${conta(s, 0, s * 1000, "int")}>${s}</b><p>${e(c[0] || "")}</p></div>`; },
  fusao: (c) => { const m = (c[0] || "").match(/^(.+?)\s*\+\s*(.+?)\s*=\s*(.+)$/) || [0, c[0] || "?", c[1] || "?", c[2] || "?"];
    return `<div class="v2-fus"><span class="f1">${e(m[1])}</span><span class="f2">${e(m[2])}</span><b>${e(m[3])}</b></div>`; },
  carregando: (c) => `<div class="v2-carr"><p>${e(c[0] || "Carregando")}…</p><div class="seg">${Array.from({ length: 20 }, (_, k) => `<i style="--i:${k}"></i>`).join("")}</div><b ${conta(0, Math.min(100, num(c[1], 100)), 3000, "pct")}>0%</b></div>`,
  pincel: (c) => `<div class="v2-pinc"><b>${e(c[0] || "")}</b>${c[1] ? `<p>${e(c[1])}</p>` : ""}<span class="selo">${e(c[2] || [...(c[0] || "印")].pop())}</span></div>`,
  jade: (c, t) => `<div class="v2-jade"><small>${e((c.length > 2 ? c[0] : "PLACA DE JADE").toUpperCase())}</small><b>${e(c.length > 2 ? c[1] : c[0] || "")}</b>${(c.length > 2 ? c.slice(2) : c.slice(1)).map((x) => `<span>${t(x)}</span>`).join("")}</div>`,
  ameixa: (c) => `<div class="v2-cheio" data-part="petala2|18"></div><div class="v2-amx"><b>${e(c[0] || "")}</b>${c.slice(1).map((x) => `<small>${e(x)}</small>`).join("")}</div>`,
  dez: (c) => `<div class="v2-dez"><small>${e((c[0] || "").toUpperCase())}</small><div class="rolo">${c.slice(1, 11).map((x, k) => `<p><em>${NUM_CN[k]}</em> ${e(x.replace(/^\d+[.)]?\s*/, ""))}</p>`).join("")}</div></div>`,
  constelacao: (c) => { const r = rnd(hash(c[0] || "x")); const P = Array.from({ length: 6 }, (_, k) => [18 + k * 37 + r() * 10, 18 + r() * 74]);
    return `<div class="v2-const"><svg viewBox="0 0 220 110" width="220" height="110"><path class="cl" d="M${P.slice(0, 5).map((p) => p.map((v) => v.toFixed(0)).join(" ")).join(" L")}"/><g>${P.map((p, k) => `<circle cx="${p[0].toFixed(0)}" cy="${p[1].toFixed(0)}" r="${k === 2 ? 3.8 : 3}" style="animation-delay:${(k * 0.1).toFixed(1)}s"/>`).join("")}</g></svg><b>${e(c[0] || "")}</b></div>`; },
  vitral: (c) => `<div class="v2-vit"><b>${e(c.join(" · "))}</b></div>`,
  brasao: (c) => { const nome = c[0] || ""; const ini = (nome.replace(/^(casa|clã|cla|família|familia|reino)\s+(de\s+)?/i, "")[0] || "?").toUpperCase();
    return `<div class="v2-bras"><div class="escudo"><b>${e(ini)}</b></div><div><strong>${e(nome)}</strong>${c[1] ? `<em>“${e(c[1])}”</em>` : ""}</div></div>`; },
  livro: (c, t) => `<div class="v2-livro"><div class="pg esq"><small>${e((c.length > 1 ? c[0] : "").toUpperCase())}</small></div><div class="pg dir"><p>${t(c.length > 1 ? c.slice(1).join(" ") : c[0] || "")}</p></div><div class="folha"></div></div>`,
  pocao: (c) => { const r = c.find((x) => /^(comum|incomum|raro|[ée]pico|lend|m[ií]t)/i.test(x)); const cor = r ? RARIDADE(r) : "#8f6bff";
    return `<div class="v2-poc" style="--c:${cor}"><div class="frasco"><div class="liq">${[30, 55, 70, 42].map((x, k) => `<i style="--x:${x}%;--d:${k * 0.5}s"></i>`).join("")}</div></div><div><b>${e(c[0] || "")}</b><small>${e(c.slice(1).join(" · "))}</small></div></div>`; },
  eclosao: (c) => `<div class="v2-ovo-w v2-toque"><div class="ovo"><i class="rach"></i></div><b>${e(c[0] || "")}</b><small class="dica">toque no ovo</small></div>`,
  juramento: (c, t) => `<div class="v2-jur"><i class="gota"></i><b>${t(c.join(" "))}</b></div>`,
  repete: (c) => { const n = Math.min(12, Math.max(2, num(c[1], 7))); return `<div class="v2-rep">${Array.from({ length: n }, (_, k) => `<p style="--k:${k}">${e(c[0] || "")}</p>`).join("")}</div>`; },
  "foto-muda": (c) => `<div class="v2-foto v2-toque"><div class="img"><i class="p1"></i><i class="p3"></i><i class="p2"></i></div><span>${e(c[0] || "")}</span>${c[1] ? `<small class="rev">${e(c[1])}</small>` : ""}</div>`,
  digital: (c) => `<div class="v2-dig"><div class="fp"><i class="scan"></i></div><div><small>BIOMETRIA</small><b>Corresponde: ${e(c[0] || "")}</b><em ${conta(0, Math.min(100, num(c[1], 98)), 2000, "pct")}>0%</em></div></div>`,
  cofre: (c, t) => { const d = (c[0] || "0-0-0").match(/\d/g)?.slice(0, 6) || ["0"]; const f = Array.from({ length: 20 }, (_, k) => k % 10).join("<br>");
    return `<div class="v2-cof" style="--n:${d.length}">${d.map((n, k) => `<div class="disco"><div class="fita" style="--n:${n};--d:${(k * 0.4).toFixed(1)}s">${f}</div></div>`).join("")}<b style="--a:${(d.length * 0.4 + 1.2).toFixed(1)}s">ABERTO</b>${c[1] ? `<p style="--a:${(d.length * 0.4 + 1.4).toFixed(1)}s">${t(c.slice(1).join(" "))}</p>` : ""}</div>`; },
  relogio: (c) => { const m = (c[0] || "00:00").match(/(\d{1,2})[:h](\d{2})/) || [0, 0, 0]; const h = +m[1] % 12, mi = +m[2];
    return `<div class="v2-rel" style="--h:${(h * 30 + mi * 0.5).toFixed(1)}deg;--m:${mi * 6}deg"><div class="mostr"><i class="h"></i><i class="m"></i><i class="ce"></i></div><div><b>${e(c[0] || "")}</b>${c[1] ? `<small>${e(c[1])}</small>` : ""}</div></div>`; },
  recortes: (c) => { const F = ["'Cinzel'", "'Special Elite'", "'Newsreader'", "'Inter'", "'Caveat'", "'Courier Prime'"], BG = [["#f2eee2", "#111"], ["#111", "#f2eee2"], ["#e8d24a", "#111"], ["#c93030", "#fff"], ["#fff", "#c93030"], ["#9cc3e6", "#111"]];
    return `<div class="v2-recorte">${[...(c.join(" "))].map((ch, i) => ch === " " ? '<span class="esp"></span>' : `<span style="--i:${i};--r:${((i * 37) % 13) - 6}deg;font-family:${F[(i * 7) % 6]},serif;background:${BG[(i * 5 + 2) % 6][0]};color:${BG[(i * 5 + 2) % 6][1]};font-weight:${i % 2 ? 800 : 400}">${e(ch)}</span>`).join("")}</div>`; },
  hud: (c) => { const dados = c.slice(1); const esq = dados.filter((_, k) => k % 2 === 0), dir = dados.filter((_, k) => k % 2 === 1);
    const li = (x) => { const m = x.match(/^(.*?)\s+([-\d.,]+\s*\S*)$/); return `<li>${m ? `${e(m[1].toUpperCase())} <b>${e(m[2])}</b>` : e(x.toUpperCase())}</li>`; };
    return `<div class="v2-hud"><i class="v2-cn tl"></i><i class="v2-cn tr"></i><i class="v2-cn bl"></i><i class="v2-cn br"></i><div class="mira"></div><ul class="ed">${esq.map(li).join("")}</ul><ul class="dd">${dir.map(li).join("")}</ul><p>${e((c[0] || "").toUpperCase())}</p></div>`; },
  autodestruicao: (c) => { const s = seg(c[0] || "60"); return `<div class="v2-auto"><div class="faixa"></div><small>${e((c[1] || "Autodestruição").toUpperCase())}</small><b ${conta(s, 0, s * 1000, "tempo")}>00:00</b><div class="faixa"></div></div>`; },
  dobra: (c) => `<div class="v2-dobra" data-part="risco|42"></div><p class="v2-dob-t">${e(c.join(" · "))}</p>`,
  dna: (c) => `<div class="v2-dna">${Array.from({ length: 16 }, (_, k) => `<i style="--k:${k}"><u></u><s></s></i>`).join("")}</div><p class="v2-dna-t">${e(c[0] || "").replace(/([\d.,]+%)/, "<b>$1</b>")}${c[1] ? " · " + e(c[1]) : ""}</p>`,
  "olho-ia": (c, t) => `<div class="v2-ia"><div class="iris"></div><div><small>${e((c.length > 1 ? c[0] : "IA").toUpperCase())}</small><p>${t(c.length > 1 ? c.slice(1).join(" ") : c[0] || "")}</p></div></div>`,
  radar: (c) => { const n = Math.min(8, Math.max(1, num(c[0], 3))); const r = rnd(hash(c.join()));
    return `<div class="v2-radar"><i class="var"></i>${Array.from({ length: n }, () => { const a = r() * Math.PI * 2, d = 0.2 + r() * 0.25; const x = 50 + Math.cos(a) * d * 100, y = 50 + Math.sin(a) * d * 100; const ang = ((Math.atan2(y - 50, x - 50) * 180 / Math.PI) + 90 + 360) % 360;
      return `<i class="blip" style="left:${x.toFixed(0)}%;top:${y.toFixed(0)}%;--d:${(ang / 360 * 3).toFixed(2)}s"></i>`; }).join("")}</div><p class="v2-radar-t">${e(c.join(" · ").toUpperCase())}</p>`; },
  video: (c) => { const s = seg(c[1] || "0"); return `<div class="v2-tela"><div class="av">${e((c[0] || "?")[0].toUpperCase())}</div><b>${e(c[0] || "")}</b><span ${conta(s, s + 3600, 3600000, "tempo")}>00:00</span><div class="eu"></div><div class="bts"><i></i><i class="desl"></i><i></i></div></div>`; },
  live: (c) => `<div class="v2-live"><div class="v2-cheio" data-part="coracao|10"></div><div class="lv-top"><span class="aov">AO VIVO</span><span>${e(c[1] || "")}</span></div><b>${e(c[0] || "")}</b><div class="lv-com">${c.slice(2).map((x, k) => { const d = x.indexOf(":"); return `<p style="--d:${(0.2 + k).toFixed(1)}s">${d > 0 ? `<em>${e(x.slice(0, d))}</em>${e(x.slice(d + 1).trim())}` : e(x)}</p>`; }).join("")}</div></div>`,
  pix: (c) => `<div class="v2-pix"><div class="ic">$</div><div><small>BANCO · agora</small><b>Pix recebido</b><p><strong ${conta(0, num(c[0], 100), 1400, "brl")}>R$ 0,00</strong>${c[1] ? ` de <em>${e(c[1])}</em>` : ""}</p></div></div>`,
  visto: (c) => `<div class="v2-visto"><div class="bal">${e(c[0] || "")}</div><div class="vi">Visto${c[1] ? " às " + e(c[1]) : ""}</div><div class="d3"><i></i><i></i><i></i></div></div>`,
  forum: (c) => `<div class="v2-forum"><div class="votos"><i>▲</i><span ${conta(Math.round(num(c[2], 100) / 20), num(c[2], 100), 2000, "int")}>0</span><i>▼</i></div><div><small>${e(c[0] || "")} · há 2 h</small><span class="tit">${e(c[1] || "")}</span><p>${num(c[3], 0)} comentários${c[4] ? ` · <em>${e(c[4])}</em>` : ""}</p></div></div>`,
  traduz: (c) => `<div class="v2-trad"><small>Traduzido${c[2] ? " do " + e(c[2]) : ""}</small><div class="tr-c"><b class="ko">${e(c[0] || "")}</b><b class="pt">${e(c[1] || "")}</b></div></div>`,
  alarme: (c) => `<div class="v2-alarme"><b>${e(c[0] || "06:00")}</b><p>${e(c[1] || "Alarme")}</p><div class="ab"><span>Adiar</span><span class="pa">Parar</span></div></div>`,
  batimento: (c, t) => { const m = (c[0] || "").match(/(\d+)\s*>\s*(\d+)/) || [0, 72, num(c[0], 120)]; return `<div class="v2-bat"><div class="cor">♥</div><div><b ${conta(+m[1], +m[2], 2600, "int")}>${m[1]}</b><span>bpm</span>${c[1] ? `<p>${t(c.slice(1).join(" "))}</p>` : ""}</div></div>`; },
  fio: (c) => `<div class="v2-fio"><b>${e(c[0] || "")}</b><svg viewBox="0 0 160 60" preserveAspectRatio="none"><path pathLength="100" d="M4 30 C 40 -10, 60 70, 80 30 S 130 -5, 156 30"/></svg><b>${e(c[1] || "")}</b></div>`,
  bilhete: (c, t) => `<div class="v2-bil v2-toque"><div class="b-in"><p>${t(c.length > 1 ? c.slice(1).join(" ") : c[0] || "")}</p></div><div class="b-tampa">${e(c.length > 1 ? c[0] : "toque para abrir")}</div></div>`,
  polaroid: (c) => `<div class="v2-pol"><div class="pi"><i class="sol"></i><i class="m1"></i><i class="m2"></i></div><span>${e(c[0] || "")}</span></div>`,
  "tempo-parado": (c, t) => `<div class="v2-tp"><div class="v2-cheio" data-part="poeira|26"></div><b>${t(c[0] || "")}</b>${c.slice(1).map((x) => `<small>${t(x)}</small>`).join("")}</div>`,
  abertura: (c) => `<div class="v2-ab"><b>${e(c[0] || "")}</b><div><small>CAPÍTULO</small><strong>${e(c[1] || "")}</strong></div></div>`,
  arco: (c) => `<div class="v2-cheio" data-part="brasa|18"></div><div class="v2-arco"><small>ARCO ${e((c[0] || "").toUpperCase())}</small><b>${e(c[1] || "")}</b><i></i></div>`,
  dias: (c) => { const m = (c[0] || "").match(/(\d+)\s*>\s*(\d+)/) || [0, 1, num(c[0], 100)]; const w = Math.max(3, String(m[2]).length);
    return `<div class="v2-dias"><small>${e((c[1] || "dias").toUpperCase())}</small><b ${conta(+m[1], +m[2], 2500, "pad" + w)}>${String(m[1]).padStart(w, "0")}</b></div>`; },
  coord: (c) => `<div class="v2-coord"><b data-embaralha="${e(c[0] || "")}">${e(c[0] || "")}</b>${c[1] ? `<p>${e(c.slice(1).join(" · "))}</p>` : ""}</div>`,
};
const APELIDO = { "patrocínio": "patrocinio", "fusão": "fusao", "poção": "pocao", "eclosão": "eclosao", ovo: "eclosao", "relógio": "relogio", "autodestruição": "autodestruicao", "vídeo": "video", "fórum": "forum", tradutor: "traduz", "brasão": "brasao", "constelação": "constelacao", recorte: "recortes", coordenadas: "coord", "foto": "foto-muda", "olho": "olho-ia", ia: "olho-ia", "tempoparado": "tempo-parado", "contador": "dias" };
export const BLOCOS2 = new Set(Object.keys(B));
/** Os que o leitor abre com um toque (não repetem ao tocar). */
const TOQUE = new Set(["eclosao", "foto-muda", "bilhete"]);
/** Os que vibram o celular (navegador do Android). */
const VIBRA = { despertar: [60, 40, 120], penalidade: [200, 100, 200], morte: [300], autodestruicao: [80, 60, 80], batimento: [40, 120, 40, 500, 40, 120, 40], recompensa: [30, 50, 30], juramento: [90] };

/** "[$sistema:azul …]" → html (ou null). */
export function blocoFx2(linha, tx) {
  const m = linha.match(/^\[\$([\wçãéíóúâêôõ-]+?)(?::([a-zçãéíóúâêô]+))?(?:\s+(.*))?\]$/i); if (!m) return null;
  const nome = APELIDO[m[1].toLowerCase()] || m[1].toLowerCase(); if (!B[nome]) return null;
  const cor = m[2] ? m[2].toLowerCase() : "";
  const estilo = cor && COR[cor] ? ` style="--cor:${COR[cor]}"` : "";
  return `<div class="fx2" data-fx2="${nome}"${estilo}>${B[nome](campos(m[3] || ""), tx || e)}</div>`;
}

// ───── trechos {$efeito:cor: texto} ─────
const INLINE2 = new Set(["corte", "corar", "aura", "cristal", "assinatura"]);
/** Recebe html já escapado. */
export function inlineFx2(h) {
  return h.replace(/\{\$([a-zçãéíóúâêô]+)(?::([a-zçãéíóúâêô]+))?:\s([^{}]+)\}/g, (all, n, cor, t) => {
    if (!INLINE2.has(n)) return all;
    const c = cor && COR[cor] ? ` style="--cor:${COR[cor]}"` : "";
    if (n === "corte") return `<span class="fx2i v2-corte" data-t="${t.replace(/"/g, "&quot;")}">${t}</span>`;
    if (n === "assinatura") return `<span class="fx2i v2-ass"${c}>${t}</span>`;
    return `<span class="fx2i v2-${n}"${c}>${t}</span>`;
  });
}

// ───── o que mexe: só começa quando chega na tela; tocar repete ─────
const FMT = {
  int: (v) => Math.round(v).toLocaleString("pt-BR"),
  pct: (v) => Math.round(v) + "%",
  brl: (v) => "R$ " + v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
  tempo: (v) => { v = Math.max(0, Math.round(v)); return (v >= 3600 ? String(v / 3600 | 0).padStart(2, "0") + ":" : "") + String((v / 60 | 0) % 60).padStart(2, "0") + ":" + String(v % 60).padStart(2, "0"); },
  tempo3: (v) => { v = Math.max(0, Math.round(v)); return [v / 3600 | 0, (v / 60 | 0) % 60, v % 60].map((n) => String(n).padStart(2, "0")).join(":"); },
};
const fmtDe = (f) => (f.startsWith("pad") ? (v) => String(Math.round(v)).padStart(+f.slice(3), "0") : FMT[f] || FMT.int);
function contar(el, vivo) {
  const [de, ate, ms, f, atraso] = el.dataset.conta.split("|"); const a = +de, b = +ate, dur = Math.max(1, +ms), fm = fmtDe(f || "int");
  const linear = f === "tempo" || f === "tempo3";
  el.textContent = fm(a);
  if (!vivo) return;
  const ini = performance.now() + (+atraso || 0);
  const passo = (t) => { if (!el.isConnected) return; const p = Math.min(1, Math.max(0, (t - ini) / dur)); const k = linear ? p : 1 - Math.pow(1 - p, 3);
    el.textContent = fm(a + (b - a) * k); if (p < 1) requestAnimationFrame(passo); };
  requestAnimationFrame(passo);
}
const R = (a, b) => a + Math.random() * (b - a);
function particulas(el) {
  const [tipo, qt] = el.dataset.part.split("|"); el.innerHTML = "";
  for (let i = 0; i < +qt; i++) {
    const s = document.createElement("i"); s.className = "v2-pt v2-pt-" + tipo; let t = R(1.6, 2.8);
    const st = { "--x": R(0, 100) + "%", "--y": R(0, 100) + "%", "--dx": R(-40, 40) + "px", "--dy": R(-60, 60) + "px" };
    if (tipo === "petala2") { t = R(1.4, 2.6); st["--y"] = R(5, 90) + "%"; st["--c"] = Math.random() < 0.6 ? "#e0405e" : "#fff2f4"; }
    if (tipo === "coracao") { t = R(2, 3.2); st["--dx"] = R(-50, 8) + "px"; st["--c"] = ["#ff3d6e", "#ff8fab", "#ffd1dc"][i % 3]; s.textContent = "♥"; }
    if (tipo === "brasa") t = R(2.5, 4.5);
    if (tipo === "risco") { t = R(0.6, 1.1); st["--ang"] = R(0, 360) + "deg"; }
    st["--t"] = t + "s"; st["--a"] = -R(0, t) + "s";
    for (const k in st) s.style.setProperty(k, st[k]);
    el.appendChild(s);
  }
}
function embaralhar(el) {
  const final = el.dataset.embaralha, ini = performance.now();
  const passo = (t) => { if (!el.isConnected) return; const p = (t - ini) / 1200; if (p >= 1) { el.textContent = final; return; }
    el.textContent = [...final].map((c, i) => /\d/.test(c) && i / final.length > p * 0.9 ? Math.random() * 10 | 0 : c).join(""); requestAnimationFrame(passo); };
  requestAnimationFrame(passo);
}
const vibrar = (nome) => { const v = VIBRA[nome]; if (!v || !navigator.vibrate || !navigator.userActivation?.hasBeenActive || matchMedia("(prefers-reduced-motion: reduce)").matches) return; try { navigator.vibrate(v); } catch (_) {} };

/** Liga os Efeitos 2.0 dentro de raiz. Devolve a função que desliga. */
export function ligarFx2(raiz) {
  const blocos = [...raiz.querySelectorAll(".fx2")];
  if (!blocos.length) return () => {};
  const comecar = (bl) => {
    if (!bl._h) bl._h = bl.innerHTML; else bl.innerHTML = bl._h;
    bl.classList.remove("on"); bl.classList.add("vivo");
    bl.querySelectorAll("[data-part]").forEach(particulas);
    bl.querySelectorAll("[data-conta]").forEach((el) => contar(el, true));
    bl.querySelectorAll("[data-embaralha]").forEach(embaralhar);
    vibrar(bl.dataset.fx2);
  };
  blocos.forEach((bl) => {
    bl._h = bl.innerHTML;
    bl.querySelectorAll("[data-conta]").forEach((el) => contar(el, false));
    const nome = bl.dataset.fx2;
    bl.onclick = (ev) => {
      if (ev.target.closest("a,button")) return;
      if (TOQUE.has(nome)) { bl.classList.toggle("on"); if (nome === "eclosao" && bl.classList.contains("on") && navigator.vibrate) try { navigator.vibrate([20, 40, 60]); } catch (_) {} return; }
      comecar(bl);
    };
  });
  const io = new IntersectionObserver((ents) => ents.forEach((en) => { if (en.isIntersecting && !en.target.classList.contains("vivo")) comecar(en.target); }), { threshold: 0.35 });
  blocos.forEach((bl) => io.observe(bl));
  return () => io.disconnect();
}

/** Para o Ouvir, a contagem e o "Remover efeito": o texto que fica. */
export function semEfeito2(linha) {
  const m = linha.trim().match(/^\[\$[^\s\]]+(?:\s+(.*))?\]$/); if (!m) return null;
  return campos(m[1] || "").join(". ");
}
export const RX_FX2_INLINE = /\{\$[a-zçãéíóúâêô]+(?::[a-zçãéíóúâêô]+)?:\s([^}]*)\}/g;

/** Catálogo (painel e cola): [nome, símbolo, gênero] */
export const MEGA2 = [
  ["Janela do Sistema 2.0", "[$sistema MISSÃO DE EMERGÊNCIA | Sobreviva até o amanhecer | Recompensa: ???]", "rpg"],
  ["Pilha de avisos", "[$avisos Você derrotou o Lobo das Cinzas | +300 XP | Novo título disponível]", "rpg"],
  ["Recompensa girando", "[$recompensa lendário | Espada do Vazio | +40 ATK]", "rpg"],
  ["Despertar", "[$despertar S | Classe: Necromante]", "rpg"],
  ["Penalidade", "[$penalidade Missão diária não cumprida | 00:00:10]", "rpg"],
  ["Patrocínio de constelação", "[$patrocinio O Rei sem Nome | 1000]", "rpg"],
  ["Portal aberto", "[$portal A | Gangnam]", "rpg"],
  ["Você morreu", "[$morte Você morreu | 3]", "rpg"],
  ["Recarga de habilidade", "[$recarga Corte Fantasma | 12s]", "rpg"],
  ["Fusão de habilidades", "[$fusao Chama + Vento = Tornado de Fogo]", "rpg"],
  ["Carregando do Sistema", "[$carregando Sincronizando com o usuário | 100]", "rpg"],
  ["Pincel de caligrafia", "[$pincel 天魔神功 | Arte Divina do Demônio Celestial]", "murim"],
  ["Placa de jade", "[$jade Ordem do Líder | Monte Hua | Quem portar esta placa fala pela seita.]", "murim"],
  ["Flores de ameixa", "[$ameixa Vinte e Quatro Ameixas Florescentes | 7ª forma]", "murim"],
  ["Os Dez Grandes", "[$dez Os Dez Grandes Mestres | Espada Celestial | Demônio Sangrento | Punho de Ferro do Norte]", "murim"],
  ["Constelação", "[$constelacao Órion Caído]", "fantasia"],
  ["Vitral", "[$vitral A Luz Não Perdoa]", "fantasia"],
  ["Brasão de casa", "[$brasao Casa Valerian | Ferro e Cinza]", "fantasia"],
  ["Livro que se abre", "[$livro Capítulo Perdido | No princípio, a torre não tinha portas.]", "fantasia"],
  ["Poção", "[$pocao Elixir da Lua | Cura 50% | raro]", "fantasia"],
  ["Ovo chocando (toque)", "[$eclosao Filhote de Wyvern]", "fantasia"],
  ["Juramento de sangue", "[$juramento Pelo meu sangue, eu juro.]", "fantasia"],
  ["Frase que se repete", "[$repete eu não estou sozinho | 7]", "terror"],
  ["Foto que muda (toque)", "[$foto-muda Acampamento, 1987]", "terror"],
  ["Digital", "[$digital Han Seo-jun | 98%]", "misterio"],
  ["Cofre", "[$cofre 4-1-7 | Dentro: uma carta e uma chave.]", "misterio"],
  ["Relógio parado", "[$relogio 03:14 | Hora da morte]", "misterio"],
  ["Bilhete de recortes", "[$recortes VOCÊ É O PRÓXIMO]", "misterio"],
  ["HUD do capacete", "[$hud Alerta: oxigênio baixo | O₂ 34% | ALT 880 m | PRESSÃO OK | TEMP -41°]", "scifi"],
  ["Autodestruição", "[$autodestruicao 00:59]", "scifi"],
  ["Salto no espaço", "[$dobra Saltando para Kepler-442]", "scifi"],
  ["Exame de DNA", "[$dna Compatível 99,7% | Humano?]", "scifi"],
  ["Olho da IA", "[$olho-ia MÃE | Bom dia, tripulação. Faltam 3 de vocês.]", "scifi"],
  ["Radar", "[$radar 3 contatos | 400 m]", "scifi"],
  ["Chamada de vídeo", "[$video Hana | 03:12]", "celular"],
  ["Live", "[$live yuna.k | 12,4 mil | jae_03: ela tá chorando?? | min.s: alguém liga pra polícia]", "celular"],
  ["Pix recebido", "[$pix 5000 | Desconhecido]", "celular"],
  ["Visto e sem resposta", "[$visto você tá bem? me responde | 03:14]", "celular"],
  ["Post de fórum", "[$forum torre_norte | Alguém mais viu a luz no 13º andar? | 342 | 87]", "celular"],
  ["Tradutor", "[$traduz 살려줘 | me ajude | coreano]", "celular"],
  ["Despertador", "[$alarme 06:00 | Primeiro dia na Academia]", "celular"],
  ["Coração acelerado", "[$batimento 72 > 140 | Ele sorriu.]", "romance"],
  ["Fio vermelho", "[$fio Hana | Seo-jun]", "romance"],
  ["Bilhete dobrado (toque)", "[$bilhete para: você | Me espera depois da aula.]", "romance"],
  ["Polaroid", "[$polaroid Nosso último verão]", "romance"],
  ["Tempo parado", "[$tempo-parado Tudo parou. | Nem a chuva caía.]", "acao"],
  ["Abertura 2.0", "[$abertura 12 | O Rei sem Nome]", "estrutura"],
  ["Começo de arco", "[$arco II | A Torre Caída]", "estrutura"],
  ["Contador de dias", "[$dias 1 > 100 | Dias desde o apocalipse]", "estrutura"],
  ["Coordenadas", "[$coord 37.5665° N · 126.9780° E | Seul]", "estrutura"],
  ["Palavra cortada", "{$corte: acabou}", "inline"],
  ["Corando", "{$corar: texto}", "inline"],
  ["Aura", "{$aura:roxo: texto}", "inline"],
  ["Cristal", "{$cristal: texto}", "inline"],
  ["Assinatura", "{$assinatura: Nome}", "inline"],
];
