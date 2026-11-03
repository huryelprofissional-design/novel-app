// Telas da obra: página, capítulos, personagens, linha do tempo, publicar, colaboração e importar.
import {
  E, $, $$, esc, raiz, agora, toast, milhar, palavrasFmt, dataFmt, corHex, ehEspecial, capaHTML, preencherCapas, casca, carregando, rota, ir, lista,
  capitulos, fichas, eventos, salvarObra, salvarCapitulo, salvarItem, apagarItem, novoCapitulo, novaObra, apagarObra, salvarFicha, FICHA_VAZIA,
  imagemMinha, escolherArquivo, comprimirArquivo, enviarImagemMinha, dialogo, confirmar, perguntar, pendencias, publicarObra, publicarCapitulos,
  despublicarCapitulo, despublicarObra, obraPub, GENEROS_GRUPOS, CLASSIFICACOES, STATUS_NOME, lerTexto, colTextos, novoId, limparMarcas, partir,
  convidar, membros, removerMembro, enviarRevisao, tirarRevisao, capsRevisao, notas, sugestoes, aceitarSugestao, descartarSugestao, local, mapaNomes,
  corDaImagem, corDaBase, usarCorDaObra, ENFEITES, enfeiteHTML,
} from "./nucleo.js";

/** Pinta a tela com a cor da capa (selo, botões, capítulo atual…). */
export async function aplicarCor(url, corBase) {
  if (!usarCorDaObra()) return;
  const cor = (url && await corDaImagem(url)) || corDaBase(corBase || 0xFF3B2A4A);
  const el = document.querySelector(".conteudo"); if (el && cor) { el.style.setProperty("--acento", cor); el.style.setProperty("--sobre-acento", "#0b0b0b"); }
}

const TIPO_NOME = { PERSONAGEM: "Personagem", LUGAR: "Lugar", OUTRO: "Outro" };

// ───────── página da obra ─────────
rota("/obra/(\\w+)(?:/(\\w+))?", async (id, aba = "caps") => {
  const o = E.obras.get(id); if (!o) { ir("/"); return; }
  const pode = o.minha && !o.trancada;
  const abaMenu = pode ? "escrever" : "biblioteca";
  carregando(abaMenu);
  const caps = await capitulos(id, true);
  // obra seguida que o celular ainda não baixou: lê direto do catálogo
  if (!caps.length && o.trancada && o.publicaId) { ir(`/pub/${o.publicaId}`); return; }
  const [capa, banner] = await Promise.all([o.capaHash ? imagemMinha(id + "-capa") : null, o.bannerHash ? imagemMinha(id + "-banner") : null]);
  const palavras = caps.reduce((s, c) => s + (c.palavras || 0), 0);
  const lidos = caps.filter((c) => (c.progresso || 0) >= .98).length;
  const alvo = caps.find((c) => c.id === o.ultimoCap) || caps.find((c) => (c.progresso || 0) < .98) || caps[0];
  const continuar = alvo && (alvo.progresso || 0) > .01 && (alvo.progresso || 0) < .98;
  const selo = pode ? (o.publicada ? "NO CATÁLOGO" : "ESCREVENDO") : o.trancada ? "PUBLICADA" : (STATUS_NOME[o.status] || "LENDO").toUpperCase();
  const abas = pode ? [["caps", `Capítulos · ${caps.length}`], ["fichas", "Personagens"], ["tempo", "Linha do tempo"]] : [["caps", `Capítulos · ${caps.length}`]];
  casca(abaMenu, `<div class="topo"><a class="voltar vidro" href="javascript:history.back()">←</a><div style="flex:1"></div><button class="btn2 peq" id="menu">⋯ Opções</button></div>
    <div class="cab-obra"><div class="fundo" style="background-image:url(${banner || capa || ""});${banner || capa ? "" : `background:${corHex(o.corBase)}`}"></div>
      <div class="frente"><div class="capa clicavel" id="capa" style="--cor:${corHex(o.corBase)}">${capa ? `<img src="${capa}">` : `<div class="sol"></div><div class="t">${esc(o.titulo)}</div>`}</div><div style="flex:1;min-width:0">
        <span class="selo">${esc(selo)}</span>${o.autor ? `<span class="suave peq" style="margin-left:10px">por ${esc(o.autor)}</span>` : ""}
        <h1>${esc(o.titulo)}</h1>
        <div class="chips"><span class="pilula">${caps.length} caps</span><span class="pilula">${palavrasFmt(palavras)} palavras</span>${caps.length && !o.minha ? `<span class="pilula">${Math.round(lidos * 100 / caps.length)}% lido</span>` : ""}${(() => { if (o.minha || !caps.length) return ""; const m = Math.round(caps.reduce((s, c) => s + (c.palavras || 0) * (1 - Math.min(1, c.progresso || 0)), 0) / 230); return m > 0 ? `<span class="pilula">faltam ${m >= 60 ? Math.floor(m / 60) + " h " + (m % 60) + " min" : m + " min"}</span>` : ""; })()}${(o.generos || "").split(",").filter(Boolean).slice(0, 3).map((g) => `<span class="pilula">${esc(g)}</span>`).join("")}</div>
        <div class="acoes">${alvo ? `<a class="btn" href="#/ler/${id}/${alvo.id}">▶ ${continuar ? "Continuar" : "Ler agora"}</a>` : ""}
          ${pode ? `<button class="btn2" id="maiscap">+ Capítulo</button>${o.publicada || caps.length ? `<a class="btn2" href="#/publicar/${id}">${o.publicada ? "Publicação" : "Publicar"}</a>` : ""}` : ""}
          ${o.trancada && o.publicaId ? `<a class="btn2" href="#/pub/${o.publicaId}">Ver no catálogo</a>` : ""}</div>
      </div></div></div>
    ${o.descricao ? `<p class="sinopse">${esc(o.descricao)}</p>` : ""}
    <div class="abas">${abas.map(([k, n]) => `<a href="#/obra/${id}/${k}" class="${k === aba ? "on" : ""}">${n}</a>`).join("")}</div>
    <div id="corpo"></div>`);
  aplicarCor(capa, o.corBase);
  const corpo = $("#corpo");
  if (aba === "fichas" && pode) await telaFichas(id, corpo, pode);
  else if (aba === "tempo" && pode) await telaTempo(id, corpo, caps);
  else listaCaps(id, o, caps, corpo, pode);
  $("#menu").onclick = (e) => menuObra(id, e.currentTarget);
  $("#capa").onclick = () => { if (pode) trocarImagem(id, "capa"); else if (capa) dialogo(`<img src="${capa}" style="width:100%;border-radius:16px">`); };
  const m = $("#maiscap"); if (m) m.onclick = async () => { const c = await novoCapitulo(id); ir(`/editar/${id}/${c}`); };
});

function listaCaps(id, o, caps, corpo, pode) {
  if (!caps.length) { corpo.innerHTML = `<div class="card vazio"><b>Nenhum capítulo ainda.</b>${pode ? `<span class="suave">Clique em + Capítulo para começar.</span>` : ""}</div>`; return; }
  let n = 0;
  const desc = local.ler("capsDesc", false);
  const itens = caps.map((c) => ({ c, num: ehEspecial(c.titulo) ? "◆" : ++n }));
  if (desc) itens.reverse();
  corpo.innerHTML = `<div style="display:flex;justify-content:flex-end;margin:-6px 0 6px"><a class="suave peq" href="javascript:void 0" id="ordem">⇅ ${desc ? "mais recentes primeiro" : "do início"}</a></div>
    <div class="caps">${itens.map(({ c, num }) => {
    const lido = (c.progresso || 0) >= .98; const atual = c.id === o.ultimoCap;
    const pub = o.publicada && pode ? (c.publicadoEm > agora() ? "◷ agendado · " : c.publicadoEm > 0 ? "● publicado · " : "○ não publicado · ") : "";
    const info = c.rascunho && pode ? "rascunho · " + palavrasFmt(c.palavras) + " palavras" : (c.progresso || 0) > .01 && !lido ? Math.round(c.progresso * 100) + "% lido" : lido ? "lido" : palavrasFmt(c.palavras) + " palavras";
    return `<div class="cap ${atual ? "atual" : ""}" data-cap="${c.id}"><span class="n">${num}</span><div style="flex:1;min-width:0"><div class="tt ${lido && !atual ? "lido" : ""}">${esc(c.titulo || "Sem título")}</div><div class="suave peq">${pub}${info}${(c.tags || "").split(",").filter((t) => t.trim()).map((t) => " #" + esc(t.trim())).join("")}</div></div>
      ${pode ? `<a class="btn2 peq" href="#/editar/${id}/${c.id}" onclick="event.stopPropagation()" title="Editar texto">✎</a>` : ""}<button class="btn2 peq" data-mc="${c.id}" title="Opções">⋯</button></div>`;
  }).join("")}</div>`;
  $("#ordem").onclick = () => { local.gravar("capsDesc", !desc); listaCaps(id, o, caps, corpo, pode); };
  $$("[data-cap]", corpo).forEach((el) => (el.onclick = () => ir(`/ler/${id}/${el.dataset.cap}`)));
  $$("[data-mc]", corpo).forEach((b) => (b.onclick = (e) => { e.stopPropagation(); menuCap(id, o, caps, caps.find((c) => c.id === b.dataset.mc), b, pode); }));
}

/** Menu suspenso simples. itens: [rotulo, acao, perigo?] (null = separador) */
export function menu(ancora, itens) {
  $$(".menu-flutuante").forEach((m) => m.remove());
  const m = document.createElement("div"); m.className = "menu-flutuante vidro";
  m.innerHTML = itens.map((it, i) => it ? `<button data-i="${i}" class="${it[2] ? "perigo" : ""}">${esc(it[0])}</button>` : `<hr>`).join("");
  document.body.appendChild(m);
  const r = ancora.getBoundingClientRect();
  const w = m.offsetWidth, h = m.offsetHeight;
  m.style.left = Math.max(8, Math.min(innerWidth - w - 8, r.right - w)) + "px";
  m.style.top = (r.bottom + h + 8 > innerHeight ? Math.max(8, r.top - h - 6) : r.bottom + 6) + "px";
  $$("[data-i]", m).forEach((b) => (b.onclick = () => { m.remove(); itens[b.dataset.i][1](); }));
  setTimeout(() => document.addEventListener("click", function fora(e) { if (!m.contains(e.target)) { m.remove(); document.removeEventListener("click", fora); } }), 0);
}

function menuCap(id, o, caps, c, ancora, pode) {
  const lido = (c.progresso || 0) >= .98;
  const i = caps.indexOf(c);
  const itens = [];
  if (pode) {
    itens.push(["✨ Estúdio de IA", () => ir(`/ia/${id}/${c.id}`)]);
    itens.push(["Renomear", async () => { const t = await perguntar("Título do capítulo", c.titulo); if (t === null) return; await salvarCapitulo(id, c.id, { titulo: t.trim() }); window.redesenhar(); }]);
    if (o.publicada) {
      if (!c.publicadoEm || c.publicadoEm > agora()) itens.push(["Publicar agora", async () => { toast("Publicando…"); await publicarCapitulos(id, [c]); toast("Capítulo publicado!"); window.redesenhar(); }]);
      else {
        itens.push(["Republicar (enviar mudanças)", async () => { toast("Republicando…"); await publicarCapitulos(id, [c]); toast("Capítulo atualizado para os leitores."); window.redesenhar(); }]);
        itens.push(["Tirar do ar", async () => { if (!(await confirmar("Tirar do ar?", `“${c.titulo}” some do catálogo. O texto continua com você.`, "Tirar do ar"))) return; await despublicarCapitulo(id, c); toast("Capítulo tirado do ar."); window.redesenhar(); }]);
      }
    } else itens.push(["Publicar…", () => ir(`/publicar/${id}`)]);
    itens.push([c.rascunho ? "Marcar como concluído" : "Voltar para rascunho", async () => { await salvarCapitulo(id, c.id, { rascunho: !c.rascunho }); window.redesenhar(); }]);
  }
  itens.push([lido ? "Marcar como não lido" : "Marcar como lido", async () => { await salvarCapitulo(id, c.id, { progresso: lido ? 0 : 1 }); window.redesenhar(); }]);
  if (pode) {
    itens.push(["Resumo (“Anteriormente…”)", async () => {
      const d = dialogo(`<h3 style="margin-top:0">Resumo do capítulo</h3><p class="suave peq">2 a 4 frases. Aparece como “Anteriormente…” no começo do próximo capítulo (no app e no PC, também para leitores do catálogo).</p>
        <textarea class="campo" id="r" rows="5">${esc(c.resumo || "")}</textarea><div class="linha-botoes"><button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" id="ok">Salvar</button></div>`);
      $("[data-n]", d).onclick = () => d.close();
      $("#ok", d).onclick = async () => { await salvarCapitulo(id, c.id, { resumo: $("#r", d).value.trim() }); d.close(); toast("Resumo salvo"); };
    }]);
    itens.push(["Etiquetas", async () => { const t = await perguntar("Etiquetas (separe por vírgula)", c.tags || "", "flashback, luta"); if (t === null) return; await salvarCapitulo(id, c.id, { tags: t.split(",").map((s) => s.trim().replace(/^#/, "")).filter(Boolean).join(",") }); window.redesenhar(); }]);
    if (i > 0) itens.push(["Subir", () => trocarOrdem(id, c, caps[i - 1])]);
    if (i < caps.length - 1) itens.push(["Descer", () => trocarOrdem(id, c, caps[i + 1])]);
    itens.push(null, ["Excluir", async () => {
      if (!(await confirmar("Excluir capítulo?", `“${c.titulo}” será apagado aqui e no celular.`, "Excluir", true))) return;
      if (c.publicadoEm > 0 && o.publicada) await despublicarCapitulo(id, c);
      await apagarItem("capitulos", id, c.id); toast("Capítulo excluído"); window.redesenhar();
    }, true]);
  }
  menu(ancora, itens);
}
async function trocarOrdem(id, a, b) {
  await salvarCapitulo(id, a.id, { ordem: b.ordem }); await salvarCapitulo(id, b.id, { ordem: a.ordem });
  window.redesenhar();
}

function menuObra(id, ancora) {
  const o = E.obras.get(id); const pode = o.minha && !o.trancada;
  const itens = [];
  if (pode) {
    itens.push(["✨ Estúdio de IA", () => ir(`/ia/${id}`)]);
    itens.push(["📖 Bíblia viva (furos e mapa)", () => ir(`/biblia/${id}`)]);
    if (o.publicada) itens.push(["📊 Leitores", () => ir(`/leitores/${id}`)]);
    itens.push(["Editar título, sinopse e status", () => editarDetalhes(id)]);
    itens.push(["Trocar capa", () => trocarImagem(id, "capa")], ["Trocar banner", () => trocarImagem(id, "banner")]);
    itens.push([o.publicada ? "Publicação" : "Publicar no catálogo", () => ir(`/publicar/${id}`)]);
    itens.push(["Coautores e revisores", () => ir(`/colaboracao/${id}`)]);
    itens.push(["Enfeite das cenas…", () => {
      const d = dialogo(`<h3 style="margin-top:0">Enfeite das trocas de cena</h3><p class="suave peq">Aparece entre as cenas (* * *) no leitor, para você e para quem lê.</p>
        <div class="enfeites">${Object.entries(ENFEITES).map(([k, n]) => `<button class="enfeite-op ${(o.enfeite || "LOSANGO") === k ? "on" : ""}" data-enf="${k}"><div class="sep">${enfeiteHTML(k)}</div><span>${n}</span></button>`).join("")}</div>`);
      $$("[data-enf]", d).forEach((b) => (b.onclick = async () => { await salvarObra(id, { enfeite: b.dataset.enf }); d.close(); toast("Enfeite trocado"); if (o.publicada) publicarObra(id); }));
    }]);
    itens.push(["Baixar como TXT", () => exportarTXT(id)]);
  } else {
    for (const s of ["LENDO", "QUERO", "CONCLUIDA"]) if (o.status !== s) itens.push([`Mover para “${STATUS_NOME[s]}”`, async () => { await salvarObra(id, { status: s }); window.redesenhar(); }]);
    if (o.exportavel !== false) itens.push(["Baixar como TXT", () => exportarTXT(id)]);
  }
  itens.push(["Coleção…", async () => { const t = await perguntar("Coleção (deixe vazio para tirar)", o.colecao || "", "Ex.: Favoritas"); if (t === null) return; await salvarObra(id, { colecao: t.trim() }); toast("Coleção salva"); }]);
  itens.push(null, [pode ? "Excluir obra" : "Remover da biblioteca", async () => {
    if (!(await confirmar(pode ? "Excluir obra?" : "Remover da biblioteca?", pode ? `“${o.titulo}” e todos os capítulos serão apagados aqui e no celular.${o.publicada ? " Ela também sai do catálogo." : ""}` : "Ela sai da sua biblioteca (você pode seguir de novo depois).", pode ? "Excluir" : "Remover", true))) return;
    if (pode && o.publicada) await despublicarObra(id);
    await apagarObra(id); toast(pode ? "Obra excluída" : "Removida da biblioteca"); ir("/");
  }, true]);
  menu(ancora, itens);
}

async function trocarImagem(id, qual) {
  const f = await escolherArquivo(); if (!f) return;
  toast("Enviando imagem…");
  const bytes = await comprimirArquivo(f, qual === "capa" ? 900 : 1400, .85);
  const h = await enviarImagemMinha(`${id}-${qual}`, bytes);
  await salvarObra(id, qual === "capa" ? { capaHash: h, capaX: .5, capaY: .5, capaZoom: 1 } : { bannerHash: h, bannerX: .5, bannerY: .5, bannerZoom: 1 });
  toast(qual === "capa" ? "Capa trocada" : "Banner trocado"); window.redesenhar();
}

function editarDetalhes(id) {
  const o = E.obras.get(id);
  const d = dialogo(`<h3 style="margin-top:0">Detalhes da obra</h3>
    <label class="rot">Título</label><input class="campo" id="t" value="${esc(o.titulo)}">
    <label class="rot">Sinopse</label><textarea class="campo" id="s" rows="7">${esc(o.descricao || "")}</textarea>
    <label class="rot">Status</label><div class="chips" id="st">${["ESCREVENDO", "CONCLUIDA"].map((s) => `<span class="chip ${o.status === s ? "on" : ""}" data-s="${s}">${s === "ESCREVENDO" ? "Em andamento" : "Concluída"}</span>`).join("")}</div>
    <div class="linha-botoes"><button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" id="ok">Salvar</button></div>`);
  let st = o.status;
  $$("[data-s]", d).forEach((c) => (c.onclick = () => { st = c.dataset.s; $$("[data-s]", d).forEach((x) => x.classList.toggle("on", x === c)); }));
  $("[data-n]", d).onclick = () => d.close();
  $("#ok", d).onclick = async () => {
    await salvarObra(id, { titulo: $("#t", d).value.trim() || o.titulo, descricao: $("#s", d).value.trim(), status: st, atualizadaEm: agora() });
    d.close(); toast("Salvo"); if (o.publicada) publicarObra(id); window.redesenhar();
  };
}

async function exportarTXT(id) {
  const o = E.obras.get(id); toast("Juntando os capítulos…");
  const caps = await capitulos(id);
  let t = o.titulo + "\n\n";
  for (const c of caps) t += c.titulo + "\n\n" + limparMarcas(await lerTexto(colTextos(), c.id, c.partes)).trim() + "\n\n\n";
  const a = document.createElement("a"); a.href = URL.createObjectURL(new Blob([t], { type: "text/plain;charset=utf-8" }));
  a.download = o.titulo.replace(/[^\p{L}\p{N} _-]/gu, "").trim() + ".txt"; a.click();
}

// ───────── personagens e lugares ─────────
async function telaFichas(id, corpo, pode) {
  const fs = await fichas(id, true);
  const filtro = local.ler("filtroFichas", "TODOS");
  const vis = fs.filter((f) => filtro === "TODOS" || f.tipo === filtro);
  corpo.innerHTML = `<div class="topo" style="margin:0 0 14px"><div class="chips" style="flex:1">${[["TODOS", "Todos"], ["PERSONAGEM", "Personagens"], ["LUGAR", "Lugares"], ["OUTRO", "Outros"]].map(([k, n]) => `<span class="chip ${k === filtro ? "on" : ""}" data-f="${k}">${n} · ${k === "TODOS" ? fs.length : fs.filter((f) => f.tipo === k).length}</span>`).join("")}</div>
      ${pode ? `<button class="btn2 peq" id="semficha">Procurar nomes sem ficha</button><button class="btn peq" id="nova">+ Nova ficha</button>` : ""}</div>
    ${vis.length ? `<div class="fichas">${vis.map((f) => `<div class="ficha vidro" data-fi="${f.id}"><span class="av ficha-av" data-foto="${f.imagemHash ? f.id : ""}">${f.tipo === "LUGAR" ? "⌂" : esc(f.nome.slice(0, 1).toUpperCase())}</span>
      <div style="min-width:0"><div class="nm">${esc(f.nome)}</div><div class="suave peq">${TIPO_NOME[f.tipo] || ""}${f.idade ? " · " + esc(f.idade) : ""}${f.apelidos ? " · " + esc(f.apelidos) : ""}</div>
      <div class="desc">${esc(f.descricao || "Sem descrição ainda.")}</div></div></div>`).join("")}</div>`
      : `<div class="card vazio"><b>Nenhuma ficha.</b><span class="suave">Escreva <b>@Nome, descrição</b> no texto ou crie aqui.</span></div>`}`;
  $$("[data-f]", corpo).forEach((c) => (c.onclick = () => { local.gravar("filtroFichas", c.dataset.f); telaFichas(id, corpo, pode); }));
  $$("[data-fi]", corpo).forEach((c) => (c.onclick = () => (pode ? editarFicha(id, fs.find((f) => f.id === c.dataset.fi), fs) : verFicha(fs.find((f) => f.id === c.dataset.fi)))));
  for (const a of $$("[data-foto]", corpo)) if (a.dataset.foto) { const u = await imagemMinha(a.dataset.foto + "-foto"); if (u) a.innerHTML = `<img src="${u}">`; }
  if (pode) {
    $("#nova").onclick = () => editarFicha(id, null, fs);
    $("#semficha").onclick = () => nomesSemFicha(id, fs);
  }
}

export async function verFicha(f, fotoUrl) {
  const u = fotoUrl ?? (f.imagemHash && E.usuario ? await imagemMinha(f.id + "-foto") : null);
  const campos = [["Aparência", f.aparencia], ["Poder", f.poder], ["Relações", f.relacoes]].filter(([, v]) => v);
  dialogo(`<div class="ficha-ver">${u ? `<div class="ficha-fundo" style="background-image:url(${u})"></div>` : ""}
    <div style="display:flex;gap:16px;align-items:center;position:relative"><span class="av" style="width:84px;height:84px;font-size:32px">${u ? `<img src="${u}">` : f.tipo === "LUGAR" ? "⌂" : esc(f.nome.slice(0, 1))}</span>
      <div><div style="font-size:28px;font-weight:400">${esc(f.nome)}</div><div class="chips" style="margin-top:8px"><span class="etiqueta">${TIPO_NOME[f.tipo] || "Personagem"}</span>${f.idade ? `<span class="etiqueta">${esc(f.idade)}</span>` : ""}${f.faccao ? `<span class="etiqueta">${esc(f.faccao)}</span>` : ""}</div></div></div>
    ${f.apelidos ? `<p class="suave peq">também chamado de ${esc(f.apelidos)}</p>` : ""}
    ${f.descricao ? `<p style="line-height:1.6">${esc(f.descricao)}</p>` : `<p class="suave">Sem descrição ainda.</p>`}
    ${campos.map(([r, v]) => `<div class="rotulo" style="color:var(--suave);margin-top:12px">${r}</div><div>${esc(v)}</div>`).join("")}</div>`);
}

function editarFicha(id, f, todas) {
  const x = { ...FICHA_VAZIA, ...(f || {}) };
  const d = dialogo(`<h3 style="margin-top:0">${f ? "Ficha" : "Nova ficha"}</h3>
    <div style="display:flex;gap:14px;align-items:center;margin-bottom:10px"><span class="av" id="foto" style="width:72px;height:72px;font-size:26px;cursor:pointer" title="Trocar foto">${esc((x.nome || "?").slice(0, 1))}</span>
      <div class="chips" id="tp">${Object.entries(TIPO_NOME).map(([k, n]) => `<span class="chip ${x.tipo === k ? "on" : ""}" data-t="${k}">${n}</span>`).join("")}</div></div>
    <div class="grade-campos">
      <div><label class="rot">Nome</label><input class="campo" id="nome" value="${esc(x.nome)}"></div>
      <div><label class="rot">Apelidos (vírgula)</label><input class="campo" id="apelidos" value="${esc(x.apelidos)}"></div>
      <div><label class="rot">Idade</label><input class="campo" id="idade" value="${esc(x.idade)}"></div>
      <div><label class="rot">Facção / clã</label><input class="campo" id="faccao" value="${esc(x.faccao)}"></div>
    </div>
    <label class="rot">Descrição</label><textarea class="campo" id="descricao" rows="4">${esc(x.descricao)}</textarea>
    <label class="rot">Aparência</label><textarea class="campo" id="aparencia" rows="2">${esc(x.aparencia)}</textarea>
    <label class="rot">Poder / habilidades</label><textarea class="campo" id="poder" rows="2">${esc(x.poder)}</textarea>
    <label class="rot">Relações</label><textarea class="campo" id="relacoes" rows="2">${esc(x.relacoes)}</textarea>
    <div class="linha-botoes">${f ? `<button class="btn2 peq" id="apagar" style="margin-right:auto;color:var(--erro)">Excluir</button>` : ""}<button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" id="ok">Salvar</button></div>`, true);
  let tipo = x.tipo, novaFoto = null;
  if (f?.imagemHash) imagemMinha(f.id + "-foto").then((u) => { if (u) $("#foto", d).innerHTML = `<img src="${u}">`; });
  $$("[data-t]", d).forEach((c) => (c.onclick = () => { tipo = c.dataset.t; $$("[data-t]", d).forEach((y) => y.classList.toggle("on", y === c)); }));
  $("#foto", d).onclick = async () => { const a = await escolherArquivo(); if (!a) return; novaFoto = await comprimirArquivo(a, 500, .85); $("#foto", d).innerHTML = `<img src="${URL.createObjectURL(new Blob([novaFoto]))}">`; };
  $("[data-n]", d).onclick = () => d.close();
  const ap = $("#apagar", d); if (ap) ap.onclick = async () => { if (!(await confirmar("Excluir ficha?", `A ficha de ${f.nome} será apagada.`, "Excluir", true))) return; await apagarItem("fichas", id, f.id); d.close(); window.redesenhar(); };
  $("#ok", d).onclick = async () => {
    const nome = $("#nome", d).value.trim(); if (!nome) { toast("Escreva o nome"); return; }
    if (!f && todas.some((y) => y.nome.toLowerCase() === nome.toLowerCase())) { toast("Já existe uma ficha com esse nome"); return; }
    const dados = { tipo }; for (const k of ["nome", "apelidos", "idade", "faccao", "descricao", "aparencia", "poder", "relacoes"]) dados[k] = $("#" + k, d).value.trim();
    const fid = f?.id || novoId();
    if (novaFoto) dados.imagemHash = await enviarImagemMinha(fid + "-foto", novaFoto);
    await salvarFicha(id, fid, f ? { ...f, ...dados } : { ...dados, criadaEm: agora() });
    d.close(); toast("Ficha salva"); window.redesenhar();
  };
}

const COMUNS = new Set(["deus", "senhor", "senhora", "mestre", "rei", "rainha", "príncipe", "princesa", "dona", "dom", "não", "sim", "então"]);
async function nomesSemFicha(id, fs) {
  toast("Lendo os capítulos…");
  const caps = await capitulos(id);
  const ja = new Set(); for (const f of fs) { ja.add(f.nome.toLowerCase()); for (const a of (f.apelidos || "").split(",")) if (a.trim()) ja.add(a.trim().toLowerCase()); for (const p of f.nome.split(/\s+/)) ja.add(p.toLowerCase()); }
  const rx = /(?<=[\p{Ll},;:)”]\s|—\s)(\p{Lu}[\p{L}'’]+(?:-[\p{L}'’]+)*(?:\s\p{Lu}[\p{L}'’]+(?:-[\p{L}'’]+)*)?)/gu;
  const cont = new Map();
  for (const c of caps) for (const m of limparMarcas(await lerTexto(colTextos(), c.id, c.partes)).matchAll(rx)) cont.set(m[1], (cont.get(m[1]) || 0) + 1);
  const l = [...cont].filter(([n, k]) => k >= 3 && !ja.has(n.toLowerCase()) && !n.split(" ").some((p) => ja.has(p.toLowerCase())) && !COMUNS.has(n.toLowerCase())).sort((a, b) => b[1] - a[1]).slice(0, 40);
  const d = dialogo(`<h3 style="margin-top:0">Nomes sem ficha</h3><p class="suave peq">Palavras com maiúscula no meio da frase que aparecem 3 vezes ou mais.</p>
    ${l.length ? `<div class="lista-nomes">${l.map(([n, k]) => `<div><b>${esc(n)}</b><span class="suave peq">${k}×</span><button class="btn2 peq" data-p="${esc(n)}" data-t="PERSONAGEM">☺ Pessoa</button><button class="btn2 peq" data-p="${esc(n)}" data-t="LUGAR">⌂ Lugar</button></div>`).join("")}</div>` : `<p>Nenhum nome novo encontrado.</p>`}
    <div class="linha-botoes"><button class="btn peq" data-n>Pronto</button></div>`);
  $$("[data-p]", d).forEach((b) => (b.onclick = async () => { await salvarFicha(id, null, { nome: b.dataset.p, tipo: b.dataset.t }); b.parentElement.remove(); toast("Ficha criada"); }));
  $("[data-n]", d).onclick = () => { d.close(); window.redesenhar(); };
}

// ───────── linha do tempo ─────────
async function telaTempo(id, corpo, caps) {
  const evs = await eventos(id, true);
  corpo.innerHTML = `<div class="topo" style="margin:0 0 14px"><p class="suave peq" style="flex:1;margin:0">Organize o que acontece na história, em ordem.</p><button class="btn peq" id="novo">+ Evento</button></div>
    ${evs.length ? `<div class="linha-tempo">${evs.map((e, i) => { const c = caps.find((x) => x.id === e.cap); return `<div class="evento vidro" data-ev="${e.id}"><span class="ponto"></span><div style="flex:1;min-width:0"><div class="rotulo">${esc(e.quando || "Sem data")}${c ? ` · ${esc(c.titulo)}` : ""}</div><div class="nm">${esc(e.titulo)}</div>${e.descricao ? `<div class="suave peq">${esc(e.descricao)}</div>` : ""}</div>
      ${i > 0 ? `<button class="btn2 peq" data-sobe="${i}">↑</button>` : ""}${i < evs.length - 1 ? `<button class="btn2 peq" data-desce="${i}">↓</button>` : ""}</div>`; }).join("")}</div>`
      : `<div class="card vazio"><b>Linha do tempo vazia.</b><span class="suave">Adicione os acontecimentos importantes.</span></div>`}`;
  const editar = (e) => {
    const x = e || { quando: "", titulo: "", descricao: "", cap: "" };
    const d = dialogo(`<h3 style="margin-top:0">${e ? "Evento" : "Novo evento"}</h3>
      <label class="rot">Quando</label><input class="campo" id="q" value="${esc(x.quando)}" placeholder="Ex.: Ano 3, inverno">
      <label class="rot">O que acontece</label><input class="campo" id="t" value="${esc(x.titulo)}">
      <label class="rot">Detalhes</label><textarea class="campo" id="d" rows="3">${esc(x.descricao)}</textarea>
      <label class="rot">Capítulo</label><select class="campo" id="c"><option value="">—</option>${caps.map((c) => `<option value="${c.id}" ${c.id === x.cap ? "selected" : ""}>${esc(c.titulo)}</option>`).join("")}</select>
      <div class="linha-botoes">${e ? `<button class="btn2 peq" id="ap" style="margin-right:auto;color:var(--erro)">Excluir</button>` : ""}<button class="btn2 peq" data-n>Cancelar</button><button class="btn peq" id="ok">Salvar</button></div>`);
    $("[data-n]", d).onclick = () => d.close();
    const ap = $("#ap", d); if (ap) ap.onclick = async () => { await apagarItem("eventos", id, e.id); d.close(); telaTempo(id, corpo, caps); };
    $("#ok", d).onclick = async () => {
      const t = $("#t", d).value.trim(); if (!t) { toast("Escreva o que acontece"); return; }
      await salvarItem("eventos", id, e?.id || novoId(), { quando: $("#q", d).value.trim(), titulo: t, descricao: $("#d", d).value.trim(), cap: $("#c", d).value, ordem: e ? e.ordem : (evs.length ? Math.max(...evs.map((v) => v.ordem || 0)) + 1 : 1) });
      d.close(); telaTempo(id, corpo, caps);
    };
  };
  $("#novo").onclick = () => editar(null);
  $$("[data-ev]", corpo).forEach((el) => (el.onclick = (ev) => { if (ev.target.closest("button")) return; editar(evs.find((e) => e.id === el.dataset.ev)); }));
  const troca = async (a, b) => { await salvarItem("eventos", id, a.id, { ...a, ordem: b.ordem }); await salvarItem("eventos", id, b.id, { ...b, ordem: a.ordem }); telaTempo(id, corpo, caps); };
  $$("[data-sobe]", corpo).forEach((b) => (b.onclick = () => troca(evs[+b.dataset.sobe], evs[+b.dataset.sobe - 1])));
  $$("[data-desce]", corpo).forEach((b) => (b.onclick = () => troca(evs[+b.dataset.desce], evs[+b.dataset.desce + 1])));
}

// ───────── publicar ─────────
rota("/publicar/(\\w+)", async (id) => {
  const o = E.obras.get(id); if (!o || !o.minha || o.trancada) { ir("/"); return; }
  carregando("escrever");
  const caps = await capitulos(id, true);
  const pub = o.publicada ? await obraPub(id).catch(() => null) : null;
  const falta = pendencias(o);
  const gen = new Set((o.generos || "").split(",").map((s) => s.trim()).filter(Boolean));
  casca("escrever", `<div class="topo"><a class="voltar vidro" href="#/obra/${id}">←</a><h1>Publicar · ${esc(o.titulo)}</h1></div>
    ${pub ? `<div class="numeros" style="margin:0 0 18px"><div><b>${milhar(pub.leituras)}</b><span>leituras</span></div><div><b>${milhar(pub.seguidores)}</b><span>seguidores</span></div><div><b>${pub.numEstrelas ? (pub.somaEstrelas / pub.numEstrelas).toFixed(1).replace(".", ",") : "—"}</b><span>nota (${pub.numEstrelas || 0})</span></div><div><b>${pub.numCaps || 0}</b><span>caps no ar</span></div></div>` : ""}
    ${falta.length ? `<div class="card aviso"><b>Antes de publicar:</b><ul>${falta.map((f) => `<li>${f}</li>`).join("")}</ul><span class="suave peq">Capa e banner: menu ⋯ Opções da obra. Sinopse: Editar título e sinopse.</span></div>` : ""}
    <div class="duas-colunas">
      <div class="card">
        <h3 style="margin-top:0">Como aparece no catálogo</h3>
        <label class="rot">Gêneros (até 4)</label>
        ${Object.entries(GENEROS_GRUPOS).map(([g, l]) => `<div class="rot" style="margin-top:10px">${g}</div><div class="chips">${l.map((x) => `<span class="chip ${gen.has(x) ? "on" : ""}" data-g="${esc(x)}">${esc(x)}</span>`).join("")}</div>`).join("")}
        <label class="rot" style="margin-top:16px">Tags (vírgula)</label><input class="campo" id="tags" value="${esc(o.tagsPub || "")}" placeholder="vingança, regressão, slow burn">
        <label class="rot">Classificação</label><div class="chips" id="cl">${CLASSIFICACOES.map((c) => `<span class="chip ${o.classificacao === c ? "on" : ""}" data-c="${c}">${c === "LIVRE" ? "Livre" : "+" + c}</span>`).join("")}</div>
        <label class="troca" style="margin-top:14px"><input type="checkbox" id="exp" ${o.exportavel !== false ? "checked" : ""}> Leitores podem baixar como arquivo</label>
        <div class="linha-botoes"><button class="btn" id="salvar" ${falta.length ? "disabled" : ""}>${o.publicada ? "Atualizar dados no catálogo" : "Publicar obra"}</button></div>
        ${o.publicada ? `<div class="linha-botoes" style="justify-content:space-between"><a class="suave peq" href="#/pub/${id}">Ver como os leitores veem ›</a><button class="btn2 peq" id="despub" style="color:var(--erro)">Tirar a obra do catálogo</button></div>` : ""}
      </div>
      <div class="card">
        <h3 style="margin-top:0">Capítulos</h3>
        <div class="caps-pub">${caps.map((c) => { const st = c.publicadoEm > agora() ? `◷ ${dataFmt(c.publicadoEm)}` : c.publicadoEm > 0 ? (c.textoHash && c.publicadoHash && c.textoHash !== c.publicadoHash ? "● no ar · mudou desde então" : "● no ar") : c.rascunho ? "rascunho" : "○ não publicado";
          return `<label class="cap-pub"><input type="checkbox" data-cp="${c.id}" ${!c.publicadoEm ? "" : ""}><span style="flex:1">${esc(c.titulo)}</span><span class="suave peq">${st}</span></label>`; }).join("")}</div>
        <div class="linha-botoes" style="flex-wrap:wrap"><input type="datetime-local" class="campo" id="quando" style="max-width:230px"><button class="btn2 peq" id="agendar">Agendar</button><button class="btn peq" id="agora" ${falta.length ? "disabled" : ""}>Publicar agora</button></div>
        <p class="suave peq">Marque os capítulos. Já publicados são atualizados (republicados) com o texto atual.</p>
      </div>
    </div>`);
  $$("[data-g]").forEach((c) => (c.onclick = () => { const g = c.dataset.g; if (gen.has(g)) gen.delete(g); else { if (gen.size >= 4) { toast("Até 4 gêneros"); return; } gen.add(g); } c.classList.toggle("on", gen.has(g)); }));
  let cl = o.classificacao || "LIVRE";
  $$("[data-c]").forEach((c) => (c.onclick = () => { cl = c.dataset.c; $$("[data-c]").forEach((y) => y.classList.toggle("on", y === c)); }));
  const guardar = async () => salvarObra(id, { generos: [...gen].join(","), tagsPub: $("#tags").value.split(",").map((s) => s.trim().replace(/^#/, "")).filter(Boolean).join(","), classificacao: cl, exportavel: $("#exp").checked });
  $("#salvar").onclick = async () => {
    await guardar(); if (pendencias(E.obras.get(id)).length) { toast("Falta: " + pendencias(E.obras.get(id)).join(", ")); return; }
    toast("Publicando…"); try { await publicarObra(id); toast(o.publicada ? "Dados atualizados" : "Obra publicada! Agora publique os capítulos."); } catch (e) { toast("Não deu: " + e.message); } window.redesenhar();
  };
  const escolhidos = () => caps.filter((c) => $(`[data-cp="${c.id}"]`).checked);
  const publicar = async (quando) => {
    const l = escolhidos(); if (!l.length) { toast("Marque pelo menos um capítulo"); return; }
    await guardar();
    toast(quando > agora() ? "Agendando…" : `Publicando ${l.length} capítulo(s)…`);
    try { await publicarCapitulos(id, l, quando); toast(quando > agora() ? "Agendado!" : "Publicado!"); } catch (e) { toast("Não deu: " + e.message); }
    window.redesenhar();
  };
  $("#agora").onclick = () => publicar(agora());
  $("#agendar").onclick = () => { const v = $("#quando").value; if (!v) { toast("Escolha dia e hora"); return; } const t = new Date(v).getTime(); if (t <= agora()) { toast("Escolha um horário no futuro"); return; } publicar(t); };
  const dp = $("#despub"); if (dp) dp.onclick = async () => { if (!(await confirmar("Tirar a obra do catálogo?", "Ela e todos os capítulos somem para os leitores. Seus textos continuam com você.", "Tirar do catálogo", true))) return; await despublicarObra(id); toast("Obra tirada do catálogo"); window.redesenhar(); };
});

// ───────── colaboração (lado do autor) ─────────
rota("/colaboracao/(\\w+)", async (id) => {
  const o = E.obras.get(id); if (!o || !o.minha) { ir("/"); return; }
  carregando("escrever");
  const [ms, rev, sug, caps] = await Promise.all([membros(id), capsRevisao(id).catch(() => []), sugestoes(id), capitulos(id)]);
  const PAPEL = { COAUTOR: "Coautor", REVISOR: "Revisor" };
  casca("escrever", `<div class="topo"><a class="voltar vidro" href="#/obra/${id}">←</a><h1>Colaboração · ${esc(o.titulo)}</h1></div>
    <div class="duas-colunas">
      <div class="card"><h3 style="margin-top:0">Pessoas</h3>
        <p class="suave peq">Gere um código e mande para a pessoa. Ela entra em Colaboração › Entrar com código. Coautores sugerem capítulos; revisores deixam notas. Nada muda seu texto sem você aceitar.</p>
        <div class="linha-botoes" style="justify-content:flex-start"><button class="btn2 peq" data-conv="COAUTOR">+ Convidar coautor</button><button class="btn2 peq" data-conv="REVISOR">+ Convidar revisor</button></div>
        ${ms.length ? ms.map((m) => `<div class="linha-item"><span class="av" style="width:34px;height:34px">${esc((m.nome || "?").slice(0, 1))}</span><div style="flex:1"><b>${esc(m.nome)}</b><div class="suave peq">${PAPEL[m.papel] || m.papel}</div></div><button class="btn2 peq" data-rm="${m.id}">Remover</button></div>`).join("") : `<p class="suave">Ninguém ainda.</p>`}
      </div>
      <div class="card"><h3 style="margin-top:0">Revisão</h3>
        <div class="linha-botoes" style="justify-content:flex-start"><select class="campo" id="caprev" style="max-width:280px">${caps.map((c) => `<option value="${c.id}">${esc(c.titulo)}</option>`).join("")}</select><button class="btn2 peq" id="enviarrev">Enviar para revisão</button></div>
        ${rev.length ? rev.map((c) => `<div class="linha-item"><div style="flex:1"><b>${esc(c.titulo)}</b><div class="suave peq">${c.notas} nota(s)</div></div><a class="btn2 peq" href="#/revisao/${id}/${c.id}">Ver notas</a><button class="btn2 peq" data-tr="${c.id}">Tirar</button></div>`).join("") : `<p class="suave">Nenhum capítulo em revisão.</p>`}
      </div>
    </div>
    <div class="card" style="margin-top:18px"><h3 style="margin-top:0">Sugestões de coautores</h3>
      ${sug.length ? sug.map((s) => `<div class="linha-item"><div style="flex:1"><b>${esc(s.titulo || "Sem título")}</b><div class="suave peq">de ${esc(s.nome)} · ${palavrasFmt((s.texto || "").split(/\s+/).length)} palavras</div></div><button class="btn2 peq" data-ver="${s.id}">Ler</button><button class="btn2 peq" data-desc="${s.id}">Descartar</button><button class="btn peq" data-ac="${s.id}">Aceitar como capítulo</button></div>`).join("") : `<p class="suave">Nenhuma sugestão.</p>`}
    </div>`);
  $$("[data-conv]").forEach((b) => (b.onclick = async () => {
    const cod = await convidar(id, b.dataset.conv);
    const d = dialogo(`<h3 style="margin-top:0">Código de convite</h3><div class="codigo">${cod}</div><p class="suave">Mande este código para a pessoa. Ela abre Colaboração › Entrar com código (no celular ou no PC).</p><div class="linha-botoes"><button class="btn2 peq" id="cp">Copiar</button><button class="btn peq" data-n>Pronto</button></div>`);
    $("#cp", d).onclick = () => { navigator.clipboard.writeText(cod); toast("Copiado"); }; $("[data-n]", d).onclick = () => d.close();
  }));
  $$("[data-rm]").forEach((b) => (b.onclick = async () => { if (await confirmar("Remover pessoa?", "Ela perde o acesso a esta obra.", "Remover", true)) { await removerMembro(id, b.dataset.rm); window.redesenhar(); } }));
  $("#enviarrev").onclick = async () => { const c = caps.find((x) => x.id === $("#caprev").value); if (!c) return; toast("Enviando…"); await enviarRevisao(id, [c]); toast("Enviado para revisão"); window.redesenhar(); };
  $$("[data-tr]").forEach((b) => (b.onclick = async () => { await tirarRevisao(id, b.dataset.tr); window.redesenhar(); }));
  $$("[data-ver]").forEach((b) => (b.onclick = () => { const s = sug.find((x) => x.id === b.dataset.ver); dialogo(`<h3 style="margin-top:0">${esc(s.titulo)}</h3><div class="texto" style="max-height:60vh;overflow:auto;font-size:17px">${esc(s.texto).replace(/\n/g, "<br>")}</div>`, true); }));
  $$("[data-desc]").forEach((b) => (b.onclick = async () => { await descartarSugestao(id, b.dataset.desc); window.redesenhar(); }));
  $$("[data-ac]").forEach((b) => (b.onclick = async () => { const c = await aceitarSugestao(id, sug.find((x) => x.id === b.dataset.ac)); toast("Virou capítulo (rascunho)"); ir(`/editar/${id}/${c}`); }));
});

// ───────── importar TXT ─────────
const RX_CAP = /^\s*(?:(cap[ií]tulo|chapter|cap\.?)\s*[\dIVXLC]+.*|pr[oó]logo.*|ep[ií]logo.*|interl[uú]dio.*|prologue.*|epilogue.*)$/i;
window.importarTXT = async () => {
  const f = await escolherArquivo(".txt,text/plain"); if (!f) return;
  const texto = (await f.text()).replace(/\r\n?/g, "\n");
  const linhas = texto.split("\n"); const caps = []; let atual = null;
  linhas.forEach((l, i) => {
    if (RX_CAP.test(l) && l.trim().length < 90 && (i === 0 || !linhas[i - 1].trim())) { atual = { titulo: l.trim(), linhas: [] }; caps.push(atual); }
    else { if (!atual) { atual = { titulo: "Capítulo 1", linhas: [] }; caps.push(atual); } atual.linhas.push(l); }
  });
  const titulo = await perguntar(`Encontrei ${caps.length} capítulo(s). Título da obra:`, f.name.replace(/\.txt$/i, ""));
  if (titulo === null) return;
  const d = dialogo(`<h3 style="margin-top:0">Importando…</h3><p class="suave" id="pr">Criando a obra</p>`);
  const id = await novaObra(titulo.trim(), "LENDO");
  await salvarObra(id, { minha: false });
  for (let i = 0; i < caps.length; i++) { $("#pr", d).textContent = `Capítulo ${i + 1} de ${caps.length}`; await novoCapitulo(id, caps[i].titulo, caps[i].linhas.join("\n").trim()); }
  for (const c of await capitulos(id, true)) await salvarCapitulo(id, c.id, { rascunho: false });
  d.close(); toast("Importado!"); ir(`/obra/${id}`);
};
