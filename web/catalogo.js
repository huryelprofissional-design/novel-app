// Catálogo: descobrir, busca, gêneros, página pública da obra, autor, avisos, colaboração (lado do membro), gerador de nomes e painel do dono.
import {
  E, $, $$, esc, raiz, agora, toast, milhar, palavrasFmt, haQuanto, dataFmt, recente, casca, carregando, rota, ir, local, lista, urlBlob, notaTxt,
  catalogo, obraPub, capsPub, imagemPub, seguindo, seguir, minhaNota, avaliar, perfilPub, obrasDoAutor, segueAutor, seguirAutor, GENEROS, GENEROS_GRUPOS,
  corGenero, dialogo, confirmar, perguntar, entrarColab, minhasColabs, sairColab, capsRevisao, sugerir, colPub, getDocs, query, orderBy, limit,
  fichasPub, EMAIL_DONO, collection, db, doc, getDoc,
} from "./nucleo.js";
import { verFicha } from "./obra.js";

const cartaoPub = (o, p) => window.cartaoPub(o, p);

// ───────── gêneros ─────────
const SIMB = { Fantasia: "✧", Murim: "⚔", Romance: "♥", Sistema: "▣", Terror: "☾", "Mistério": "?", "Ação": "✶", Aventura: "➶", "Ficção científica": "◎", "Comédia": "☺", Cultivo: "☯", "Regressão": "↺", "Reencarnação": "∞", Isekai: "⇄", "Histórico": "♜", Drama: "◐", "Slice of life": "☕", Wuxia: "⚔", Xianxia: "☁", Sobrenatural: "✦", "Vilã": "♛", Esportes: "◉" };
const tile = (g) => { const [a, b] = corGenero(g); return `<a class="tile-genero" href="#/genero/${encodeURIComponent(g)}" style="background:linear-gradient(135deg,${a},${b})"><span class="simb">${SIMB[g] || "◆"}</span><b>${esc(g)}</b></a>`; };
window.tilesGenero = (n) => GENEROS.slice(0, n).map(tile).join("");

rota("/generos", async () => {
  casca("descobrir", `<div class="topo"><a class="voltar vidro" href="javascript:history.back()">←</a><h1>Gêneros</h1></div>
    ${Object.entries(GENEROS_GRUPOS).map(([g, l]) => `<div class="sec"><h2>${esc(g)}</h2></div><div class="grade-generos">${l.map(tile).join("")}</div>`).join("")}`);
});
rota("/genero/(.+)", async (g) => {
  const [a, b] = corGenero(g);
  casca("descobrir", `<div class="cab-genero" style="background:linear-gradient(135deg,${a},${b},#000)"><a class="voltar vidro" href="javascript:history.back()">←</a><div><div class="rotulo" style="color:#fff9">GÊNERO</div><h1>${esc(g)}</h1></div></div><div class="carregando" style="height:200px">◆</div>`);
  let l = []; try { l = await catalogo("genero:" + g); } catch (_) {}
  $(".carregando").outerHTML = l.length ? `<div class="grade">${l.map((o) => cartaoPub(o)).join("")}</div>` : `<div class="card vazio"><b>Ainda não tem obra de ${esc(g)}.</b><span class="suave">Escreva a primeira!</span></div>`;
});

// ───────── DESCOBRIR ─────────
rota("/descobrir(?:/(\\w+))?", async (modo = "ALTA") => {
  const nomes = { ALTA: "Em alta", SEMANA: "Ranking da semana", NOVIDADES: "Novidades", CONCLUIDAS: "Concluídas" };
  casca("descobrir", `<div class="topo"><h1>Descobrir</h1><a class="btn2 peq" href="#/buscar">⌕ Buscar</a><a class="btn2 peq" href="#/generos">Gêneros</a></div>
    <div class="abas">${Object.keys(nomes).map((k) => `<a href="#/descobrir/${k}" class="${k === modo ? "on" : ""}">${nomes[k]}</a>`).join("")}</div><div class="carregando" style="height:200px">◆</div>`);
  let l = [], erro = "";
  try { l = await catalogo(modo); } catch (e) { erro = String(e.message || e).includes("permission") ? "O catálogo ainda não está liberado (faltam as Regras do Firestore)." : "Sem conexão com o catálogo."; }
  const ranking = modo === "ALTA" || modo === "SEMANA";
  $(".carregando").outerHTML = erro ? `<p class="erro">${erro}</p>` : l.length ? `<div class="grade" id="grade">${l.map((o, i) => cartaoPub(o, ranking ? i + 1 : 0)).join("")}</div>` : `<div class="card vazio"><b>Nada por aqui ainda.</b></div>`;
});

// ───────── BUSCAR ─────────
rota("/buscar", async () => {
  casca("buscar", `<div class="topo"><h1>Buscar</h1></div><input class="campo vidro grande" id="q" placeholder="Obra, autor, tag ou gênero…" autofocus>
    <div class="chips" style="margin:14px 0">${GENEROS.slice(0, 14).map((g) => `<span class="chip" data-g="${esc(g)}">${esc(g)}</span>`).join("")}</div><div id="res"></div>`);
  const res = $("#res");
  let todos = null;
  const carregar = async () => { if (todos) return todos; const [a, b] = await Promise.all([catalogo("ALTA"), catalogo("NOVIDADES")]); todos = [...new Map([...a, ...b].map((o) => [o.id, o])).values()]; return todos; };
  const buscar = async () => {
    const t = $("#q").value.trim().toLowerCase();
    if (t.length < 2) { res.innerHTML = ""; return; }
    const minhas = lista().filter((o) => o.titulo.toLowerCase().includes(t) || (o.autor || "").toLowerCase().includes(t));
    const pub = (await carregar()).filter((o) => [o.titulo, o.autorNome, ...(o.tags || []), ...(o.generos || [])].join(" ").toLowerCase().includes(t));
    res.innerHTML = `${minhas.length ? `<div class="sec"><h2>Na sua biblioteca</h2></div><div class="grade">${minhas.map((o) => window.cartaoLocal(o, o.autor ? "por " + o.autor : "")).join("")}</div>` : ""}
      <div class="sec"><h2>No catálogo</h2></div>${pub.length ? `<div class="grade">${pub.map((o) => cartaoPub(o)).join("")}</div>` : `<p class="suave">Nada encontrado.</p>`}`;
    const { preencherCapas } = await import("./nucleo.js"); preencherCapas(res);
  };
  let t = null; $("#q").oninput = () => { clearTimeout(t); t = setTimeout(buscar, 250); };
  $$("[data-g]").forEach((c) => (c.onclick = () => ir("/genero/" + encodeURIComponent(c.dataset.g))));
});

// ───────── PÁGINA PÚBLICA DA OBRA ─────────
rota("/pub/(\\w+)", async (pid) => {
  carregando("descobrir");
  const o = await obraPub(pid);
  if (!o) { casca("descobrir", `<div class="card vazio"><b>Esta obra não está mais publicada.</b></div>`); return; }
  const souAutor = o.autorUid === E.usuario.uid;
  if (o.classificacao === "18" && !souAutor && !local.ler("maior18", false)) {
    casca("descobrir", `<div class="card vazio" style="margin-top:60px"><b>Conteúdo adulto (+18)</b><span class="suave">Esta obra foi marcada pelo autor como +18. Continue só se você tiver 18 anos ou mais.</span>
      <div class="linha-botoes" style="justify-content:center"><a class="btn2" href="javascript:history.back()">Voltar</a><button class="btn" id="ok18">Tenho 18 anos ou mais</button></div></div>`);
    $("#ok18").onclick = () => { local.gravar("maior18", true); window.redesenhar(); }; return;
  }
  const [caps, capa, segue, nota, fs, autor] = await Promise.all([capsPub(pid, souAutor), imagemPub(pid, "capa"), seguindo(pid), minhaNota(pid), fichasPub(pid), perfilPub(o.autorUid)]);
  const desc = local.ler("pubDesc", false);
  const ordenados = desc ? [...caps].reverse() : caps;
  const naBib = lista().find((x) => x.publicaId === pid);
  let segueAut = segueAutor(o.autorUid);
  casca("descobrir", `<div class="topo"><a class="voltar vidro" href="javascript:history.back()">←</a><div style="flex:1"></div>${souAutor ? `<a class="btn2 peq" href="#/publicar/${pid}">Gerenciar publicação</a>` : ""}</div>
    <div class="cab-obra"><div class="fundo" style="${capa ? `background-image:url(${capa})` : "background:#3B2A4A"}"></div>
      <div class="frente"><div class="capa">${capa ? `<img src="${capa}">` : `<div class="sol"></div><div class="t">${esc(o.titulo)}</div>`}</div><div style="flex:1;min-width:0">
        <span class="selo">${recente(o.atualizadaEm) ? "NOVO CAP" : o.status === "CONCLUIDA" ? "CONCLUÍDA" : "EM ANDAMENTO"}</span><span class="suave peq" style="margin-left:10px">${esc((o.generos || []).slice(0, 3).join(" · "))}${o.classificacao && o.classificacao !== "LIVRE" ? " · +" + o.classificacao : ""}</span>
        <h1>${esc(o.titulo)}</h1>
        <div class="autor-linha"><a href="#/autor/${esc(o.autorUid)}" class="av">${autor?.foto ? `<img src="${urlBlob(autor.foto)}">` : esc((o.autorNome || "?").slice(0, 1).toUpperCase())}</a><div style="flex:1"><a href="#/autor/${esc(o.autorUid)}"><b>${esc(o.autorNome)}</b></a><div class="suave peq">${milhar(autor?.seguidores || 0)} seguidores</div></div>
          ${souAutor ? "" : `<button class="btn2 peq" id="segaut">${segueAut ? "✓ Seguindo" : "Seguir autor"}</button>`}</div>
        <div class="chips"><span class="pilula">▶ ${milhar(o.leituras)}</span><span class="pilula">♥ ${milhar(o.seguidores)}</span><span class="pilula">${notaTxt(o)}${o.numEstrelas ? ` (${o.numEstrelas})` : ""}</span><span class="pilula">${caps.length} caps</span><span class="pilula">${palavrasFmt(o.palavras)} palavras</span></div>
        <div class="acoes">${caps.length ? `<a class="btn" href="#/publer/${pid}/${(naBib?.ultimoCap && caps.find((c) => c.id === naBib.ultimoCap)?.id) || caps[0].id}">▶ Ler agora</a>` : `<span class="suave">Sem capítulos ainda.</span>`}
          ${souAutor ? "" : `<button class="btn2 ${segue ? "on" : ""}" id="seguir">${segue ? "♥ Seguindo" : "♡ Seguir"}</button>`}</div>
      </div></div></div>
    <div class="obra-pub">
      <div>
        ${o.sinopse ? `<p class="sinopse">${esc(o.sinopse)}</p>` : ""}
        ${(o.tags || []).length ? `<div class="chips" style="margin-bottom:16px">${o.tags.map((t) => `<span class="etiqueta">#${esc(t)}</span>`).join("")}</div>` : ""}
        ${souAutor ? "" : `<div class="estrelas">Sua nota: ${[1, 2, 3, 4, 5].map((n) => `<button data-e="${n}" class="${n <= nota ? "on" : ""}">${n <= nota ? "★" : "☆"}</button>`).join("")}</div>`}
        <div class="sec"><h2>Capítulos</h2><a class="suave peq" href="javascript:void 0" id="ord">⇅ ${desc ? "mais recentes" : "do início"}</a></div>
        <div class="caps">${ordenados.map((c) => `<a class="cap" href="#/publer/${pid}/${c.id}"><span class="n">${caps.indexOf(c) + 1}</span><div style="flex:1;min-width:0"><div class="tt">${esc(c.titulo)}</div><div class="suave peq">${c.publicadoEm > agora() ? "agendado · " + dataFmt(c.publicadoEm) : haQuanto(c.publicadoEm)}${c.atualizadoEm - c.publicadoEm > 3600000 ? " · atualizado " + haQuanto(c.atualizadoEm) : ""}</div></div><span class="suave peq">♥ ${c.curtidas || 0} · 💬 ${c.comentarios || 0}</span></a>`).join("")}</div>
      </div>
    </div>`);
  const sb = $("#seguir"); if (sb) sb.onclick = async () => {
    const sim = !sb.classList.contains("on"); sb.disabled = true;
    try { await seguir(pid, sim, o); sb.classList.toggle("on", sim); sb.textContent = sim ? "♥ Seguindo" : "♡ Seguir"; toast(sim ? "Na sua biblioteca! O celular baixa os capítulos para ler offline." : "Deixou de seguir"); } catch (e) { toast("Não deu: " + e.message); }
    sb.disabled = false;
  };
  const sa = $("#segaut"); if (sa) sa.onclick = async () => { segueAut = !segueAut; sa.textContent = segueAut ? "✓ Seguindo" : "Seguir autor"; try { await seguirAutor(o.autorUid, segueAut); } catch (_) {} };
  $$("[data-e]").forEach((b) => (b.onclick = async () => { const n = +b.dataset.e; $$("[data-e]").forEach((x) => { x.classList.toggle("on", +x.dataset.e <= n); x.textContent = +x.dataset.e <= n ? "★" : "☆"; }); try { await avaliar(pid, n); toast("Obrigado pela nota!"); } catch (_) {} }));
  $("#ord").onclick = () => { local.gravar("pubDesc", !desc); window.redesenhar(); };
  $$("[data-f]").forEach((b) => (b.onclick = () => verFicha(fs.find((f) => f.id === b.dataset.f), null)));
});

// ───────── AUTOR ─────────
rota("/autor/(\\w+)", async (uid) => {
  carregando("descobrir");
  const [p, obras] = await Promise.all([perfilPub(uid), obrasDoAutor(uid).catch(() => [])]);
  const nome = p?.nome || obras[0]?.autorNome || "Autor";
  let segue = segueAutor(uid);
  const generos = Object.entries(obras.flatMap((o) => o.generos || []).reduce((m, g) => ((m[g] = (m[g] || 0) + 1), m), {})).sort((a, b) => b[1] - a[1]).slice(0, 5).map(([g]) => g);
  casca("descobrir", `<div class="perfil-topo">${p?.banner ? `<img src="${urlBlob(p.banner)}">` : p?.foto ? `<img src="${urlBlob(p.foto)}" style="filter:blur(20px);transform:scale(1.2)">` : ""}<a class="voltar vidro" href="javascript:history.back()" style="position:absolute;top:16px;left:16px;z-index:3">←</a></div>
    <div class="perfil-card vidro">
      <div style="display:flex;align-items:center;gap:12px"><span class="av" style="width:88px;height:88px;font-size:34px">${p?.foto ? `<img src="${urlBlob(p.foto)}">` : esc(nome.slice(0, 1).toUpperCase())}</span><div style="flex:1"></div>
        ${uid === E.usuario.uid ? `<a class="btn2 peq" href="#/perfil">Editar meu perfil</a>` : `<button class="btn ${segue ? "" : ""}" id="seg">${segue ? "✓ Seguindo autor" : "+ Seguir autor"}</button>`}</div>
      <div class="rotulo" style="margin-top:16px">Autor</div><div class="nome">${esc(nome)}</div>
      ${p?.bio ? `<div class="suave">${esc(p.bio)}</div>` : ""}
      <div class="numeros"><div><b>${milhar(p?.seguidores || 0)}</b><span>seguidores</span></div><div><b>${obras.length}</b><span>obras</span></div><div><b>${milhar(obras.reduce((s, o) => s + (o.leituras || 0), 0))}</b><span>leituras</span></div></div>
      ${generos.length ? `<div class="chips" style="margin-top:14px">${generos.map((g) => `<a class="etiqueta" href="#/genero/${encodeURIComponent(g)}">#${esc(g.toLowerCase())}</a>`).join("")}</div>` : ""}
      ${uid !== E.usuario.uid ? `<p class="suave peq" style="margin-bottom:0">Ao seguir, você recebe aviso quando sair obra nova.</p>` : ""}
    </div>
    <div class="sec"><h2>Obras</h2></div>${obras.length ? `<div class="grade">${obras.map((o) => cartaoPub(o)).join("")}</div>` : `<p class="suave">Nenhuma obra publicada ainda.</p>`}`);
  const b = $("#seg"); if (b) b.onclick = async () => { segue = !segue; b.textContent = segue ? "✓ Seguindo autor" : "+ Seguir autor"; try { await seguirAutor(uid, segue); } catch (_) {} };
});

// ───────── AVISOS (capítulos novos das obras seguidas e obras novas de autores seguidos) ─────────
window.verificarNovidades = async () => {
  const ult = local.ler("novidadesChecadoEm", 0); if (agora() - ult < 10 * 60000) return;
  local.gravar("novidadesChecadoEm", agora());
  const vistos = local.ler("vistosPub", {}); const novidades = local.ler("novidades", []);
  for (const o of lista().filter((x) => x.publicaId && x.trancada)) {
    try {
      const p = await obraPub(o.publicaId); if (!p) continue;
      if (!vistos[o.publicaId]) vistos[o.publicaId] = p.atualizadaEm;
      else if (p.atualizadaEm > vistos[o.publicaId]) { novidades.unshift({ tipo: "cap", pid: o.publicaId, titulo: p.titulo, em: p.atualizadaEm }); vistos[o.publicaId] = p.atualizadaEm; }
    } catch (_) {}
  }
  const autores = local.ler("autoresSeguidos", []); const desde = local.ler("autoresChecadoEm", agora());
  for (const a of autores) { try { for (const p of await obrasDoAutor(a)) if ((p.publicadaEm || 0) > desde && !novidades.some((n) => n.pid === p.id)) novidades.unshift({ tipo: "obra", pid: p.id, titulo: `${p.autorNome}: ${p.titulo}`, em: p.publicadaEm }); } catch (_) {} }
  local.gravar("autoresChecadoEm", agora()); local.gravar("vistosPub", vistos); local.gravar("novidades", novidades.slice(0, 50));
};
rota("/avisos", async () => {
  await window.verificarNovidades();
  const l = local.ler("novidades", []);
  casca("avisos", `<div class="topo"><h1>Avisos</h1>${l.length ? `<button class="btn2 peq" id="limpar">Limpar</button>` : ""}</div>
    ${l.length ? `<div class="lista-avisos">${l.map((n) => `<a class="aviso-item vidro" href="#/pub/${n.pid}"><span class="selo">${n.tipo === "obra" ? "OBRA NOVA" : "NOVO CAP"}</span><div style="flex:1"><b>${esc(n.titulo)}</b><div class="suave peq">${haQuanto(n.em)}</div></div><span>›</span></a>`).join("")}</div>`
      : `<div class="card vazio"><b>Nenhum aviso.</b><span class="suave">Siga obras e autores para ser avisado quando sair coisa nova.</span></div>`}`);
  const b = $("#limpar"); if (b) b.onclick = () => { local.gravar("novidades", []); window.redesenhar(); };
});

// ───────── COLABORAÇÃO (lado de quem foi convidado) ─────────
rota("/colaborar", async () => {
  const minhas = minhasColabs();
  const autor = lista().filter((o) => o.minha && !o.trancada);
  casca("colab", `<div class="topo"><h1>Colaboração</h1><button class="btn" id="entrar">Entrar com código</button></div>
    <div class="sec"><h2>Obras em que eu ajudo</h2></div>
    ${minhas.length ? minhas.map((m) => `<div class="linha-item card"><div style="flex:1"><b>${esc(m.titulo)}</b><div class="suave peq">${m.papel === "COAUTOR" ? "Coautor" : "Revisor"} · de ${esc(m.autor)}</div></div><a class="btn2 peq" href="#/colab/${m.pid}">Abrir</a><button class="btn2 peq" data-sair="${m.pid}">Sair</button></div>`).join("")
      : `<p class="suave">Quando alguém te mandar um código, toque em Entrar com código.</p>`}
    <div class="sec"><h2>Minhas obras</h2></div><p class="suave peq">Convide coautores e revisores pela página de cada obra (⋯ Opções › Coautores e revisores).</p>
    <div class="chips">${autor.map((o) => `<a class="chip" href="#/colaboracao/${o.id}">${esc(o.titulo)}</a>`).join("")}</div>`);
  $("#entrar").onclick = async () => { const c = await perguntar("Código do convite", "", "Ex.: K7M2PQ"); if (!c) return; const e = await entrarColab(c); if (e) toast(e); else { toast("Pronto! Você entrou."); window.redesenhar(); } };
  $$("[data-sair]").forEach((b) => (b.onclick = async () => { if (await confirmar("Sair da obra?", "Você perde o acesso a ela.", "Sair", true)) { await sairColab(b.dataset.sair); window.redesenhar(); } }));
});
rota("/colab/(\\w+)", async (pid) => {
  const m = minhasColabs().find((x) => x.pid === pid); if (!m) { ir("/colaborar"); return; }
  carregando("colab");
  const caps = await capsRevisao(pid).catch(() => []);
  casca("colab", `<div class="topo"><a class="voltar vidro" href="#/colaborar">←</a><h1>${esc(m.titulo)}</h1><span class="pilula">${m.papel === "COAUTOR" ? "Coautor" : "Revisor"}</span></div>
    <div class="sec"><h2>Capítulos para revisar</h2></div>
    ${caps.length ? caps.map((c) => `<a class="linha-item card" href="#/revisao/${pid}/${c.id}"><div style="flex:1"><b>${esc(c.titulo)}</b><div class="suave peq">enviado ${haQuanto(c.enviadoEm)} · ${c.notas} nota(s)</div></div><span>›</span></a>`).join("") : `<p class="suave">O autor ainda não mandou capítulos.</p>`}
    ${m.papel === "COAUTOR" ? `<div class="sec"><h2>Sugerir um capítulo</h2></div><div class="card"><input class="campo" id="st" placeholder="Título"><textarea class="campo" id="sx" rows="10" placeholder="Escreva o capítulo…" style="margin-top:10px"></textarea><div class="linha-botoes"><button class="btn" id="env">Enviar sugestão</button></div></div>` : ""}`);
  const env = $("#env"); if (env) env.onclick = async () => { const t = $("#sx").value.trim(); if (!t) return; await sugerir(pid, $("#st").value, t); toast("Sugestão enviada ao autor"); $("#st").value = ""; $("#sx").value = ""; };
});

// ───────── GERADOR DE NOMES ─────────
const N = {
  ori: ["ka", "ri", "shin", "yun", "hae", "jin", "mu", "ryu", "seo", "han", "ten", "lin", "xiao", "mei", "wei", "zhen", "kai", "sora", "en", "ga", "yeon", "hwa", "jun", "tae", "ming"],
  oci: ["al", "ric", "el", "dor", "an", "thar", "is", "mar", "ven", "lor", "ae", "wyn", "ca", "rin", "dra", "kel", "os", "the", "ra", "vel", "ion", "bran", "syl", "mor", "eth"],
  br: ["ra", "fa", "el", "lu", "ca", "jo", "ão", "ma", "ri", "ta", "be", "ni", "ce", "lo", "di", "go", "vi", "na", "ru", "be", "ga", "te"],
  nb: ["Ana", "Beatriz", "Camila", "Clara", "Helena", "Isabela", "Júlia", "Larissa", "Letícia", "Luana", "Manuela", "Mariana", "Marina", "Natália", "Rafaela", "Sofia", "Valentina", "Yasmin", "Lívia", "Cecília", "Alice", "Lorena", "Bianca", "Giovana", "Heloísa", "Elisa", "Aurora", "Iara", "Maitê", "Jade", "Arthur", "Bernardo", "Caio", "Daniel", "Davi", "Eduardo", "Enzo", "Felipe", "Gabriel", "Gustavo", "Heitor", "Henrique", "Igor", "João", "Leonardo", "Lucas", "Matheus", "Miguel", "Murilo", "Otávio", "Pedro", "Rafael", "Renan", "Samuel", "Thiago", "Vicente", "Vinícius", "Benício", "Caetano", "Joaquim"],
  sb: ["Albuquerque", "Almeida", "Andrade", "Barbosa", "Barros", "Bezerra", "Braga", "Cardoso", "Carvalho", "Castro", "Cavalcanti", "Correia", "Costa", "Duarte", "Farias", "Fernandes", "Ferraz", "Freitas", "Galvão", "Gomes", "Lacerda", "Lima", "Macedo", "Medeiros", "Monteiro", "Moraes", "Nascimento", "Nogueira", "Pacheco", "Peixoto", "Pereira", "Queiroz", "Ramos", "Rezende", "Ribeiro", "Rocha", "Sampaio", "Siqueira", "Souza", "Tavares", "Teixeira", "Vasconcelos", "Vieira", "Xavier"],
  tg: [[["Espada", "Corte", "Lâmina"], ["da Lua Crescente", "das Nove Nuvens", "do Vento do Sul", "Celestial", "da Flor de Ameixa", "do Rio Gelado", "Sem Sombra", "do Céu Partido", "das Mil Folhas", "do Horizonte"]], [["Palma", "Punho"], ["do Dragão Furioso", "que Parte Montanhas", "do Tigre Branco", "Trovejante", "dos Dez Mil Budas", "do Vazio", "Escarlate", "do Leão Dourado", "do Trovão Silencioso"]], [["Passos", "Dança"], ["da Sombra Errante", "sobre as Nuvens", "do Vento Fantasma", "das Sete Estrelas", "da Garça Branca", "da Névoa"]], [["Método", "Sutra", "Técnica"], ["do Coração Celeste", "das Nove Yang", "da Lua Gélida", "do Corpo Imortal", "do Yin Profundo", "do Sol Nascente", "da Respiração do Dragão", "das Cinco Fases"]], [["Lança", "Arco"], ["do Dragão Ascendente", "que Perfura o Céu", "da Estrela Cadente", "do Relâmpago Negro", "da Fênix"]]],
  tf: ["Corpo de Ferro", "Sino Dourado", "Pele de Jade", "Olho Celestial", "Selo dos Nove Infernos", "Chama do Lótus Negro", "Rugido do Leão", "Mil Mãos de Buda", "Ventos que Cortam Almas"],
  tfo: ["Primeira Forma", "Segunda Forma", "Terceira Forma", "Sétima Forma", "Forma Final", "Estilo Secreto"],
  iq: ["uma ferreira cega", "o filho mais fraco de um clã de espadachins", "uma vilã que acorda dentro do próprio livro", "um entregador que vê a contagem regressiva da vida das pessoas", "o último discípulo de uma seita destruída", "uma princesa prometida ao inimigo", "um boxeador amador de 17 anos", "um mago que perdeu a memória", "uma detetive que fala com objetos", "um soldado que voltou dez anos no tempo", "uma cozinheira do palácio", "um herdeiro rejeitado pelo próprio sistema"], idd: ["o mestre dele foi quem destruiu sua família", "pode reviver o mesmo dia sete vezes", "o sistema que a ajuda tem vontade própria", "o rival é seu irmão perdido", "a profecia fala dela, mas está errada", "a cidade inteira esquece quem ele é a cada lua cheia", "o dragão que todos temem está do lado dele", "o próprio corpo é a chave de um selo antigo", "o melhor amigo é um espião da seita rival", "ela já morreu uma vez nessa história"], im: ["só tem até o fim do inverno", "ninguém pode descobrir o segredo", "precisa vencer um torneio para sobreviver", "cada vez que usa o poder perde uma lembrança", "o único que pode ajudá-lo quer matá-lo", "a pessoa que ama está do outro lado da guerra", "o tempo está acabando para a irmã doente", "precisa fingir ser outra pessoa"],
  lp: ["Pico", "Vale", "Cidade", "Torre", "Floresta", "Abismo", "Ilha", "Templo", "Portão", "Mar", "Planície", "Fortaleza", "Seita", "Reino", "Lago"],
  ls: ["das Cinzas", "do Lótus Negro", "Celestial", "dos Mil Ventos", "Esquecido", "da Lua Partida", "de Jade", "do Dragão Adormecido", "Carmesim", "das Nove Névoas", "Sem Nome", "do Sol Poente", "de Ferro", "das Almas", "Eterno"],
  ta: ["Lâmina", "Punho", "Palma", "Passo", "Espada", "Chama", "Selo", "Olho", "Canção", "Corte", "Escudo", "Garra", "Lança"],
  tb: ["do Trovão Silencioso", "das Mil Sombras", "do Céu Partido", "da Lua Vazia", "do Dragão Ascendente", "Carmesim", "da Neve Eterna", "do Vazio", "da Fênix Negra", "dos Nove Infernos", "da Aurora", "do Vento Cortante", "Celestial"],
  ti: ["o Imortal", "a Sem Coração", "o Errante", "da Espada Quebrada", "a Bruxa Cinzenta", "o Último Herdeiro", "o Demônio Celeste", "a Lâmina Fria"],
};
const sorte = (l) => l[Math.floor(Math.random() * l.length)];
const palavra = (s, a = 2, b = 3) => { const n = a + Math.floor(Math.random() * (b - a + 1)); let w = ""; for (let i = 0; i < n; i++) w += sorte(s); return w[0].toUpperCase() + w.slice(1); };
function gerarNome(t) {
  if (t === "ORIENTE") return palavra(N.ori, 1, 1) + " " + palavra(N.ori, 2, 2);
  if (t === "OCIDENTE") return palavra(N.oci) + (Math.random() < .33 ? " " + palavra(N.oci, 2, 3) : "");
  if (t === "BR") { const a = sorte(N.sb); let b = sorte(N.sb); if (b === a) b = sorte(N.sb); return sorte(N.nb) + " " + a + (Math.random() < .33 && b !== a ? " " + b : ""); }
  if (t === "IDEIA") { const q = sorte(N.iq); return q[0].toUpperCase() + q.slice(1) + " descobre que " + sorte(N.idd) + " — mas " + sorte(N.im) + "."; }
  if (t === "LUGAR") return sorte(N.lp) + " " + sorte(N.ls);
  if (t === "TECNICA") { const [a, b] = sorte(N.tg); const base = Math.random() < .2 ? sorte(N.tf) : sorte(a) + " " + sorte(b); return Math.random() < .33 ? base + ": " + sorte(N.tfo) : base; }
  return palavra(Math.random() < .5 ? N.ori : N.oci) + ", " + sorte(N.ti);
}
rota("/nomes", async () => {
  const tipo = local.ler("tipoNome", "ORIENTE");
  const nomes = [...new Set(Array.from({ length: 16 }, () => gerarNome(tipo)))];
  casca("escrever", `<div class="topo"><a class="voltar vidro" href="#/escrever">←</a><h1>Gerador de nomes</h1><button class="btn" id="outros">↻ Gerar outros</button></div>
    <div class="chips" style="margin-bottom:18px">${[["ORIENTE", "Oriental"], ["OCIDENTE", "Fantasia"], ["BR", "Brasileiro"], ["LUGAR", "Lugares"], ["TECNICA", "Técnicas"], ["TITULO", "Com título"], ["IDEIA", "Ideias de história"]].map(([k, n]) => `<span class="chip ${k === tipo ? "on" : ""}" data-t="${k}">${n}</span>`).join("")}</div>
    <div class="grade-nomes">${nomes.map((n) => `<button class="nome-gerado vidro" data-n="${esc(n)}"><b>${esc(n)}</b><span class="suave peq">copiar</span></button>`).join("")}</div>`);
  $("#outros").onclick = () => window.redesenhar();
  $$("[data-t]").forEach((c) => (c.onclick = () => { local.gravar("tipoNome", c.dataset.t); window.redesenhar(); }));
  $$("[data-n]").forEach((b) => (b.onclick = () => { navigator.clipboard.writeText(b.dataset.n); b.querySelector("span").textContent = "copiado ✓"; }));
});

// ───────── PAINEL DO DONO ─────────
rota("/dono", async () => {
  if ((E.usuario.email || "").toLowerCase() !== EMAIL_DONO) { ir("/"); return; }
  carregando("perfil");
  const obras = (await getDocs(query(colPub(), orderBy("atualizadaEm", "desc"), limit(200)))).docs.map((d) => ({ id: d.id, ...d.data() }));
  let pessoas = []; try { pessoas = (await getDocs(query(collection(db, "perfis"), limit(500)))).docs.map((d) => ({ id: d.id, ...d.data() })); } catch (_) {}
  casca("perfil", `<div class="topo"><a class="voltar vidro" href="#/ajustes">←</a><h1>Painel de dono</h1></div>
    <div class="numeros" style="margin:0 0 18px"><div><b>${obras.length}</b><span>obras no catálogo</span></div><div><b>${pessoas.length}</b><span>perfis</span></div><div><b>${milhar(obras.reduce((s, o) => s + (o.leituras || 0), 0))}</b><span>leituras</span></div></div>
    <div class="sec"><h2>Obras</h2></div>
    ${obras.map((o) => `<div class="linha-item card"><div style="flex:1"><a href="#/pub/${o.id}"><b>${esc(o.titulo)}</b></a><div class="suave peq">${esc(o.autorNome)} · ${o.numCaps || 0} caps · ${milhar(o.leituras)} leituras · ${haQuanto(o.atualizadaEm)}</div></div><button class="btn2 peq" data-rm="${o.id}" style="color:var(--erro)">Remover do catálogo</button></div>`).join("")}
    <div class="sec"><h2>Pessoas</h2></div><div class="chips">${pessoas.map((p) => `<a class="chip" href="#/autor/${p.id}">${esc(p.nome || "sem nome")} · ${p.seguidores || 0}</a>`).join("")}</div>`);
  $$("[data-rm]").forEach((b) => (b.onclick = async () => {
    if (!(await confirmar("Remover do catálogo?", "A obra some para todos os leitores. O autor continua com os textos.", "Remover", true))) return;
    try { await removerComoDono(b.dataset.rm); toast("Removida"); window.redesenhar(); } catch (e) { toast("Não deu: " + e.message); }
  }));
});
async function removerComoDono(pid) {
  const { deleteDoc: apagar } = await import("https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js");
  for (const sub of ["caps", "textos", "arquivos", "fichas"]) for (const d of (await getDocs(collection(db, "publicas", pid, sub))).docs) await apagar(d.ref);
  await apagar(doc(db, "publicas", pid));
}
