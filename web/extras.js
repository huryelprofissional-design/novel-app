// Recursos que vieram do app Android para o PC: painel de efeitos com prévia, remover efeito, contagem completa,
// versões com nome e rascunho de emergência no editor, Lixeira (capítulos, fichas e obras), aparelhos conectados
// e os números de escrita da semana e do mês.
import {
  E, $, $$, esc, toast, casca, carregando, rota, ir, local, dialogo, confirmar, agora, auth, signOut,
  colObras, colItens, getDocs, getDoc, setDoc, doc, query, where, serverTimestamp, db, collection, contarPalavras,
} from "./nucleo.js";
import { blocoFx, inlineFx, ligarFx, CORES_FX, campos } from "./fx.js";

// ───────── efeitos: escrever, tirar e avisar ─────────
const RX_FX_INLINE = /\{[a-zçãéíóúâêô]+[+-]?(?::[a-zçãéíóúâêô]+)?:\s([^}]*)\}/g;
/** Tira os símbolos de efeito de um trecho (o texto fica). Mesma regra do app. */
export function semEfeitoNoTrecho(s) {
  let t = s.replace(RX_FX_INLINE, "$1");
  for (const m of ["!!", "^^", "((", "))", "||", "%%", "~~"]) t = t.split(m).join("");
  t = t.replace(/<<(.*?)>>/g, "$1").replace(/(?<!\{)\{([^{}]+)\}(?!\})/g, "$1");
  return t;
}
/** "Remover efeito" de uma linha inteira (bloco vira parágrafo comum). */
export function semEfeito(linha) {
  const l = linha.trim();
  const juntar = (x) => campos(x).map((c) => semEfeitoNoTrecho(c.replace(/^\[x\]|^\[ \]/, "").trim())).filter(Boolean).join(". ");
  if (l.startsWith("[#") && l.endsWith("]")) { const corpo = l.slice(2, -1); const i = corpo.indexOf(" "); return i < 0 ? "" : juntar(corpo.slice(i + 1)); }
  if (l.startsWith("[[") && l.endsWith("]]")) return juntar(l.slice(2, -2));
  if (l.startsWith("[!") && l.endsWith("!]")) return semEfeitoNoTrecho(l.slice(2, -2).trim());
  if (l.startsWith("{{") && l.endsWith("}}")) return juntar(l.slice(2, -2));
  if (l.startsWith("[+") && l.endsWith("]")) return campos(l.slice(2, -1))[0] || "";
  if (l.startsWith("::")) return campos(l.slice(2)).join(", ");
  if (/^##(?!#)/.test(l)) return semEfeitoNoTrecho(l.replace(/^#+/, "").trim());
  for (const p of [">>>", ">>", "> ", "=="]) if (l.startsWith(p)) return semEfeitoNoTrecho(l.slice(p.length).trim());
  return semEfeitoNoTrecho(l);
}
export function escreverEfeito(nome, cor, forca, texto, bloco) {
  const cab = nome + (forca > 0 ? "+" : forca < 0 ? "-" : "") + (cor && !(bloco && cor === "roxo") ? ":" + cor : "");
  return bloco ? `[#${cab} ${texto.trim()}]` : `{${cab}: ${texto}}`;
}
const TRECHO = [["fogo", "Fogo"], ["gelo", "Gelo"], ["neon", "Neon"], ["raio", "Raio"], ["sombra", "Sombra"], ["divino", "Divino"], ["feitico", "Feitiço"], ["runa", "Runas"], ["medo", "Medo"], ["eco", "Eco"], ["sangue", "Sangue"], ["veneno", "Veneno"], ["ouro", "Ouro"], ["coracao", "Coração"], ["quebra", "Quebrando"], ["vento", "Vento"], ["cor", "Só a cor"], ["idioma", "Outro idioma"]];
const PARAGRAFO = [["sistema", "Sistema"], ["perigo", "Perigo"], ["conquista", "Conquista"], ["missao-ok", "Missão ok"], ["missao-falhou", "Missão falhou"], ["aviso", "Aviso"], ["titulo", "Título"], ["alerta-vermelho", "Alerta"], ["terminal", "Terminal"], ["profecia", "Profecia"], ["diario", "Diário"], ["telegrama", "Telegrama"], ["ia", "IA falando"], ["proximo", "Próximo cap."]];
const PISCAM = new Set(["raio", "neon", "medo", "quebra", "vento", "coracao", "perigo", "alerta-vermelho", "terminal"]);
const COR_HEX = { roxo: "#A47CFF", vermelho: "#FF3B4E", azul: "#4FB8FF", verde: "#3DDC84", dourado: "#FFC94A", rosa: "#FF5FB0", branco: "#E9E7F2", sombrio: "#7A6F8F", laranja: "#FF6A2B" };
function alerta(ef, cor, forca, bloco, n) {
  if (forca > 0 && PISCAM.has(ef)) return "Forte e mexendo pode cansar a leitura. Quem pede menos animação no sistema vê a versão parada.";
  if (cor === "sombrio" && !bloco) return "Sombrio fica com pouco contraste no fundo escuro. Prefira roxo ou branco.";
  if (!bloco && n > 280) return "Efeito num trecho longo fica pesado de ler. Para um parágrafo inteiro, use a aba Parágrafo.";
  return "";
}

/** Painel de efeitos com prévia ao vivo (o mesmo desenho do leitor). aoAplicar(texto, bloco). */
export function painelEfeitos(trecho, aoAplicar, verSimbolos) {
  const texto = trecho.trim() || "Você subiu de nível";
  let bloco = trecho.trim().length > 60 || !trecho.trim(), ef = bloco ? "sistema" : "fogo", cor = "roxo", forca = 0, parar = null;
  const d = dialogo(`<div class="painel-fx"><div style="display:flex;justify-content:space-between;align-items:baseline;gap:10px"><h3 style="margin:0">Efeito no trecho</h3><span class="suave peq">“${esc(texto.slice(0, 30))}${texto.length > 30 ? "…" : ""}”</span></div>
    <div class="pfx-previa"><span class="rotulo">Prévia ao vivo</span><div class="texto" id="pfx-p"></div></div>
    <div class="chips" id="pfx-aba"><span class="chip" data-a="0">No trecho</span><span class="chip" data-a="1">Parágrafo</span></div>
    <div class="rotulo">Efeito</div><div class="chips" id="pfx-ef"></div>
    <div class="rotulo">Cor</div><div class="chips" id="pfx-cor">${CORES_FX.map((c) => `<button class="pfx-cor" data-c="${c}" title="${c}" aria-label="${c}" style="background:${COR_HEX[c]}"></button>`).join("")}</div>
    <div class="rotulo">Intensidade</div><div class="chips" id="pfx-f"><span class="chip" data-f="-1">Suave</span><span class="chip" data-f="0">Normal</span><span class="chip" data-f="1">Forte</span></div>
    <div class="pfx-alerta" id="pfx-al" hidden></div>
    <div class="suave peq">No texto fica: <code id="pfx-s"></code></div>
    <div class="linha-botoes" style="justify-content:space-between"><button class="btn2 peq" id="pfx-todos">Ver todos os símbolos</button><span><button class="btn2" id="pfx-x">Cancelar</button> <button class="btn" id="pfx-ok">Aplicar</button></span></div></div>`, true);
  const desenhar = () => {
    $$("#pfx-aba [data-a]", d).forEach((c) => c.classList.toggle("on", +c.dataset.a === (bloco ? 1 : 0)));
    $("#pfx-ef", d).innerHTML = (bloco ? PARAGRAFO : TRECHO).map(([k, n]) => `<span class="chip ${k === ef ? "on" : ""}" data-e="${k}">${n}</span>`).join("");
    $$("[data-e]", d).forEach((c) => (c.onclick = () => { ef = c.dataset.e; desenhar(); }));
    $$("[data-c]", d).forEach((c) => c.classList.toggle("on", c.dataset.c === cor));
    $$("[data-f]", d).forEach((c) => c.classList.toggle("on", +c.dataset.f === forca));
    const s = escreverEfeito(ef, cor, forca, texto, bloco);
    const alvo = $("#pfx-p", d); parar?.();
    alvo.innerHTML = bloco ? (blocoFx(s, (x) => esc(x)) || esc(texto)) : `<p>Ele sentiu: ${inlineFx(esc(s))}</p>`;
    try { parar = ligarFx(alvo); } catch (_) { parar = null; }
    $("#pfx-s", d).textContent = escreverEfeito(ef, cor, forca, texto.length > 18 ? texto.slice(0, 18) + "…" : texto, bloco);
    const a = alerta(ef, cor, forca, bloco, texto.length); $("#pfx-al", d).hidden = !a; $("#pfx-al", d).textContent = "⚠ " + a;
  };
  $$("#pfx-aba [data-a]", d).forEach((c) => (c.onclick = () => { bloco = c.dataset.a === "1"; ef = bloco ? "sistema" : "fogo"; desenhar(); }));
  $$("[data-c]", d).forEach((c) => (c.onclick = () => { cor = c.dataset.c; desenhar(); }));
  $$("[data-f]", d).forEach((c) => (c.onclick = () => { forca = +c.dataset.f; desenhar(); }));
  const fechar = () => { parar?.(); d.close?.(); d.remove?.(); };
  $("#pfx-x", d).onclick = fechar;
  $("#pfx-todos", d).onclick = () => { fechar(); verSimbolos(); };
  $("#pfx-ok", d).onclick = () => { fechar(); aoAplicar(escreverEfeito(ef, cor, forca, trecho.trim() ? trecho : "texto", bloco), bloco); };
  desenhar();
}

// ───────── contagem completa ─────────
export function contagens(t) {
  const pal = contarPalavras(t), n = (x) => x.toLocaleString("pt-BR");
  const min = pal / 230;
  return [["Palavras", n(pal)], ["Caracteres", n(t.length)], ["Sem espaços", n(t.replace(/\s/g, "").length)], ["Parágrafos", n(t.split("\n").filter((l) => l.trim()).length)],
    ["Páginas (~250 palavras)", pal ? (pal / 250).toFixed(1).replace(".", ",") : "0"], ["Tempo de leitura", pal && min < 1 ? "menos de 1 min" : Math.round(min) + " min"]];
}
export function mostrarContagem(t, sel) {
  const linhas = (l) => l.map(([k, v]) => `<div style="display:flex;justify-content:space-between;gap:20px;padding:4px 0"><span class="suave">${k}</span><b>${v}</b></div>`).join("");
  const d = dialogo(`<h3 style="margin-top:0">Contagem</h3>${linhas(contagens(t))}${sel ? `<div class="rotulo" style="margin-top:12px">Trecho selecionado</div>${linhas(contagens(sel).slice(0, 3))}` : ""}<div class="linha-botoes"><button class="btn peq" data-n>Fechar</button></div>`);
  $("[data-n]", d).onclick = () => d.close();
}

// ───────── versões com nome e rascunho de emergência (guardados neste computador) ─────────
const chVer = (cap) => "versoes:" + cap, chRasc = (cap) => "rascunho:" + cap;
export const versoes = (cap) => local.ler(chVer(cap), []);
export function guardarVersao(cap, texto, nome) {
  const l = versoes(cap); l.unshift({ nome: nome || "Versão sem nome", quando: agora(), texto });
  // o navegador tem pouco espaço: até 15 versões por capítulo
  for (let n = 15; n > 0; n--) { try { localStorage.setItem(chVer(cap), JSON.stringify(l.slice(0, n))); return true; } catch (_) {} }
  return false;
}
export const gravarRascunho = (cap, texto) => { try { localStorage.setItem(chRasc(cap), JSON.stringify({ quando: agora(), texto })); } catch (_) {} };
export const lerRascunho = (cap) => local.ler(chRasc(cap), null);
export const apagarRascunho = (cap) => { try { localStorage.removeItem(chRasc(cap)); } catch (_) {} };
export function mostrarVersoes(cap, restaurar) {
  const l = versoes(cap), f = (q) => new Date(q).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
  const d = dialogo(`<h3 style="margin-top:0">Versões guardadas neste computador</h3>${l.length ? l.map((v, i) => `<div class="linha-item"><div style="flex:1"><b>★ ${esc(v.nome)}</b><div class="suave peq">${f(v.quando)} · ${contarPalavras(v.texto).toLocaleString("pt-BR")} palavras</div></div><button class="btn2 peq" data-r="${i}">Restaurar</button></div>`).join("") : `<p class="suave">Nenhuma ainda. Use ★ Versão para guardar uma antes de mexer pesado.</p>`}<p class="suave peq">No celular há também o Histórico automático (a cada 10 min).</p><div class="linha-botoes"><button class="btn peq" data-n>Fechar</button></div>`);
  $$("[data-r]", d).forEach((b) => (b.onclick = async () => { const v = l[+b.dataset.r]; if (!(await confirmar("Restaurar esta versão?", "O texto atual vira uma versão antes, por segurança.", "Restaurar"))) return; d.close(); restaurar(v.texto); }));
  $("[data-n]", d).onclick = () => d.close();
}

// ───────── números de escrita (semana, mês, média, melhor dia) ─────────
const hojeId = () => { const d = new Date(); return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`; };
export function registrarDia(n) {
  if (n <= 0) return; const m = local.ler("escritaDias", {}); const k = hojeId(); m[k] = (m[k] || 0) + n;
  const ks = Object.keys(m).sort().reverse().slice(0, 120); local.gravar("escritaDias", Object.fromEntries(ks.map((x) => [x, m[x]])));
}
export function numerosEscrita() {
  const m = local.ler("escritaDias", {}), id = (d) => `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
  const hoje = new Date(); const desdeSeg = (hoje.getDay() + 6) % 7; let semana = 0;
  for (let i = 0; i <= desdeSeg; i++) { const d = new Date(hoje); d.setDate(hoje.getDate() - i); semana += m[id(d)] || 0; }
  const pre = id(hoje).slice(0, 6); const doMes = Object.entries(m).filter(([k, v]) => k.startsWith(pre) && v > 0);
  const vals = Object.values(m).filter((v) => v > 0); const melhor = Object.entries(m).filter(([, v]) => v > 0).sort((a, b) => b[1] - a[1])[0];
  return { semana, mes: doMes.reduce((s, [, v]) => s + v, 0), dias: doMes.length, media: vals.length ? Math.round(vals.reduce((a, b) => a + b, 0) / vals.length) : 0, melhor };
}
export function numerosHTML() {
  const n = numerosEscrita(), f = (x) => x.toLocaleString("pt-BR");
  return `<div class="numeros" style="margin:0 0 18px"><div><b>${f(n.semana)}</b><span>esta semana</span></div><div><b>${f(n.mes)}</b><span>este mês · ${n.dias} dia(s)</span></div><div><b>${f(n.media)}</b><span>média por dia escrito</span></div><div><b>${n.melhor ? f(n.melhor[1]) : "—"}</b><span>${n.melhor ? `melhor dia (${n.melhor[0].slice(6, 8)}/${n.melhor[0].slice(4, 6)})` : "melhor dia"}</span></div></div>`;
}

// ───────── aparelhos conectados ─────────
const colAp = () => collection(db, "users", E.usuario.uid, "aparelhos");
const meuId = () => { let i = local.ler("aparelhoId", ""); if (!i) { i = Math.random().toString(36).slice(2, 10) + Date.now().toString(36); local.gravar("aparelhoId", i); } return i; };
const nomePC = () => { const u = navigator.userAgent; const nav = /Edg\//.test(u) ? "Edge" : /Chrome\//.test(u) ? "Chrome" : /Firefox\//.test(u) ? "Firefox" : /Safari\//.test(u) ? "Safari" : "Navegador"; const so = /Windows/.test(u) ? "Windows" : /Mac/.test(u) ? "Mac" : /Linux/.test(u) ? "Linux" : "PC"; return `${nav} no ${so}`; };
/** Avisa que este PC está vivo; se outro aparelho pediu, sai da conta. */
export async function registrarAparelho() {
  if (!E.usuario) return;
  const ref = doc(colAp(), meuId());
  try {
    const atual = await getDoc(ref);
    if (atual.exists() && atual.data().sair) { await setDoc(ref, { sair: true, saiu: agora() }, { merge: true }); toast("Este computador foi desconectado pela sua conta."); signOut(auth); return; }
    await setDoc(ref, { nome: nomePC(), build: 0, visto: agora(), sair: false });
  } catch (_) {}
}
export async function listaAparelhos() {
  const eu = meuId();
  return (await getDocs(colAp())).docs.filter((d) => !d.data().sair).map((d) => ({ id: d.id, ...d.data(), esteAqui: d.id === eu })).sort((a, b) => (b.esteAqui - a.esteAqui) || (b.visto || 0) - (a.visto || 0));
}
export const desconectarAparelho = (id) => setDoc(doc(colAp(), id), { sair: true }, { merge: true });
export const sairDesteAparelho = () => desconectarAparelho(meuId()).catch(() => {});
export function aparelhosHTML(l) {
  const quando = (v) => { const m = Math.floor((agora() - (v || 0)) / 60000); return m < 2 ? "agora" : m < 60 ? `há ${m} min` : m < 1440 ? `há ${Math.floor(m / 60)} h` : `há ${Math.floor(m / 1440)} dias`; };
  return l.length ? l.map((a) => `<div class="linha-item"><div style="flex:1"><b>${esc(a.nome || "Aparelho")}${a.esteAqui ? " · este aqui" : ""}</b><div class="suave peq">visto ${quando(a.visto)}${a.build ? ` · build ${a.build}` : ""}</div></div>${a.esteAqui ? "" : `<button class="btn2 peq" data-desc-ap="${a.id}" style="color:var(--erro)">Desconectar</button>`}</div>`).join("") : `<p class="suave peq">Nenhum ainda.</p>`;
}

// ───────── Lixeira (capítulos, fichas e obras apagados nos últimos 30 dias) ─────────
const TRINTA = 30 * 24 * 3600_000;
rota("/lixeira", async () => {
  carregando("perfil");
  const limite = agora() - TRINTA;
  const [obras, itens] = await Promise.all([
    getDocs(query(colObras(), where("apagado", "==", true))).then((s) => s.docs.map((d) => ({ id: d.id, ...d.data() }))).catch(() => []),
    getDocs(query(colItens(), where("apagado", "==", true))).then((s) => s.docs.map((d) => ({ id: d.id, ...d.data() }))).catch(() => []),
  ]);
  const vis = [...obras.filter((o) => (o.alteradoEm || 0) > limite && o.titulo).map((o) => ({ ...o, _tipo: "obras" })),
    ...itens.filter((i) => (i.alteradoEm || 0) > limite && (i.tipo === "capitulos" || i.tipo === "fichas") && i.dados).map((i) => ({ ...i, _tipo: i.tipo }))]
    .sort((a, b) => (b.alteradoEm || 0) - (a.alteradoEm || 0));
  const nomeObra = (id) => E.obras.get(id)?.titulo || obras.find((o) => o.id === id)?.titulo || "obra apagada";
  const f = (q) => new Date(q).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
  casca("perfil", `<div class="topo"><a class="voltar vidro" href="#/ajustes">←</a><h1>Lixeira</h1></div>
    <p class="suave">Capítulos, fichas e obras apagados (aqui ou no celular) ficam 30 dias e podem voltar. Restaurar uma obra traz junto os capítulos e fichas dela.</p>
    <div class="card">${vis.length ? vis.map((x) => {
      const dias = Math.max(0, Math.ceil((x.alteradoEm + TRINTA - agora()) / 86400000));
      const titulo = x._tipo === "obras" ? "📕 " + x.titulo : x._tipo === "fichas" ? "👤 " + (x.dados.nome || "Ficha") : x.dados.titulo || "Capítulo sem título";
      const de = x._tipo === "obras" ? "obra inteira" : x._tipo === "fichas" ? "ficha de " + nomeObra(x.obra) : "capítulo de " + nomeObra(x.obra);
      return `<div class="linha-item"><div style="flex:1"><b>${esc(titulo)}</b><div class="suave peq">${esc(de)} · apagado em ${f(x.alteradoEm)} · some em ${dias} dias</div></div><button class="btn2 peq" data-rest="${x._tipo}|${x.id}">Restaurar</button></div>`;
    }).join("") : `<p class="suave">A lixeira está vazia.</p>`}</div>`);
  $$("[data-rest]").forEach((b) => (b.onclick = async () => {
    const [tipo, id] = b.dataset.rest.split("|"); b.disabled = true;
    const volta = { apagado: false, alteradoEm: agora(), servidorEm: serverTimestamp() };
    try {
      if (tipo === "obras") {
        await setDoc(doc(colObras(), id), volta, { merge: true });
        for (const i of itens.filter((i) => i.obra === id && (i.alteradoEm || 0) > limite)) await setDoc(doc(colItens(), i.id), volta, { merge: true });
      } else {
        const it = itens.find((i) => i.id === id);
        if (it && !E.obras.get(it.obra)) await setDoc(doc(colObras(), it.obra), volta, { merge: true }); // a obra dele também volta
        await setDoc(doc(colItens(), id), volta, { merge: true });
      }
      toast("Restaurado — aparece no celular na próxima sincronização"); window.redesenhar?.();
    } catch (e) { toast("Não deu: " + e.message); b.disabled = false; }
  }));
});
