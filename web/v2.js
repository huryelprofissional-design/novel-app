// Novely V2 no PC: visual editorial (Ler, Descobrir, Escrever, Eu) — o mesmo conceito do celular.
import {
  E, $, $$, esc, raiz, toast, milhar, corHex, local, lista, capaHTML, preencherCapas, casca, carregando, rota, ir, catalogo, urlBlob, imagemPub, imagemMinha,
  nomeAutor, novaObra, perguntar, dialogo, salvarPerfil, escolherArquivo, comprimirArquivo, enviarImagemMinha, perfilPub, segueAutor, seguirAutor, seguindo, seguir,
  capitulos, capsPub, lerTexto, collection, db, CONQUISTAS, carteira, GENEROS,
} from "./nucleo.js";
import { infoNivel, tituloNivel, missoes, bauPronto, celebrar } from "./efeitos.js";

// ───────── pecinhas ─────────
const ROM = [[1000, "M"], [900, "CM"], [500, "D"], [400, "CD"], [100, "C"], [90, "XC"], [50, "L"], [40, "XL"], [10, "X"], [9, "IX"], [5, "V"], [4, "IV"], [1, "I"]];
export const romano = (n) => { let r = ""; for (const [v, s] of ROM) while (n >= v) { r += s; n -= v; } return r || "0"; };
export const mil = (n) => (n >= 1e6 ? (n / 1e6).toFixed(1).replace(".0", "").replace(".", ",") + " mi" : n >= 1e4 ? Math.floor(n / 1e3) + " mil" : n >= 1e3 ? (n / 1e3).toFixed(1).replace(".0", "").replace(".", ",") + " mil" : String(n || 0));
const dois = (n) => String(Math.max(1, Math.floor(n || 1))).padStart(2, "0");
const hojeId = (d = new Date()) => `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
const mesNome = () => new Date().toLocaleDateString("pt-BR", { month: "long" });
/** Dias seguidos escrevendo (conta hoje ou, se ainda não escreveu hoje, a partir de ontem). */
export function diasSeguidos() {
  const m = local.ler("escritaDias", {}); const d = new Date(); let n = 0;
  if (!(m[hojeId(d)] > 0)) d.setDate(d.getDate() - 1);
  while (m[hojeId(d)] > 0) { n++; d.setDate(d.getDate() - 1); }
  return n;
}
const palavrasHoje = () => { const h = local.ler("palavrasHoje", { d: "", n: 0 }); return h.d === new Date().toISOString().slice(0, 10) ? h.n : 0; };
export const nivelAtual = () => infoNivel(local.ler("xp", 0));
const capaFaixa = (o, cls = "") => `<div class="ed-capa ${cls}">${capaHTML(o, o.id, corHex(o.corBase))}</div>`;

// ───────── LER ─────────
rota("/", async () => {
  const l = lista();
  const cont = l.filter((o) => o.lidaEm > 0 && o.ultimoCap).sort((a, b) => b.lidaEm - a.lidaEm)[0];
  const outros = l.filter((o) => !o.minha);
  const grupos = { LENDO: outros.filter((o) => o.status === "LENDO").sort((a, b) => b.lidaEm - a.lidaEm), QUERO: outros.filter((o) => o.status === "QUERO"), LIDOS: outros.filter((o) => o.status === "CONCLUIDA"), ARQUIVOS: outros.filter((o) => !o.trancada) };
  const aba = local.ler("v2Estante", "LENDO");
  const h = new Date().getHours(); const saud = h >= 5 && h < 12 ? "Bom dia" : h >= 12 && h < 18 ? "Boa tarde" : "Boa noite";
  const data = new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  let cap = null;
  if (cont) { try { cap = (await capitulos(cont.id)).find((c) => c.id === cont.ultimoCap) || null; } catch (_) {} }
  const autorCont = cont ? (cont.minha ? nomeAutor() : cont.autor || "") : "";
  const falta = cap ? Math.max(1, Math.round((1 - (cap.progresso || 0)) * (cap.palavras || 0) / 230)) : 0;
  casca("inicio", `<div class="ed-cab"><div><div class="ed-mono">${esc(data)}</div><h1 class="ed-h1">${saud}${E.perfil.nome ? `, <i>${esc(E.perfil.nome.split(" ")[0])}</i>` : ""}</h1></div>
      <a class="ed-autores" href="#/perfil"><span class="ed-mono">Eu</span><span class="ed-av" id="avTopo">${esc(nomeAutor().slice(0, 1).toUpperCase())}</span></a></div>
    ${cont ? `<section class="ed-hero" id="hero">
        <div class="ed-hero-luz"></div>
        ${capaFaixa(cont, "grande")}
        <div class="ed-hero-tx"><div class="ed-mono">Continuar · capítulo ${dois(cap?.ordem)}</div>
          <h2 class="ed-titulo">${esc(cont.titulo)}</h2>
          ${cap ? `<div class="ed-sub"><i>${esc(cap.titulo || "Capítulo")} — falta ${falta} ${falta === 1 ? "minuto" : "minutos"}</i></div>
          <div class="ed-prog"><i style="width:${Math.round((cap.progresso || 0) * 100)}%"></i></div><span class="ed-pct">${Math.round((cap.progresso || 0) * 100)}%</span>` : ""}
          <div class="ed-botoes"><a class="ed-btn" href="#/ler/${cont.id}/${cont.ultimoCap}">Continuar leitura <span>→</span></a><a class="ed-btn2" href="#/obra/${cont.id}">Ver capítulos</a></div></div>
        <div class="ed-vertical ed-mono">${esc(cont.titulo)} · ${esc(autorCont)} · cap. ${dois(cap?.ordem)}</div></section>`
    : `<section class="ed-hero vazio"><div class="ed-hero-tx"><div class="ed-mono">Comece por aqui</div><h2 class="ed-titulo">Nada sendo lido</h2>
        <div class="ed-sub"><i>Ache uma história em Descobrir ou importe um livro.</i></div><div class="ed-botoes"><a class="ed-btn" href="#/descobrir">Descobrir <span>→</span></a><button class="ed-btn2" data-importar>Importar TXT ou Word</button></div></div></section>`}
    <div class="ed-duas">
      <section><div class="ed-sec"><h2 class="ed-h2">Estante</h2><div class="ed-abas">${[["LENDO", "Lendo"], ["QUERO", "Quero ler"], ["LIDOS", "Lidos"], ["ARQUIVOS", "Arquivos"]].map(([k, n]) => `<button class="${k === aba ? "on" : ""}" data-est="${k}">${n} ${grupos[k].length}</button>`).join("")}</div></div>
        <div class="ed-estante">${grupos[aba].slice(0, 12).map((o) => `<a href="#/obra/${o.id}" class="ed-livro">${capaFaixa(o)}${aba === "LENDO" ? `<span class="ed-mono laranja">${o.totalCaps ? Math.round((o.lidos || 0) * 100 / o.totalCaps) + "%" : esc((o.titulo || "").slice(0, 14))}</span>` : `<span class="ed-mono">${esc((o.titulo || "").slice(0, 16))}</span>`}</a>`).join("")}
          <button class="ed-importar" data-importar>+<span class="ed-mono">Importar<br>TXT ou Word</span></button></div>
        <a class="ed-link ed-mono" href="#/biblioteca/${aba}">Estante completa →</a></section>
      <section><div class="ed-sec"><h2 class="ed-h2">Saindo agora</h2><a class="ed-mono laranja" href="#/descobrir/NOVIDADES">Ver tudo →</a></div><div id="saindo"><div class="ed-mono">Carregando…</div></div></section>
    </div>`);
  $$("[data-est]").forEach((b) => (b.onclick = () => { local.gravar("v2Estante", b.dataset.est); window.redesenhar(); }));
  $$("[data-importar]").forEach((b) => (b.onclick = () => window.importarTXT()));
  preencherCapas(); avatarTopo();
  try {
    const novas = (await catalogo("NOVIDADES")).slice(0, 5);
    const el = $("#saindo"); if (!el) return;
    el.innerHTML = novas.length ? novas.map((o) => `<a class="ed-linha" href="#/pub/${o.id}"><span class="ed-mini">${o.capaMini ? `<img src="${urlBlob(o.capaMini)}">` : ""}</span><div><div class="ed-nome">${esc(o.titulo)}</div><div class="suave peq">por ${esc(o.autorNome)} · ${o.numCaps || 0} ${o.numCaps === 1 ? "capítulo" : "capítulos"}</div></div><span class="ed-mono laranja">Novo cap.</span></a>`).join("") : `<p class="suave">Nada saiu ainda.</p>`;
  } catch (_) { const el = $("#saindo"); if (el) el.innerHTML = `<p class="suave">Sem conexão com o catálogo.</p>`; }
});

async function avatarTopo() {
  const el = $("#avTopo"); if (!el || !E.perfil.fotoHash) return;
  const f = await imagemMinha("perfil"); if (f && el.isConnected) el.innerHTML = `<img src="${f}">`;
}

// ───────── DESCOBRIR ─────────
const MODOS = { LINHAS: "Primeiras linhas", ALTA: "Em alta", SEMANA: "Ranking da semana", NOVIDADES: "Novidades", CONCLUIDAS: "Concluídas" };
const frases = new Map();
async function primeiraFrase(o) {
  if (frases.has(o.id)) return frases.get(o.id);
  let r = "";
  try {
    const c = (await capsPub(o.id))[0];
    if (c) {
      const t = await lerTexto(collection(db, "publicas", o.id, "textos"), c.id, c.partes);
      const linha = t.split("\n").map((x) => x.replace(/\{[a-z+\-:]+:\s*|\}|\[#[^\]]*\]|[@&*_~]/gi, "").trim()).find((x) => x.length > 30 && !x.startsWith("#")) || "";
      const fs = linha.match(/[^.!?…]+[.!?…]+["”»]?/g) || [linha];
      r = fs[0].trim(); if (r.length < 50 && fs[1]) r += " " + fs[1].trim();
    }
  } catch (_) {}
  r = (r || o.sinopse || "").slice(0, 220);
  frases.set(o.id, r); return r;
}

rota("/descobrir(?:/(\\w+))?", async (modo = "LINHAS") => {
  if (!MODOS[modo]) modo = "LINHAS";
  casca("descobrir", `<div class="ed-cab"><h1 class="ed-h1 grande">Descobrir</h1><label class="ed-busca">${'<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>'}<input id="q" placeholder="Obras, autores, tags"></label><a class="ed-btn2 peq" href="#/generos">Gêneros</a></div>
    <nav class="ed-abas-l">${Object.entries(MODOS).map(([k, n]) => `<a href="#/descobrir/${k}" class="${k === modo ? "on" : ""}">${n}</a>`).join("")}</nav>
    <div class="ed-desc"><div id="principal"><div class="ed-mono" style="padding:40px 0">Carregando…</div></div>
      <aside><div class="ed-sec"><h2 class="ed-h2">Em alta</h2><span class="ed-mono">Esta semana</span></div><div id="alta"></div>
        <div class="ed-sec" style="margin-top:34px"><h2 class="ed-h2">Gêneros</h2></div><div class="ed-tags">${GENEROS.slice(0, 10).map((g) => `<a href="#/genero/${encodeURIComponent(g)}">${esc(g)}</a>`).join("")}</div></aside></div>`);
  $("#q").onkeydown = (e) => { if (e.key === "Enter") { local.gravar("buscaInicial", e.target.value); ir("/buscar"); } };
  let l = [], alta = [];
  try {
    alta = await catalogo("ALTA");
    l = modo === "LINHAS" ? [...new Map([...alta, ...(await catalogo("NOVIDADES").catch(() => []))].map((o) => [o.id, o])).values()] : await catalogo(modo);
  } catch (e) { const p = $("#principal"); if (p) p.innerHTML = `<p class="erro">Sem conexão com o catálogo.</p>`; return; }
  const elA = $("#alta"); if (elA) elA.innerHTML = alta.slice(0, 5).map((o, i) => linhaRank(o, i + 1)).join("") || `<p class="suave">Nada ainda.</p>`;
  const p = $("#principal"); if (!p) return;
  if (modo !== "LINHAS") { p.innerHTML = l.length ? l.map((o, i) => linhaRank(o, modo === "ALTA" || modo === "SEMANA" ? i + 1 : 0)).join("") : `<p class="suave">Nada por aqui ainda.</p>`; return; }
  if (!l.length) { p.innerHTML = `<p class="suave">Quando houver obras publicadas, as primeiras linhas aparecem aqui.</p>`; return; }
  // embaralha uma vez por dia
  let s = new Date().getDate(); l.sort(() => ((s = (s * 9301 + 49297) % 233280) / 233280) - .5);
  let i = 0;
  const mostrar = async () => {
    const o = l[i]; const meu = o.autorUid === E.usuario.uid;
    p.innerHTML = `<article class="ed-linhas"><div class="ed-aspas">“</div><div class="ed-frase" id="frase"><i>…</i></div>
      <div class="ed-regua"><span class="ed-mono">— Capítulo 1</span><i></i><span class="ed-mono">Pág. 1</span></div>
      <div class="ed-setas"><button id="ant" title="Anterior" ${i === 0 ? "disabled" : ""}>↑</button><button id="prox" title="Próxima" ${i >= l.length - 1 ? "disabled" : ""}>↓</button></div>
      <div class="ed-obra-l"><span class="ed-mini g">${o.capaMini ? `<img src="${urlBlob(o.capaMini)}">` : ""}</span>
        <div style="flex:1;min-width:0"><div class="ed-nome g">${esc(o.titulo)}</div><div class="suave">por <a href="#/autor/${esc(o.autorUid)}">${esc(o.autorNome)}</a> · ${o.numCaps || 0} capítulos</div>${meu ? "" : `<button class="ed-mono ed-seg" id="segA">${segueAutor(o.autorUid) ? "✓ Seguindo" : "+ Seguir autor"}</button>`}</div>
        <a class="ed-btn" href="#/pub/${o.id}">Ler capítulo 1 <span>→</span></a><button class="ed-quad" id="marca" title="Seguir a obra">${'<svg viewBox="0 0 24 24"><path d="M6 3h12v18l-6-4-6 4z"/></svg>'}</button></div></article>`;
    $("#ant").onclick = () => { i--; mostrar(); }; $("#prox").onclick = () => { i++; mostrar(); };
    const sa = $("#segA"); if (sa) sa.onclick = async () => { const v = !segueAutor(o.autorUid); sa.textContent = v ? "✓ Seguindo" : "+ Seguir autor"; try { await seguirAutor(o.autorUid, v); } catch (_) {} };
    const mk = $("#marca"); seguindo(o.id).then((v) => mk.classList.toggle("on", v));
    mk.onclick = async () => { const v = !mk.classList.contains("on"); mk.classList.toggle("on", v); try { await seguir(o.id, v); toast(v ? "Seguindo a obra" : "Deixou de seguir"); } catch (_) { mk.classList.toggle("on", !v); } };
    const f = await primeiraFrase(o); const el = $("#frase"); if (el) el.innerHTML = `<i>${esc(f || "…")}</i>`;
    if (l[i + 1]) primeiraFrase(l[i + 1]);
  };
  window.onkeydown = (e) => { if (e.target.closest("input,textarea")) return; if (e.key === "ArrowDown" && i < l.length - 1) { i++; mostrar(); } if (e.key === "ArrowUp" && i > 0) { i--; mostrar(); } };
  mostrar();
});
function linhaRank(o, pos) {
  return `<a class="ed-linha" href="#/pub/${o.id}">${pos ? `<b class="ed-pos ${pos === 1 ? "laranja" : ""}">${pos}</b>` : ""}<span class="ed-mini">${o.capaMini ? `<img src="${urlBlob(o.capaMini)}">` : ""}</span>
    <div style="flex:1;min-width:0"><div class="ed-nome">${esc(o.titulo)}</div><div class="suave peq">${esc(o.autorNome)} · ${o.numCaps || 0} caps</div></div>${o.classificacao === "18" ? `<span class="ed-18">+18</span>` : ""}</a>`;
}

// ───────── ESCREVER ─────────
rota("/escrever", async () => {
  const minhas = lista().filter((o) => o.minha && !o.trancada).sort((a, b) => b.atualizadaEm - a.atualizadaEm);
  const atual = minhas[0];
  const hoje = palavrasHoje(), meta = local.ler("metaDiaria", 1000), vezes = hoje / Math.max(1, meta), seq = diasSeguidos();
  const dias = local.ler("escritaDias", {}); const agoraD = new Date(); const pre = hojeId(agoraD).slice(0, 6);
  const ultimo = new Date(agoraD.getFullYear(), agoraD.getMonth() + 1, 0).getDate(), hd = agoraD.getDate();
  let caps = []; if (atual) { try { caps = await capitulos(atual.id); } catch (_) {} }
  const rasc = caps.filter((c) => c.rascunho).sort((a, b) => (b.atualizadoEm || 0) - (a.atualizadoEm || 0))[0];
  const pubs = caps.filter((c) => !c.rascunho).length;
  const cal = Array.from({ length: ultimo }, (_, k) => { const d = k + 1; const esc_ = (dias[pre + String(d).padStart(2, "0")] || 0) > 0; return `<span class="${d === hd ? "hoje" : esc_ ? "feito" : d > hd ? "vir" : ""}">${d}</span>`; }).join("");
  const ferr = [
    atual && [`#/ia/${atual.id}`, "Estúdio de IA", "prompts com o contexto da obra"],
    [atual ? `#/biblia/${atual.id}` : "#/escrever", "Personagens", "@ e & no texto"],
    ["#/nomes", "Gerador de nomes", "coreano, japonês, fantasia"],
    ["#/colaborar", "Colaboração", "coautores por código"],
    ["#/generos", "Gêneros", "o que está em alta"],
    [atual ? `#/leitores/${atual.id}` : "#/escrever", "Painel do escritor", "leituras e métricas"],
  ].filter(Boolean);
  casca("escrever", `<div class="ed-cab"><div><div class="ed-mono">Mesa do escritor</div><h1 class="ed-h1 grande">Escrever</h1></div><button class="ed-btn2 peq ed-mono" id="importar">Importar TXT ou Word</button><button class="ed-laranja ed-mono" id="nova">+ Nova obra</button></div>
    <div class="ed-mesa">
      <section class="ed-caixa"><div class="ed-hoje"><div><div class="ed-num">${mil(hoje)}</div><div class="ed-mono2">palavras hoje · meta de ${mil(meta)}</div></div>
          <button class="ed-circ" id="meta" title="Mudar a meta"><i>${vezes >= 1 ? Math.floor(vezes) + "×" : Math.round(vezes * 100) + "%"}</i><span class="ed-mono">a meta</span></button></div>
        <div class="ed-sec" style="margin-top:26px"><span class="ed-mono claro">${mesNome()}</span>${seq ? `<span class="ed-mono laranja">${seq} ${seq === 1 ? "dia seguido" : "dias seguidos"}</span>` : ""}</div>
        <div class="ed-cal">${cal}</div>
        <div class="ed-legenda"><span><i class="feito"></i>Escreveu</span><span><i class="hoje"></i>Hoje</span><span><i></i>Por vir</span></div></section>
      <section>${atual ? `<div class="ed-atual">${capaFaixa(atual, "media")}<div><div class="ed-mono laranja">${atual.publicada ? "Em andamento · publicada" : "Em andamento"}</div><h2 class="ed-titulo m">${esc(atual.titulo)}</h2>
          <div class="ed-mono">${rasc ? `cap. ${dois(rasc.ordem)} rascunho · ` : ""}${pubs} publicados · ${mil(caps.reduce((s, c) => s + (c.palavras || 0), 0))} palavras</div><div class="ed-mono verde">● salvo na nuvem</div></div></div>
        <div class="ed-botoes"><a class="ed-btn" href="#/editar/${atual.id}/${(rasc || caps[caps.length - 1])?.id || ""}" id="abrir">${rasc ? "Abrir rascunho" : "Escrever"} <span>→</span></a><button class="ed-btn2" id="novoCap">Novo capítulo</button></div>`
        : `<h2 class="ed-titulo m">Sua primeira história começa aqui.</h2><div class="ed-botoes"><button class="ed-btn" id="nova2">Criar obra <span>→</span></button></div>`}
        <div class="ed-mono" style="margin:30px 0 8px">Sumário de ferramentas</div>
        ${ferr.map(([h, n, s], k) => `<a class="ed-sumario" href="${h}"><span class="ed-mono">${String(k + 1).padStart(2, "0")}</span><b>${n}</b><small>${s}</small><i></i><span>→</span></a>`).join("")}
        ${minhas.length > 1 ? `<div class="ed-mono" style="margin:30px 0 8px">Outras obras</div>${minhas.slice(1).map((o) => `<a class="ed-linha" href="#/obra/${o.id}"><span class="ed-mini">${capaHTML(o, o.id, corHex(o.corBase))}</span><div style="flex:1"><div class="ed-nome">${esc(o.titulo)}</div><div class="suave peq">${mil(o.palavras || 0)} palavras</div></div></a>`).join("")}` : ""}
      </section></div>
    <div class="ed-dica"><span class="ed-mono laranja">Dica</span> Escreva <code>@Nome, descrição…</code> para criar um personagem e <code>&amp;Lugar, …</code> para um lugar. As fichas são montadas sozinhas.</div>`);
  const criar = async () => { const t = await perguntar("Título da nova obra", "", "Ex.: O Herdeiro do Vazio"); if (t === null) return; const id = await novaObra(t.trim()); ir(`/obra/${id}`); };
  $("#nova").onclick = criar; const n2 = $("#nova2"); if (n2) n2.onclick = criar;
  $("#importar").onclick = () => window.importarTXT();
  $("#meta").onclick = async () => { const r = await perguntar("Meta diária de palavras", String(meta), "1000"); const v = parseInt(r); if (v > 0) { local.gravar("metaDiaria", v); window.redesenhar(); } };
  const nc = $("#novoCap"); if (nc) nc.onclick = () => ir(`/obra/${atual.id}`);
  const ab = $("#abrir"); if (ab && !(rasc || caps.length)) ab.href = `#/obra/${atual.id}`;
  preencherCapas();
});

// ───────── EU ─────────
rota("/perfil", async () => {
  const minhas = lista().filter((o) => o.minha && !o.trancada).sort((a, b) => b.atualizadaEm - a.atualizadaEm);
  carregando("perfil");
  const [banner, foto] = await Promise.all([E.perfil.bannerHash ? imagemMinha("perfilBanner") : null, E.perfil.fotoHash ? imagemMinha("perfil") : null]);
  const pub = await perfilPub(E.usuario.uid);
  const palavras = minhas.reduce((s, o) => s + (o.palavras || 0), 0);
  const i = nivelAtual(); const seq = diasSeguidos(); const ms = missoes();
  const ini = Math.max(1, i.nivel - 2); const niveis = [0, 1, 2, 3, 4].map((k) => ini + k);
  const lendo = lista().filter((o) => !o.minha && o.status === "LENDO").length;
  const infos = await Promise.all(minhas.map(async (o) => {
    try { const cs = await capitulos(o.id); const pubs = cs.filter((c) => !c.rascunho); return { u: pubs[pubs.length - 1] || null, n: pubs.length, p: cs.reduce((s, c) => s + (c.palavras || 0), 0) }; }
    catch (_) { return { u: null, n: 0, p: o.palavras || 0 }; }
  }));
  const palavrasT = infos.reduce((s, x) => s + x.p, 0) || palavras;
  casca("perfil", `<section class="ed-perfil">${banner ? `<img class="ed-banner" src="${banner}" alt="">` : `<div class="ed-banner vazio"></div>`}
      <div class="ed-perfil-topo"><span class="ed-mono claro">Página do autor</span></div>
      <div class="ed-perfil-nome"><span class="ed-foto" id="foto">${foto ? `<img src="${foto}">` : esc(nomeAutor().slice(0, 1).toUpperCase())}</span>
        <div style="flex:1;min-width:0"><h1 class="ed-titulo">${esc(nomeAutor())}</h1><div class="ed-sub"><i>${minhas.length ? `autor de ${esc(minhas[0].titulo)}` : "leitor"}</i></div></div>
        <div class="ed-botoes"><button class="ed-btn" id="editar">Editar página</button><a class="ed-btn2" href="#/autor/${E.usuario.uid}">Ver como leitor</a>
          <button class="ed-quad" id="partilhar" title="Compartilhar">${'<svg viewBox="0 0 24 24"><path d="M12 3v12M8 7l4-4 4 4M5 13v7h14v-7"/></svg>'}</button>
          <a class="ed-quad" href="#/ajustes" title="Ajustes">${'<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></svg>'}</a></div></div></section>
    <button class="ed-bio" id="bio"><span class="ed-aspas p">“</span><div><i>${esc(E.perfil.bio || "Diga, em uma frase, o que move as suas histórias.")}</i>${E.perfil.bio ? "" : `<div class="ed-mono laranja">Clique para escrever sua bio</div>`}</div></button>
    <div class="ed-duas">
      <section><div class="ed-numeros"><div><b>${mil(pub?.seguidores || 0)}</b><span class="ed-mono">seguidores</span></div><div><b>${minhas.length}</b><span class="ed-mono">${minhas.length === 1 ? "obra" : "obras"}</span></div><div><b>${mil(palavrasT)}</b><span class="ed-mono">palavras</span></div><div><b>${lendo}</b><span class="ed-mono">lendo</span></div></div>
        ${minhas.length ? `<div class="ed-sec" style="margin-top:34px"><h2 class="ed-h2">Obras</h2><span class="ed-mono">${String(minhas.length).padStart(2, "0")} ${minhas.length === 1 ? "título" : "títulos"}</span></div>
        ${minhas.map((o, k) => `<a class="ed-obra" href="#/obra/${o.id}">${capaFaixa(o, "obra")}<div><div class="ed-mono laranja">${o.status === "CONCLUIDA" ? "Concluída" : "Em andamento"}</div><h3 class="ed-titulo m">${esc(o.titulo)}</h3>
          <div class="suave">${infos[k].n} ${infos[k].n === 1 ? "capítulo publicado" : "capítulos publicados"} · ${mil(infos[k].p)} palavras</div>${infos[k].u ? `<div class="ed-mono ed-ult">Último · cap. ${dois(infos[k].u.ordem)} ${esc(infos[k].u.titulo || "")}</div>` : ""}</div></a>`).join("")}` : ""}</section>
      <section><div class="ed-sec"><span class="ed-mono">Sua jornada</span>${seq ? `<span class="ed-mono laranja">${seq} ${seq === 1 ? "dia seguido" : "dias seguidos"}</span>` : ""}</div>
        <h2 class="ed-jornada">Capítulo ${romano(i.nivel)} — <i>${esc(tituloNivel(i.nivel))}</i></h2>
        <div class="ed-trilha">${niveis.map((n) => `<span class="${n < i.nivel ? "feito" : n === i.nivel ? "agora" : ""}"><i></i><b>${romano(n)}</b></span>`).join("")}</div>
        <div class="ed-mono2">${i.noNivel} / ${i.precisa} XP · faltam ${i.precisa - i.noNivel} para o capítulo ${romano(i.nivel + 1)}</div>
        <div class="ed-missoes"><div class="ed-sec"><h3 class="ed-h3">Missões de hoje</h3><span class="ed-mono">${ms.filter((m) => m.feito >= m.alvo).length} / ${ms.length}</span></div>
          ${ms.map((m) => `<div class="ed-missao ${m.feito >= m.alvo ? "ok" : ""}"><i></i><span>${esc(m.t)}</span><span class="ed-mono">${m.feito}/${m.alvo}</span></div>`).join("")}
          ${bauPronto() ? `<button class="ed-laranja ed-mono" id="bau" style="width:100%;margin-top:12px">Abrir o baú de hoje · +50 XP</button>` : ""}</div>
        <a class="ed-mes" href="#/retro"><div><div class="ed-mono laranja">Edição de ${mesNome()}</div><b>Seu mês no Novely</b></div><span>→</span></a>
        <div id="moedas"></div><div id="conquistas"></div></section></div>`);
  $("#editar").onclick = () => editarPerfil(foto, banner); $("#bio").onclick = () => editarPerfil(foto, banner); $("#foto").onclick = () => editarPerfil(foto, banner);
  $("#partilhar").onclick = () => { const u = location.origin + location.pathname + "#/autor/" + E.usuario.uid; navigator.clipboard?.writeText(u); toast("Link da sua página copiado"); };
  const b = $("#bau"); if (b) b.onclick = () => { local.gravar("bau", new Date().toISOString().slice(0, 10)); local.gravar("xp", local.ler("xp", 0) + 50); celebrar("🎁", "Baú aberto!", "+50 XP de bônus"); window.redesenhar(); };
  preencherCapas();
  const ganhas = new Set((E.perfil.conquistas || "").split(",").filter(Boolean));
  carteira().then((c) => { const el = $("#moedas"); if (el) el.innerHTML = `<div class="ed-sec" style="margin-top:30px"><span class="ed-mono">Moedas</span><span class="ed-mono laranja">${c.saldo}</span></div><p class="suave peq">Grátis, para ler capítulos antes de todo mundo. Ganhe abrindo o Novely (+3 por dia), terminando capítulos e comentando.</p>`; }).catch(() => {});
  $("#conquistas").innerHTML = `<div class="ed-sec" style="margin-top:22px"><span class="ed-mono">Conquistas</span><span class="ed-mono">${ganhas.size} de ${CONQUISTAS.length}</span></div>
    <div class="ed-conq">${[...CONQUISTAS].sort((a, b) => ganhas.has(b[0]) - ganhas.has(a[0])).map(([id, ic, t]) => `<span class="${ganhas.has(id) ? "on" : ""}" title="${esc(t)}">${ic}</span>`).join("")}</div>`;
});

function editarPerfil(foto, banner) {
  const d = dialogo(`<h3 style="margin-top:0">Editar página</h3>
    <div class="banner-ed" id="bn">${banner ? `<img src="${banner}">` : ""}<span>${banner ? "Trocar banner" : "+ Banner da página"}</span></div>
    <div style="display:flex;align-items:center;gap:14px;margin:14px 0"><span class="av" id="ft" style="width:76px;height:76px;font-size:28px;cursor:pointer">${foto ? `<img src="${foto}">` : esc(nomeAutor().slice(0, 1))}</span><span class="suave peq">Clique na foto ou no banner para trocar</span></div>
    <label class="rot">Nome de autor</label><input class="campo" id="n" value="${esc(E.perfil.nome || "")}">
    <label class="rot">Bio (o que move as suas histórias)</label><textarea class="campo" id="b" rows="3" maxlength="160">${esc(E.perfil.bio || "")}</textarea>
    <div class="linha-botoes">${banner ? `<button class="btn2 peq" id="tirar" style="margin-right:auto">Tirar banner</button>` : ""}<button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" id="ok">Salvar</button></div>`);
  const trocar = async (qual) => {
    const f = await escolherArquivo(); if (!f) return;
    toast("Enviando imagem…");
    const bytes = await comprimirArquivo(f, qual === "perfil" ? 600 : 1600, .85);
    const h = await enviarImagemMinha(qual, bytes);
    await salvarPerfil(qual === "perfil" ? { fotoHash: h } : { bannerHash: h });
    d.close(); toast("Imagem trocada"); window.redesenhar();
  };
  $("#ft", d).onclick = () => trocar("perfil"); $("#bn", d).onclick = () => trocar("perfilBanner");
  const t = $("#tirar", d); if (t) t.onclick = async () => { await salvarPerfil({ bannerHash: "" }); d.close(); window.redesenhar(); };
  $("[data-n]", d).onclick = () => d.close();
  $("#ok", d).onclick = async () => { await salvarPerfil({ nome: $("#n", d).value.trim(), bio: $("#b", d).value.trim() }); d.close(); toast("Salvo"); window.redesenhar(); };
}
