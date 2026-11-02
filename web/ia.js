// Estúdio de IA (PC): monta prompts com o contexto da obra e traz a resposta de volta.
// Mesmo texto de app/src/main/java/com/huryel/novely/data/Prompts.kt — mudar os dois juntos.
import {
  E, $, $$, esc, toast, casca, carregando, rota, ir, capitulos, fichas, eventos, lerTexto, colTextos, limparMarcas, salvarCapitulo, salvarFicha,
  novoCapitulo, salvarTexto, RX_MARCA, dialogo, confirmar,
} from "./nucleo.js";

const TIPOS = [
  ["CONTINUAR", "Próximo capítulo", "✍", "Escreve o capítulo seguinte no seu estilo, com gancho no fim", true, "O que deve acontecer? (opcional)", true],
  ["CENA", "Escrever uma cena", "🎬", "Uma cena específica a partir da sua ideia", true, "Ex.: Seo-jun enfrenta o valentão no vestiário", true],
  ["REVISAR", "Revisar capítulo", "🔍", "Corrige erros e ritmo sem mudar seu estilo", false, "", true],
  ["DIALOGOS", "Melhorar diálogos", "💬", "Falas mais naturais e cada personagem com voz própria", false, "", true],
  ["GANCHO", "Final com gancho", "🪝", "3 versões mais fortes para o fim do capítulo", false, "", true],
  ["REVIRAVOLTAS", "Ideias de reviravolta", "🌀", "5 viradas coerentes com as pistas que já existem", true, "Algum rumo que você quer? (opcional)", true],
  ["PERSONAGEM", "Criar personagens", "☺", "Vem pronto no formato @Nome, descrição", true, "Ex.: um rival da mesma academia", false],
  ["MUNDO", "Lugar, seita ou sistema", "⌂", "Lugares e regras do mundo no formato &Lugar", true, "Ex.: o sistema de cultivo / a capital", false],
  ["TECNICAS", "Técnicas e golpes", "⚔", "10 nomes de técnicas coerentes com o poder da história", true, "Para quem? (opcional)", false],
  ["CONSISTENCIA", "Checar furos", "🧩", "Procura contradições entre fichas, linha do tempo e texto", false, "", true],
  ["RESUMOS", "Resumir capítulos", "📝", "Gera os resumos do “Anteriormente…” de todos os capítulos", false, "", false],
  ["SINOPSE", "Sinopse e título", "📣", "3 sinopses que prendem, 5 títulos e tags", false, "", false],
  ["TITULOS", "Título do capítulo", "🏷", "5 títulos para o capítulo", false, "", true],
].map(([id, nome, icone, desc, pedeIdeia, dica, usaCap]) => ({ id, nome, icone, desc, pedeIdeia, dica, usaCap }));
const TONS = ["Manter o da obra", "Mais sombrio", "Mais leve", "Épico", "Romântico", "Tenso / suspense", "Engraçado"];
const PONTOS = ["Manter o da obra", "1ª pessoa", "3ª pessoa"];
const FORMATO = `REGRAS DE FORMATO (o texto vai direto para o app Novely):
- Cada fala em um parágrafo próprio, começando com travessão (—). Ex.: — Você veio — disse ela.
- Troca de cena: uma linha sozinha com * * *
- Itálico com *assim* e negrito com **assim**. Nada de títulos em markdown além do título do capítulo.
- Personagem NOVO, na primeira vez que aparece: @Nome, descrição curta (ex.: @Kang Min-ho, treinador de quarenta anos com o nariz quebrado duas vezes, …). O app cria a ficha sozinho.
- Lugar NOVO: &Nome, descrição curta (ex.: &Academia Taesik, um porão úmido que cheira a borracha…).
- Nome com palavra minúscula no meio: @[Seo-yeon do clã Baek]. Apelidos entre parênteses: @Lin Feng (Feng, Herdeiro), …
- Personagens e lugares que JÁ existem: escreva só o nome, sem @ nem &.
- Responda só com o texto pedido, sem comentários antes ou depois.`;
const especial = (t) => /^(pr[oó]logo|ep[ií]logo|interl[uú]dio|prologue|epilogue|interlude)/i.test((t || "").trim());
const norm = (s) => (s || "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/\s+/g, " ").trim();

function linhaFicha(f) {
  let s = "• " + f.nome + (f.apelidos ? ` (${f.apelidos})` : "") + " — " + (f.tipo === "LUGAR" ? "lugar" : f.tipo === "OUTRO" ? "outro" : "personagem");
  if (f.idade) s += ", idade: " + f.idade; if (f.faccao) s += ", facção: " + f.faccao;
  if (f.descricao) s += ". " + f.descricao.trim(); if (f.aparencia) s += " Aparência: " + f.aparencia.trim();
  if (f.poder) s += " Poder: " + f.poder.trim(); if (f.relacoes) s += " Relações: " + f.relacoes.trim();
  return s.slice(0, 520);
}
function amostraEstilo(texto) {
  const pars = limparMarcas(texto).split("\n").map((l) => l.trim()).filter((l) => l.length > 120 && !l.startsWith("—") && !l.startsWith("#"));
  if (!pars.length) return "";
  const m = Math.floor(pars.length / 2);
  return [...new Set([pars[0], ...pars.slice(m, m + 2)])].join("\n\n").slice(0, 2600);
}

export function montarPrompt(c, op) {
  const o = c.obra, n = c.caps.filter((x) => !especial(x.titulo)).length;
  const L = [];
  L.push("Você é coautor e editor experiente de webnovels em português do Brasil. Vou te passar o contexto da minha obra.", "");
  L.push("=== OBRA ===", "Título: " + o.titulo);
  if (o.generos) L.push("Gêneros: " + o.generos.replace(/,/g, ", "));
  if (o.classificacao && o.classificacao !== "LIVRE") L.push("Classificação: +" + o.classificacao);
  if (o.descricao) L.push("Sinopse: " + o.descricao.trim());
  L.push("Capítulos até agora: " + c.caps.length, "");
  const estilo = amostraEstilo(c.texto || "");
  if (estilo && ["CONTINUAR", "CENA", "GANCHO", "DIALOGOS", "REVISAR"].includes(op.tipo)) L.push("=== MEU ESTILO (imite o ritmo, o vocabulário e o tamanho dos parágrafos) ===", estilo, "");
  if (c.fichas.length) { L.push("=== PERSONAGENS E LUGARES QUE JÁ EXISTEM ==="); c.fichas.slice(0, 70).forEach((f) => L.push(linhaFicha(f))); L.push(""); }
  if (c.eventos.length) { L.push("=== LINHA DO TEMPO ==="); c.eventos.slice(0, 60).forEach((e) => L.push(`• ${e.quando ? `[${e.quando}] ` : ""}${e.titulo}${e.descricao ? " — " + e.descricao : ""}`)); L.push(""); }
  const res = c.caps.filter((x) => (x.resumo || "").trim());
  if (res.length && op.tipo !== "RESUMOS") { L.push("=== O QUE JÁ ACONTECEU (resumo por capítulo) ==="); res.slice(-60).forEach((x) => L.push(`${x.titulo}: ${x.resumo.trim()}`)); L.push(""); }
  const capT = c.cap?.titulo || "";
  if (!["RESUMOS", "PERSONAGEM", "MUNDO", "TECNICAS", "SINOPSE"].includes(op.tipo) && (c.texto || "").trim()) {
    const t = limparMarcas(c.texto).trim(); const corte = t.length > 30000 ? "[…início cortado…]\n" + t.slice(-30000) : t;
    L.push(op.tipo === "CONTINUAR" ? `=== ÚLTIMO CAPÍTULO: ${capT} ===` : `=== CAPÍTULO: ${capT} ===`, corte, "");
  }
  if (op.tipo === "RESUMOS") {
    L.push("=== CAPÍTULOS PARA RESUMIR ==="); let total = 0;
    for (const [cap, txt] of c.semResumo || []) {
      let t = limparMarcas(txt).trim(); if (t.length > 9000) t = t.slice(0, 4500) + "\n[…]\n" + t.slice(-4500);
      if (total + t.length > 60000) break; total += t.length; L.push(`--- ${cap.titulo} ---`, t, "");
    }
  }
  const tom = op.tom === TONS[0] ? "" : ` Tom: ${op.tom.toLowerCase()}.`;
  const pov = op.ponto === PONTOS[0] ? "" : ` Narre em ${op.ponto}.`;
  const ideia = (op.ideia || "").trim();
  L.push("=== TAREFA ===");
  const T = {
    CONTINUAR: `Escreva o Capítulo ${n + 1}, continuando exatamente de onde o último parou.${ideia ? ` Neste capítulo: ${ideia}.` : " Avance a história de forma coerente com o que já aconteceu."} Tamanho: cerca de ${op.tamanho} palavras.${tom}${pov} Mantenha a personalidade e o jeito de falar de cada personagem. Mostre em vez de explicar. Termine com um gancho que deixe o leitor querendo o próximo. Comece com uma linha só com o título: Capítulo ${n + 1} — (título).`,
    CENA: `Escreva esta cena: ${ideia || "(descreva a cena)"}. Tamanho: cerca de ${op.tamanho} palavras.${tom}${pov} Use os personagens e o mundo acima, com coerência com o capítulo atual. Foque em ação, emoção e detalhes dos sentidos.`,
    REVISAR: "Revise o capítulo acima: ortografia, gramática, pontuação, repetições de palavras, frases confusas e ritmo. NÃO mude o enredo, os nomes nem o meu estilo. Devolva o capítulo inteiro já corrigido. Depois do capítulo, escreva uma linha com === e liste as 5 mudanças mais importantes que você fez.",
    DIALOGOS: "Reescreva o capítulo acima melhorando só os diálogos: falas mais naturais, subtexto, cada personagem com uma voz própria e reconhecível, menos explicação dentro das falas e tags de fala mais variadas. Mantenha o enredo e a narração. Devolva o capítulo inteiro.",
    GANCHO: "Escreva 3 versões diferentes para o final do capítulo acima (os últimos 3 a 6 parágrafos), cada uma com um gancho mais forte: revelação, perigo iminente ou pergunta sem resposta. Separe as versões com * * * e dê um nome curto a cada uma antes.",
    REVIRAVOLTAS: `Proponha 5 reviravoltas para os próximos capítulos, coerentes com tudo acima.${ideia ? ` Direção desejada: ${ideia}.` : ""} Para cada uma: (1) o que acontece; (2) por que faz sentido — quais pistas já existem no texto; (3) em qual capítulo plantar uma pista nova antes da revelação; (4) o impacto em cada personagem principal. Evite clichês óbvios.`,
    PERSONAGEM: `Crie ${op.quantos} personagem(ns) novo(s) que caibam nesta história${ideia ? ": " + ideia : ""}. Dê nomes coerentes com o mundo. Responda SOMENTE com uma linha por personagem, neste formato exato:\n@Nome (apelidos), descrição de 3 a 5 frases com idade, aparência, personalidade, desejo/medo, poder ou habilidade e a relação com personagens que já existem.`,
    MUNDO: `Crie ${ideia || "lugares importantes para a história"}, coerente com o que já existe. Primeiro, uma linha por lugar no formato exato: &Nome, descrição de 2 a 4 frases (aparência, clima, quem manda, por que importa para a trama). Depois, se for um sistema (de poder, cultivo, magia, castas…), explique as regras, níveis e limites em tópicos curtos.`,
    TECNICAS: `Crie 10 técnicas, golpes ou habilidades${ideia ? " para " + ideia : ""}, coerentes com o sistema de poder e o tom da história. Os nomes devem soar bem em português (evite tradução literal estranha). Formato: Nome da técnica — efeito em 1 frase — custo ou limite.`,
    CONSISTENCIA: "Procure furos e contradições entre as fichas, a linha do tempo, os resumos e o capítulo acima: idades, aparência, nomes trocados, poderes que mudam, quem sabe o quê, datas e lugares. Liste cada problema com: o trecho, o que contradiz e a correção sugerida. Se não achar nada grave, diga o que pode confundir o leitor.",
    RESUMOS: "Para cada capítulo acima, escreva um resumo de 2 a 4 frases, no passado, com os acontecimentos que importam para a continuidade (sem opinião). Responda SOMENTE neste formato, uma linha por capítulo, com o título exatamente igual:\nTítulo do capítulo: resumo",
    SINOPSE: "Escreva: (1) 3 opções de sinopse de até 600 caracteres que deixem o leitor curioso sem spoilers, cada uma com um ângulo diferente (personagem, mistério, conflito); (2) 5 opções de título; (3) 8 tags curtas em minúsculas.",
    TITULOS: "Sugira 5 títulos curtos e marcantes para o capítulo acima, sem spoiler do final, cada um com uma frase explicando a ideia.",
  };
  L.push(T[op.tipo]);
  if (["CONTINUAR", "CENA", "REVISAR", "DIALOGOS", "GANCHO", "PERSONAGEM", "MUNDO"].includes(op.tipo)) L.push("", FORMATO);
  return L.join("\n").trim();
}

rota("/ia/(\\w+)(?:/(\\w+))?", async (obraId, capInicial) => {
  const o = E.obras.get(obraId); if (!o || !o.minha) { ir("/"); return; }
  carregando("escrever");
  let caps = await capitulos(obraId, true);
  const [fs, evs] = await Promise.all([fichas(obraId).catch(() => []), eventos(obraId).catch(() => [])]);
  const op = { tipo: "CONTINUAR", ideia: "", tom: TONS[0], tamanho: 3000, ponto: PONTOS[0], quantos: 3, cap: capInicial || [...caps].reverse().find((c) => c.palavras > 0)?.id || caps.at(-1)?.id };
  const textos = new Map();
  const texto = async (id) => { if (!textos.has(id)) { const c = caps.find((x) => x.id === id); textos.set(id, c ? await lerTexto(colTextos(), id, c.partes) : ""); } return textos.get(id); };
  let prompt = "";
  casca("escrever", `<div class="topo"><a class="voltar vidro" href="#/obra/${obraId}">←</a><h1>Estúdio de IA</h1><span class="suave">${esc(o.titulo)}</span></div>
    <p class="suave" style="max-width:820px;margin-top:-6px">Escolha o que pedir. O Novely monta um prompt com tudo da sua obra (estilo, fichas, linha do tempo, resumos e o capítulo) para colar no ChatGPT, Gemini ou Claude. Depois cole a resposta embaixo.</p>
    <div class="ia">
      <div class="ia-tipos">${TIPOS.map((t) => `<button class="ia-tipo vidro" data-tipo="${t.id}"><span>${t.icone}</span><b>${t.nome}</b><small>${t.desc}</small></button>`).join("")}</div>
      <div class="ia-direita">
        <div class="card ia-op" id="op"></div>
        <div class="ia-prompt"><div class="topo-prompt"><span class="rotulo" style="color:var(--suave)">Prompt</span><span class="suave peq" id="tam"></span></div><pre id="pr"></pre></div>
        <div class="linha-botoes" style="justify-content:flex-start;flex-wrap:wrap"><button class="btn" id="copiar">Copiar prompt</button>
          <a class="btn2" href="https://chatgpt.com/" target="_blank" rel="noopener">ChatGPT ↗</a><a class="btn2" href="https://gemini.google.com/" target="_blank" rel="noopener">Gemini ↗</a><a class="btn2" href="https://claude.ai/new" target="_blank" rel="noopener">Claude ↗</a></div>
        <div class="sec"><h2>Colar a resposta</h2></div>
        <textarea class="campo" id="resp" rows="9" placeholder="Cole aqui o que a IA respondeu…"></textarea>
        <div class="linha-botoes" style="justify-content:flex-start;flex-wrap:wrap" id="acoes"></div>
        <p class="suave peq">Dica: personagens novos que a IA escrever com @Nome viram fichas sozinhos quando o celular sincroniza o capítulo.</p>
      </div></div>`);
  const tipo = () => TIPOS.find((t) => t.id === op.tipo);
  const desenharOp = () => {
    const t = tipo();
    $$("[data-tipo]").forEach((b) => b.classList.toggle("on", b.dataset.tipo === op.tipo));
    const chips = (lista, chave, fmt = (x) => x) => `<div class="chips">${lista.map((x) => `<span class="chip ${op[chave] === x ? "on" : ""}" data-${chave}="${x}">${fmt(x)}</span>`).join("")}</div>`;
    $("#op").innerHTML = `<h3 style="margin:0 0 10px">${t.icone} ${t.nome}</h3>
      ${t.usaCap && caps.length ? `<label class="rot">Capítulo</label><select class="campo" id="cap">${caps.map((c) => `<option value="${c.id}" ${c.id === op.cap ? "selected" : ""}>${esc(c.titulo)}</option>`).join("")}</select>` : ""}
      ${t.pedeIdeia ? `<label class="rot">Ideia</label><textarea class="campo" id="ideia" rows="2" placeholder="${esc(t.dica)}">${esc(op.ideia)}</textarea>` : ""}
      ${op.tipo === "CONTINUAR" || op.tipo === "CENA" ? `<label class="rot">Tamanho</label>${chips([1500, 3000, 5000, 8000], "tamanho", (n) => n.toLocaleString("pt-BR") + " palavras")}<label class="rot">Tom</label>${chips(TONS, "tom")}<label class="rot">Narrador</label>${chips(PONTOS, "ponto")}` : ""}
      ${op.tipo === "PERSONAGEM" ? `<label class="rot">Quantos</label>${chips([1, 3, 5], "quantos")}` : ""}`;
    const cap = $("#cap"); if (cap) cap.onchange = (e) => { op.cap = e.target.value; atualizar(); };
    const id = $("#ideia"); if (id) id.oninput = (e) => { op.ideia = e.target.value; clearTimeout(id.t); id.t = setTimeout(atualizar, 300); };
    for (const chave of ["tamanho", "tom", "ponto", "quantos"]) $$(`[data-${chave}]`).forEach((c) => (c.onclick = () => { op[chave] = typeof op[chave] === "number" ? +c.dataset[chave] : c.dataset[chave]; desenharOp(); atualizar(); }));
    desenharAcoes();
  };
  const atualizar = async () => {
    $("#tam").textContent = "montando…";
    const cap = caps.find((c) => c.id === op.cap);
    const semResumo = op.tipo === "RESUMOS" ? await Promise.all(caps.filter((c) => !(c.resumo || "").trim() && c.palavras > 0).slice(0, 40).map(async (c) => [c, await texto(c.id)])) : [];
    prompt = montarPrompt({ obra: o, caps, fichas: fs, eventos: evs, cap, texto: cap ? await texto(cap.id) : "", semResumo }, op);
    $("#pr").textContent = prompt;
    $("#tam").textContent = prompt.length.toLocaleString("pt-BR") + " caracteres" + (prompt.length > 60000 ? " · grande: apps gratuitos podem cortar" : "");
  };
  const desenharAcoes = () => {
    const a = []; const t = op.tipo;
    if (["CONTINUAR", "CENA", "GANCHO"].includes(t)) a.push(`<button class="btn" data-acao="cap">Criar capítulo novo</button>`);
    if (["REVISAR", "DIALOGOS"].includes(t)) a.push(`<button class="btn" data-acao="trocar">Trocar o texto do capítulo</button>`);
    if (["PERSONAGEM", "MUNDO"].includes(t)) a.push(`<button class="btn" data-acao="fichas">Criar fichas</button>`);
    if (t === "RESUMOS") a.push(`<button class="btn" data-acao="resumos">Salvar resumos</button>`);
    a.push(`<button class="btn2" data-acao="colar">📋 Colar da área de transferência</button>`);
    $("#acoes").innerHTML = a.join("");
    $$("[data-acao]").forEach((b) => (b.onclick = () => acao(b.dataset.acao)));
  };
  const acao = async (qual) => {
    const r = $("#resp").value.trim();
    if (qual === "colar") { try { $("#resp").value = await navigator.clipboard.readText(); } catch (_) { toast("Use Ctrl+V na caixa"); } return; }
    if (!r) { toast("Cole a resposta primeiro"); return; }
    if (qual === "cap") {
      const linhas = r.split("\n"); const i = linhas.findIndex((l) => l.trim());
      const primeira = (linhas[i] || "").trim().replace(/^[#*\s]+|[*\s]+$/g, "");
      const ehTitulo = primeira.length < 90 && /^(cap[ií]tulo|cena|pr[oó]logo|ep[ií]logo)\b/i.test(primeira);
      const titulo = ehTitulo ? primeira : op.tipo === "CENA" ? "Cena nova" : `Capítulo ${caps.length + 1}`;
      const corpo = ehTitulo ? linhas.slice(i + 1).join("\n").trim() : r;
      const id = await novoCapitulo(obraId, titulo, corpo);
      toast("Capítulo criado!"); ir(`/editar/${obraId}/${id}`);
    }
    if (qual === "trocar") {
      const c = caps.find((x) => x.id === op.cap); if (!c) return;
      if (!(await confirmar("Trocar o texto?", "O texto atual vai para o histórico de versões no celular (quando ele sincronizar) e dá para voltar.", "Trocar"))) return;
      await salvarTexto(obraId, c, r.split("\n===")[0].trim(), c.titulo); textos.delete(c.id); toast("Capítulo atualizado");
    }
    if (qual === "fichas") {
      const ja = new Set(fs.map((f) => f.nome.toLowerCase())); let n = 0;
      for (const linha of r.split("\n")) for (const m of linha.matchAll(new RegExp(RX_MARCA.source, "gu"))) {
        const nome = (m[2] || m[3] || "").trim(); if (!nome || ja.has(nome.toLowerCase())) continue;
        const desc = linha.slice(m.index + m[0].length).trim();
        await salvarFicha(obraId, null, { nome, tipo: m[1] === "&" ? "LUGAR" : "PERSONAGEM", apelidos: (m[5] || "").split(",").map((s) => s.trim()).filter(Boolean).join(", "), descricao: desc });
        ja.add(nome.toLowerCase()); n++; break;
      }
      toast(n ? `${n} ficha(s) criada(s)!` : "Não achei @Nome ou &Lugar novos na resposta.");
    }
    if (qual === "resumos") {
      let n = 0;
      for (const l of r.split("\n").map((x) => x.trim().replace(/^[-•*\s]+/, "")).filter((x) => x.includes(":"))) {
        const t = norm(l.slice(0, l.indexOf(":")).replace(/\*/g, "")); const res = l.slice(l.indexOf(":") + 1).trim(); if (res.length < 15) continue;
        const c = caps.find((x) => norm(x.titulo) === t) || caps.find((x) => x.titulo.length > 5 && t.includes(norm(x.titulo)));
        if (c) { await salvarCapitulo(obraId, c.id, { resumo: res }); c.resumo = res; n++; }
      }
      toast(n ? `${n} resumo(s) salvos — já aparecem no “Anteriormente…”` : "Não reconheci os títulos. Peça para a IA manter os títulos iguais.");
    }
  };
  $$("[data-tipo]").forEach((b) => (b.onclick = () => { op.tipo = b.dataset.tipo; desenharOp(); atualizar(); }));
  $("#copiar").onclick = async () => { await navigator.clipboard.writeText(prompt); toast("Prompt copiado! Cole no app de IA."); };
  desenharOp(); atualizar();
});
