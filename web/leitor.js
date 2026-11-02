// Leitor (biblioteca e catálogo), editor e revisão.
import {
  E, $, $$, esc, raiz, agora, toast, milhar, palavrasFmt, haQuanto, casca, carregando, rota, ir, local, limparMarcas, mapaNomes, contarPalavras,
  capitulos, fichas, marcadores, salvarCapitulo, salvarObra, salvarItem, apagarItem, lerTexto, colTextos, salvarTexto, novoId, dialogo, confirmar,
  obraPub, capsPub, fichasPub, imagemPub, seguindo, seguir, minhaNota, avaliar, contarLeitura, curtiu, curtir, comentarios, comentar, apagarComentario,
  collection, db, textoRevisao, notas, anotar, apagarNota, nomeAutor, salvarFicha, perguntar, enfeiteHTML, corDaImagem, corDaBase, usarCorDaObra,
} from "./nucleo.js";
import { verFicha } from "./obra.js";

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
    <div class="linha-botoes"><button class="btn2 peq" id="pad">Voltar ao padrão</button><button class="btn peq" data-n>Pronto</button></div>`);
  const grava = (k, v) => { local.gravar("leitura", { ...pref(), [k]: v }); aoMudar(); };
  $("#tam", d).oninput = (e) => { $("#vt", d).textContent = e.target.value + "px"; grava("tam", +e.target.value); };
  $("#alt", d).oninput = (e) => grava("altura", +e.target.value);
  $("#larg", d).oninput = (e) => grava("largura", +e.target.value);
  $("#just", d).onchange = (e) => grava("justificar", e.target.checked);
  $("#capi", d).onchange = (e) => { grava("capitular", e.target.checked); window.redesenhar(); };
  $("#mara", d).onchange = (e) => grava("maratona", e.target.checked);
  $("#cobra", d).onchange = (e) => { local.gravar("corDaObra", e.target.checked); window.redesenhar(); };
  $$("[data-fonte]", d).forEach((c) => (c.onclick = () => { grava("fonte", c.dataset.fonte); $$("[data-fonte]", d).forEach((x) => x.classList.toggle("on", x === c)); }));
  $$("[data-tema]", d).forEach((c) => (c.onclick = () => { grava("tema", c.dataset.tema); $$("[data-tema]", d).forEach((x) => x.classList.toggle("on", x === c)); }));
  $("#pad", d).onclick = () => { local.gravar("leitura", {}); aoMudar(); d.close(); };
  $("[data-n]", d).onclick = () => d.close();
}

// ───────── texto → html ─────────
const CORES_GRIFO = { AMBAR: "#E9A65A", VERDE: "#6FC2B4", AZUL: "#7FA7E6", ROSA: "#E88AA8" };
export function htmlTexto(t, nomes = new Map(), grifos = new Map(), op = {}) {
  let capitularFeita = !pref().capitular;
  const linhas = limparMarcas(t).split("\n").map((l) => l.trim()).filter(Boolean);
  const lista = [...nomes.keys()].sort((a, b) => b.length - a.length).map((n) => n.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
  const rxNomes = lista.length ? new RegExp(`(?<![\\p{L}\\p{N}'’-])(${lista.join("|")})(?![\\p{L}\\p{N}]|[-'’]\\p{L})`, "gu") : null;
  return linhas.map((l, i) => {
    const g = grifos.get(i);
    if (/^(\* \* \*|\*\*\*|---|◆)$/.test(l)) return `<div class="sep enfeite" data-b="${i}">${enfeiteHTML(op.enfeite)}</div>`;
    if (/^#/.test(l)) return `<h3 data-b="${i}">${esc(l.replace(/^#+\s*/, ""))}</h3>`;
    let h = esc(l).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>").replace(/\*(.+?)\*/g, "<i>$1</i>");
    if (rxNomes) h = h.replace(rxNomes, (m) => `<span class="nome" data-ficha="${esc(nomes.get(m))}">${m}</span>`);
    const fala = /^[—–―\-“"«]/.test(l) && l.length > 1 && !l.startsWith("---");
    if (fala) h = h.replace(/^([—–―\-“"«]\s?)/, '<b class="trav">$1</b>');
    const nota = g?.nota ? `<span class="nota-grifo" title="${esc(g.nota)}">✎</span>` : "";
    const capit = !capitularFeita && !fala && l.length > 40 && !/^(cap[ií]tulo|pr[oó]logo|ep[ií]logo)/i.test(l); if (capit) capitularFeita = true;
    const cls = [fala ? "fala" : "", g ? "grifo" : "", capit ? "capitular" : ""].filter(Boolean).join(" ");
    const nc = op.coms?.get(i) || 0;
    const balao = op.coms ? `<button class="balao-trecho ${nc ? "" : "vazio"}" data-trecho="${i}" title="Comentar este trecho">💬${nc ? " " + nc : ""}</button>` : "";
    return `<p data-b="${i}"${cls ? ` class="${cls}"` : ""}${g ? ` style="--grifo:${CORES_GRIFO[g.cor] || "#E9A65A"}"` : ""}>${h}${nota}${balao}</p>`;
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

// ───────── ouvir (voz do navegador) ─────────
let falando = null;
function pararVoz() { speechSynthesis.cancel(); falando = null; $$(".falando").forEach((p) => p.classList.remove("falando")); const b = $("#ouvir"); if (b) b.textContent = "▶ Ouvir"; }
function ouvir(apartir = 0) {
  const pars = $$("#texto [data-b]").filter((p) => p.tagName !== "DIV");
  if (falando !== null) { pararVoz(); return; }
  const vel = local.ler("velVoz", 1);
  let i = Math.max(0, pars.findIndex((p) => +p.dataset.b >= apartir));
  const voz = speechSynthesis.getVoices().find((v) => v.lang?.toLowerCase().startsWith("pt-br")) || speechSynthesis.getVoices().find((v) => v.lang?.startsWith("pt"));
  const prox = () => {
    if (falando === null || i >= pars.length) { pararVoz(); return; }
    const p = pars[i]; $$(".falando").forEach((x) => x.classList.remove("falando")); p.classList.add("falando");
    p.scrollIntoView({ block: "center", behavior: "smooth" });
    const u = new SpeechSynthesisUtterance(p.textContent.replace("✎", "")); u.lang = "pt-BR"; if (voz) u.voice = voz; u.rate = vel;
    u.onend = () => { i++; prox(); }; u.onerror = () => pararVoz();
    falando = i; speechSynthesis.speak(u);
  };
  falando = 0; const b = $("#ouvir"); if (b) b.textContent = "■ Parar"; prox();
}

/** Liga clique nos nomes (abre a ficha). */
function ligarNomes(fs, fotoDe) {
  $$("#texto .nome").forEach((n) => (n.onclick = async () => { const f = fs.find((x) => x.id === n.dataset.ficha); if (f) verFicha(f, fotoDe ? await fotoDe(f) : undefined); }));
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
      <div class="controles vidro"><a class="voltar" href="#/obra/${obraId}" title="Voltar">←</a><span class="tt">${esc(o.titulo)} · ${esc(c.titulo)}</span>
        <button class="btn2 peq" id="ouvir">▶ Ouvir</button><button class="btn2 peq" id="lcaps" title="Capítulos">☰</button><button class="btn2 peq" id="aj" title="Leitura">Aa</button>${pode ? `<a class="btn2 peq" href="#/editar/${obraId}/${capId}">✎ Editar</a>` : ""}</div>
      <div class="cabec"><div class="rotulo">${esc(o.titulo)}</div><h1>${esc(c.titulo || "Sem título")}</h1><div class="suave peq">${palavrasFmt(c.palavras || 0)} palavras · ◷ ${Math.max(1, Math.round((c.palavras || 0) / 230))} min</div></div>
      ${(c.progresso || 0) < .05 ? cartaoAnterior(ant) : ""}
      <div class="texto" id="texto">${htmlTexto(texto, mapaNomes(fs), grifos, { enfeite: o.enfeite }) || '<p class="suave">Capítulo vazio.</p>'}</div>
      <div class="fim"><div class="losango" style="width:14px;height:14px"></div><div class="rotulo" style="color:var(--suave)">Fim do capítulo</div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center">${ant ? `<a class="btn2" href="#/ler/${obraId}/${ant.id}">‹ Anterior</a>` : ""}${prox ? `<a class="btn" href="#/ler/${obraId}/${prox.id}">Próximo: ${esc(prox.titulo)} ›</a>` : `<span class="suave">Último capítulo.</span>`}</div>
        ${!prox && o.status === "LENDO" && !o.minha ? `<button class="btn2 peq" id="concluir">Marcar obra como lida</button>` : ""}</div>
      <p class="suave peq dica-leitor">Dica: clique num nome para ver a ficha · clique com o botão direito num parágrafo para grifar · ← → trocam de capítulo</p></div>`);
    ligar();
  };
  const ligar = () => {
    ligarNomes(fs, async (f) => (f.imagemHash ? (await import("./nucleo.js")).imagemMinha(f.id + "-foto") : null));
    $("#ouvir").onclick = () => ouvir(0);
    $("#aj").onclick = () => painelAjustes(() => { $(".leitor").setAttribute("style", estiloLeitura()); });
    $("#lcaps").onclick = () => painelCapitulos(caps, capId, (x) => `#/ler/${obraId}/${x.id}`);
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
  window.onscroll = () => { aoRolar(); maratona(prox && `/ler/${obraId}/${prox.id}`); }; aoRolar();
  window.onkeydown = (e) => { if (e.target.closest("input,textarea,dialog")) return; if (e.key === "ArrowRight" && prox) ir(`/ler/${obraId}/${prox.id}`); if (e.key === "ArrowLeft" && ant) ir(`/ler/${obraId}/${ant.id}`); };
  E.saindo = async () => { pararVoz(); };
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
    await salvarItem("marcadores", obraId, id, dados); grifos.set(bloco, { ...dados, id }); redesenhar();
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
      <button data-ins="\n\n* * *\n\n" title="Separador de cena">◆</button><button data-ins="\n# " title="Subtítulo">H</button><span class="div"></span>
      <button id="bbusca" title="Buscar e substituir (Ctrl+F)">⌕ Buscar</button><button id="bfichas" title="Personagens">☺ Fichas</button><button id="bfoco" title="Modo foco">⛶ Foco</button>
    </div>
    <div class="busca-ed vidro" id="busca" hidden><input class="campo" id="q" placeholder="Buscar…"><input class="campo" id="r" placeholder="Substituir por…"><span class="suave peq" id="qn"></span>
      <button class="btn2 peq" id="qant">‹</button><button class="btn2 peq" id="qprox">›</button><button class="btn2 peq" id="rum">Substituir</button><button class="btn2 peq" id="rtodos">Todos</button><button class="btn2 peq" id="qx">✕</button></div>
    <div class="sugestoes-ed" id="sug" hidden></div>
    <textarea id="txt" spellcheck="true" placeholder="Escreva aqui… (@Personagem, descrição · &Lugar, descrição)"></textarea>
    <div class="rodape-ed suave peq"><span id="cont"></span><span id="sessao"></span></div></div>`);
  const ta = $("#txt"), ti = $("#titulo"), est = $("#estado"), cont = $("#cont");
  ta.value = original;
  const base = contarPalavras(original);
  let salvo = original, tituloSalvo = c.titulo || "", tempo = null, gravando = false;
  const contar = () => { const n = contarPalavras(ta.value); cont.textContent = `${n.toLocaleString("pt-BR")} palavras · salva sozinho e aparece no celular`; $("#sessao").textContent = n !== base ? `sessão: ${n - base >= 0 ? "+" : ""}${(n - base).toLocaleString("pt-BR")}` : ""; };
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
  const inserir = (s, envolver = false) => {
    ta.focus(); const a = ta.selectionStart, b = ta.selectionEnd; const sel = ta.value.slice(a, b);
    const novo = envolver ? s + sel + s : s;
    ta.setRangeText(novo, a, b, "end"); if (envolver && !sel) ta.selectionStart = ta.selectionEnd = a + s.length;
    ta.dispatchEvent(new Event("input"));
  };
  $$("[data-ins]").forEach((b) => (b.onclick = () => inserir(b.dataset.ins.replace(/\\n/g, "\n"))));
  $$("[data-env]").forEach((b) => (b.onclick = () => inserir(b.dataset.env, true)));
  $("[data-fala]").onclick = () => { const a = ta.selectionStart; const antes = ta.value.slice(0, a); inserir((antes && !antes.endsWith("\n") ? "\n\n" : "") + "— "); };
  $("#bfoco").onclick = () => document.body.classList.toggle("foco");
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
  contar(); ta.focus();
  window.onbeforeunload = (e) => { if (ta.value !== salvo) { salvar(); e.preventDefault(); } };
  E.saindo = async () => { window.onbeforeunload = null; document.body.classList.remove("foco"); clearTimeout(tempo); await salvar(); };
});

// ───────── LEITOR DO CATÁLOGO ─────────
rota("/publer/(\\w+)/(\\w+)", async (pid, cid) => {
  carregando("descobrir");
  const [o, caps, fs] = await Promise.all([obraPub(pid), capsPub(pid), fichasPub(pid)]);
  if (!o) { casca("descobrir", `<p class="suave">Obra não encontrada.</p>`); return; }
  const i = caps.findIndex((c) => c.id === cid); const c = caps[i]; if (!c) { ir(`/pub/${pid}`); return; }
  const texto = await lerTexto(collection(db, "publicas", pid, "textos"), cid, c.partes);
  await corDoLeitor(await imagemPub(pid, "capa"), null);
  const prox = caps[i + 1], ant = caps[i - 1];
  const [gostei, coms] = await Promise.all([curtiu(pid, cid), comentarios(pid, cid).catch(() => [])]);
  const fichasLista = fs.map((f) => ({ ...f, id: f.id }));
  const porTrecho = new Map(); for (const m of coms) if ((m.bloco ?? -1) >= 0) porTrecho.set(m.bloco, (porTrecho.get(m.bloco) || 0) + 1);
  casca("descobrir", `<div class="prog" id="prog"></div><div class="leitor tema-leitura" style="${estiloLeitura()}"><div class="controles vidro"><a class="voltar" href="#/pub/${pid}">←</a><span class="tt">${esc(o.titulo)} · ${esc(c.titulo)}</span>
      <button class="btn2 peq" id="ouvir">▶ Ouvir</button><button class="btn2 peq" id="lcaps">☰</button><button class="btn2 peq" id="aj">Aa</button></div>
    <div class="cabec"><div class="rotulo">${esc(o.titulo)}</div><h1>${esc(c.titulo)}</h1><div class="suave peq">por <a href="#/autor/${esc(o.autorUid)}" style="text-decoration:underline">${esc(o.autorNome)}</a> · ${palavrasFmt(c.palavras)} palavras</div></div>
    ${cartaoAnterior(ant)}
    <div class="texto" id="texto">${htmlTexto(texto, mapaNomes(fichasLista), new Map(), { enfeite: o.enfeite, coms: porTrecho })}</div>
    <div class="fim"><div class="losango" style="width:14px;height:14px"></div>
      <div class="social"><button class="btn2 ${gostei ? "on" : ""}" id="curtir">${gostei ? "♥" : "♡"} <span id="nc">${c.curtidas || 0}</span></button><span class="suave">💬 ${coms.length}</span></div>
      <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center">${ant ? `<a class="btn2" href="#/publer/${pid}/${ant.id}">‹ Anterior</a>` : ""}${prox ? `<a class="btn" href="#/publer/${pid}/${prox.id}">Próximo ›</a>` : `<span class="suave">Último capítulo publicado.</span>`}</div></div>
    <div class="comentarios vidro"><h3 style="margin-top:0">Comentários</h3>
      <div style="display:flex;gap:10px"><textarea class="campo" id="com" rows="2" placeholder="O que achou do capítulo?"></textarea><button class="btn peq" id="enviar">Enviar</button></div>
      <div id="lcom">${coms.map((m) => `<div class="comentario"><span class="av" style="width:34px;height:34px">${esc((m.nome || "?").slice(0, 1))}</span><div style="flex:1"><b>${esc(m.nome)}</b> <span class="suave peq">${haQuanto(m.em)}</span><div>${esc(m.texto)}</div></div>${m.uid === E.usuario.uid || o.autorUid === E.usuario.uid ? `<button class="btn2 peq" data-ac="${m.id}">Apagar</button>` : ""}</div>`).join("") || `<p class="suave">Seja o primeiro a comentar.</p>`}</div></div></div>`);
  ligarNomes(fichasLista);
  $("#ouvir").onclick = () => ouvir(0);
  $("#aj").onclick = () => painelAjustes(() => $(".leitor").setAttribute("style", estiloLeitura()));
  $("#lcaps").onclick = () => painelCapitulos(caps, cid, (x) => `#/publer/${pid}/${x.id}`);
  $("#curtir").onclick = async (e) => { const b = e.currentTarget; const sim = !b.classList.contains("on"); b.classList.toggle("on", sim); b.firstChild.textContent = sim ? "♥ " : "♡ "; $("#nc").textContent = (+$("#nc").textContent) + (sim ? 1 : -1); try { await curtir(pid, cid, sim); } catch (_) {} };
  $("#enviar").onclick = async () => { const t = $("#com").value.trim(); if (!t) return; await comentar(pid, cid, t); toast("Comentário enviado"); window.redesenhar(); };
  $$("[data-ac]").forEach((b) => (b.onclick = async () => { if (await confirmar("Apagar comentário?", "", "Apagar", true)) { await apagarComentario(pid, cid, b.dataset.ac); window.redesenhar(); } }));
  scrollTo(0, 0);
  contarLeitura(pid, cid);
  $$("[data-trecho]").forEach((b) => (b.onclick = (e) => { e.stopPropagation(); comentariosTrecho(pid, cid, +b.dataset.trecho, b.closest("p").textContent.replace(/💬.*$/, "").trim(), coms); }));
  // progresso de quem segue: guarda o último capítulo lido nesta obra
  const naBib = [...E.obras.entries()].find(([, x]) => x.publicaId === pid && !x.apagado);
  if (naBib) salvarObra(naBib[0], { lidaEm: agora() });
  const aoRolar = () => { const alt = document.documentElement.scrollHeight - innerHeight; const b = $("#prog"); if (b) b.style.width = (alt > 0 ? Math.min(1, scrollY / alt) : 1) * 100 + "%"; maratona(prox && `/publer/${pid}/${prox.id}`); };
  window.onscroll = aoRolar;
  window.onkeydown = (e) => { if (e.target.closest("input,textarea,dialog")) return; if (e.key === "ArrowRight" && prox) ir(`/publer/${pid}/${prox.id}`); if (e.key === "ArrowLeft" && ant) ir(`/publer/${pid}/${ant.id}`); };
  E.saindo = async () => pararVoz();
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
