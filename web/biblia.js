// Bíblia viva (furos, retratos, mapa de relações) e Leitores (estatísticas) no PC.
// Mesmas regras de app/src/main/java/com/huryel/novely/data/Biblia.kt — mudar os dois juntos.
import {
  E, $, $$, esc, toast, casca, carregando, rota, ir, local, capitulos, fichas, lerTexto, colTextos, limparMarcas, partesUnicas, salvarFicha, leiturasObra, capsPub,
} from "./nucleo.js";

const L = "\\p{L}\\p{N}";
const FRASES = /(?<=[.!?…])["”»]?\s+/u;
const VERBO_FALA = "disse|diz|perguntou|pergunta|respondeu|responde|gritou|grita|murmurou|sussurrou|falou|fala|retrucou|rosnou|riu|completou|continuou|explicou|insistiu|berrou|resmungou|exclamou|declarou|rebateu|admitiu|repetiu|chamou|cortou|provocou|zombou|suspirou|ordenou|avisou";
const RX_VERBO = new RegExp(`\\b(${VERBO_FALA})\\b`, "g");
const NUMS = { um: 1, uma: 1, dois: 2, duas: 2, "três": 3, tres: 3, quatro: 4, cinco: 5, seis: 6, sete: 7, oito: 8, nove: 9, dez: 10, onze: 11, doze: 12, treze: 13, catorze: 14, quatorze: 14, quinze: 15, dezesseis: 16, dezessete: 17, dezoito: 18, dezenove: 19, vinte: 20, trinta: 30, quarenta: 40, cinquenta: 50, sessenta: 60, setenta: 70, oitenta: 80, noventa: 90, cem: 100 };
const NW = Object.keys(NUMS).join("|");
const NUM = `\\d{1,3}|(?:${NW})(?:\\s+e\\s+(?:${NW}))?`;
const PESSOA = "garot[oa]|menin[oa]|homem|mulher|rapaz|moça|jovem|velh[oa]|senhor[a]?|criança|adolescente|estudante|guerreir[oa]|filh[oa]|irmã[o]?|lutador[a]?|soldado|aluno|aluna|monge|princes[a]|príncipe|herdeir[oa]|órfã[o]?";
const RX_IDADE = [
  new RegExp(`\\b(${NUM})\\s+anos\\s+de\\s+idade`, "giu"),
  new RegExp(`\\b(?:tinha|tem|tenho|completou|completara|completava|fez|fizera|faria|completaria)\\s+(?:apenas\\s+|só\\s+|quase\\s+)?(${NUM})\\s+anos\\b(?!\\s+de\\s+(?!idade))`, "giu"),
  new RegExp(`\\b(?:${PESSOA})\\s+de\\s+(${NUM})\\s+anos\\b`, "giu"),
  new RegExp(`,\\s+de\\s+(${NUM})\\s+anos\\s*[,.]`, "gu"),
];
const CORES = { azul: "azul", azuis: "azul", verde: "verde", verdes: "verde", castanho: "castanho", castanhos: "castanho", castanha: "castanho", preto: "preto", pretos: "preto", preta: "preto", negro: "preto", negros: "preto", cinza: "cinza", cinzas: "cinza", cinzento: "cinza", cinzentos: "cinza", dourado: "dourado", dourados: "dourado", vermelho: "vermelho", vermelhos: "vermelho", rubros: "vermelho", carmesim: "vermelho", escarlate: "vermelho", roxo: "roxo", roxos: "roxo", violeta: "roxo", violetas: "roxo", "âmbar": "âmbar", mel: "âmbar", prateado: "prateado", prateados: "prateado", branco: "branco", brancos: "branco", loiro: "loiro", loiros: "loiro", louro: "loiro", louros: "loiro", ruivo: "ruivo", ruivos: "ruivo", rosa: "rosa", amarelo: "dourado", amarelos: "dourado" };
const COR = Object.keys(CORES).sort((a, b) => b.length - a.length).join("|");
const RX_OLHOS = new RegExp(`\\bolhos?\\s+(?:[${L}]+\\s+){0,2}?(${COR})\\b`, "giu");
const RX_CABELO = new RegExp(`\\b(?:cabelos?|fios|mechas|franja)\\s+(?:[${L}]+\\s+){0,2}?(${COR})\\b`, "giu");
const RX_MUDA = /\b(ficaram|ficou|tornaram|tornou|mudaram|mudou|acenderam|acendeu|lentes?|disfar\w*|tingi\w*|pint\w*|transform\w*|despert\w*)\b/iu;
const RX_MORTE = /\b(morreu|morrera|faleceu|foi mort[oa]|foi assassinad[oa]|est(?:á|ava) mort[oa]|caiu mort[oa]|sem vida|cadáver d[eoa]|enterraram|funeral d[eoa]|túmulo d[eoa]|deu o último suspiro)\b/giu;
const RX_NAO_MORTE = new RegExp(`\\b(não|nunca|quase|fingi\\w*|como se|se |talvez|achou que|achavam que|pensou que|parecia|teria|poderia|ia |vai )\\s*(?:[${L}]+\\s+){0,2}$`, "iu");
const RX_FERIDO = new RegExp(`\\b((?:braço|perna|costela|costelas|nariz|mão|ombro|tornozelo|pulso|mandíbula|clavícula|joelho|dedos?)\\s+(?:[${L}]+\\s+){0,2}?(?:quebrad[oa]s?|fraturad[oa]s?|deslocad[oa]|esmagad[oa]s?|partid[oa]s?)|perdeu o (?:braço|olho|movimento)|ficou cego|ferid[oa] gravemente|gravemente ferid[oa]|em coma)\\b`, "giu");
const RX_LUTA = /\b(socou|soco|chutou|golpeou|lutou|lutava|atacou|saltou|correu|esquivou|bloqueou|agarrou|arremessou|derrubou)\b/iu;
const RX_CURA = /\b(cur(?:ou|ado|ada|ar|ou-se)|cicatriz(?:ou|ado)|recuper|regener|tala|gesso|hospital|enfermaria|curandeir|poção|semanas depois|meses depois|dias depois)/iu;
const RX_PODER = /\b(?:[Nn]ível|[Rr]ank|[Cc]lasse|[Gg]rau|[Ee]stágio|[Pp]atamar|[Íí]ndice|[Cc]amada|[Rr]eino)\s+(?:de\s+)?(S{1,3}|[A-F][+-]?|\d{1,3})\b/gu;
const RX_JURA = /\b(eu juro|juro que|eu prometo|prometo que|prometeu que|jurou que|um dia eu vou|vou te matar|vou matar|vingança|vingar)\b/iu;
const FAMILIA = /\b(irmã[o]?|irmãos|pai|mãe|filh[oa]|avô|avó|ti[oa]|prim[oa]|família|clã)\b/gu;
const ROMANCE = /\b(beij\w*|cor(?:ou|ando)|coração acelerou|apaixon\w*|amor|abraç\w*|mãos dadas|ciúmes?)\b/gu;
const RIVAL = /\b(inimig\w*|rival|odi\w*|socou|golpe\w*|atacou|lut\w*|derrot\w*|desprez\w*|ameaç\w*)\b/gu;
const ALIADO = /\b(amig\w*|junt[oa]s|obrigad[oa]|ajud\w*|trein\w*|confi\w*|protege\w*|mestre|discípul\w*|parceir\w*)\b/gu;
const SEPARADOR = /^(\*\s*){3,}$|^[◆◇~\-—=_]{3,}$|^#.*/;
const TIPO_NOME = { IDADE: "Idade", OLHOS: "Olhos", CABELO: "Cabelo", FERIMENTO: "Ferimento", MORTE: "Morte", PODER: "Nível", JURAMENTO: "Juramento" };

const numero = (s) => { const n = parseInt(s, 10); if (!isNaN(n)) return n; let t = 0; for (const p of s.toLowerCase().split(/\s+e\s+/)) { const v = NUMS[p.trim()]; if (v == null) return null; t += v; } return t; };
const ordemRank = (v) => /^\d+$/.test(v) ? +v : /^S{1,3}$/.test(v) ? 10 + v.length : /^[A-F]/.test(v) ? (70 - v.charCodeAt(0)) * 3 + 1 + (v.endsWith("+") ? 1 : v.endsWith("-") ? -1 : 0) : null;
const conta = (rx, t) => (t.match(rx) || []).length;
const distancia = (a, b) => { if (Math.abs(a.length - b.length) > 1) return 2; a = a.toLowerCase(); b = b.toLowerCase(); let ant = Array.from({ length: b.length + 1 }, (_, i) => i); for (let i = 1; i <= a.length; i++) { const cur = [i]; for (let j = 1; j <= b.length; j++) cur[j] = Math.min(ant[j] + 1, cur[j - 1] + 1, ant[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1)); ant = cur; } return ant[b.length]; };

export function analisar(caps, fs, ignorados = new Set(), obraId = "") {
  const nomes = new Map();
  for (const f of fs) { nomes.set(f.nome.trim(), f.id); for (const a of (f.apelidos || "").split(",").map((x) => x.trim())) if (a.length >= 2 && a[0] === a[0].toUpperCase() && !nomes.has(a)) nomes.set(a, f.id); }
  for (const [p, id] of partesUnicas(fs)) if (!nomes.has(p)) nomes.set(p, id);
  const porId = new Map(fs.map((f) => [f.id, f])); const pessoas = new Set(fs.filter((f) => f.tipo !== "LUGAR" && f.tipo !== "OUTRO").map((f) => f.id));
  const n = caps.length, contagem = new Map(), falas = new Map(), falasCap = new Map(), fatos = [], lugares = new Map(), pares = new Map(), alertas = [], tokens = new Map();
  if (!nomes.size) return { caps, retratos: [], relacoes: [], alertas: [] };
  const alt = [...nomes.keys()].filter((k) => k.length >= 2).sort((a, b) => b.length - a.length).map((k) => k.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + (k.includes(" ") ? "" : "(?!\\s\\p{Lu}\\p{Ll})")).join("|");
  const rx = new RegExp(`(?<![${L}'’-])(?:${alt})(?![${L}'’-])`, "gu");
  const achar = (s) => [...s.matchAll(rx)].map((m) => ({ id: nomes.get(m[0]), ini: m.index, fim: m.index + m[0].length }));
  const perto = (ms, pos, max = 70) => { let best = null, bd = 1e9; for (const m of ms) { if (!pessoas.has(m.id)) continue; const d = pos >= m.fim ? pos - m.fim : pos < m.ini ? m.ini - pos : 0; if (d < bd) { bd = d; best = m.id; } } return bd <= max ? best : null; };
  const rxToken = /(?<=[\p{Ll},;:)”]\s|—\s)\p{Lu}[\p{L}'’]+(?:-[\p{L}'’]+)*/gu;
  caps.forEach((cap, ci) => {
    const texto = limparMarcas(cap.texto); let cena = [], pars = []; const cenas = [], textos = [];
    const fechar = () => { if (cena.length) { cenas.push([...new Set(cena)]); textos.push(pars.join("\n")); } cena = []; pars = []; };
    for (const linha of texto.split("\n")) {
      const par = linha.trim(); if (!par) continue;
      if ((par[0] === "*" || par[0] === "#" || par.length <= 12) && SEPARADOR.test(par)) { fechar(); continue; }
      pars.push(par);
      const ms = achar(par);
      for (const m of ms) { if (!contagem.has(m.id)) contagem.set(m.id, new Array(n).fill(0)); contagem.get(m.id)[ci]++; cena.push(m.id); }
      for (const t of par.matchAll(rxToken)) if (t[0].length >= 4 && !nomes.has(t[0])) tokens.set(t[0], (tokens.get(t[0]) || 0) + 1);
      if (/^[—-]/.test(par)) for (const v of par.matchAll(RX_VERBO)) {
        const ve = v.index + v[0].length;
        const quem = ms.find((m) => m.ini >= ve && m.ini - ve <= 25 && pessoas.has(m.id)) || [...ms].reverse().find((m) => m.fim <= v.index && v.index - m.fim <= 12 && pessoas.has(m.id));
        if (quem) { falas.set(quem.id, (falas.get(quem.id) || 0) + 1); if (!falasCap.has(quem.id)) falasCap.set(quem.id, new Set()); falasCap.get(quem.id).add(ci); }
      }
      if (!ms.length) continue;
      let iniF = 0;
      for (const frase of par.split(FRASES)) {
        const d = Math.max(par.indexOf(frase, iniF), iniF); iniF = d + frase.length;
        if (frase.length < 8) continue;
        const fm = ms.filter((m) => m.ini >= d && m.fim <= d + frase.length).map((m) => ({ id: m.id, ini: m.ini - d, fim: m.fim - d }));
        if (!fm.length) continue;
        const curta = frase.trim().slice(0, 260), min = frase.toLowerCase();
        const add = (id, tipo, valor) => { if (id != null) fatos.push({ id, cap: ci, tipo, valor, frase: curta }); };
        if (min.includes("anos")) for (const r of RX_IDADE) for (const m of frase.matchAll(r)) { const i = numero(m[1]); if (i && i <= 150) add(perto(fm, m.index), "IDADE", `${i} anos`); }
        for (const [r, tipo] of [[RX_OLHOS, "OLHOS"], [RX_CABELO, "CABELO"]]) for (const m of frase.matchAll(r)) {
          const mud = RX_MUDA.test(frase.slice(Math.max(0, m.index - 40), m.index + m[0].length + 30)); const c = CORES[m[1].toLowerCase()];
          if (c) add(perto(fm, m.index, 60), tipo, mud ? `${c} (mudou)` : c);
        }
        for (const m of frase.matchAll(RX_MORTE)) if (!RX_NAO_MORTE.test(frase.slice(Math.max(0, m.index - 30), m.index))) add(perto(fm, m.index, 50), "MORTE", "morreu");
        for (const m of frase.matchAll(RX_FERIDO)) add(perto(fm, m.index, 60), "FERIMENTO", m[0].toLowerCase());
        for (const m of frase.matchAll(RX_PODER)) add(perto(fm, m.index, 60), "PODER", m[0]);
        const j = frase.match(RX_JURA); if (j) add(perto(fm, frase.search(RX_JURA), 80), "JURAMENTO", j[0].toLowerCase());
        const ps = [...new Set(fm.filter((m) => pessoas.has(m.id)).map((m) => m.id))];
        for (const lg of new Set(fm.filter((m) => porId.get(m.id)?.tipo === "LUGAR").map((m) => m.id))) for (const pe of ps) { if (!lugares.has(pe)) lugares.set(pe, new Map()); const l = lugares.get(pe); l.set(lg, (l.get(lg) || 0) + 1); }
      }
    }
    fechar();
    cenas.forEach((ids, k) => {
      const ps = ids.filter((i) => pessoas.has(i)).sort(); if (ps.length < 2) return;
      const tx = textos[k].toLowerCase(); const pal = ps.length <= 4 ? [conta(FAMILIA, tx), conta(ROMANCE, tx), conta(RIVAL, tx), conta(ALIADO, tx)] : null;
      for (let i = 0; i < ps.length; i++) for (let j = i + 1; j < ps.length; j++) {
        const kk = ps[i] + "|" + ps[j]; if (!pares.has(kk)) pares.set(kk, { a: ps[i], b: ps[j], porCap: new Array(n).fill(0), cenas: 0, primeiro: -1, ultimo: -1, fam: 0, rom: 0, riv: 0, ali: 0 });
        const p = pares.get(kk); p.cenas++; p.porCap[ci]++; if (p.primeiro < 0) p.primeiro = ci; p.ultimo = ci;
        if (pal) { p.fam += pal[0]; p.rom += pal[1]; p.riv += pal[2]; p.ali += pal[3]; }
      }
    });
  });
  const capT = (i) => `cap. ${i + 1}`; const nome = (id) => porId.get(id)?.nome || "?";
  const alerta = (nivel, titulo, detalhe, cap, ficha, frase, chave) => { const k = obraId + "|" + chave; if (!ignorados.has(k)) alertas.push({ nivel, titulo, detalhe, cap, ficha, frase, chave: k }); };
  const porFicha = new Map(); for (const f of fatos) { if (!porFicha.has(f.id)) porFicha.set(f.id, []); porFicha.get(f.id).push(f); }
  for (const [id, fsx] of porFicha) {
    const f = porId.get(id); if (!f) continue;
    const idades = fsx.filter((x) => x.tipo === "IDADE"); const daFicha = parseInt((f.idade || "").match(/\d{1,3}/)?.[0] ?? "", 10);
    if (!isNaN(daFicha)) { const x = idades.find((i) => parseInt(i.valor, 10) !== daFicha); if (x) alerta("ATENCAO", `Idade de ${f.nome} diferente da ficha`, `A ficha diz ${daFicha}; o ${capT(x.cap)} diz ${x.valor}. Se o tempo passou, atualize a ficha.`, x.cap, id, x.frase, `idade|${id}|${x.valor}`); }
    for (let i = 1; i < idades.length; i++) if (parseInt(idades[i].valor, 10) < parseInt(idades[i - 1].valor, 10)) { const [a, b] = [idades[i - 1], idades[i]]; alerta("FURO", `${f.nome} ficou mais novo`, `${a.valor} no ${capT(a.cap)}, ${b.valor} no ${capT(b.cap)}.`, b.cap, id, b.frase, `idade-|${id}|${b.valor}`); break; }
    for (const tipo of ["OLHOS", "CABELO"]) {
      const cores = fsx.filter((x) => x.tipo === tipo && !x.valor.endsWith("(mudou)"));
      const rf = new RegExp((tipo === "OLHOS" ? RX_OLHOS : RX_CABELO).source, "iu").exec(`${f.aparencia || ""} ${f.descricao || ""}`); const corF = rf ? CORES[rf[1].toLowerCase()] : null;
      const cont = {}; for (const c of cores) cont[c.valor] = (cont[c.valor] || 0) + 1;
      const base = corF || Object.entries(cont).sort((a, b) => b[1] - a[1])[0]?.[0]; if (!base) continue;
      const x = cores.find((c) => c.valor !== base);
      if (x) alerta("ATENCAO", `${f.nome}: ${tipo === "OLHOS" ? "olhos" : "cabelo"} ${x.valor} ou ${base}?`, `${corF ? "A ficha diz " + base : "Na maior parte do texto é " + base}; no ${capT(x.cap)} aparece ${x.valor}. Se mudou de propósito (poder, disfarce), ignore.`, x.cap, id, x.frase, `cor|${id}|${tipo}|${x.valor}`);
    }
    const m = fsx.find((x) => x.tipo === "MORTE");
    if (m) { const dep = [...(falasCap.get(id) || [])].filter((c) => c > m.cap).sort((a, b) => a - b)[0]; if (dep != null) alerta("FURO", `${f.nome} fala depois de morrer`, `Morreu no ${capT(m.cap)} e fala de novo no ${capT(dep)}. Se é lembrança, flashback ou ele voltou, ignore.`, dep, id, m.frase, `morte|${id}|${dep}`); }
    for (const fer of fsx.filter((x) => x.tipo === "FERIMENTO" && !x.valor.startsWith("perdeu"))) {
      let achou = false;
      for (let ci = fer.cap; ci <= Math.min(n - 1, fer.cap + 1) && !achou; ci++) {
        const t = limparMarcas(caps[ci].texto); const ini = ci === fer.cap ? Math.max(0, t.indexOf(fer.frase.slice(0, 40))) + fer.frase.length : 0;
        const resto = t.slice(ini); const cura = resto.search(RX_CURA); let pos = 0;
        for (const fr of resto.split(FRASES)) { if (cura >= 0 && pos >= cura) break; if (RX_LUTA.test(fr) && achar(fr).some((x) => x.id === id)) { alerta("DICA", `${f.nome} luta com ${fer.valor}?`, `Machucou no ${capT(fer.cap)} e luta no ${capT(ci)} sem cura ou passagem de tempo no meio.`, ci, id, fr.trim().slice(0, 260), `ferido|${id}|${fer.cap}|${ci}`); achou = true; break; } pos += fr.length + 1; }
      }
    }
    const pod = fsx.filter((x) => x.tipo === "PODER").map((x) => { const v = new RegExp(RX_PODER.source, "u").exec(x.valor)?.[1]; return v ? [x, ordemRank(v)] : null; }).filter((x) => x && x[1] != null);
    for (let i = 1; i < pod.length; i++) if (pod[i][1] < pod[i - 1][1]) { const [a, b] = [pod[i - 1][0], pod[i][0]]; alerta("ATENCAO", `Nível de ${f.nome} caiu`, `“${a.valor}” no ${capT(a.cap)}, “${b.valor}” no ${capT(b.cap)}. Se ele perdeu poder de propósito, ignore.`, b.cap, id, b.frase, `poder|${id}|${b.valor}`); break; }
  }
  if (n >= 8) for (const [id, pc] of contagem) {
    if (!pessoas.has(id) || (porFicha.get(id) || []).some((x) => x.tipo === "MORTE")) continue;
    const total = pc.reduce((a, b) => a + b, 0), ult = pc.map((v, i) => v > 0 ? i : -1).reduce((a, b) => Math.max(a, b), -1), cc = pc.filter((v) => v > 0).length;
    if (total >= 15 && cc >= 3 && n - 1 - ult >= 5) alerta("DICA", `${nome(id)} sumiu`, `Apareceu em ${cc} capítulos e não aparece há ${n - 1 - ult} (desde o ${capT(ult)}). O leitor pode perguntar dele.`, ult, id, "", `sumiu|${id}|${ult}`);
  }
  for (const [tok, q] of tokens) {
    if (q > 4) continue;
    const par = [...nomes.keys()].find((k) => k.length >= 4 && !k.includes(" ") && distancia(k, tok) === 1 && (contagem.get(nomes.get(k)) || []).reduce((a, b) => a + b, 0) >= q * 5); if (!par) continue;
    const ci = Math.max(0, caps.findIndex((c) => c.texto.includes(tok)));
    const frase = (limparMarcas(caps[ci].texto).split(FRASES).find((x) => x.includes(tok)) || "").trim().slice(0, 260);
    alerta("ATENCAO", `“${tok}” ou “${par}”?`, `“${tok}” aparece ${q} vez(es) e é quase igual a “${par}”. Pode ser erro de digitação.`, ci, nomes.get(par), frase, `grafia|${tok}`);
  }
  const retratos = fs.map((f) => { const pc = contagem.get(f.id) || new Array(n).fill(0); return { ficha: f, porCap: pc, total: pc.reduce((a, b) => a + b, 0), falas: falas.get(f.id) || 0, fatos: porFicha.get(f.id) || [], lugares: [...(lugares.get(f.id) || new Map())].sort((a, b) => b[1] - a[1]).slice(0, 4) }; }).sort((a, b) => b.total - a.total);
  const relacoes = [...pares.values()].filter((p) => p.cenas >= 2).map((p) => { const t = [["família", p.fam * 1.4], ["romance", p.rom * 1.6], ["rivais", p.riv * 0.8], ["aliados", p.ali]].sort((a, b) => b[1] - a[1])[0]; return { ...p, tipo: t[1] >= 1.2 * Math.min(p.cenas, 5) ? t[0] : "convivem" }; }).sort((a, b) => b.cenas - a.cenas);
  const ordem = { FURO: 0, ATENCAO: 1, DICA: 2 };
  return { caps, retratos, relacoes, alertas: alertas.sort((a, b) => ordem[a.nivel] - ordem[b.nivel] || a.cap - b.cap) };
}

const COR_NIVEL = { FURO: "#FF4D4D", ATENCAO: "#FF9A3C", DICA: "#4DA3FF" };
const NOME_NIVEL = { FURO: "FURO", ATENCAO: "ATENÇÃO", DICA: "DICA" };
const COR_REL = { "família": "#6FCF97", romance: "#FF6FA8", rivais: "#FF4D4D", aliados: "#4DA3FF", convivem: "#9A9A9A" };
const barras = (pc) => { const mx = Math.max(1, ...pc); return `<div class="barras">${pc.map((v) => `<i style="height:${v ? Math.max(8, v / mx * 100) : 6}%;${v ? "" : "background:var(--borda)"}"></i>`).join("")}</div>`; };

rota("/biblia/(\\w+)(?:/(\\w+))?", async (obraId, aba = "alertas") => {
  const o = E.obras.get(obraId); if (!o) { ir("/"); return; }
  carregando("escrever");
  const [caps, fs] = await Promise.all([capitulos(obraId), fichas(obraId)]);
  const textos = await Promise.all(caps.map(async (c) => ({ id: c.id, titulo: c.titulo, texto: await lerTexto(colTextos(), c.id, c.partes) })));
  const ignorados = new Set(local.ler("bibliaIgnorados", []));
  const r = analisar(textos, fs, ignorados, obraId);
  const ficha = (id) => fs.find((f) => f.id === id);
  const abas = [["alertas", `Alertas · ${r.alertas.length}`], ["pessoas", "Personagens"], ["mapa", "Mapa"], ["relacoes", `Relações · ${r.relacoes.length}`]];
  let corpo = "";
  if (aba === "alertas") corpo = r.alertas.length ? r.alertas.map((a, i) => `<div class="card alerta" style="--c:${COR_NIVEL[a.nivel]}"><div><span class="nivel">${NOME_NIVEL[a.nivel]}</span> <span class="suave peq">cap. ${a.cap + 1} · ${esc(r.caps[a.cap]?.titulo || "")}</span></div>
      <h3>${esc(a.titulo)}</h3><p class="suave">${esc(a.detalhe)}</p>${a.frase ? `<blockquote>“${esc(a.frase)}”</blockquote>` : ""}
      <div class="linha-botoes" style="justify-content:space-between"><a class="btn2 peq" href="#/editar/${obraId}/${r.caps[a.cap]?.id}">Abrir capítulo</a><button class="btn2 peq" data-ign="${i}">Está certo, ignorar</button></div></div>`).join("")
    : `<div class="card vazio"><b>✓ Nenhum furo encontrado</b><span class="suave">Idades, olhos, cabelo, mortes, ferimentos, níveis e nomes estão batendo.</span></div>`;
  else if (aba === "pessoas") corpo = r.retratos.filter((x) => x.ficha.tipo !== "LUGAR").map((x) => {
    const pc = x.porCap.filter((v) => v > 0).length, pri = x.porCap.findIndex((v) => v > 0), ult = x.porCap.length - 1 - [...x.porCap].reverse().findIndex((v) => v > 0);
    const parc = r.relacoes.filter((rl) => rl.a === x.ficha.id || rl.b === x.ficha.id).slice(0, 4);
    const fatos = []; const vistos = new Set(); for (const f of x.fatos) { const k = f.tipo + f.valor; if (!vistos.has(k)) { vistos.add(k); fatos.push(f); } }
    return `<details class="card retrato"><summary><b>${esc(x.ficha.nome)}</b> ${x.fatos.some((f) => f.tipo === "MORTE") ? "✝" : ""}<span class="suave peq">${x.total ? `${pc} de ${r.caps.length} caps · ${x.falas} ${x.falas === 1 ? "fala" : "falas"} · do cap. ${pri + 1} ao ${ult + 1}` : "Ainda não aparece no texto"}</span>${barras(x.porCap)}</summary>
      ${parc.length ? `<div class="rotulo">Mais junto de</div>${parc.map((rl) => `<div>• ${esc(ficha(rl.a === x.ficha.id ? rl.b : rl.a)?.nome || "?")} — ${rl.tipo}, ${rl.cenas} cenas</div>`).join("")}` : ""}
      ${x.lugares.length ? `<div class="rotulo">Onde esteve</div><div>${x.lugares.map(([id, q]) => `${esc(ficha(id)?.nome || "?")} (${q})`).join(" · ")}</div>` : ""}
      ${fatos.length ? `<div class="rotulo">O que o texto diz</div>${fatos.map((f) => `<div class="fato"><b>${TIPO_NOME[f.tipo]}:</b> ${esc(f.valor)} <span class="suave peq">cap. ${f.cap + 1}</span><div class="suave peq"><i>${esc(f.frase)}</i></div>${["IDADE", "OLHOS", "CABELO", "MORTE", "PODER", "JURAMENTO"].includes(f.tipo) ? `<button class="btn2 peq" data-por="${x.ficha.id}|${x.fatos.indexOf(f)}">+ Pôr na ficha</button>` : ""}</div>`).join("")}` : ""}</details>`;
  }).join("");
  else if (aba === "relacoes") corpo = (r.relacoes.length ? `<div class="legenda">${Object.entries(COR_REL).map(([k, c]) => `<span><i style="background:${c}"></i>${k}</span>`).join("")}</div>` : `<p class="suave">Ainda não há personagens dividindo cenas (duas ou mais).</p>`) +
    r.relacoes.slice(0, 80).map((rl) => `<div class="card relacao"><div style="display:flex;justify-content:space-between;gap:8px"><b>${esc(ficha(rl.a)?.nome)} ⇄ ${esc(ficha(rl.b)?.nome)}</b><span class="tag" style="--c:${COR_REL[rl.tipo]}">${rl.tipo}</span></div>
      <div class="trilha"><i style="width:${rl.cenas / r.relacoes[0].cenas * 100}%;background:${COR_REL[rl.tipo]}"></i></div><span class="suave peq">${rl.cenas} cenas juntos · cap. ${rl.primeiro + 1}${rl.ultimo !== rl.primeiro ? " a " + (rl.ultimo + 1) : ""}</span></div>`).join("");
  else corpo = `<div class="mapa card"><svg id="mapa"></svg><div class="dica-mapa suave peq">arraste · roda do mouse para zoom · clique num nome</div></div>
    <div class="mapa-tempo"><b id="mt"></b><input type="range" id="ate" min="0" max="${Math.max(0, r.caps.length - 1)}" value="${Math.max(0, r.caps.length - 1)}"></div><div id="mapa-info"></div>`;
  casca("escrever", `<div class="topo"><a class="voltar vidro" href="#/obra/${obraId}">←</a><h1>Bíblia viva</h1><span class="suave">${esc(o.titulo)}</span></div>
    <p class="suave" style="margin-top:-6px">Lida agora dos ${r.caps.length} capítulos. São pistas, não regras: você decide.</p>
    <div class="chips" style="margin:6px 0 14px">${abas.map(([k, t]) => `<a class="chip ${k === aba ? "on" : ""}" href="#/biblia/${obraId}/${k}">${t}</a>`).join("")}</div>
    <div class="biblia">${corpo}</div>`);
  $$("[data-ign]").forEach((b) => (b.onclick = () => { ignorados.add(r.alertas[+b.dataset.ign].chave); local.gravar("bibliaIgnorados", [...ignorados].slice(-500)); window.redesenhar(); }));
  $$("[data-por]").forEach((b) => (b.onclick = async () => {
    const [fid, i] = b.dataset.por.split("|"); const f = ficha(fid); const fato = r.retratos.find((x) => x.ficha.id === fid).fatos[+i];
    const junta = (a, nv) => (a || "").toLowerCase().includes(nv.toLowerCase()) ? a : [(a || "").trim().replace(/\.$/, ""), nv].filter(Boolean).join(". ");
    const v = fato.valor.replace(" (mudou)", ""); const nova = { ...f };
    if (fato.tipo === "IDADE") nova.idade = v.split(" ")[0]; else if (fato.tipo === "OLHOS") nova.aparencia = junta(f.aparencia, "olhos " + v); else if (fato.tipo === "CABELO") nova.aparencia = junta(f.aparencia, "cabelo " + v);
    else if (fato.tipo === "PODER") nova.poder = junta(f.poder, `${v} (cap. ${fato.cap + 1})`); else if (fato.tipo === "MORTE") nova.descricao = junta(f.descricao, `Morreu no cap. ${fato.cap + 1}`); else nova.descricao = junta(f.descricao, `Jurou (cap. ${fato.cap + 1}): “${fato.frase.slice(0, 140)}”`);
    await salvarFicha(obraId, fid, nova); toast("Ficha atualizada"); b.remove();
  }));
  if (aba === "mapa") desenharMapa(r);
});

/** Mapa de relações em SVG: força de mola (quem divide cena fica perto), pan/zoom e a barra volta no tempo. */
function desenharMapa(r) {
  const pessoas = r.retratos.filter((x) => x.ficha.tipo !== "LUGAR" && x.ficha.tipo !== "OUTRO" && x.total > 0).slice(0, 40);
  const ids = pessoas.map((p) => p.ficha.id); const set = new Set(ids);
  const rels = r.relacoes.filter((rl) => set.has(rl.a) && set.has(rl.b));
  const svg = $("#mapa"); if (pessoas.length < 2) { svg.parentElement.innerHTML = `<p class="suave" style="padding:20px">O mapa aparece quando dois ou mais personagens dividem cenas.</p>`; return; }
  const nn = ids.length; const idx = new Map(ids.map((id, i) => [id, i]));
  const x = ids.map((_, i) => 0.5 + 0.35 * Math.cos(i * 2 * Math.PI / nn)), y = ids.map((_, i) => 0.5 + 0.35 * Math.sin(i * 2 * Math.PI / nn));
  const k = Math.sqrt(0.6 / nn); const ar = rels.map((rl) => [idx.get(rl.a), idx.get(rl.b), 1 + Math.log(1 + rl.cenas)]);
  for (let v = 0; v < 350; v++) {
    const dx = new Array(nn).fill(0), dy = new Array(nn).fill(0);
    for (let i = 0; i < nn; i++) for (let j = i + 1; j < nn; j++) { const ex = x[i] - x[j], ey = y[i] - y[j], d = Math.max(0.01, Math.hypot(ex, ey)), f = k * k / d; dx[i] += ex / d * f; dy[i] += ey / d * f; dx[j] -= ex / d * f; dy[j] -= ey / d * f; }
    for (const [a, b, w] of ar) { const ex = x[a] - x[b], ey = y[a] - y[b], d = Math.max(0.01, Math.hypot(ex, ey)), f = d * d / k * w * 0.5; dx[a] -= ex / d * f; dy[a] -= ey / d * f; dx[b] += ex / d * f; dy[b] += ey / d * f; }
    for (let i = 0; i < nn; i++) { dx[i] += (0.5 - x[i]) * 0.02; dy[i] += (0.5 - y[i]) * 0.02; }
    const t = 0.08 * (1 - v / 350) + 0.002;
    for (let i = 0; i < nn; i++) { const d = Math.max(1e-9, Math.hypot(dx[i], dy[i])); x[i] = Math.min(0.94, Math.max(0.06, x[i] + dx[i] / d * Math.min(d, t))); y[i] = Math.min(0.94, Math.max(0.06, y[i] + dy[i] / d * Math.min(d, t))); }
  }
  const mortes = new Map(r.retratos.map((rt) => [rt.ficha.id, rt.fatos.find((f) => f.tipo === "MORTE")?.cap ?? 1e9]));
  let sel = null, esc_ = 1, ox = 0, oy = 0;
  const W = 1000, H = 640; svg.setAttribute("viewBox", `0 0 ${W} ${H}`);
  const desenhar = () => {
    const ate = +$("#ate").value; $("#mt").textContent = `Até o cap. ${ate + 1}: ${r.caps[ate]?.titulo || ""}`;
    const soma = (a) => a.slice(0, ate + 1).reduce((s, v) => s + v, 0);
    const men = new Map(pessoas.map((p) => [p.ficha.id, soma(p.porCap)])); const fo = new Map(rels.map((rl) => [rl, soma(rl.porCap)]));
    const mxM = Math.max(1, ...men.values()), mxF = Math.max(1, ...fo.values());
    const P = (id) => [W / 2 + (x[idx.get(id)] - 0.5) * W * 0.9 * esc_ + ox, H / 2 + (y[idx.get(id)] - 0.5) * H * 0.9 * esc_ + oy];
    let h = "";
    for (const rl of rels) { const f = fo.get(rl); if (!f) continue; const [x1, y1] = P(rl.a), [x2, y2] = P(rl.b); const foco = !sel || rl.a === sel || rl.b === sel;
      h += `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${COR_REL[rl.tipo]}" stroke-opacity="${foco ? 0.85 : 0.12}" stroke-width="${1.5 + 6 * f / mxF}" stroke-linecap="round"/>`; }
    for (const p of pessoas) { const id = p.ficha.id, m = men.get(id); if (!m) continue; const [cx, cy] = P(id), raio = (9 + 15 * Math.sqrt(m / mxM)) * Math.min(1.6, Math.max(0.8, esc_)); const morto = mortes.get(id) <= ate;
      const foco = !sel || sel === id || rels.some((rl) => ((rl.a === sel && rl.b === id) || (rl.b === sel && rl.a === id)) && fo.get(rl)); const partes = p.ficha.nome.split(" ");
      const nome = (morto ? "✝ " : "") + (partes.length > 1 && partes.at(-1).length > 2 ? partes.slice(1).join(" ") : partes[0]);
      h += `<g data-id="${id}" style="cursor:pointer;opacity:${foco ? 1 : 0.28}"><circle cx="${cx}" cy="${cy}" r="${raio}" fill="${morto ? "#6B6B6B" : "var(--acento, var(--amb))"}" ${sel === id ? 'stroke="#fff" stroke-width="3"' : ""}/><text x="${cx}" y="${cy + raio + 15}" text-anchor="middle" fill="var(--tx)" font-size="13" font-weight="700">${esc(nome)}</text></g>`; }
    svg.innerHTML = h;
    $$("g[data-id]", svg).forEach((g) => (g.onclick = (e) => { e.stopPropagation(); sel = sel === g.dataset.id ? null : g.dataset.id; desenhar(); info(men, fo); }));
  };
  const info = (men, fo) => {
    const el = $("#mapa-info"); if (!sel) { el.innerHTML = ""; return; } const p = pessoas.find((q) => q.ficha.id === sel);
    el.innerHTML = `<div class="card"><b>${esc(p.ficha.nome)}</b><div class="suave peq">${men.get(sel)} menções até aqui · ${p.falas} falas na obra</div>${rels.filter((rl) => (rl.a === sel || rl.b === sel) && fo.get(rl)).sort((a, b) => fo.get(b) - fo.get(a)).slice(0, 5).map((rl) => `<div><i class="ponto" style="background:${COR_REL[rl.tipo]}"></i> ${esc(r.retratos.find((q) => q.ficha.id === (rl.a === sel ? rl.b : rl.a))?.ficha.nome || "?")} — ${rl.tipo}, ${fo.get(rl)} cenas</div>`).join("")}</div>`;
  };
  $("#ate").oninput = () => { desenhar(); };
  svg.onclick = () => { sel = null; desenhar(); $("#mapa-info").innerHTML = ""; };
  svg.onwheel = (e) => { e.preventDefault(); esc_ = Math.min(4, Math.max(0.5, esc_ * (e.deltaY < 0 ? 1.12 : 0.89))); desenhar(); };
  let arr = null; svg.onpointerdown = (e) => { arr = [e.clientX, e.clientY, ox, oy]; }; window.onpointerup = () => (arr = null);
  svg.onpointermove = (e) => { if (!arr) return; const s = W / svg.clientWidth; ox = arr[2] + (e.clientX - arr[0]) * s; oy = arr[3] + (e.clientY - arr[1]) * s; desenhar(); };
  desenhar();
}

// ───────── LEITORES (estatísticas do autor) ─────────
rota("/leitores/(\\w+)", async (obraId) => {
  const o = E.obras.get(obraId); if (!o || !o.minha) { ir("/"); return; }
  carregando("escrever");
  const pid = obraId; // a obra do autor é publicada com o mesmo id
  let leituras = [], soc = new Map(), erro = "";
  try { leituras = await leiturasObra(pid); } catch (e) { erro = /permission/i.test(e.message) ? "O Firebase ainda não deixa ler as estatísticas. Atualize as regras do Firestore." : "Não deu para carregar: " + e.message; }
  try { for (const c of await capsPub(pid, true)) soc.set(c.id, [c.curtidas || 0, c.comentarios || 0]); } catch (_) {}
  const caps = (await capitulos(obraId)).filter((c) => c.publicadoEm && c.publicadoEm <= Date.now());
  const pars = await Promise.all(caps.map(async (c) => limparMarcas(await lerTexto(colTextos(), c.id, c.partes)).split("\n").map((l) => l.trim()).filter(Boolean)));
  const porCap = new Map(); for (const l of leituras) { if (!porCap.has(l.cap)) porCap.set(l.cap, []); porCap.get(l.cap).push(l); }
  const nums = caps.map((c, i) => {
    const ls = porCap.get(c.id) || []; const term = (l) => l.bloco >= Math.floor(l.total * 0.95) - 1; const par = ls.filter((l) => !term(l));
    const faixas = new Array(10).fill(0); for (const l of par) faixas[Math.min(9, Math.max(0, Math.floor((l.bloco + 0.5) / Math.max(1, l.total) * 10)))]++;
    const cnt = {}; for (const l of par) cnt[l.bloco] = (cnt[l.bloco] || 0) + 1; const moda = Object.entries(cnt).filter(([, v]) => v >= 2).sort((a, b) => b[1] - a[1])[0]?.[0];
    return { titulo: c.titulo, leitores: ls.length, terminaram: ls.filter(term).length, min: ls.length ? ls.reduce((s, l) => s + (l.segundos || 0), 0) / ls.length / 60 : 0, faixas, trecho: moda != null ? (pars[i][+moda] || "").slice(0, 180) : "", soc: soc.get(c.id) || [0, 0] };
  });
  const pessoas = new Map(); for (const l of leituras) { if (!pessoas.has(l.uid)) pessoas.set(l.uid, []); pessoas.get(l.uid).push(l); }
  const ativos = [...pessoas.values()].filter((ls) => ls.some((l) => Date.now() - l.em < 7 * 864e5)).length;
  const conc = nums.filter((n) => n.leitores).map((n) => n.terminaram / n.leitores); const mediaC = conc.length ? conc.reduce((a, b) => a + b, 0) / conc.length : 0;
  const pc = [...pessoas.values()].filter((ls) => ls.some((l) => l.plataforma === "pc")).length; const an = [...pessoas.values()].filter((ls) => ls.some((l) => l.plataforma === "android")).length;
  const com = nums.map((n, i) => ({ n, i })).filter((x) => x.n.leitores >= 3); let queda = null; for (let i = 1; i < com.length; i++) { const d = com[i - 1].n.leitores - com[i].n.leitores; if (d > 0 && (!queda || d > queda.d)) queda = { a: com[i - 1], b: com[i], d }; }
  const segura = com.sort((a, b) => b.n.terminaram / b.n.leitores - a.n.terminaram / a.n.leitores)[0];
  const primeiro = Math.max(1, nums[0]?.leitores || 1), maior = Math.max(1, ...nums.map((n) => n.leitores));
  casca("escrever", `<div class="topo"><a class="voltar vidro" href="#/obra/${obraId}">←</a><h1>Leitores</h1><span class="suave">${esc(o.titulo)}</span></div>
    ${erro ? `<div class="card">${esc(erro)}</div>` : ""}
    <div class="numeros">${[[pessoas.size, "leitores"], [ativos, "nos últimos 7 dias"], [Math.round(mediaC * 100) + "%", "terminam o capítulo"], [`${an} · ${pc}`, "celular · PC"]].map(([v, t]) => `<div class="card"><b>${v}</b><span class="suave peq">${t}</span></div>`).join("")}</div>
    ${!pessoas.size && !erro ? `<p class="suave">Ainda ninguém leu pela versão nova. Os números aparecem conforme as pessoas leem (só contam leitores, não você).</p>` : ""}
    ${queda ? `<div class="card dica-l">📉 <div><b>Maior perda: do cap. ${queda.a.i + 1} para o ${queda.b.i + 1}</b><div class="suave peq">${queda.d} pessoas não seguiram. Vale olhar o gancho do fim do cap. ${queda.a.i + 1}.</div></div></div>` : ""}
    ${segura ? `<div class="card dica-l">🔥 <div><b>O que mais prende: cap. ${segura.i + 1}</b><div class="suave peq">${Math.round(segura.n.terminaram / segura.n.leitores * 100)}% de quem começa termina “${esc(segura.n.titulo)}”.</div></div></div>` : ""}
    <div class="rotulo" style="margin-top:14px">Capítulo a capítulo</div>
    ${nums.map((n, i) => { const topo = Math.max(1, ...n.faixas); return `<details class="card cap-l"><summary><span class="num">${i + 1}</span><b>${esc(n.titulo)}</b><span><b>${n.leitores}</b> <span class="suave peq">· ${Math.round(n.leitores * 100 / primeiro)}%</span></span>
      <div class="trilha"><i style="width:${n.leitores / maior * 100}%"></i></div><span class="suave peq">${n.leitores ? Math.round(n.terminaram / n.leitores * 100) : 0}% terminam · ${n.min.toFixed(1)} min em média · ♥ ${n.soc[0]} · 💬 ${n.soc[1]}</span></summary>
      <div class="rotulo">Onde quem largou parou (início → fim)</div><div class="calor">${n.faixas.map((v) => `<i style="opacity:${0.08 + 0.92 * v / topo}">${v || ""}</i>`).join("")}</div>
      ${n.trecho ? `<div class="suave peq">Mais gente para neste parágrafo:</div><blockquote>“${esc(n.trecho)}”</blockquote>` : n.leitores && n.leitores === n.terminaram ? `<div class="suave peq">Todo mundo que começou terminou. 👏</div>` : ""}</details>`; }).join("")}`);
});
