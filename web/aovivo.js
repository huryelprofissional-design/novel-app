// Escrever junto ao vivo (PC) — mesma sala do celular: colabs/{obra}/aovivo/{cap}/pars|gente|chat.
// Regras iguais a app/src/main/java/com/huryel/novely/data/AoVivo.kt.
import {
  E, $, $$, esc, toast, casca, carregando, rota, ir, collection, doc, getDoc, getDocs, setDoc, onSnapshot, query, where, orderBy, limit,
  db, nomeAutor, garantirColab, lerTexto, colTextos, capitulos, salvarTexto, confirmar, limparMarcas, updateDoc, addDoc, deleteDoc,
} from "./nucleo.js";

const CORES = ["#FF6A2B", "#4DA3FF", "#6FCF97", "#C13DFF", "#FFC94A", "#FF6FA8"];
/** Mesma conta do String.hashCode do Kotlin: a pessoa tem a mesma cor no celular e no PC. */
export function corDe(uid) { let h = 0; for (const ch of uid) h = (Math.imul(31, h) + ch.charCodeAt(0)) | 0; return CORES[(h & 0x7fffffff) % CORES.length]; }
const sala = (pid, cid) => doc(db, "colabs", pid, "aovivo", cid);
const sub = (pid, cid, n) => collection(db, "colabs", pid, "aovivo", cid, n);

/** Autor abre (ou reabre) a sala com o texto atual do capítulo. */
export async function abrirSala(obraId, cap) {
  await garantirColab(obraId);
  const ref = sala(obraId, cap.id); const o = E.obras.get(obraId);
  await setDoc(ref, { titulo: cap.titulo, obra: o.titulo, aberta: true, autorUid: E.usuario.uid, em: Date.now() }, { merge: true });
  const ja = await getDocs(query(sub(obraId, cap.id, "pars"), limit(1)));
  if (ja.empty) {
    const texto = await lerTexto(colTextos(), cap.id, cap.partes);
    const pars = texto.split("\n").map((l) => l.trimEnd()).filter((l) => l.trim()); if (!pars.length) pars.push("");
    for (let i = 0; i < pars.length; i += 50) await Promise.all(pars.slice(i, i + 50).map((t, k) => setDoc(doc(sub(obraId, cap.id, "pars")), { ordem: i + k + 1, texto: t, uid: E.usuario.uid, nome: "", cor: 0, editando: "", ate: 0 })));
  }
}
export async function salasAbertas(pid) {
  try { return (await getDocs(query(collection(db, "colabs", pid, "aovivo"), where("aberta", "==", true)))).docs.map((d) => ({ pid, cid: d.id, ...d.data() })); } catch (_) { return []; }
}
export function htmlSalas(salas, autor) {
  if (!salas.length) return "";
  return `<div class="salas"><div class="rotulo" style="color:#FF4D4D">● Ao vivo agora</div>${salas.map((s) => `<a class="card sala" href="#/aovivo/${s.pid}/${s.cid}"><div style="flex:1"><b>${esc(s.titulo)}</b><div class="suave peq">Clique para entrar e escrever junto</div></div>${autor ? `<button class="btn2 peq" data-fechar="${s.cid}">Fechar sala</button>` : "›"}</a>`).join("")}</div>`;
}
export function ligarSalas(pid) {
  $$("[data-fechar]").forEach((b) => (b.onclick = async (e) => { e.preventDefault(); e.stopPropagation(); await updateDoc(sala(pid, b.dataset.fechar), { aberta: false }); window.redesenhar(); }));
}

// ───────── a sala ─────────
rota("/aovivo/(\\w+)/(\\w+)", async (pid, cid) => {
  carregando("colab");
  const eu = E.usuario.uid, meuNome = nomeAutor(), minhaCor = corDe(eu);
  const info = (await getDoc(sala(pid, cid))).data() || {};
  const souAutor = info.autorUid === eu;
  casca("colab", `<div class="topo"><a class="voltar vidro" href="javascript:history.back()">←</a><h1>${esc(info.titulo || "Escrever junto")}</h1><div id="gente" class="gente"></div>
      <button class="btn2 peq" id="bchat">💬 Chat</button>${souAutor ? `<button class="btn peq" id="trazer">⤓ Trazer para a obra</button>` : ""}</div>
    <p class="suave peq" id="quem" style="margin-top:-6px"></p>
    <div class="vivo"><div class="pars-vivo" id="pars"></div><button class="btn2 peq" id="novo">+ Novo parágrafo</button>
      <p class="suave peq"><i>Clique num parágrafo para escrever nele. A cor na lateral mostra quem escreveu por último. Enter cria um parágrafo novo.</i></p></div>
    <div class="chat-vivo card" id="chat" hidden><b>Chat da sala</b><div class="msgs" id="msgs"></div><div style="display:flex;gap:8px"><input class="campo" id="msg" placeholder="Mensagem"><button class="btn peq" id="enviar">Enviar</button></div></div>`);
  let pars = [], gente = [], foco = null, timerTrava = null, timerGrava = null, vistos = 0, msgs = [];
  const box = $("#pars"); const els = new Map();
  const travado = (p) => p.editando && p.editando !== eu && p.ate > Date.now();
  const refPar = (id) => doc(sub(pid, cid, "pars"), id);
  const travar = (id) => updateDoc(refPar(id), { editando: eu, nomeEditando: meuNome, ate: Date.now() + 25000 }).catch(() => {});
  const soltar = (id) => updateDoc(refPar(id), { editando: "", ate: 0 }).catch(() => {});
  const escrever = (id, texto) => updateDoc(refPar(id), { texto, uid: eu, nome: meuNome, cor: parseInt(minhaCor.slice(1), 16) + 0xFF000000, editando: eu, nomeEditando: meuNome, ate: Date.now() + 25000 }).catch(() => {});
  const presenca = () => setDoc(doc(sub(pid, cid, "gente"), eu), { nome: meuNome, cor: parseInt(minhaCor.slice(1), 16) + 0xFF000000, par: foco || "", em: Date.now() }).catch(() => {});
  const corHexLong = (c) => c ? "#" + (Number(c) & 0xFFFFFF).toString(16).padStart(6, "0") : "var(--borda)";
  const inserir = (antes, depois, texto) => { const ref = doc(sub(pid, cid, "pars")); setDoc(ref, { ordem: depois == null ? antes + 1 : (antes + depois) / 2, texto, uid: eu, nome: meuNome, cor: parseInt(minhaCor.slice(1), 16) + 0xFF000000, editando: eu, nomeEditando: meuNome, ate: Date.now() + 25000 }); return ref.id; };

  function largar() {
    if (!foco) return; const id = foco; const el = els.get(id); const ta = el?.querySelector("textarea"); const txt = ta ? ta.value : "";
    clearInterval(timerTrava); clearTimeout(timerGrava); foco = null;
    if (!txt.trim() && pars.length > 1) deleteDoc(refPar(id)).catch(() => {});
    else { const p = pars.find((x) => x.id === id); if (p && p.texto !== txt) escrever(id, txt).then(() => soltar(id)); else soltar(id); }
    presenca(); desenhar();
  }
  function pegar(id, cursorFim = true) {
    const p = pars.find((x) => x.id === id); if (!p) return;
    if (travado(p)) { toast(`${p.nomeEditando} está escrevendo aqui`); return; }
    if (foco === id) return; largar(); foco = id; travar(id); presenca();
    timerTrava = setInterval(() => travar(id), 10000);
    desenhar();
    const ta = els.get(id)?.querySelector("textarea"); if (ta) { ta.focus(); if (cursorFim) ta.setSelectionRange(ta.value.length, ta.value.length); }
  }
  function linha(p) {
    let el = els.get(p.id);
    if (!el) { el = document.createElement("div"); el.className = "par-vivo"; els.set(p.id, el); el.onclick = () => { if (foco !== p.id) pegar(p.id); }; }
    el.style.setProperty("--cor", corHexLong(p.cor));
    const quem = gente.filter((g) => g.uid !== eu && Date.now() - g.em < 60000 && g.par === p.id);
    if (foco === p.id) {
      if (!el.querySelector("textarea")) {
        el.innerHTML = `<textarea rows="1"></textarea>`; const ta = el.querySelector("textarea"); ta.value = p.texto;
        const crescer = () => { ta.style.height = "auto"; ta.style.height = ta.scrollHeight + "px"; }; crescer();
        ta.oninput = () => { crescer(); clearTimeout(timerGrava); timerGrava = setTimeout(() => escrever(p.id, ta.value), 600); };
        ta.onkeydown = (e) => {
          if (e.key !== "Enter" || e.shiftKey) return; e.preventDefault();
          const a = ta.value.slice(0, ta.selectionStart), b = ta.value.slice(ta.selectionEnd);
          ta.value = a; escrever(p.id, a); soltar(p.id); clearInterval(timerTrava);
          const i = pars.findIndex((x) => x.id === p.id); const novo = inserir(p.ordem, pars[i + 1]?.ordem, b);
          foco = null; setTimeout(() => pegar(novo, false), 60);
        };
        ta.onblur = () => setTimeout(() => { if (foco === p.id && document.activeElement !== ta) largar(); }, 150);
      }
    } else {
      el.innerHTML = `<p>${esc(limparMarcas(p.texto)) || "&nbsp;"}</p>${travado(p) ? `<span class="quem" style="color:${corDe(p.editando)}">✎ ${esc(p.nomeEditando)} está escrevendo…</span>` : quem.length ? `<span class="quem suave">👀 ${esc(quem.map((g) => g.nome).join(", "))}</span>` : ""}`;
      el.classList.toggle("travado", travado(p));
    }
    return el;
  }
  function desenhar() {
    const ids = new Set(pars.map((p) => p.id));
    for (const [id, el] of els) if (!ids.has(id)) { el.remove(); els.delete(id); }
    pars.forEach((p) => box.appendChild(linha(p)));
    const on = gente.filter((g) => g.uid !== eu && Date.now() - g.em < 60000);
    $("#quem").textContent = on.length ? "Aqui agora: " + on.map((g) => g.nome).join(", ") : "Só você por aqui. Mande o convite de coautor para escreverem juntos.";
    $("#gente").innerHTML = gente.filter((g) => Date.now() - g.em < 60000).slice(0, 6).map((g) => `<span class="av-vivo" style="background:${corHexLong(g.cor)}" title="${esc(g.nome)}">${esc((g.nome || "?").slice(0, 1).toUpperCase())}</span>`).join("");
  }
  const parar = [
    onSnapshot(sub(pid, cid, "pars"), (s) => { pars = s.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => a.ordem - b.ordem); desenhar(); }),
    onSnapshot(sub(pid, cid, "gente"), (s) => { gente = s.docs.map((d) => ({ uid: d.id, ...d.data() })); desenhar(); }),
    onSnapshot(query(sub(pid, cid, "chat"), orderBy("em"), limit(300)), (s) => {
      msgs = s.docs.map((d) => d.data()); const caixa = $("#msgs"); if (!caixa) return;
      caixa.innerHTML = msgs.map((m) => `<div class="msg ${m.uid === eu ? "minha" : ""}">${m.uid === eu ? "" : `<b style="color:${corDe(m.uid)}">${esc(m.nome)}</b>`}<span>${esc(m.texto)}</span></div>`).join("") || `<p class="suave peq">Combine a cena por aqui.</p>`;
      caixa.scrollTop = caixa.scrollHeight;
      const b = $("#bchat"); if ($("#chat").hidden && msgs.length > vistos) b.textContent = `💬 Chat · ${msgs.length - vistos}`;
    }),
  ];
  presenca(); const vida = setInterval(presenca, 20000);
  $("#novo").onclick = () => { largar(); const novo = inserir(pars.at(-1)?.ordem || 0, null, ""); setTimeout(() => pegar(novo), 80); };
  $("#bchat").onclick = () => { const c = $("#chat"); c.hidden = !c.hidden; vistos = msgs.length; $("#bchat").textContent = "💬 Chat"; if (!c.hidden) $("#msg").focus(); };
  const mandar = () => { const t = $("#msg").value.trim(); if (!t) return; addDoc(sub(pid, cid, "chat"), { uid: eu, nome: meuNome, texto: t.slice(0, 1000), em: Date.now() }); $("#msg").value = ""; };
  $("#enviar").onclick = mandar; $("#msg").onkeydown = (e) => { if (e.key === "Enter") mandar(); };
  const tb = $("#trazer"); if (tb) tb.onclick = async () => {
    largar();
    if (!(await confirmar("Trazer para a obra?", "O texto desta sala vira o texto do capítulo (a versão de antes fica no histórico do celular).", "Trazer"))) return;
    const s = await getDocs(sub(pid, cid, "pars")); const texto = s.docs.map((d) => d.data()).sort((a, b) => a.ordem - b.ordem).map((p) => p.texto).join("\n\n");
    const cap = (await capitulos(pid, true)).find((c) => c.id === cid); if (!cap) { toast("Capítulo não encontrado na sua obra"); return; }
    await salvarTexto(pid, cap, texto, cap.titulo); toast("Capítulo atualizado com o texto da sala");
  };
  E.saindo = async () => { largar(); parar.forEach((f) => f()); clearInterval(vida); deleteDoc(doc(sub(pid, cid, "gente"), eu)).catch(() => {}); };
});
