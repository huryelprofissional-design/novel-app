// Leitor (biblioteca e catálogo), editor e revisão.
import { blocoFx, inlineFx, MEGA, ligarFx } from "./fx.js";
import { ligarEfeitosTexto, ligarClima, registrar, confete, reacoesHTML, ligarReacoes, fimHTML } from "./efeitos.js";
import {
  E, $, $$, esc, raiz, agora, toast, milhar, palavrasFmt, haQuanto, casca, carregando, rota, ir, local, limparMarcas, mapaNomes, contarPalavras, partesUnicas, confianca, LIMIAR_FORTE, LIMIAR_LEVE, ignoradosNome,
  capitulos, fichas, marcadores, salvarCapitulo, salvarObra, salvarItem, apagarItem, lerTexto, colTextos, salvarTexto, novoId, dialogo, confirmar,
  obraPub, capsPub, fichasPub, imagemPub, seguindo, seguir, minhaNota, avaliar, contarLeitura, registrarLeitura, curtiu, curtir, comentarios, comentar, apagarComentario,
  collection, db, textoRevisao, notas, anotar, apagarNota, nomeAutor, salvarFicha, perguntar, enfeiteHTML, corDaImagem, corDaBase, usarCorDaObra, carteira, liberado, ganharMoedas,
} from "./nucleo.js";
import { verFicha } from "./obra.js";
import * as NV from "./vozneural.js";
import * as X from "./extras.js";

// ───────── preferências de leitura ─────────
const PADRAO = { tam: 20, fonte: "serif", altura: 1.75, largura: 760, tema: "escuro", justificar: false, capitular: true, maratona: false };
const pref = () => ({ ...PADRAO, ...local.ler("leitura", {}) });
const FONTES = { serif: 'Newsreader, Georgia, serif', sans: 'Inter, system-ui, sans-serif', livro: '"Literata", Georgia, serif', mono: '"JetBrains Mono", ui-monospace, monospace' };
const TEMAS = { escuro: ["#000", "#E8E8E8"], sepia: ["#1E1813", "#E9DCC6"], claro: ["#F6F3EE", "#1C1C1C"], cinza: ["#151719", "#D7DADF"], noite: ["#000", "#A9824F"] };
let acentoLeitura = null; // cor da capa da obra aberta
function estiloLeitura() {
  const p = pref(); const [f, t] = TEMAS[p.tema] || TEMAS.escuro;
  const ac = p.tema === "noite" ? "#D39A55" : acentoLeitura;
  return `--tam:${p.tam}px;--fonte-leitura:${FONTES[p.fonte]};--altura:${p.altura};--largura:${p.largura}px;--fundo-leitura:${f};--texto-leitura:${t};--alinhar:${p.justificar ? "justify" : "left"}${ac ? `;--fala:${ac};--verde:${ac};--acento:${ac}` : ""}`;
}
async function corDoLeitor(url, base) { acentoLeitura = usarCorDaObra() ? ((url && await corDaImagem(url)) || (base ? corDaBase(base) : null)) : null; }
const cartaoAnterior = (c) => c?.resumo ? `<div class="anteriormente"><div class="rotulo">Anteriormente…</div><p>${esc(c.resumo)}</p><span class="suave peq">${esc(c.titulo || "")}</span></div>` : "";
// ───────── HUD do leitor (protótipo "Reader HUD"): barra de vidro em cima e embaixo ─────────
const ICO = {
  voltar: '<path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/>', editar: '<path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>',
  ouvir: '<path d="M11 5L6 9H2v6h4l5 4V5z"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/>',
  buscar: '<circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>', capitulos: '<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>',
  rolar: '<path d="M12 5v14"/><path d="M19 12l-7 7-7-7"/>', leitura: '<path d="M4 19L10 5h4l6 14"/><path d="M6.5 14h11"/>',
};
const svg = (k, t = 20) => `<svg width="${t}" height="${t}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICO[k]}</svg>`;
const ocultosHud = () => new Set(local.ler("ocultosHud", []));
function hudTopo(voltar, titulo, editar) {
  return `<div class="controles vidro hud-topo"><a class="hud-ic" href="${voltar}" title="Voltar" aria-label="Voltar">${svg("voltar")}</a><span class="tt">${titulo}</span>${editar ? `<a class="hud-ic" href="${editar}" title="Editar" aria-label="Editar capítulo">${svg("editar", 18)}</a>` : ""}</div>`;
}
function hudBaixo() {
  const o = ocultosHud();
  const b = (id, k, rot) => o.has(k) ? "" : `<button class="hud-bt" id="${id}" aria-label="${rot}">${svg(k)}<span class="rot-hud">${rot}</span></button>`;
  return `<nav class="hud-baixo vidro">${b("ouvir", "ouvir", "Ouvir")}${b("lbusca", "buscar", "Buscar")}${b("lcaps", "capitulos", "Capítulos")}${b("lrolar", "rolar", "Rolar")}<button class="hud-bt" id="aj" aria-label="Leitura">${svg("leitura")}<span class="rot-hud">Leitura</span></button></nav>`;
}
/** As barras fixas (em cima, embaixo e a da voz) ficam no centro da coluna do texto, não da janela (com o menu lateral, ficavam tortas). */
function centralizarHud() {
  const t = $("#texto") || $(".leitor"); if (!t) return;
  const r = t.getBoundingClientRect();
  document.documentElement.style.setProperty("--hud-x", `${Math.round(r.left + r.width / 2)}px`);
}
addEventListener("resize", centralizarHud, { passive: true });
let rolagem = null;
function pararRolagem() { if (rolagem) { clearInterval(rolagem); rolagem = null; $("#lrolar")?.classList.remove("on"); } }
/** Buscar no capítulo (marca todas e vai passando) e Rolar sozinho. */
function ligarHud() {
  centralizarHud(); requestAnimationFrame(centralizarHud);
  pararRolagem();
  const r = $("#lrolar");
  if (r) r.onclick = () => { if (rolagem) { pararRolagem(); return; } r.classList.add("on"); rolagem = setInterval(() => scrollBy(0, 1), 28); };
  addEventListener("wheel", pararRolagem, { passive: true, once: true });
  let termo = "", achados = [], k = 0;
  const bb = $("#lbusca");
  if (bb) bb.onclick = async () => {
    const t = await perguntar("Buscar neste capítulo", termo, "nome, frase…"); if (t === null || !t.trim()) return;
    if (t.trim().toLowerCase() !== termo.toLowerCase()) {
      termo = t.trim(); k = 0;
      $$("#texto mark.achado").forEach((m) => m.replaceWith(document.createTextNode(m.textContent)));
      const rx = new RegExp(termo.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "gi");
      const andar = document.createTreeWalker($("#texto"), NodeFilter.SHOW_TEXT); const nos = []; while (andar.nextNode()) nos.push(andar.currentNode);
      nos.forEach((n) => { if (!rx.test(n.textContent)) return; rx.lastIndex = 0; const f = document.createElement("span");
        f.innerHTML = esc(n.textContent).replace(new RegExp(esc(termo).replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "gi"), (m) => `<mark class="achado">${m}</mark>`); n.replaceWith(f); });
      achados = $$("#texto mark.achado");
    } else k = (k + 1) % Math.max(1, achados.length);
    if (!achados.length) { toast("Não achei “" + termo + "”"); return; }
    achados.forEach((m, j) => m.classList.toggle("atual", j === k)); achados[k].scrollIntoView({ block: "center", behavior: "smooth" });
    toast(`${k + 1} de ${achados.length} · toque em Buscar de novo para o próximo`);
  };
}

function painelAjustes(aoMudar) {
  const p = pref();
  const d = dialogo(`<h3 style="margin-top:0">Leitura</h3>
    <label class="rot">Tamanho do texto · <span id="vt">${p.tam}px</span></label><input type="range" min="14" max="32" value="${p.tam}" id="tam" class="faixa">
    <label class="rot">Espaço entre linhas</label><input type="range" min="1.3" max="2.3" step=".05" value="${p.altura}" id="alt" class="faixa">
    <label class="rot">Largura da coluna</label><input type="range" min="520" max="1000" step="20" value="${p.largura}" id="larg" class="faixa">
    <label class="rot">Fonte</label><div class="chips">${[["serif", "Serifa"], ["sans", "Sem serifa"], ["livro", "Livro"], ["mono", "Máquina"]].map(([k, n]) => `<span class="chip ${p.fonte === k ? "on" : ""}" data-fonte="${k}">${n}</span>`).join("")}</div>
    <label class="rot">Fundo</label><div class="chips">${[["escuro", "Preto"], ["cinza", "Grafite"], ["sepia", "Sépia"], ["claro", "Claro"], ["noite", "Noite profunda"]].map(([k, n]) => `<span class="chip ${p.tema === k ? "on" : ""}" data-tema="${k}">${n}</span>`).join("")}</div>
    <label class="troca" style="margin-top:14px"><input type="checkbox" id="just" ${p.justificar ? "checked" : ""}> Justificar o texto</label>
    <label class="troca" style="margin-top:10px"><input type="checkbox" id="capi" ${p.capitular ? "checked" : ""}> Primeira letra grande (capitular)</label>
    <label class="troca" style="margin-top:10px"><input type="checkbox" id="mara" ${p.maratona ? "checked" : ""}> Maratona: próximo capítulo começa sozinho</label>
    <label class="troca" style="margin-top:10px"><input type="checkbox" id="cobra" ${usarCorDaObra() ? "checked" : ""}> Cores da capa da obra</label>
    <label class="troca" style="margin-top:10px"><input type="checkbox" id="clima" ${local.ler("climaCena", true) ? "checked" : ""}> Clima da cena (chuva, neve, fogo…)</label>
    <label class="rot" style="margin-top:14px">Botões da barra de baixo</label><div class="chips">${[["ouvir", "Ouvir"], ["buscar", "Buscar"], ["capitulos", "Capítulos"], ["rolar", "Rolar"]].map(([k, n]) => `<span class="chip ${ocultosHud().has(k) ? "" : "on"}" data-hud="${k}">${n}</span>`).join("")}</div>
    <span class="suave peq">Vale ao abrir o próximo capítulo.</span>
    <label class="rot" style="margin-top:14px">Ouvir</label>
    <label class="troca"><input type="checkbox" id="vpers" ${local.ler("vozesPersonagens", true) ? "checked" : ""}> Uma voz para cada personagem (pelo “disse Fulano”)</label>
    <select class="campo" id="narr" style="margin-top:8px"><option value="">Narrador: voz padrão</option>${speechSynthesis.getVoices().filter((v) => v.lang?.toLowerCase().startsWith("pt")).map((v) => `<option value="${esc(v.name)}" ${v.name === local.ler("narradorPC", "") ? "selected" : ""}>Narrador: ${esc(v.name)}</option>`).join("")}</select>
    <label class="rot" style="margin-top:14px">Ler em outro idioma</label>
    <select class="campo" id="idi">${IDIOMAS.map(([k, n]) => `<option value="${k}" ${k === local.ler("idiomaLeitura", "") ? "selected" : ""}>${n}</option>`).join("")}</select>
    <span class="suave peq">Tradução feita pelo próprio navegador (Chrome ou Edge recentes), sem internet depois de baixar o idioma.</span>
    <div class="linha-botoes"><button class="btn2 peq" id="pad">Voltar ao padrão</button><button class="btn peq" data-n>Pronto</button></div>`);
  const grava = (k, v) => { local.gravar("leitura", { ...pref(), [k]: v }); aoMudar(); };
  $("#tam", d).oninput = (e) => { $("#vt", d).textContent = e.target.value + "px"; grava("tam", +e.target.value); };
  $("#alt", d).oninput = (e) => grava("altura", +e.target.value);
  $("#larg", d).oninput = (e) => grava("largura", +e.target.value);
  $("#just", d).onchange = (e) => grava("justificar", e.target.checked);
  $("#capi", d).onchange = (e) => { grava("capitular", e.target.checked); window.redesenhar(); };
  $("#mara", d).onchange = (e) => grava("maratona", e.target.checked);
  $("#cobra", d).onchange = (e) => { local.gravar("corDaObra", e.target.checked); window.redesenhar(); };
  $("#vpers", d).onchange = (e) => local.gravar("vozesPersonagens", e.target.checked);
  $("#clima", d).onchange = (e) => { local.gravar("climaCena", e.target.checked); window.redesenhar(); };
  $("#narr", d).onchange = (e) => local.gravar("narradorPC", e.target.value);
  $("#idi", d).onchange = (e) => { local.gravar("idiomaLeitura", e.target.value); d.close(); window.redesenhar(); };
  $$("[data-hud]", d).forEach((c) => (c.onclick = () => { const o = ocultosHud(); const k = c.dataset.hud; if (o.has(k)) o.delete(k); else o.add(k); local.gravar("ocultosHud", [...o]); c.classList.toggle("on", !o.has(k)); }));
  $$("[data-fonte]", d).forEach((c) => (c.onclick = () => { grava("fonte", c.dataset.fonte); $$("[data-fonte]", d).forEach((x) => x.classList.toggle("on", x === c)); }));
  $$("[data-tema]", d).forEach((c) => (c.onclick = () => { grava("tema", c.dataset.tema); $$("[data-tema]", d).forEach((x) => x.classList.toggle("on", x === c)); }));
  $("#pad", d).onclick = () => { local.gravar("leitura", {}); aoMudar(); d.close(); };
  $("[data-n]", d).onclick = () => d.close();
}

// ───────── texto → html ─────────
const CORES_GRIFO = { AMBAR: "#E9A65A", VERDE: "#6FC2B4", AZUL: "#7FA7E6", ROSA: "#E88AA8" };
// ───────── efeitos de escrita (iguais ao app) ─────────
export const EFEITOS_CENA = { chuva: "🌧 Chuva", neve: "❄ Neve", fogo: "🔥 Fogo e brasas", vento: "🌬 Vento", noite: "🌙 Noite e vaga-lumes", tensao: "💓 Tensão", sangue: "🩸 Sangue", flashback: "🎞 Lembrança", sonho: "☁ Sonho", magia: "✨ Magia" };
const SIN_CENA = { "tensão": "tensao", lembranca: "flashback", "lembrança": "flashback", memoria: "flashback", "memória": "flashback", chamas: "fogo", estrelas: "noite" };
/** "~chuva" → "chuva"; "~fim" → ""; outra linha → null */
export function marcaCena(l) {
  if (!l.startsWith("~") || l.length > 24 || l.includes(" ")) return null;
  const k = l.slice(1).replace(/~+$/, "").toLowerCase();
  if (["fim", "pare", "parar", "/"].includes(k)) return "";
  const n = SIN_CENA[k] || k; return EFEITOS_CENA[n] ? n : null;
}
const ABRE = "(^|[\\s—–“\"«(])";
function efeitosInline(h) {
  return inlineFx(h).replace(new RegExp(ABRE + "!!(\\S.*?)!!", "g"), '$1<span class="grito">$2</span>')
    .replace(new RegExp(ABRE + "\\^\\^(\\S.*?)\\^\\^", "g"), '$1<span class="brilho-t">$2</span>')
    .replace(/\(\((.+?)\)\)/g, '<span class="sussurro">$1</span>')
    .replace(/\|\|(.+?)\|\|/g, '<span class="segredo" title="Toque para revelar">$1</span>')
    .replace(/%%(.+?)%%/g, '<span class="glitch" data-glitch="$1">$1</span>')
    .replace(/&lt;&lt;(.+?)&gt;&gt;/g, '<span class="sombria">$1</span>')
    .replace(/~~(.+?)~~/g, '<span class="riscado">$1</span>')
    .replace(/(^|[^{])\{([^{}]+)\}(?!\})/g, '$1<span class="tele">$2</span>')
    .replace(/\[(SSS|SS|S|A|B|C|D|E|F)\+?\]/g, (_, r) => `<span class="rank r-${r.length > 1 ? "S" : r}">${r}</span>`);
}
const BARRA = /^([\p{L}][\p{L} ]{0,14}?)\s*:?\s*(\d+)\s*\/\s*(\d+)$/u;
const COR_BARRA = (n) => (/^(HP|VIDA|SA[UÚ]DE)$/i.test(n) ? "#E0283C,#FF6A6A" : /^(MP|MANA|QI|CHI|KI|ENERGIA)$/i.test(n) ? "#3A7BFF,#5CC8FF" : /^(XP|EXP|EXPERI[EÊ]NCIA)$/i.test(n) ? "#FFB020,#FFC94A" : /^(SP|STAMINA|VIGOR)$/i.test(n) ? "#2FBF71,#6FCF97" : "#8E5CFF,#C59BFF");
function sistemaHTML(l) {
  const partes = l.split("|").map((x) => x.trim()); const bs = partes.map((x) => x.match(BARRA)), cs = partes.map((x) => x.match(/^\[([xX✓ ]?)\]/));
  if (!bs.some(Boolean) && !cs.some(Boolean)) return null;
  return `<span class="t-sis">◈ ${cs.some(Boolean) ? "MISSÕES" : "STATUS"}</span>` + partes.map((x, k) => { const m = bs[k], c = cs[k];
    if (c) return `<span class="missao-sis ${c[1].trim() ? "feita" : ""}"><i>${c[1].trim() ? "✓" : ""}</i>${efeitosInline(esc(x.slice(c[0].length).trim()))}</span>`;
    if (!m) return `<span class="linha-sis">${efeitosInline(esc(x))}</span>`;
    const f = Math.min(100, (+m[2] / Math.max(1, +m[3])) * 100);
    return `<span class="barra-sis"><span>${esc(m[1].trim())}</span><i><b style="--f:${f}%;background:linear-gradient(90deg,${COR_BARRA(m[1].trim())})"></b></i><span>${m[2]} / ${m[3]}</span></span>`; }).join("");
}

const RAR = { comum: "#B0B0B8", incomum: "#6FCF97", raro: "#5CC8FF", "épico": "#C59BFF", epico: "#C59BFF", "lendário": "#FFC94A", lendario: "#FFC94A", "mítico": "#FF4458", mitico: "#FF4458" };
const CORES_NOME = ["#6FCF97", "#5CC8FF", "#C59BFF", "#FF8A65", "#F07AAE", "#FFC94A"];
const corNome = (n) => n.startsWith("[") ? "#FFC94A" : CORES_NOME[[...n].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7) % CORES_NOME.length];
function iconeItem(n) { const l = n.toLowerCase(); return /espada|lâmina|lamina|faca|adaga|katana|sabre/.test(l) ? "🗡" : /poção|pocao|elixir|pílula|pilula/.test(l) ? "🧪" : /anel|colar|joia|amuleto/.test(l) ? "💍" : /livro|manual|técnica|tecnica|pergaminho/.test(l) ? "📜" : /escudo|armadura|elmo/.test(l) ? "🛡" : /arco|flecha/.test(l) ? "🏹" : /moeda|ouro|gema|cristal|pedra/.test(l) ? "💎" : /chave/.test(l) ? "🗝" : "✦"; }
/** Blocos da leva 3: [classe, html] ou null. */
function blocoEspecial(l, op) {
  if (/^\[[#$]/.test(l)) { const fx = blocoFx(l, (x) => efeitosInline(esc(x))); if (fx) return ["fx-bloco", fx]; }
  if (/^@@./.test(l)) { const nome = l.slice(2).trim().replace(/[.,]$/, ""); return ["ficha-cartao", `<span class="av-f" data-ficha-cartao="${esc(nome)}">${esc(nome.slice(0, 1).toUpperCase())}</span><span><b>${esc(nome)}</b><small class="sub-f"></small></span>`]; }
  if (/^>>>./.test(l)) return ["aovivo", `<span class="tag-vivo">AO VIVO</span>` + l.split("\n").map((x) => { x = x.replace(/^>>>\s*/, ""); const d = x.startsWith("[") && x.indexOf("]") > 0 ? x.indexOf("]") + 1 + Math.max(0, x.slice(x.indexOf("]") + 1).indexOf(":")) : x.indexOf(":"); const n = d > 0 ? x.slice(0, d).trim().replace(/:$/, "") : ""; return `<span class="cmt">${n ? `<b style="color:${corNome(n)}">${esc(n)}</b> ` : ""}<span${n.startsWith("[") ? ' class="const"' : ""}>${efeitosInline(esc(d > 0 ? x.slice(d + 1).trim() : x))}</span></span>`; }).join("")];
  const conv = l.match(/^>>\s*([^:\s>][^:]{0,24}):\s(.*)$/); if (conv) return ["conversa", `<small style="color:${corNome(conv[1].trim())}">${esc(conv[1].trim())}</small>${efeitosInline(esc(conv[2]))}`];
  if (/^\{\{.+\}\}$/.test(l)) { const c = l.slice(2, -2), b = c.indexOf("|"); return ["pergaminho", `${b > 0 ? `<b>${esc(c.slice(0, b).trim())}</b>` : ""}<span>${efeitosInline(esc((b > 0 ? c.slice(b + 1) : c).trim()))}</span><i>印</i>`]; }
  if (/^\[\+.+\]$/.test(l)) { const [n, r = ""] = l.slice(2, -1).split("|").map((x) => x.trim()); const cor = RAR[r.toLowerCase()] || "#FFC94A"; return ["item-obtido", `<span class="ic" style="--c:${cor}">${iconeItem(n)}</span><span><small style="color:${cor}">ITEM ${r ? esc(r.toUpperCase()) + " " : ""}OBTIDO</small><b>${esc(n)}</b></span>`]; }
  if (/^::./.test(l)) { const [a, ...r] = l.slice(2).split("|").map((x) => x.trim()); return ["local-hora", `<b>${esc(a)}</b>${r.map((x) => `<span>${esc(x)}</span>`).join("")}`]; }
  if (/^> ./.test(l)) { const t = l.slice(2), k = Math.max(t.lastIndexOf(" — "), t.lastIndexOf(" - ")); return ["epigrafe", `<span>${efeitosInline(esc(k > 0 ? t.slice(0, k) : t))}</span>${k > 0 ? `<small>— ${esc(t.slice(k + 3).trim())}</small>` : ""}`]; }
  const na = l.match(/^(N\.\s?A\.|Nota do autor)\s*:?\s*/i); if (na) return ["nota-autor", `<span class="av-na">${esc((op.autor || "A").slice(0, 1).toUpperCase())}</span><span><small>NOTA DO AUTOR</small>${efeitosInline(esc(l.slice(na[0].length)))}</span>`];
  return null;
}

export function htmlTexto(t, nomes = new Map(), grifos = new Map(), op = {}) {
  let capitularFeita = !pref().capitular;
  // linhas ~chuva … ~fim ligam um efeito de cena e não aparecem no texto
  // ~pausa deixa o próximo parágrafo devagar; linhas "== …" seguidas viram uma carta só
  let clima = "", pausa = false; const climas = [], pausas = [], linhas = [];
  const brutas = limparMarcas(t).split("\n"), origens = [];
  for (let k = 0; k < brutas.length; k++) {
    const l = brutas[k].trim(); if (!l) continue;
    if (/^~pausa$/i.test(l)) { pausa = true; continue; }
    const m = marcaCena(l); if (m !== null) { clima = m; continue; }
    if (/^==./.test(l) && /^==/.test(linhas[linhas.length - 1] || "")) { linhas[linhas.length - 1] += "\n" + l; continue; }
    if (/^>>>./.test(l) && /^>>>/.test(linhas[linhas.length - 1] || "")) { linhas[linhas.length - 1] += "\n" + l; continue; }
    linhas.push(l); origens.push(k); climas.push(clima); pausas.push(pausa); pausa = false;
  }
  if (op.origens) op.origens.push(...origens); // linha do texto de cada bloco (a prévia do editor usa)
  // palavra solta (parte do nome) não vale colada a outro nome: em "Gwon Jang-ho" o "Gwon" é de outra pessoa
  const lista = [...nomes.keys()].sort((a, b) => b.length - a.length).map((n) => n.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + (n.trim().includes(" ") ? "" : "(?!\\s\\p{Lu}\\p{Ll})"));
  const rxNomes = lista.length ? new RegExp(`(?<![\\p{L}\\p{N}'’-])(${lista.join("|")})(?![\\p{L}\\p{N}]|[-'’]\\p{L})`, "gu") : null;
  return linhas.map((l, i) => {
    const g = grifos.get(i); const cl = climas[i] ? ` data-clima="${climas[i]}"` : "";
    if (/^(\* \* \*|\*\*\*|---|◆)$/.test(l)) return `<div class="sep enfeite" data-b="${i}"${cl}>${enfeiteHTML(op.enfeite)}</div>`;
    if (/^##(?!#)/.test(l)) return `<h2 class="impacto" data-b="${i}"${cl}>${esc(l.replace(/^#+\s*/, ""))}</h2>`;
    if (/^#/.test(l)) return `<h3 data-b="${i}"${cl}>${esc(l.replace(/^#+\s*/, ""))}</h3>`;
    const especial = blocoEspecial(l, op);
    if (especial) return `<div data-b="${i}"${cl} class="${especial[0]}">${especial[1]}</div>`;
    const sistema = /^\[\[.+\]\]$/.test(l), msg = /^>>/.test(l), aviso = /^\[!.+!\]$/.test(l), carta = /^==/.test(l);
    if (sistema) l = l.slice(2, -2).trim(); else if (msg) l = l.slice(2).trim(); else if (aviso) l = l.slice(2, -2).trim(); else if (carta) l = l.split("\n").map((x) => x.replace(/^==\s*/, "")).join("\n");
    let h = (sistema && sistemaHTML(l)) || efeitosInline(esc(l)).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>").replace(/\*(.+?)\*/g, "<i>$1</i>");
    if (carta) h = h.replace(/\n/g, "<br>");
    if (aviso) h = "🔔 " + h;
    if (rxNomes) { const ign = ignoradosNome(); h = h.replace(rxNomes, (m, _g, off, tudo) => {
      const id = nomes.get(m); const puro = tudo.slice(0, off).replace(/<[^>]*>/g, "").replace(/&[a-z#0-9]+;/gi, "x");
      const c = confianca(puro + m, puro.length, m, !nomes.palpites?.has(m), ign.has(`${id}|${m}`));
      if (c < LIMIAR_LEVE) return m;
      return `<span class="nome${c < LIMIAR_FORTE ? " palpite" : ""}" data-ficha="${esc(id)}" data-termo="${esc(m)}">${m}</span>`; }); }
    const fala = !sistema && !msg && !aviso && !carta && /^[—–―\-“"«]/.test(l) && l.length > 1 && !l.startsWith("---");
    if (fala) h = h.replace(/^([—–―\-“"«]\s?)/, '<b class="trav">$1</b>');
    const nota = g?.nota ? `<span class="nota-grifo" title="${esc(g.nota)}">✎</span>` : "";
    const capit = !capitularFeita && !fala && !sistema && !msg && !aviso && !carta && l.length > 40 && !/^(cap[ií]tulo|pr[oó]logo|ep[ií]logo)/i.test(l); if (capit) capitularFeita = true;
    const cls = [fala ? "fala" : "", g ? "grifo" : "", capit ? "capitular" : "", sistema ? "sistema" : "", msg ? "mensagem" : "", aviso ? "aviso" : "", carta ? "carta" : "", pausas[i] ? "pausa" : "", sistema && h.startsWith('<span class="t-sis">') ? "com-barras" : ""].filter(Boolean).join(" ");
    const nc = op.coms?.get(i) || 0;
    const balao = op.coms ? `<button class="balao-trecho ${nc ? "" : "vazio"}" data-trecho="${i}" title="Comentar este trecho">💬${nc ? " " + nc : ""}</button>` : "";
    return `${pausas[i] ? `<div class="pausa-pontos">· · ·</div>` : ""}<p data-b="${i}"${cl}${cls ? ` class="${cls}"` : ""}${g ? ` style="--grifo:${CORES_GRIFO[g.cor] || "#E9A65A"}"` : ""}>${h}${nota}${balao}</p>`;
  }).join("");
}

// ───────── maratona: no fim do capítulo, o próximo começa sozinho ─────────
let contagem = null;
function maratona(proxima) {
  if (!proxima || !pref().maratona || contagem || $(".contagem")) return;
  const alt = document.documentElement.scrollHeight - innerHeight;
  if (alt > 0 && scrollY / alt < .995) return;
  let n = 5;
  const el = document.createElement("div"); el.className = "contagem vidro";
  el.innerHTML = `<span>Próximo capítulo em <b>${n}</b>…</span><button class="btn2 peq">Cancelar</button>`;
  document.body.appendChild(el);
  const parar = () => { clearInterval(contagem); contagem = "cancelado"; el.remove(); };
  el.querySelector("button").onclick = parar;
  contagem = setInterval(() => { n--; if (n <= 0) { clearInterval(contagem); contagem = null; el.remove(); ir(proxima); } else el.querySelector("b").textContent = n; }, 1000);
  const antes = E.saindo; E.saindo = async () => { clearInterval(contagem); contagem = null; el.remove(); if (antes) await antes(); };
}
window.addEventListener("hashchange", () => { if (contagem === "cancelado") contagem = null; });

// ───────── comentários de um trecho ─────────
function comentariosTrecho(pid, cid, bloco, trecho, todos) {
  const l = todos.filter((m) => m.bloco === bloco);
  const d = dialogo(`<h3 style="margin-top:0">${l.length} comentário${l.length === 1 ? "" : "s"} neste trecho</h3><blockquote class="trecho-citado">“${esc(trecho.slice(0, 260))}”</blockquote>
    <div class="lista-coms">${l.map((m) => `<div class="comentario"><span class="av" style="width:34px;height:34px">${esc((m.nome || "?").slice(0, 1))}</span><div style="flex:1"><b>${esc(m.nome)}</b> <span class="suave peq">${haQuanto(m.em)}</span><div>${esc(m.texto)}</div></div></div>`).join("") || `<p class="suave">Ninguém comentou este trecho ainda.</p>`}</div>
    <div style="display:flex;gap:10px;margin-top:14px"><textarea class="campo" id="ct" rows="2" placeholder="Comentar neste trecho…"></textarea><button class="btn peq" id="ce">Enviar</button></div>`, true);
  $("#ct", d).focus();
  $("#ce", d).onclick = async () => { const t = $("#ct", d).value.trim(); if (!t) return; await comentar(pid, cid, t, bloco, trecho); d.close(); toast("Comentário enviado"); window.redesenhar(); };
}

// ───────── ler traduzido (tradutor do próprio navegador) ─────────
const IDIOMAS = [["", "Original (sem traduzir)"], ["pt", "Português"], ["en", "Inglês"], ["es", "Espanhol"], ["fr", "Francês"], ["it", "Italiano"], ["de", "Alemão"], ["ja", "Japonês"], ["ko", "Coreano"], ["zh", "Chinês"], ["ru", "Russo"]];
async function traduzirTela() {
  const para = local.ler("idiomaLeitura", ""); if (!para) return;
  if (!("Translator" in self)) { toast("Este navegador não traduz sozinho. Use o Chrome ou Edge mais novo (ou o tradutor do navegador)."); return; }
  const pars = $$("#texto [data-b]").filter((p) => p.tagName !== "DIV");
  let de = "pt";
  try { if ("LanguageDetector" in self) { const det = await LanguageDetector.create(); de = (await det.detect(pars.slice(0, 6).map((p) => p.textContent).join(" ")))[0]?.detectedLanguage?.split("-")[0] || "pt"; } } catch (_) {}
  if (de === para) return;
  const aviso = document.createElement("div"); aviso.className = "aviso-trad vidro"; aviso.textContent = "Preparando a tradução…"; document.body.appendChild(aviso);
  try {
    const t = await Translator.create({ sourceLanguage: de, targetLanguage: para, monitor(m) { m.addEventListener("downloadprogress", (e) => (aviso.textContent = `Baixando o idioma… ${Math.round(e.loaded * 100)}%`)); } });
    let n = 0;
    for (const p of pars) {
      if (!document.body.contains(p)) break;
      const fala = /^[—–-]\s*/.exec(p.textContent)?.[0] || "";
      const txt = p.textContent.replace(/✎|💬.*$/g, "").trim().replace(/^[—–-]\s*/, "");
      if (txt) { const tr = await t.translate(txt); const extra = [...p.querySelectorAll(".balao-trecho,.nota-grifo")]; p.textContent = fala + tr; extra.forEach((x) => p.appendChild(x)); }
      if (++n % 5 === 0) aviso.textContent = `Traduzindo ${n} de ${pars.length}…`;
    }
  } catch (e) { toast("Não deu para traduzir: " + (e.message || e)); }
  aviso.remove();
}

// ───────── ouvir (voz do navegador), com uma voz para cada personagem ─────────
let falando = null, fichasVoz = [];
const VERBOS_FALA = /\b(disse|diz|perguntou|pergunta|respondeu|responde|gritou|grita|murmurou|sussurrou|falou|fala|retrucou|rosnou|riu|completou|continuou|explicou|insistiu|berrou|resmungou|exclamou|declarou|rebateu|admitiu|repetiu|chamou|cortou|provocou|zombou|suspirou|ordenou|avisou|sorriu|bufou)\b/g;
const FEM = /\b(garota|mulher|menina|moça|irmã|mãe|filha|rainha|princesa|senhora|avó|tia|ela|dela|esposa|namorada|guerreira|aluna)\b/giu;
const MASC = /\b(garoto|homem|menino|rapaz|irmão|pai|filho|rei|príncipe|senhor|avô|tio|ele|dele|marido|namorado|guerreiro|aluno)\b/giu;
const VOZES_F = /francisca|thalita|brenda|elza|giovanna|leila|leticia|letícia|manuela|yara|maria|luciana|vitoria|vitória|female|feminin/i;
const VOZES_M = /antonio|antônio|donato|fabio|fábio|humberto|julio|júlio|nicolau|valerio|valério|daniel|male|masculin/i;
export function nomesVoz(fs) {
  const m = new Map(); const ps = fs.filter((f) => !f.tipo || f.tipo === "PERSONAGEM");
  for (const f of ps) { m.set(f.nome.trim(), f.id); for (const a of (f.apelidos || "").split(",").map((x) => x.trim())) if (a.length >= 2 && a[0] === a[0].toUpperCase() && !m.has(a)) m.set(a, f.id); }
  for (const [p, id] of partesUnicas(ps)) if (!m.has(p)) m.set(p, id);
  const alt = [...m.keys()].filter((k) => k.length >= 2).sort((a, b) => b.length - a.length).map((k) => k.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + (k.includes(" ") ? "" : "(?!\\s\\p{Lu}\\p{Ll})")).join("|");
  const rx = alt ? new RegExp(`(?<![\\p{L}\\p{N}'’-])(?:${alt})(?![\\p{L}\\p{N}'’-])`, "gu") : null;
  return (s) => rx ? [...s.matchAll(rx)].map((x) => ({ id: m.get(x[0]), ini: x.index, fim: x.index + x[0].length })) : [];
}
function falante(narr, achar) {
  const ns = achar(narr); if (!ns.length) return null;
  for (const v of narr.matchAll(VERBOS_FALA)) { const ve = v.index + v[0].length; const d = ns.find((x) => x.ini >= ve && x.ini - ve <= 25); if (d) return d.id; const a = [...ns].reverse().find((x) => x.fim <= v.index && v.index - x.fim <= 14); if (a) return a.id; }
  const u = [...new Set(ns.map((x) => x.id))]; return u.length === 1 ? u[0] : null;
}
/** Divide cada parágrafo em narração e falas e descobre quem fala (mesma regra do app). */
export function dividirElenco(textos, achar) {
  let ult = null, pen = null;
  return textos.map((t, i) => {
    if (!/^[—–-]/.test(t)) return [{ texto: t, quem: null }];
    const pedacos = t.replace(/^[—–-]\s*/, "").split(/\s+[—–]\s+/);
    let quem = falante(pedacos.filter((_, k) => k % 2).join(" "), achar);
    if (!quem && i > 0 && /:\s*$/.test(textos[i - 1])) quem = falante(textos[i - 1].slice(-160), achar);
    if (!quem && ult && pen && ult !== pen) quem = pen;
    if (quem && quem !== ult) { pen = ult; ult = quem; }
    return pedacos.map((x, k) => ({ texto: x.trim(), quem: k % 2 ? null : quem })).filter((x) => x.texto);
  });
}
function generoFicha(f, texto, achar) {
  let fem = ((`${f.descricao || ""} ${f.aparencia || ""}`).match(FEM) || []).length * 3, masc = ((`${f.descricao || ""} ${f.aparencia || ""}`).match(MASC) || []).length * 3;
  for (const m of achar(texto).filter((x) => x.id === f.id).slice(0, 80)) { const p = texto.slice(m.fim, m.fim + 70); fem += (p.match(FEM) || []).length; masc += (p.match(MASC) || []).length; }
  return fem > masc * 1.3 && fem >= 2 ? "F" : masc > fem * 1.3 && masc >= 2 ? "M" : null;
}
let geracao = 0, ctrlVoz = null;
function pararVoz() {
  geracao++; speechSynthesis.cancel(); NV.calar(); NV.continuar(); falando = null; ctrlVoz = null;
  $$(".falando").forEach((p) => p.classList.remove("falando"));
  const b = $("#ouvir .rot-hud"); if (b) b.textContent = "Ouvir"; $("#ouvir")?.classList.remove("on");
  $("#barra-voz")?.remove();
}
/** Primeiro parágrafo visível na tela (o Ouvir começa dele, não do começo do capítulo). */
function blocoNaTela() {
  const p = $$("#texto [data-b]").find((x) => x.getBoundingClientRect().bottom > 90);
  return p ? +p.dataset.b : 0;
}
/** Todas as vozes do computador, português primeiro, agrupadas por idioma (para o <select>). */
function opcoesVozes(atual, padrao = "Automática") {
  const vs = speechSynthesis.getVoices();
  const grupos = new Map();
  for (const v of vs) { const l = (v.lang || "?").replace("_", "-"); const k = /^pt/i.test(l) ? "0Português" : "1" + (l.split("-")[0] || "?"); if (!grupos.has(k)) grupos.set(k, []); grupos.get(k).push(v); }
  const nomeLingua = (k) => { if (k === "0Português") return "Português"; try { return new Intl.DisplayNames(["pt-BR"], { type: "language" }).of(k.slice(1)) || k.slice(1); } catch (_) { return k.slice(1); } };
  const novely = NV.instaladas();
  return `<option value="">${padrao}</option>` + (novely.length ? `<optgroup label="✦ Vozes do Novely">${novely.map((v) => `<option value="${v.id}" ${v.id === atual ? "selected" : ""}>✦ ${esc(v.nome)} (${esc(v.descricao)})</option>`).join("")}</optgroup>` : "") + [...grupos.keys()].sort().map((k) => `<optgroup label="${esc(nomeLingua(k))}">${grupos.get(k)
    .map((v) => `<option value="${esc(v.name)}" ${v.name === atual ? "selected" : ""}>${esc(v.name.replace(/^Microsoft |^Google /, "").replace(/ - .*$/, ""))}${/natural|online|neural/i.test(v.name) ? " ✦" : ""}</option>`).join("")}</optgroup>`).join("");
}
function testarVoz(nome) {
  speechSynthesis.cancel();
  const n = NV.vozNeural(nome); if (n && NV.instalada(n)) { NV.testar(n, local.ler("velVoz", 1)).catch((e) => toast("A voz falhou: " + e.message)); return; }
  const v = speechSynthesis.getVoices().find((x) => x.name === nome);
  const u = new SpeechSynthesisUtterance(v && !/^pt/i.test(v.lang) ? "Hello! This is how I sound." : "Olá! É assim que eu leio a sua história.");
  if (v) { u.voice = v; u.lang = v.lang; } u.rate = local.ler("velVoz", 1); speechSynthesis.speak(u);
}
const ICV = {
  ant: '<path d="M19 20L9 12l10-8v16z"/><path d="M5 19V5"/>', prox: '<path d="M5 4l10 8-10 8V4z"/><path d="M19 5v14"/>',
  pausa: '<rect x="6" y="4" width="4" height="16" rx="1"/><rect x="14" y="4" width="4" height="16" rx="1"/>', play: '<path d="M6 4l14 8-14 8V4z"/>',
  parar: '<rect x="5" y="5" width="14" height="14" rx="2"/>', engrenagem: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
};
const icv = (k, t = 18, cheio = false) => `<svg width="${t}" height="${t}" viewBox="0 0 24 24" fill="${cheio ? "currentColor" : "none"}" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICV[k]}</svg>`;

/** Painel da voz (em cima da barra enquanto ouve): progresso, controles, velocidade e, em "Vozes", narrador e voz de cada personagem. */
function mostrarBarraVoz(personagens) {
  $("#barra-voz")?.remove();
  const vel = local.ler("velVoz", 1), aberto = local.ler("vozesAbertas", false);
  const d = document.createElement("div"); d.id = "barra-voz"; d.className = "barra-voz";
  d.innerHTML = `
    <div class="bv-topo"><span class="bv-ponto"></span><b>Ouvindo</b><span class="bv-onde" id="bv-onde"></span>
      <button class="bv-tela" id="bv-tela" hidden>Ouvir o que está na tela</button></div>
    <div class="bv-trilho"><div class="bv-feito" id="bv-feito"></div></div>
    <div class="bv-linha">
      <button class="bv-bt" id="bv-ant" title="Parágrafo anterior" aria-label="Parágrafo anterior">${icv("ant", 18, true)}</button>
      <button class="bv-bt bv-play" id="bv-pausa" title="Pausar" aria-label="Pausar">${icv("pausa", 20, true)}</button>
      <button class="bv-bt" id="bv-prox" title="Próximo parágrafo" aria-label="Próximo parágrafo">${icv("prox", 18, true)}</button>
      <button class="bv-bt bv-sec" id="bv-parar" title="Parar" aria-label="Parar">${icv("parar", 16, true)}</button>
      <label class="bv-vel" title="Velocidade"><input type="range" id="bv-vel" min="0.5" max="2.5" step="0.05" value="${vel}" aria-label="Velocidade"><span id="bv-velt">${vel.toFixed(2).replace(".", ",")}×</span></label>
      <button class="bv-bt bv-sec ${aberto ? "on" : ""}" id="bv-cfg" title="Vozes" aria-label="Vozes" aria-expanded="${aberto}">${icv("engrenagem", 18)}</button>
    </div>
    <div class="bv-vozes" id="bv-vozes" ${aberto ? "" : "hidden"}>
      <div class="bv-sub">✦ Vozes do Novely · mais humanas</div>
      <div class="bv-novely">${NV.VOZES.map((v) => { const narrAgora = local.ler("narradorPC", "") === v.id; return `<div class="bv-nv ${narrAgora ? "on" : ""}"><span class="bv-nvn">${v.feminina ? "♀" : "♂"} ${esc(v.nome)}<em>${narrAgora ? "narrador agora" : esc(v.descricao)}</em></span>${NV.instalada(v)
        ? `<button class="bv-testar" data-nv-testar="${v.id}">Testar</button>${narrAgora ? "" : `<button class="bv-testar bv-usar" data-nv-usar="${v.id}">Narrar</button>`}`
        : `<button class="bv-testar bv-usar" data-nv-baixar="${v.id}">Baixar · ${NV.faltaMb(v)} MB</button>`}</div>`; }).join("")}</div>
      <div class="bv-dica">Baixadas uma vez, ficam no navegador. Depois aparecem no Narrador e nos personagens. As Doras são mais pesadas: podem demorar alguns segundos para começar.</div>
      <div class="bv-campo"><span>Narrador</span><select id="bv-narr">${opcoesVozes(local.ler("narradorPC", ""), "Voz padrão do sistema")}</select><button class="bv-testar" data-testa="bv-narr">Testar</button></div>
      <label class="troca bv-campo"><input type="checkbox" id="bv-pers" ${local.ler("vozesPersonagens", true) ? "checked" : ""}> Uma voz para cada personagem</label>
      ${personagens.length ? `<div class="bv-sub">Personagens deste capítulo</div>` + personagens.map((p) => `<div class="bv-campo"><span>${esc(p.nome)}</span><select data-pers="${esc(p.id)}">${opcoesVozes(local.ler("vozPers:" + p.id, ""), "Automática (" + esc((p.auto || "").replace(/^Microsoft |^Google /, "").replace(/ - .*$/, "")) + ")")}</select><button class="bv-testar" data-testa-pers="${esc(p.id)}">Testar</button></div>`).join("") : ""}
      <div class="bv-dica">✦ = voz natural (mais humana). Faltam vozes? No Windows: Configurações › Hora e idioma › Fala › Adicionar vozes. No Edge aparecem as vozes naturais da Microsoft.</div>
    </div>`;
  document.body.appendChild(d); centralizarHud();
  const recomecar = () => { const b = ctrlVoz?.bloco() ?? blocoNaTela(); pararVoz(); ouvir(b); };
  $("#bv-ant").onclick = () => ctrlVoz?.pular(-1);
  $("#bv-prox").onclick = () => ctrlVoz?.pular(1);
  $("#bv-parar").onclick = () => pararVoz();
  $("#bv-pausa").onclick = (e) => {
    const b = e.currentTarget;
    if (d.classList.contains("pausado")) { speechSynthesis.resume(); NV.continuar(); b.innerHTML = icv("pausa", 20, true); b.title = "Pausar"; d.classList.remove("pausado"); }
    else { speechSynthesis.pause(); NV.pausar(); b.innerHTML = icv("play", 20, true); b.title = "Continuar"; d.classList.add("pausado"); }
  };
  let tv = null;
  $("#bv-vel").oninput = (e) => { $("#bv-velt").textContent = (+e.target.value).toFixed(2).replace(".", ",") + "×"; };
  $("#bv-vel").onchange = (e) => { local.gravar("velVoz", +e.target.value); clearTimeout(tv); tv = setTimeout(recomecar, 150); };
  $("#bv-cfg").onclick = (e) => { const v = $("#bv-vozes"); v.hidden = !v.hidden; local.gravar("vozesAbertas", !v.hidden); e.currentTarget.classList.toggle("on", !v.hidden); e.currentTarget.setAttribute("aria-expanded", String(!v.hidden)); };
  $("#bv-narr").onchange = (e) => { local.gravar("narradorPC", e.target.value); recomecar(); };
  $("#bv-pers").onchange = (e) => { local.gravar("vozesPersonagens", e.target.checked); recomecar(); };
  $$("[data-pers]", d).forEach((sel) => (sel.onchange = () => { local.gravar("vozPers:" + sel.dataset.pers, sel.value); recomecar(); }));
  $$("[data-testa]", d).forEach((b) => (b.onclick = () => { ctrlVoz?.pausarPara(); testarVoz($("#" + b.dataset.testa).value); }));
  $$("[data-testa-pers]", d).forEach((b) => (b.onclick = () => { ctrlVoz?.pausarPara(); const p = personagens.find((x) => x.id === b.dataset.testaPers); testarVoz($(`[data-pers="${b.dataset.testaPers}"]`, d).value || p?.auto || ""); }));
  $("#bv-tela").onclick = () => { const b = blocoNaTela(); pararVoz(); ouvir(b); };
  $$("[data-nv-testar]", d).forEach((b) => (b.onclick = () => { ctrlVoz?.pausarPara(); testarVoz(b.dataset.nvTestar); }));
  $$("[data-nv-usar]", d).forEach((b) => (b.onclick = () => { local.gravar("narradorPC", b.dataset.nvUsar); recomecar(); }));
  $$("[data-nv-baixar]", d).forEach((b) => (b.onclick = async () => {
    const v = NV.vozNeural(b.dataset.nvBaixar); $$("[data-nv-baixar]", d).forEach((x) => (x.disabled = true));
    try { await NV.baixar(v, (f) => (b.textContent = `Baixando… ${Math.round(f * 100)}%`)); toast(`✦ ${v.nome} pronta`); const blk = ctrlVoz?.bloco() ?? blocoNaTela(); pararVoz(); ouvir(blk); }
    catch (e) { toast("Não deu para baixar: " + e.message); b.textContent = "Tentar de novo"; $$("[data-nv-baixar]", d).forEach((x) => (x.disabled = false)); }
  }));
}
// a voz num lugar e a tela em outro: oferece "ouvir o que está na tela"
addEventListener("scroll", () => {
  const t = $("#bv-tela"); if (!t || !ctrlVoz) return;
  t.hidden = Math.abs(blocoNaTela() - ctrlVoz.bloco()) <= 2;
}, { passive: true });
function ouvir(apartir = 0) {
  const pars = $$("#texto [data-b]").filter((p) => p.tagName !== "DIV");
  if (falando !== null) { pararVoz(); return; }
  const vel = local.ler("velVoz", 1), vozesPers = local.ler("vozesPersonagens", true);
  let i = Math.max(0, pars.findIndex((p) => +p.dataset.b >= apartir));
  const pt = speechSynthesis.getVoices().filter((v) => v.lang?.toLowerCase().startsWith("pt"));
  const br = pt.filter((v) => /br/i.test(v.lang)); const todas = br.length ? br : pt;
  const narr = speechSynthesis.getVoices().find((v) => v.name === local.ler("narradorPC", "")) || todas[0];
  const narrN = ((n) => (n && NV.instalada(n) ? n : null))(NV.vozNeural(local.ler("narradorPC", "")));
  const textos = pars.map((p) => p.textContent.replace(/✎|💬.*$/g, "").trim());
  const achar = nomesVoz(fichasVoz); const segs = dividirElenco(textos, achar);
  // voz e tom de cada personagem: mulheres com vozes femininas, homens com masculinas, cada um com um tom
  // vozes do Novely baixadas vêm primeiro para os personagens (soam melhor)
  const nvF = NV.instaladas().filter((v) => v.feminina && v !== narrN).map((v) => ({ neural: v })), nvM = NV.instaladas().filter((v) => !v.feminina && v !== narrN).map((v) => ({ neural: v }));
  const fem = [...nvF, ...todas.filter((v) => VOZES_F.test(v.name) && v !== narr)], masc = [...nvM, ...todas.filter((v) => VOZES_M.test(v.name) && v !== narr)];
  const tons = [1, 0.85, 1.15, 0.93, 1.07]; const elenco = new Map(); let nf = 0, nm = 0, nx = 0; const tudo = textos.join("\n");
  for (const id of [...new Set(segs.flat().map((s) => s.quem).filter(Boolean))]) {
    const f = fichasVoz.find((x) => x.id === id); const g = f ? generoFicha(f, tudo, achar) : null;
    const lista = (g === "F" ? fem : g === "M" ? masc : todas).length ? (g === "F" ? fem : g === "M" ? masc : todas) : todas;
    const k = g === "F" ? nf++ : g === "M" ? nm++ : nx++;
    const nomeEsc = local.ler("vozPers:" + id, ""); const nEsc = NV.vozNeural(nomeEsc);
    const escolhida = nEsc && NV.instalada(nEsc) ? { neural: nEsc } : speechSynthesis.getVoices().find((v) => v.name === nomeEsc);
    const auto = lista[k % Math.max(1, lista.length)];
    elenco.set(id, escolhida ? (escolhida.neural ? { neural: escolhida.neural, tom: 1 } : { voz: escolhida, tom: 1 })
      : auto?.neural ? { neural: auto.neural, tom: 1 } : { voz: auto, tom: tons[(Math.floor(k / Math.max(1, lista.length)) + (g ? 0 : 2)) % tons.length] });
  }
  // vozes do Novely: trechos curtos (uma ou duas frases) começam a tocar bem mais rápido
  const neuralDe = (s) => { const e = vozesPers && s.quem ? elenco.get(s.quem) : null; return e ? e.neural || null : narrN; };
  const cachePed = new Map();
  const pedacos = (j) => {
    if (!cachePed.has(j)) {
      let ps = (segs[j] || []).flatMap((s) => (neuralDe(s) ? NV.frases(s.texto).map((t) => ({ ...s, texto: t })) : [s]));
      if (j === inicio && ps.length && neuralDe(ps[0])) ps = [...NV.inicioCurto(ps[0].texto).map((t) => ({ ...ps[0], texto: t })), ...ps.slice(1)];
      cachePed.set(j, ps);
    }
    return cachePed.get(j);
  };
  const inicio = i;
  if (narrN) NV.aquecer(narrN);
  const prox = () => {
    const minha = ++geracao; // pular ou parar invalida as falas antigas (o cancel do navegador dispara onend/onerror)
    if (falando === null || i >= pars.length) { pararVoz(); return; }
    const p = pars[i]; $$(".falando").forEach((x) => x.classList.remove("falando")); p.classList.add("falando");
    p.scrollIntoView({ block: "center", behavior: "smooth" });
    const onde = $("#bv-onde"); if (onde) onde.textContent = `${i + 1} de ${pars.length} · ${Math.round(((i + 1) / pars.length) * 100)}%`;
    const feito = $("#bv-feito"); if (feito) feito.style.width = `${((i + 1) / pars.length) * 100}%`;
    const lista = pedacos(i); let k = 0;
    // vozes do Novely: primeiro o trecho que vai tocar agora, depois os próximos (vão sendo gerados enquanto este toca)
    const pre = []; for (let j = i; j < Math.min(pars.length, i + 4); j++) for (const s of pedacos(j)) { const n = neuralDe(s); if (n) pre.push([n, s.texto]); }
    pre.forEach(([n, t]) => NV.preparar(n, t, vel).catch(() => {}));
    const fala = () => {
      if (falando === null || minha !== geracao) return;
      if (k >= lista.length) { i++; prox(); return; }
      const s = lista[k++]; const e = vozesPers && s.quem ? elenco.get(s.quem) : null;
      const n = neuralDe(s);
      if (n) {
        if (!NV.tocouAlgo()) { const o = $("#bv-onde"); if (o) o.textContent = "Preparando a voz… (só demora na primeira vez)"; }
        NV.preparar(n, s.texto, vel).then((a) => { if (minha !== geracao) return null; const o = $("#bv-onde"); if (o) o.textContent = `${i + 1} de ${pars.length} · ${Math.round(((i + 1) / pars.length) * 100)}%`; return NV.tocar(a); }).then(() => { if (minha === geracao) fala(); })
          .catch((err) => { if (minha === geracao) { toast("A voz do Novely falhou: " + err.message); pararVoz(); } });
        return;
      }
      const u = new SpeechSynthesisUtterance(s.texto); u.voice = e?.voz || narr || null; u.lang = u.voice?.lang || "pt-BR"; u.pitch = e?.tom || 1; u.rate = vel;
      u.onend = () => { if (minha === geracao) fala(); };
      u.onerror = (ev) => { if (minha === geracao && ev.error !== "interrupted" && ev.error !== "canceled") pararVoz(); };
      speechSynthesis.speak(u);
    };
    falando = i; fala();
  };
  ctrlVoz = {
    bloco: () => +(pars[Math.min(i, pars.length - 1)]?.dataset.b ?? 0),
    pausarPara: () => { geracao++; speechSynthesis.cancel(); NV.calar(); const pb = $("#bv-pausa"); if (pb) { pb.innerHTML = icv("play", 20, true); pb.title = "Continuar"; pb.onclick = () => { const b = ctrlVoz?.bloco() ?? blocoNaTela(); pararVoz(); ouvir(b); }; } }, // testar uma voz pausa a leitura; ▶ continua do parágrafo
    pular: (d) => { i = Math.max(0, Math.min(pars.length - 1, i + d)); speechSynthesis.cancel(); NV.calar(); if (speechSynthesis.paused) speechSynthesis.resume(); const pb = $("#bv-pausa"); if (pb) pb.textContent = "⏸"; prox(); },
  };
  falando = 0; const b = $("#ouvir .rot-hud"); if (b) b.textContent = "Parar"; $("#ouvir")?.classList.add("on");
  mostrarBarraVoz([...elenco.keys()].map((id) => ({ id, nome: fichasVoz.find((f) => f.id === id)?.nome || "?", auto: elenco.get(id)?.neural?.id || elenco.get(id)?.voz?.name || "" })));
  prox();
}

/** Liga clique nos nomes (abre a ficha). */
function ligarNomes(fs, fotoDe) {
  $$("#texto .nome").forEach((n) => (n.onclick = async () => { const f = fs.find((x) => x.id === n.dataset.ficha); if (f) verFicha(f, fotoDe ? await fotoDe(f) : undefined, fotoDe ? n.dataset.termo : null); }));
  // @@Nome: cartão do personagem com os dados da ficha
  $$("#texto [data-ficha-cartao]").forEach(async (av) => {
    const nome = av.dataset.fichaCartao.toLowerCase(), card = av.closest(".ficha-cartao");
    const f = fs.find((x) => x.nome.toLowerCase() === nome) || fs.find((x) => (x.apelidos || "").split(",").some((a) => a.trim().toLowerCase() === nome)) || fs.find((x) => x.nome.toLowerCase().startsWith(nome));
    if (!f) { card.querySelector(".sub-f").textContent = "Sem ficha com esse nome ainda"; return; }
    card.querySelector("b").textContent = f.nome;
    const idade = (f.idade || "").trim(); const sub = [idade && (/\d/.test(idade) && !/ano/.test(idade) ? idade + " anos" : idade), f.faccao].filter(Boolean).join(" · ");
    card.querySelector(".sub-f").innerHTML = esc(sub) + `<span class="tags-f">${[f.aparencia, f.poder].filter(Boolean).join(",").split(",").map((x) => x.trim()).filter((x) => x && x.length <= 24).slice(0, 4).map((x) => `<i>${esc(x)}</i>`).join("")}</span>`;
    card.style.cursor = "pointer"; card.onclick = async () => verFicha(f, fotoDe ? await fotoDe(f) : undefined);
    const url = fotoDe ? await fotoDe(f) : null; if (url) av.innerHTML = `<img src="${url}" alt="">`;
  });
}

function painelCapitulos(caps, atual, link) {
  let n = 0;
  const d = dialogo(`<h3 style="margin-top:0">Capítulos</h3><input class="campo" id="fc" placeholder="Procurar capítulo…"><div class="lista-caps">${caps.map((c) => {
    const num = /^(pr[oó]logo|ep[ií]logo|interl)/i.test(c.titulo || "") ? "◆" : ++n;
    return `<a href="${link(c)}" class="${c.id === atual ? "on" : ""}"><span class="n">${num}</span><span>${esc(c.titulo || "Sem título")}</span>${(c.progresso || 0) >= .98 ? `<span class="suave peq">lido</span>` : ""}</a>`;
  }).join("")}</div>`);
  $("#fc", d).oninput = (e) => { const t = e.target.value.toLowerCase(); $$(".lista-caps a", d).forEach((a) => (a.style.display = a.textContent.toLowerCase().includes(t) ? "" : "none")); };
  $$(".lista-caps a", d).forEach((a) => (a.onclick = () => d.close()));
  setTimeout(() => $(".lista-caps a.on", d)?.scrollIntoView({ block: "center" }), 50);
}

// ───────── LEITOR (minha biblioteca) ─────────
rota("/ler/(\\w+)/(\\w+)", async (obraId, capId) => {
  const o = E.obras.get(obraId); if (!o) { ir("/"); return; }
  const aba = o.minha && !o.trancada ? "escrever" : "biblioteca";
  carregando(aba);
  const caps = await capitulos(obraId, true);
  const i = caps.findIndex((c) => c.id === capId); const c = caps[i]; if (!c) { ir(`/obra/${obraId}`); return; }
  const [texto, fs, mks] = await Promise.all([lerTexto(colTextos(), capId, c.partes), fichas(obraId).catch(() => []), marcadores(obraId).catch(() => [])]);
  const grifos = new Map(mks.filter((m) => m.cap === capId).map((m) => [m.bloco, m]));
  const ant = caps[i - 1], prox = caps[i + 1];
  const pode = o.minha && !o.trancada;
  const { imagemMinha } = await import("./nucleo.js");
  await corDoLeitor(o.capaHash ? await imagemMinha(obraId + "-capa") : null, o.corBase);
  const desenhar = () => {
    casca(aba, `<div class="prog" id="prog"></div><div class="leitor tema-leitura" style="${estiloLeitura()}">
      ${hudTopo(`#/obra/${obraId}`, `${esc(o.titulo)} · ${esc(c.titulo)}`, pode ? `#/editar/${obraId}/${capId}` : "")}${hudBaixo()}
      <div class="cabec"><div class="rotulo">${esc(o.titulo)}</div><h1>${esc(c.titulo || "Sem título")}</h1><div class="suave peq">${palavrasFmt(c.palavras || 0)} palavras · ◷ ${Math.max(1, Math.round((c.palavras || 0) / 230))} min</div></div>
      ${(c.progresso || 0) < .05 ? cartaoAnterior(ant) : ""}
      <div class="texto" id="texto">${htmlTexto(texto, mapaNomes(fs), grifos, { enfeite: o.enfeite, autor: o.autor || nomeAutor() }) || '<p class="suave">Capítulo vazio.</p>'}</div>
      <div class="fim"><div class="losango" style="width:14px;height:14px"></div><div class="rotulo" style="color:var(--suave)">Fim do capítulo</div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center">${ant ? `<a class="btn2" href="#/ler/${obraId}/${ant.id}">‹ Anterior</a>` : ""}${prox ? `<a class="btn" href="#/ler/${obraId}/${prox.id}">Próximo: ${esc(prox.titulo)} ›</a>` : `<span class="suave">Último capítulo.</span>`}</div>
        ${!prox && o.status === "LENDO" && !o.minha ? `<button class="btn2 peq" id="concluir">Marcar obra como lida</button>` : ""}</div>
      <p class="suave peq dica-leitor">Dica: clique num nome para ver a ficha · clique com o botão direito num parágrafo para grifar · ← → trocam de capítulo</p></div>`);
    ligar();
  };
  const ligar = () => {
    ligarNomes(fs, async (f) => (f.imagemHash ? (await import("./nucleo.js")).imagemMinha(f.id + "-foto") : null));
    fichasVoz = fs; traduzirTela();
    if ($("#ouvir")) $("#ouvir").onclick = () => ouvir(blocoNaTela());
    ligarHud();
    $("#aj").onclick = () => painelAjustes(() => { $(".leitor").setAttribute("style", estiloLeitura()); });
    if ($("#lcaps")) $("#lcaps").onclick = () => painelCapitulos(caps, capId, (x) => `#/ler/${obraId}/${x.id}`);
    const cc = $("#concluir"); if (cc) cc.onclick = async () => { await salvarObra(obraId, { status: "CONCLUIDA" }); toast("Obra marcada como lida"); cc.remove(); };
    // grifar: botão direito num parágrafo
    $("#texto").oncontextmenu = (e) => {
      const p = e.target.closest("[data-b]"); if (!p || p.tagName === "DIV") return;
      e.preventDefault(); menuParagrafo(p, +p.dataset.b, obraId, capId, grifos, desenhar);
    };
  };
  desenhar();
  // posição: volta onde parou e salva o progresso (o celular recebe)
  const pars = () => $$("#texto [data-b]");
  if (c.posicaoBloco > 1 && (c.progresso || 0) < .98) pars()[Math.min(pars().length - 1, c.posicaoBloco - 1)]?.scrollIntoView({ block: "start" }); else scrollTo(0, 0);
  let tempo = null, ultimoProg = c.progresso || 0;
  const aoRolar = () => {
    const alt = document.documentElement.scrollHeight - innerHeight;
    const prog = alt > 0 ? Math.min(1, scrollY / alt) : 1;
    const barra = $("#prog"); if (barra) barra.style.width = prog * 100 + "%";
    clearTimeout(tempo);
    tempo = setTimeout(async () => {
      const bloco = pars().findIndex((p) => p.getBoundingClientRect().bottom > 80);
      const p = Math.max(prog > .97 ? 1 : prog, (c.progresso || 0) >= .98 ? 1 : 0);
      if (Math.abs(p - ultimoProg) < .01 && p < 1) return;
      ultimoProg = p;
      await salvarCapitulo(obraId, capId, { progresso: p, posicaoBloco: Math.max(0, bloco + 1), posicaoDeslocamento: 0 });
      await salvarObra(obraId, { lidaEm: agora(), ultimoCap: capId });
    }, 1200);
  };
  const fimXp = () => { if (o.minha || local.ler("xpLido/" + capId, false)) return; const alt = document.documentElement.scrollHeight - innerHeight; if (alt <= 0 || scrollY / alt < .97) return; local.gravar("xpLido/" + capId, true); registrar("ler"); confete(90); };
  window.onscroll = () => { aoRolar(); fimXp(); maratona(prox && `/ler/${obraId}/${prox.id}`); }; aoRolar();
  window.onkeydown = (e) => { if (e.target.closest("input,textarea,dialog")) return; if (e.key === "ArrowRight" && prox) ir(`/ler/${obraId}/${prox.id}`); if (e.key === "ArrowLeft" && ant) ir(`/ler/${obraId}/${ant.id}`); };
  const desligarClima = (() => { const a = ligarClima(), z = ligarEfeitosTexto(); return () => { a(); z(); }; })();
  E.saindo = async () => { pararVoz(); desligarClima(); };
});

function menuParagrafo(p, bloco, obraId, capId, grifos, redesenhar) {
  const g = grifos.get(bloco);
  $$(".menu-flutuante").forEach((m) => m.remove());
  const m = document.createElement("div"); m.className = "menu-flutuante vidro";
  m.innerHTML = `<div class="cores-grifo">${Object.entries(CORES_GRIFO).map(([k, c]) => `<button data-cor="${k}" style="background:${c}" title="Grifar"></button>`).join("")}</div>
    <button data-a="nota">${g?.nota ? "Editar nota" : "Escrever nota"}</button><button data-a="copiar">Copiar trecho</button><button data-a="ouvir">Ouvir daqui</button>${g ? `<hr><button data-a="tirar" class="perigo">Tirar grifo</button>` : ""}`;
  document.body.appendChild(m);
  const r = p.getBoundingClientRect(); m.style.left = Math.min(innerWidth - m.offsetWidth - 10, r.left + 20) + "px"; m.style.top = Math.min(innerHeight - m.offsetHeight - 10, Math.max(10, r.top)) + "px";
  const salvar = async (campos) => {
    const id = g?.id || novoId();
    const dados = { cap: capId, bloco, trecho: p.textContent.slice(0, 400), cor: g?.cor || "AMBAR", nota: g?.nota || "", criadoEm: g?.criadoEm || agora(), ...campos };
    await salvarItem("marcadores", obraId, id, dados); if (!g) registrar("grifar"); grifos.set(bloco, { ...dados, id }); redesenhar();
  };
  $$("[data-cor]", m).forEach((b) => (b.onclick = () => { m.remove(); salvar({ cor: b.dataset.cor }); }));
  $$("[data-a]", m).forEach((b) => (b.onclick = async () => {
    m.remove();
    if (b.dataset.a === "copiar") { navigator.clipboard.writeText(p.textContent.replace("✎", "")); toast("Copiado"); }
    if (b.dataset.a === "ouvir") ouvir(bloco);
    if (b.dataset.a === "nota") { const t = await perguntar("Nota sobre este trecho", g?.nota || ""); if (t !== null) salvar({ nota: t.trim() }); }
    if (b.dataset.a === "tirar") { await apagarItem("marcadores", obraId, g.id); grifos.delete(bloco); redesenhar(); }
  }));
  setTimeout(() => document.addEventListener("click", function fora(e) { if (!m.contains(e.target)) { m.remove(); document.removeEventListener("click", fora); } }), 0);
}

// ───────── EDITOR ─────────
rota("/editar/(\\w+)/(\\w+)", async (obraId, capId) => {
  const o = E.obras.get(obraId); if (!o || !o.minha || o.trancada) { ir("/"); return; }
  carregando("escrever");
  const caps = await capitulos(obraId, true);
  const c = caps.find((x) => x.id === capId); if (!c) { ir(`/obra/${obraId}`); return; }
  const [original, fs] = await Promise.all([lerTexto(colTextos(), capId, c.partes), fichas(obraId).catch(() => [])]);
  const i = caps.indexOf(c);
  casca("escrever", `<div class="editor">
    <div class="barra-ed vidro"><a class="voltar" href="#/obra/${obraId}" title="Voltar">←</a><input class="titulo" id="titulo" value="${esc(c.titulo || "")}" placeholder="Título do capítulo">
      <span class="estado suave" id="estado">Salvo</span>${i > 0 ? `<a class="btn2 peq" href="#/editar/${obraId}/${caps[i - 1].id}" title="Capítulo anterior">‹</a>` : ""}${i < caps.length - 1 ? `<a class="btn2 peq" href="#/editar/${obraId}/${caps[i + 1].id}" title="Próximo capítulo">›</a>` : ""}<a class="btn2 peq" href="#/ler/${obraId}/${capId}">Ler</a></div>
    <div class="ferramentas-ed vidro">
      <button data-ins="@" class="cor-nome" title="Personagem">@</button><button data-ins="&" class="cor-lugar" title="Lugar">&amp;</button><span class="div"></span>
      <button data-fala title="Nova fala">—</button><button data-env="**" title="Negrito (Ctrl+B)"><b>B</b></button><button data-env="*" title="Itálico (Ctrl+I)"><i>I</i></button><button data-ins="“" title="Aspas">“ ”</button>
      <button data-ins="\n\n* * *\n\n" title="Separador de cena">◆</button><button data-ins="\n# " title="Subtítulo">H</button><button id="befx" title="Efeitos com prévia (Ctrl+E)" style="color:#FFC94A">✦ Efeitos</button><button id="bprev" title="Prévia: ver como o leitor vê (Ctrl+P)">👁 Prévia</button><span class="div"></span>
      <button id="bbusca" title="Buscar e substituir (Ctrl+F)">⌕ Buscar</button><button id="bfichas" title="Personagens">☺ Fichas</button><button id="bfoco" title="Modo foco">⛶ Foco</button><button id="bmaq" title="Máquina de escrever: som de tecla, papel e a linha sempre no meio">⌨ Máquina</button><button id="bia" class="ia-btn" title="Estúdio de IA">✨ IA</button><button id="bvivo" title="Escrever junto ao vivo com coautores">👥 Ao vivo</button><button id="bver" title="Guardar uma versão com nome / ver versões">★ Versões</button>
    </div>
    <div class="busca-ed vidro" id="busca" hidden><input class="campo" id="q" placeholder="Buscar…"><input class="campo" id="r" placeholder="Substituir por…"><span class="suave peq" id="qn"></span>
      <button class="btn2 peq" id="qant">‹</button><button class="btn2 peq" id="qprox">›</button><button class="btn2 peq" id="rum">Substituir</button><button class="btn2 peq" id="rtodos">Todos</button><button class="btn2 peq" id="qx">✕</button></div>
    <div class="sugestoes-ed" id="sug" hidden></div>
    <textarea id="txt" spellcheck="true" placeholder="Escreva aqui… (@Personagem, descrição · &Lugar, descrição)"></textarea>
    <div class="texto previa-ed" id="prev" hidden></div>
    <div class="rodape-ed suave peq"><span id="cont" style="cursor:pointer" title="Contagem completa"></span><span id="sessao"></span></div></div>`);
  const ta = $("#txt"), ti = $("#titulo"), est = $("#estado"), cont = $("#cont");
  ta.value = original;
  const base = contarPalavras(original);
  let salvo = original, tituloSalvo = c.titulo || "", tempo = null, gravando = false;
  // fogueira: anel da meta do dia + chama que cresce + XP a cada 10 palavras novas
  const fog = document.createElement("div"); fog.className = "fogueira"; fog.title = "Meta do dia"; fog.innerHTML = `<div class="anel-xp" style="--f:0"><b>🔥</b></div><small></small>`; document.body.append(fog);
  let ultN = null;
  const fogueira = (n) => {
    const d = new Date().toISOString().slice(0, 10); let h = local.ler("palavrasHoje", { d, n: 0 }); if (h.d !== d) h = { d, n: 0 };
    if (ultN !== null && n > ultN) { const antes = h.n; h.n += n - ultN; local.gravar("palavrasHoje", h); X.registrarDia(n - ultN); const dez = Math.floor(h.n / 10) - Math.floor(antes / 10); if (dez > 0) registrar("escrever", dez * 10); }
    ultN = n;
    const meta = local.ler("metaDiaria", 1000), f = Math.min(1, h.n / meta);
    fog.querySelector(".anel-xp").style.setProperty("--f", f); fog.querySelector("b").style.fontSize = 14 + 16 * f + "px"; fog.classList.toggle("batida", f >= 1);
    fog.querySelector("small").textContent = `${h.n}/${meta}`;
  };
  fog.onclick = async () => { const r = await perguntar("Meta diária de palavras", String(local.ler("metaDiaria", 1000)), "1000"); const v = parseInt(r); if (v > 0) { local.gravar("metaDiaria", v); fogueira(contarPalavras(ta.value)); } };
  const contar = () => { const n = contarPalavras(ta.value); fogueira(n); cont.textContent = `${n.toLocaleString("pt-BR")} palavras · salva sozinho e aparece no celular`; $("#sessao").textContent = n !== base ? `sessão: ${n - base >= 0 ? "+" : ""}${(n - base).toLocaleString("pt-BR")}` : ""; };
  const salvar = async () => {
    if (gravando || (ta.value === salvo && ti.value === tituloSalvo)) return;
    gravando = true; est.textContent = "Salvando…"; est.style.color = "var(--amb)";
    const t = ta.value, tt = ti.value.trim();
    try { await salvarTexto(obraId, c, t, tt); salvo = t; tituloSalvo = ti.value; est.textContent = "Salvo"; est.style.color = "var(--verde)"; if (ta.value === salvo) X.apagarRascunho(capId); }
    catch (e) { est.textContent = "Sem conexão — tentando de novo"; est.style.color = "var(--erro)"; setTimeout(agendar, 4000); }
    gravando = false;
    if (ta.value !== salvo || ti.value !== tituloSalvo) agendar();
  };
  $("#bia").onclick = async () => { clearTimeout(tempo); await salvar(); ir(`/ia/${obraId}/${capId}`); };
  $("#bvivo").onclick = async () => {
    clearTimeout(tempo); await salvar(); toast("Abrindo a sala…");
    try { const { abrirSala } = await import("./aovivo.js"); await abrirSala(obraId, (await capitulos(obraId, true)).find((x) => x.id === capId)); ir(`/aovivo/${obraId}/${capId}`); } catch (e) { toast("Não deu para abrir a sala: " + e.message); }
  };
  const agendar = () => { clearTimeout(tempo); est.textContent = "Editando…"; est.style.color = "var(--suave)"; tempo = setTimeout(salvar, 1500); };
  const inserir = (s, envolver = false) => {
    ta.focus(); const a = ta.selectionStart, b = ta.selectionEnd; const sel = ta.value.slice(a, b);
    const novo = envolver ? s + sel + s : s;
    ta.setRangeText(novo, a, b, "end"); if (envolver && !sel) ta.selectionStart = ta.selectionEnd = a + s.length;
    ta.dispatchEvent(new Event("input"));
  };
  $$("[data-ins]").forEach((b) => (b.onclick = () => inserir(b.dataset.ins.replace(/\\n/g, "\n"))));
  $$("[data-env]").forEach((b) => (b.onclick = () => inserir(b.dataset.env, true)));
  $("#befx").onclick = () => {
    const a = ta.selectionStart, z = ta.selectionEnd;
    X.painelEfeitos(ta.value.slice(a, z), (sintaxe, bloco) => {
      let ins = sintaxe;
      if (bloco) { if (a > 0 && ta.value[a - 1] !== "\n") ins = "\n" + ins; if (z < ta.value.length && ta.value[z] !== "\n") ins += "\n"; }
      ta.focus(); ta.setRangeText(ins, a, z, "end"); ta.dispatchEvent(new Event("input"));
    }, verTodos);
  };
  const verTodos = () => {
    const linha = (ex, d, ins, env) => `<button class="efx" data-x="${esc(ins)}" ${env ? "data-e" : ""}><code>${esc(ex)}</code><span>${esc(d)}</span></button>`;
    const d = dialogo(`<h3 style="margin-top:0">Efeitos de escrita</h3><p class="suave peq">Escreva os símbolos e o leitor faz o efeito sozinho (no PC e no celular). Clique para inserir.</p>
      <div class="rotulo">No texto (selecione um trecho)</div>${linha("!!GRITO!!", "Fala gritada, grande e tremendo", "!!", 1)}${linha("((sussurro))", "Baixinho, menor e em itálico", "((", 2)}${linha("^^brilho^^", "Palavra mágica dourada", "^^", 1)}${linha("{telepatia}", "Voz mental azul", "{", 4)}${linha("~~riscado~~", "Palavra riscada", "~~", 1)}${linha("[S]", "Selo de rank (F até SSS)", "[S]")}${linha("||segredo||", "Borrado até o leitor tocar", "||", 1)}${linha("%%glitch%%", "Letras corrompidas trocando", "%%", 1)}${linha("<<voz sombria>>", "Vermelho com sombra: vilão, demônio", "<<", 3)}
      <div class="rotulo" style="margin-top:12px">Parágrafo inteiro</div>${linha("[[texto]]", "Janela azul de Sistema", "\n[[Habilidade adquirida: Punho de Ferro]]\n")}${linha("[[ [x] feito | [ ] falta ]]", "Lista de missões no Sistema", "\n[[Missões | [x] Sobreviver à noite | [ ] Achar o mestre]]\n")}${linha("[+item | raridade]", "Item obtido (comum, raro, épico, lendário, mítico)", "\n[+Espada do Vazio | lendário]\n")}${linha("{{TÍTULO | texto}}", "Pergaminho com selo", "\n{{DECRETO DA SEITA | texto do decreto}}\n")}${linha(":: local | hora", "Local e hora, abertura de cena", "\n:: Seul, Gangnam | 03:14 da manhã\n")}${linha("> frase — autor", "Epígrafe", "\n> Frase marcante — Autor\n")}${linha("@@Nome", "Cartão do personagem (puxa a ficha)", "\n@@")}${linha(">>Nome: texto", "Mensagem de outra pessoa (com nome)", "\n>>Kang: ")}${linha(">>>Nome: texto", "Comentários ao vivo (várias linhas)", "\n>>>Nome: comentário\n")}${linha("N.A.: texto", "Nota do autor", "\nN.A.: ")}${linha("[[HP 30/100]]", "Barras de status (separe com |)", "\n[[HP 30/100 | MP 80/100 | XP 45/100]]\n")}${linha("[!aviso!]", "Notificação que desliza do topo", "\n[!Você subiu de nível!]\n")}${linha("== texto", "Carta escrita à mão (várias linhas ==)", "\n== ")}${linha("## TÍTULO", "Título de impacto", "\n## ")}${linha(">> texto", "Mensagem de celular", "\n>> ")}${linha("~pausa", "Pausa dramática antes do próximo parágrafo", "\n~pausa\n")}
      <div class="rotulo" style="margin-top:12px">Efeito de cena (numa linha sozinha, vale até ~fim)</div>${Object.entries(EFEITOS_CENA).map(([k, n]) => linha("~" + k, n, `\n~${k}\n`)).join("")}${linha("~fim", "⏹ Desligar o efeito", "\n~fim\n")}
      <div class="rotulo" style="margin-top:12px">Mega Pack · {efeito: texto} num trecho · [#efeito campo | campo] no parágrafo · cor: {fogo:azul: …} [#sistema:vermelho …]</div>${MEGA.map(([n, c, cat]) => linha(c, n, cat === "inline" ? c.replace(/texto|tradução/, "") : "\n" + c + "\n")).join("")}
      <div class="linha-botoes"><button class="btn peq" data-n>Fechar</button></div>`);
    d.querySelectorAll("[data-x]").forEach((b) => (b.onclick = () => { d.close?.(); d.remove?.(); const x = b.dataset.x; if (x === "((" || x === "<<" || x === "{") { const f = x === "((" ? "))" : x === "<<" ? ">>" : "}"; const a = ta.selectionStart, z = ta.selectionEnd; ta.setRangeText(x + ta.value.slice(a, z) + f, a, z, "end"); ta.dispatchEvent(new Event("input")); ta.focus(); } else inserir(x, b.hasAttribute("data-e")); }));
  };
  $("[data-fala]").onclick = () => { const a = ta.selectionStart; const antes = ta.value.slice(0, a); inserir((antes && !antes.endsWith("\n") ? "\n\n" : "") + "— "); };
  $("#bfoco").onclick = () => document.body.classList.toggle("foco");
  // máquina de escrever: papel, letra de máquina, som de cada tecla e a linha atual sempre no meio da tela
  const maq = maquinaDeEscrever(ta);
  $("#bmaq").classList.toggle("on", maq.ligada());
  $("#bmaq").onclick = () => { $("#bmaq").classList.toggle("on", maq.alternar()); ta.focus(); };
  $("#bfichas").onclick = () => {
    const d = dialogo(`<h3 style="margin-top:0">Personagens e lugares</h3>${fs.length ? `<div class="lista-nomes">${fs.map((f) => `<div><b>${esc(f.nome)}</b><span class="suave peq">${f.tipo === "LUGAR" ? "lugar" : "personagem"}</span><button class="btn2 peq" data-mencao="${esc(f.nome)}" data-s="${f.tipo === "LUGAR" ? "&" : "@"}">Inserir</button></div>`).join("")}</div>` : `<p class="suave">Nenhuma ficha ainda. Escreva @Nome, descrição no texto.</p>`}<div class="linha-botoes"><a class="btn2 peq" href="#/obra/${obraId}/fichas">Abrir fichas</a><button class="btn peq" data-n>Fechar</button></div>`);
    $$("[data-mencao]", d).forEach((b) => (b.onclick = () => { d.close(); inserir(marcar(b.dataset.s, b.dataset.mencao)); }));
    $("[data-n]", d).onclick = () => d.close();
  };
  // sugestões de @ e &
  const marcar = (s, nome) => /^[\p{L}][\p{L}\p{N}'’-]*(\s(?:(?:d[aeo]s?|de|di|del|du|la|le|van|von|bin|al)\s)?\p{Lu}[\p{L}\p{N}'’-]*){0,4}$/u.test(nome) ? s + nome : `${s}[${nome}]`;
  const sug = $("#sug");
  const atualizarSug = () => {
    const ate = ta.value.slice(0, ta.selectionStart);
    const m = ate.match(/(?:^|[\s(“"—–-])([@&])\[?([\p{L}\p{N}'’ -]{0,30})$/u);
    if (!m || m[2].endsWith("  ")) { sug.hidden = true; return; }
    const lugar = m[1] === "&"; const termo = m[2].trim().toLowerCase();
    const op = fs.filter((f) => (f.tipo === "LUGAR") === lugar && (!termo || f.nome.toLowerCase().includes(termo) || (f.apelidos || "").toLowerCase().includes(termo))).slice(0, 10);
    sug.hidden = false;
    sug.innerHTML = `<span class="suave peq">${lugar ? "LUGARES" : "PERSONAGENS"}</span>${op.map((f) => `<button data-sn="${esc(f.nome)}">${esc(f.nome)}</button>`).join("")}${termo.length >= 2 && !op.some((f) => f.nome.toLowerCase() === termo) ? `<button class="criar" data-novo="${esc(m[2].trim())}">+ Criar “${esc(m[2].trim())}”</button>` : ""}${!op.length && termo.length < 2 ? `<span class="suave peq">digite o nome… (vírgula depois = descrever)</span>` : ""}`;
    const completar = (nome) => { const ini = ta.selectionStart - m[2].length - 1 - (ate.endsWith("[" + m[2]) ? 1 : 0); ta.setRangeText(marcar(m[1], nome) + " ", ini, ta.selectionStart, "end"); ta.focus(); sug.hidden = true; ta.dispatchEvent(new Event("input")); };
    $$("[data-sn]", sug).forEach((b) => (b.onmousedown = (e) => { e.preventDefault(); completar(b.dataset.sn); }));
    $$("[data-novo]", sug).forEach((b) => (b.onmousedown = async (e) => { e.preventDefault(); const nome = b.dataset.novo; const id = await salvarFicha(obraId, null, { nome, tipo: lugar ? "LUGAR" : "PERSONAGEM" }); fs.push({ id, nome, tipo: lugar ? "LUGAR" : "PERSONAGEM", apelidos: "" }); completar(nome); toast("Ficha criada"); }));
  };
  ta.oninput = () => { contar(); agendar(); atualizarSug(); };
  ta.onclick = atualizarSug; ta.onkeyup = (e) => { if (e.key.startsWith("Arrow")) atualizarSug(); };
  ti.oninput = agendar;
  // buscar e substituir
  const busca = $("#busca"); let achados = [], atual = -1;
  const procurar = () => {
    const q = $("#q").value; achados = []; atual = -1;
    if (q.length >= 1) { const t = ta.value.toLowerCase(), k = q.toLowerCase(); let p = t.indexOf(k); while (p >= 0 && achados.length < 5000) { achados.push(p); p = t.indexOf(k, p + k.length); } }
    $("#qn").textContent = q ? `${achados.length} achado(s)` : "";
  };
  const irPara = (d) => { if (!achados.length) return; atual = (atual + d + achados.length) % achados.length; const p = achados[atual]; ta.focus(); ta.setSelectionRange(p, p + $("#q").value.length); const linha = ta.value.slice(0, p).split("\n").length; ta.scrollTop = Math.max(0, (linha - 5) * 33); $("#qn").textContent = `${atual + 1} de ${achados.length}`; };
  $("#bbusca").onclick = () => { busca.hidden = !busca.hidden; if (!busca.hidden) $("#q").focus(); };
  $("#q").oninput = procurar; $("#q").onkeydown = (e) => { if (e.key === "Enter") irPara(e.shiftKey ? -1 : 1); };
  $("#qprox").onclick = () => irPara(1); $("#qant").onclick = () => irPara(-1); $("#qx").onclick = () => { busca.hidden = true; };
  $("#rum").onclick = () => { if (atual < 0) { irPara(1); return; } const q = $("#q").value; ta.setRangeText($("#r").value, achados[atual], achados[atual] + q.length, "end"); ta.dispatchEvent(new Event("input")); procurar(); irPara(0); };
  $("#rtodos").onclick = async () => { const q = $("#q").value; if (!q) return; const n = achados.length; if (!n || !(await confirmar("Substituir todos?", `${n} ocorrência(s) de “${q}” viram “${$("#r").value}”.`, "Substituir"))) return; ta.value = ta.value.split(q).join($("#r").value); ta.dispatchEvent(new Event("input")); procurar(); toast(`${n} substituída(s)`); };
  window.onkeydown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "s") { e.preventDefault(); salvar(); }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "f") { e.preventDefault(); busca.hidden = false; $("#q").focus(); }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "b" && document.activeElement === ta) { e.preventDefault(); inserir("**", true); }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "i" && document.activeElement === ta) { e.preventDefault(); inserir("*", true); }
    if (e.key === "Escape") { sug.hidden = true; document.body.classList.remove("foco"); }
  };
  // ── vindos do celular: salva a cada 5 s digitando, rascunho de emergência, versões, contagem e prévia ──
  const cada5 = setInterval(() => { if (!gravando && (ta.value !== salvo || ti.value !== tituloSalvo)) salvar(); }, 5000);
  let tRasc = null;
  ta.addEventListener("input", () => { clearTimeout(tRasc); tRasc = setTimeout(() => { if (ta.value !== salvo) X.gravarRascunho(capId, ta.value); }, 600); });
  const rasc = X.lerRascunho(capId);
  if (rasc && rasc.texto && rasc.texto !== original) {
    const quando = new Date(rasc.quando).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
    confirmar("Texto não salvo encontrado", `Da última vez a página fechou antes de salvar. Achei um rascunho de ${quando} com ${contarPalavras(rasc.texto).toLocaleString("pt-BR")} palavras (o salvo tem ${base.toLocaleString("pt-BR")}). Recuperar? O texto salvo vira uma versão antes.`, "Recuperar")
      .then((ok) => { if (ok) { X.guardarVersao(capId, original, "Antes de recuperar o rascunho"); ta.value = rasc.texto; ta.dispatchEvent(new Event("input")); } else X.apagarRascunho(capId); });
  }
  cont.onclick = () => X.mostrarContagem(ta.value, ta.value.slice(ta.selectionStart, ta.selectionEnd));
  $("#bver").onclick = async () => {
    const d = dialogo(`<h3 style="margin-top:0">Versões</h3><p class="suave peq">Guarde o texto como está agora, com um nome, antes de mexer pesado.</p><input class="campo" id="vnome" placeholder="Ex.: Antes de reescrever o final"><div class="linha-botoes"><button class="btn2 peq" id="vlista">Ver versões</button><button class="btn peq" id="vok">Guardar versão</button></div>`);
    $("#vok", d).onclick = () => { const ok = X.guardarVersao(capId, ta.value, $("#vnome", d).value.trim()); d.close(); toast(ok ? "★ Versão guardada" : "Sem espaço no navegador para guardar"); };
    $("#vlista", d).onclick = () => { d.close(); X.mostrarVersoes(capId, (t) => { X.guardarVersao(capId, ta.value, "Antes de restaurar"); ta.value = t; ta.dispatchEvent(new Event("input")); toast("Versão restaurada"); }); };
  };
  // Prévia: o capítulo desenhado como o leitor vê; clicar num efeito oferece tirá-lo (o texto fica)
  const prev = $("#prev"); let pararPrev = null;
  const mostrarPrevia = (sim) => {
    prev.hidden = !sim; ta.hidden = sim; $("#bprev").classList.toggle("on", sim);
    pararPrev?.(); pararPrev = null;
    if (!sim) { ta.focus(); return; }
    const origens = [];
    prev.innerHTML = htmlTexto(ta.value, mapaNomes(fs), new Map(), { enfeite: o.enfeite, autor: o.autor || nomeAutor(), origens });
    try { pararPrev = ligarFx(prev); } catch (_) { pararPrev = null; }
    $$("[data-b]", prev).forEach((el) => {
      const i = origens[+el.dataset.b] ?? -1; const ln = ta.value.split("\n")[i] ?? "";
      const temFx = ln.trim() && X.semEfeito(ln) !== ln.trim();
      el.style.cursor = "pointer";
      el.title = temFx ? "Clique: remover efeito ou ver os símbolos" : "Clique: ver no texto";
      el.onclick = async () => {
        const linhas = ta.value.split("\n");
        const irTexto = () => { mostrarPrevia(false); const ini = linhas.slice(0, i).join("\n").length + (i ? 1 : 0); ta.setSelectionRange(ini, ini + (linhas[i] || "").length); ta.scrollTop = Math.max(0, (i - 5) * 33); };
        if (!temFx) { irTexto(); return; }
        const d = dialogo(`<h3 style="margin-top:0">Efeito neste parágrafo</h3><p class="suave peq">${esc(ln.slice(0, 120))}</p><div class="linha-botoes"><button class="btn2 peq" id="pv-ver">Ver os símbolos</button><button class="btn peq" id="pv-tirar" style="background:var(--erro)">Remover efeito (o texto fica)</button></div>`);
        $("#pv-ver", d).onclick = () => { d.close(); irTexto(); };
        $("#pv-tirar", d).onclick = () => { d.close(); X.guardarVersao(capId, ta.value, "Antes de remover um efeito"); linhas[i] = X.semEfeito(linhas[i]); ta.value = linhas.join("\n"); ta.dispatchEvent(new Event("input")); mostrarPrevia(true); toast("Efeito removido (a versão de antes ficou em ★ Versões)"); };
      };
    });
  };
  $("#bprev").onclick = () => mostrarPrevia(prev.hidden);
  const atalhosExtra = (e) => {
    if (!(e.ctrlKey || e.metaKey)) return;
    if (e.key.toLowerCase() === "e") { e.preventDefault(); $("#befx")?.click(); }
    if (e.key.toLowerCase() === "p") { e.preventDefault(); $("#bprev")?.click(); }
  };
  window.addEventListener("keydown", atalhosExtra);
  contar(); ta.focus();
  window.onbeforeunload = (e) => { if (ta.value !== salvo) { salvar(); e.preventDefault(); } };
  E.saindo = async () => { fog.remove(); clearInterval(cada5); window.removeEventListener("keydown", atalhosExtra); pararPrev?.(); window.onbeforeunload = null; document.body.classList.remove("foco"); clearTimeout(tempo); await salvar(); };
});

// ───────── LEITOR DO CATÁLOGO ─────────
rota("/publer/(\\w+)/(\\w+)", async (pid, cid) => {
  carregando("descobrir");
  const [o, todos, fs] = await Promise.all([obraPub(pid), capsPub(pid, true), fichasPub(pid), carteira()]);
  if (!o) { casca("descobrir", `<p class="suave">Obra não encontrada.</p>`); return; }
  // capítulos no ar + os agendados que a pessoa liberou com moedas (ou todos, se for o autor)
  const caps = todos.filter((c) => (c.publicadoEm || 0) <= agora() || o.autorUid === E.usuario.uid || liberado(pid, c.id));
  const i = caps.findIndex((c) => c.id === cid); const c = caps[i]; if (!c) { ir(`/pub/${pid}`); return; }
  const texto = await lerTexto(collection(db, "publicas", pid, "textos"), cid, c.partes);
  await corDoLeitor(await imagemPub(pid, "capa"), null);
  const prox = caps[i + 1], ant = caps[i - 1];
  const [gostei, coms] = await Promise.all([curtiu(pid, cid), comentarios(pid, cid).catch(() => [])]);
  const fichasLista = fs.map((f) => ({ ...f, id: f.id }));
  const porTrecho = new Map(); for (const m of coms) if ((m.bloco ?? -1) >= 0) porTrecho.set(m.bloco, (porTrecho.get(m.bloco) || 0) + 1);
  casca("descobrir", `<div class="prog" id="prog"></div><div class="leitor tema-leitura" style="${estiloLeitura()}">${hudTopo(`#/pub/${pid}`, `${esc(o.titulo)} · ${esc(c.titulo)}`, "")}${hudBaixo()}
    <div class="cabec"><div class="rotulo">${esc(o.titulo)}</div><h1>${esc(c.titulo)}</h1><div class="suave peq">por <a href="#/autor/${esc(o.autorUid)}" style="text-decoration:underline">${esc(o.autorNome)}</a> · ${palavrasFmt(c.palavras)} palavras</div></div>
    ${cartaoAnterior(ant)}
    <div class="texto" id="texto">${htmlTexto(texto, mapaNomes(fichasLista), new Map(), { enfeite: o.enfeite, coms: porTrecho, autor: o.autorNome })}</div>
    <div class="fim">${fimHTML(i + 1, caps.length, i + 1)}${await reacoesHTML(pid, cid)}
      <div class="social"><button class="btn2 ${gostei ? "on" : ""}" id="curtir">${gostei ? "♥" : "♡"} <span id="nc">${c.curtidas || 0}</span></button><span class="suave">💬 ${coms.length}</span></div>
      <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center">${ant ? `<a class="btn2" href="#/publer/${pid}/${ant.id}">‹ Anterior</a>` : ""}${prox ? `<a class="btn" href="#/publer/${pid}/${prox.id}">Próximo ›</a>` : `<span class="suave">Último capítulo publicado.</span>`}</div></div>
    <div class="comentarios vidro"><h3 style="margin-top:0">Comentários</h3>
      <div style="display:flex;gap:10px"><textarea class="campo" id="com" rows="2" placeholder="O que achou do capítulo?"></textarea><button class="btn peq" id="enviar">Enviar</button></div>
      <div id="lcom">${coms.map((m) => `<div class="comentario"><span class="av" style="width:34px;height:34px">${esc((m.nome || "?").slice(0, 1))}</span><div style="flex:1"><b>${esc(m.nome)}</b> <span class="suave peq">${haQuanto(m.em)}</span><div>${esc(m.texto)}</div></div>${m.uid === E.usuario.uid || o.autorUid === E.usuario.uid ? `<button class="btn2 peq" data-ac="${m.id}">Apagar</button>` : ""}</div>`).join("") || `<p class="suave">Seja o primeiro a comentar.</p>`}</div></div></div>`);
  ligarNomes(fichasLista);
  fichasVoz = fichasLista; traduzirTela();
  if ($("#ouvir")) $("#ouvir").onclick = () => ouvir(blocoNaTela());
  ligarHud();
  $("#aj").onclick = () => painelAjustes(() => $(".leitor").setAttribute("style", estiloLeitura()));
  if ($("#lcaps")) $("#lcaps").onclick = () => painelCapitulos(caps, cid, (x) => `#/publer/${pid}/${x.id}`);
  $("#curtir").onclick = async (e) => { const b = e.currentTarget; const sim = !b.classList.contains("on"); b.classList.toggle("on", sim); b.firstChild.textContent = sim ? "♥ " : "♡ "; $("#nc").textContent = (+$("#nc").textContent) + (sim ? 1 : -1); try { await curtir(pid, cid, sim); } catch (_) {} };
  $("#enviar").onclick = async () => { const t = $("#com").value.trim(); if (!t) return; await comentar(pid, cid, t); registrar("comentar"); toast("Comentário enviado"); window.redesenhar(); };
  ligarReacoes();
  $$("[data-ac]").forEach((b) => (b.onclick = async () => { if (await confirmar("Apagar comentário?", "", "Apagar", true)) { await apagarComentario(pid, cid, b.dataset.ac); window.redesenhar(); } }));
  scrollTo(0, 0);
  contarLeitura(pid, cid);
  $$("[data-trecho]").forEach((b) => (b.onclick = (e) => { e.stopPropagation(); comentariosTrecho(pid, cid, +b.dataset.trecho, b.closest("p").textContent.replace(/💬.*$/, "").trim(), coms); }));
  // progresso de quem segue: guarda o último capítulo lido nesta obra
  const naBib = [...E.obras.entries()].find(([, x]) => x.publicaId === pid && !x.apagado);
  if (naBib) salvarObra(naBib[0], { lidaEm: agora() });
  let ganhou = false;
  const aoRolar = () => { const alt = document.documentElement.scrollHeight - innerHeight; const fr = alt > 0 ? Math.min(1, scrollY / alt) : 1; const b = $("#prog"); if (b) b.style.width = fr * 100 + "%"; maratona(prox && `/publer/${pid}/${prox.id}`);
    if (fr > 0.97 && !ganhou && o.autorUid !== E.usuario.uid) { ganhou = true; if (!local.ler("xpLido/" + cid, false)) { local.gravar("xpLido/" + cid, true); registrar("ler"); confete(90); } ganharMoedas(1, "leitura", 10, pid + "/" + cid).then((n) => n && toast("🪙 +1 moeda por terminar o capítulo")); } };
  // estatística para o autor: parágrafo mais longe que a pessoa chegou e tempo na página
  const blocos = $$("#texto [data-b]"); const totalB = blocos.length ? +blocos[blocos.length - 1].dataset.b + 1 : 0;
  let maxB = -1, inicio = Date.now();
  const medir = () => { const lim = innerHeight * 0.7; let ult = -1; for (const el of blocos) { if (el.getBoundingClientRect().top < lim) ult = +el.dataset.b; else break; } if (ult > maxB) maxB = ult; };
  const enviar = () => { if (o.autorUid === E.usuario.uid) return; const seg = Math.min(3 * 3600, Math.round((Date.now() - inicio) / 1000)); inicio = Date.now(); if (seg >= 3) registrarLeitura(pid, cid, maxB, totalB, seg); };
  const aoEsconder = () => { if (document.visibilityState === "hidden") enviar(); else inicio = Date.now(); };
  document.addEventListener("visibilitychange", aoEsconder);
  medir();
  window.onscroll = () => { aoRolar(); medir(); };
  window.onkeydown = (e) => { if (e.target.closest("input,textarea,dialog")) return; if (e.key === "ArrowRight" && prox) ir(`/publer/${pid}/${prox.id}`); if (e.key === "ArrowLeft" && ant) ir(`/publer/${pid}/${ant.id}`); };
  const desligarClima = (() => { const a = ligarClima(), z = ligarEfeitosTexto(); return () => { a(); z(); }; })();
  E.saindo = async () => { document.removeEventListener("visibilitychange", aoEsconder); enviar(); pararVoz(); desligarClima(); };
});

// ───────── REVISÃO (notas de revisores) ─────────
rota("/revisao/(\\w+)/(\\w+)", async (pid, cid) => {
  carregando("colab");
  const { capsRevisao } = await import("./nucleo.js");
  const caps = await capsRevisao(pid); const c = caps.find((x) => x.id === cid);
  if (!c) { casca("colab", `<p class="suave">Este capítulo saiu da revisão.</p>`); return; }
  const [texto, ns] = await Promise.all([textoRevisao(pid, c), notas(pid, cid)]);
  const souAutor = E.obras.has(pid);
  const linhas = limparMarcas(texto).split("\n").map((l) => l.trim()).filter(Boolean);
  casca("colab", `<div class="topo"><a class="voltar vidro" href="javascript:history.back()">←</a><h1>Revisão · ${esc(c.titulo)}</h1></div>
    <div class="revisao"><div class="texto rev-texto" id="texto" style="font-size:18px">${linhas.map((l, i) => `<p data-b="${i}" class="${ns.some((n) => n.trecho && l.startsWith(n.trecho.slice(0, 40))) ? "com-nota" : ""}">${esc(l)}</p>`).join("")}</div>
      <aside class="notas vidro"><h3 style="margin-top:0">Notas · ${ns.length}</h3><p class="suave peq">Clique num parágrafo para comentar.</p>
        ${ns.map((n) => `<div class="nota-rev"><div class="suave peq"><b>${esc(n.nome)}</b> · ${haQuanto(n.em)}</div>${n.trecho ? `<blockquote>${esc(n.trecho.slice(0, 160))}</blockquote>` : ""}<div>${esc(n.texto)}</div>${n.uid === E.usuario.uid || souAutor ? `<button class="btn2 peq" data-an="${n.id}">Apagar</button>` : ""}</div>`).join("")}</aside></div>`);
  $$("#texto [data-b]").forEach((p) => (p.onclick = async () => { const t = await perguntar("Sua nota sobre este trecho", ""); if (!t) return; await anotar(pid, cid, p.textContent, t); toast("Nota enviada"); window.redesenhar(); }));
  $$("[data-an]").forEach((b) => (b.onclick = async () => { await apagarNota(pid, cid, b.dataset.an); window.redesenhar(); }));
});

// ───────── máquina de escrever ─────────
let audio = null;
function som(tipo) {
  try {
    audio = audio || new AudioContext(); const a = audio, t = a.currentTime;
    const ruido = (dur, vol, freq, q = 1) => {
      const b = a.createBuffer(1, Math.floor(a.sampleRate * dur), a.sampleRate), d = b.getChannelData(0);
      for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / d.length, 5);
      const s = a.createBufferSource(); s.buffer = b; const f = a.createBiquadFilter(); f.type = "bandpass"; f.frequency.value = freq; f.Q.value = q;
      const g = a.createGain(); g.gain.value = vol; s.connect(f).connect(g).connect(a.destination); s.start(t);
    };
    const tom = (freq, dur, vol, quando = 0) => {
      const o = a.createOscillator(), g = a.createGain(); o.frequency.value = freq; o.type = "sine";
      g.gain.setValueAtTime(vol, t + quando); g.gain.exponentialRampToValueAtTime(0.0001, t + quando + dur); o.connect(g).connect(a.destination); o.start(t + quando); o.stop(t + quando + dur);
    };
    if (tipo === "tecla") { ruido(0.035, 0.9, 2200 + Math.random() * 900, 0.8); ruido(0.05, 0.5, 260, 1.2); }
    else if (tipo === "apagar") ruido(0.03, 0.5, 1400, 1);
    else if (tipo === "espaco") { ruido(0.05, 0.6, 900, 0.7); ruido(0.06, 0.4, 180, 1); }
    else if (tipo === "enter") { tom(2093, 0.9, 0.18); ruido(0.32, 0.5, 700, 0.5); setTimeout(() => ruido(0.06, 0.8, 300, 1), 260); }
  } catch (_) {}
}
function caretY(ta) {
  const m = document.createElement("div"); const cs = getComputedStyle(ta);
  for (const k of ["fontFamily", "fontSize", "fontWeight", "lineHeight", "letterSpacing", "padding", "border", "boxSizing", "whiteSpace", "wordWrap", "width"]) m.style[k] = cs[k];
  m.style.position = "absolute"; m.style.visibility = "hidden"; m.style.whiteSpace = "pre-wrap"; m.style.overflowWrap = "break-word";
  m.textContent = ta.value.slice(0, ta.selectionStart); const sp = document.createElement("span"); sp.textContent = "|"; m.appendChild(sp);
  document.body.appendChild(m); const y = sp.offsetTop; m.remove(); return y;
}
export function maquinaDeEscrever(ta) {
  let on = !!local.ler("maquina", false);
  const aplicar = () => document.body.classList.toggle("maquina", on);
  const centrar = () => { if (!on) return; const alvo = caretY(ta) - ta.clientHeight * 0.42; ta.scrollTo({ top: Math.max(0, alvo), behavior: "smooth" }); };
  ta.addEventListener("keydown", (e) => {
    if (!on || e.ctrlKey || e.metaKey || e.altKey) return;
    if (e.key === "Enter") som("enter"); else if (e.key === "Backspace" || e.key === "Delete") som("apagar"); else if (e.key === " ") som("espaco"); else if (e.key.length === 1) som("tecla");
    if (e.key.length === 1 || e.key === "Enter") { ta.classList.remove("batida"); void ta.offsetWidth; ta.classList.add("batida"); }
  });
  ta.addEventListener("input", () => requestAnimationFrame(centrar));
  aplicar(); setTimeout(centrar, 50);
  return { ligada: () => on, alternar: () => { on = !on; local.gravar("maquina", on); aplicar(); centrar(); return on; } };
}
