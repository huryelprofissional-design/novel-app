// Pronúncia (espeak-ng em WebAssembly): texto em português → fonemas que as vozes do Novely leem.
const BASE = self.location.href.replace(/[^/]*$/, "") + "piper-phonemize/";
let pronto; const prontoP = new Promise((r) => (pronto = r));
self.Module = { locateFile: (p) => BASE + p, onRuntimeInitialized: () => { piperPhonemizeInitialize(); pronto(); } };
importScripts(BASE + "piper-phonemize-wasm-main.js", BASE + "piper-phonemize.js");
onmessage = async (e) => {
  const { id, texto, lingua } = e.data;
  await prontoP;
  try { postMessage({ id, fonemas: (piperPhonemize(texto, lingua || "pt-br") || []).filter((s) => s.length > 0) }); }
  catch (err) { postMessage({ id, erro: String(err) }); }
};
