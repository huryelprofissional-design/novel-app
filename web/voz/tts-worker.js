// Vozes do Novely no navegador (sherpa-onnx em WebAssembly): recebe os arquivos do pacote e gera o áudio a partir dos fonemas.
const BASE = self.location.href.replace(/[^/]*$/, "");
let pronto; const prontoP = new Promise((r) => (pronto = r));
self.Module = { locateFile: (p) => BASE + p, onRuntimeInitialized: () => pronto(), print: () => {}, printErr: () => {} };
importScripts(BASE + "sherpa-onnx-wasm-main-tts.js", BASE + "sherpa-onnx-tts.js");
const motores = {};
/** Monta a configuração padrão da biblioteca e troca só o modelo (kokoro ou vits/piper). */
function criar(pacote) {
  const d = "/" + pacote;
  let cfg = null; const Original = OfflineTts;
  OfflineTts = class { constructor(c) { cfg = c; } };
  const tipoAntes = modelType; modelType = 99;
  try { createOfflineTts(self.Module); } finally { OfflineTts = Original; modelType = tipoAntes; }
  const m = cfg.offlineTtsModelConfig; m.debug = 0; m.numThreads = 1;
  if (pacote === "kokoro") Object.assign(m.offlineTtsKokoroModelConfig, { model: d + "/model.int8.onnx", voices: d + "/vozes-novely.bin", tokens: d + "/tokens.txt" });
  else Object.assign(m.offlineTtsVitsModelConfig, { model: d + "/model.onnx", tokens: d + "/tokens.txt" });
  return new Original(cfg, self.Module);
}
onmessage = async (e) => {
  const { id, tipo, pacote } = e.data;
  await prontoP;
  try {
    if (tipo === "carregar") {
      if (!motores[pacote]) {
        try { self.Module.FS_createPath("/", pacote, true, true); } catch (_) {}
        for (const [nome, buf] of Object.entries(e.data.arquivos)) {
          try { self.Module.FS_unlink(`/${pacote}/${nome}`); } catch (_) {}
          self.Module.FS_createDataFile(`/${pacote}`, nome, new Uint8Array(buf), true, false, false);
        }
        motores[pacote] = criar(pacote);
      }
      postMessage({ id, ok: true });
    } else if (tipo === "gerar") {
      const a = motores[pacote].generateWithConfig("", { phonemeCodepoints: e.data.fonemas, sid: e.data.sid || 0, speed: e.data.velocidade || 1 });
      postMessage({ id, amostras: a.samples, taxa: a.sampleRate }, [a.samples.buffer]);
    }
  } catch (err) { postMessage({ id, erro: String(err && err.message || err) }); }
};
