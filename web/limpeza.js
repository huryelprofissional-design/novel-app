// Limpeza única: os efeitos de escrita saíram do Novely. Os símbolos que já estavam nos textos viram texto comum.
// Mesma regra do app (data/Limpeza.kt). Antes de mudar um capítulo, uma versão fica guardada.
import { lista, capitulos, lerTexto, colTextos, salvarTexto, local } from "./nucleo.js";
import { guardarVersao } from "./extras.js";

const campos = (s) => s.split(/(?<!\|)\|(?!\|)/).map((x) => x.trim()).filter(Boolean);

export function limparTrecho(s) {
  let t = s.replace(/\{\$?[a-zçãéíóúâêô]+[+-]?(?::[a-zçãéíóúâêô]+)?:\s([^{}]*)\}/g, "$1");
  t = t.replace(/!!(.+?)!!/g, "$1").replace(/\^\^(.+?)\^\^/g, "$1").replace(/\(\((.+?)\)\)/g, "$1").replace(/\|\|(.+?)\|\|/g, "$1")
    .replace(/%%(.+?)%%/g, "$1").replace(/~~(.+?)~~/g, "$1").replace(/<<(.+?)>>/g, "$1");
  t = t.replace(/(?<!\{)\{([^{}\n]+)\}(?!\})/g, "$1");
  return t.replace(/\[(SSS|SS|S|A|B|C|D|E|F)\+?\]/g, "$1");
}

/** Uma linha: devolve o texto comum, ou null quando a linha só existia para o efeito. */
export function limparLinha(l0) {
  const l = l0.trim(); if (!l) return l0;
  if (/^~\S{1,22}$/.test(l)) return null;
  const juntar = (x) => campos(x).map((c) => limparTrecho(c.replace(/^\[[xX✓ ]?\]\s*/, ""))).filter(Boolean).join(". ") || null;
  if (/^\[[#$]\S/.test(l) && l.endsWith("]")) { const i = l.indexOf(" "); return i < 0 ? null : juntar(l.slice(i + 1, -1)); }
  if (l.startsWith("[[") && l.endsWith("]]")) return juntar(l.slice(2, -2));
  if (l.startsWith("[!") && l.endsWith("!]")) return limparTrecho(l.slice(2, -2).trim());
  if (l.startsWith("{{") && l.endsWith("}}")) return juntar(l.slice(2, -2));
  if (l.startsWith("[+") && l.endsWith("]")) return campos(l.slice(2, -1))[0] || null;
  if (l.startsWith("::")) return campos(l.slice(2)).join(", ") || null;
  if (/^##(?!#)/.test(l)) return limparTrecho(l.replace(/^#+\s*/, ""));
  if (l.startsWith("@@")) return null;
  const na = l.match(/^(N\.\s?A\.|Nota do autor)\s*:?\s*/i); if (na) return limparTrecho(l.slice(na[0].length));
  for (const p of [">>>", ">>", "> ", "=="]) if (l.startsWith(p)) return limparTrecho(l.slice(p.length).trim());
  return l0.replace(l, limparTrecho(l));
}

export const limparTexto = (t) => t.split("\n").map(limparLinha).filter((x) => x !== null).join("\n");

/** Roda uma vez por obra minha (marca no aparelho). */
export async function limparObrasUmaVez() {
  for (const o of lista().filter((x) => x.minha && !x.trancada)) {
    const marca = "efeitosLimpos/" + o.id; if (local.ler(marca, false)) continue;
    try {
      for (const c of await capitulos(o.id, true)) {
        const t = await lerTexto(colTextos(), c.id, c.partes); const novo = limparTexto(t);
        if (novo !== t) { guardarVersao(c.id, t, "Antes de tirar os efeitos"); await salvarTexto(o.id, c, novo, c.titulo); }
      }
      local.gravar(marca, true);
    } catch (_) { /* sem internet: tenta na próxima vez */ }
  }
}
