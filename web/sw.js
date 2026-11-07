// Guarda o app para abrir sem internet (os dados ficam no cache do próprio Firestore).
const CACHE = "novely-v33";
const BASE = ["./", "index.html", "app.js", "nucleo.js", "obra.js", "leitor.js", "vozneural.js", "catalogo.js", "ia.js", "biblia.js", "aovivo.js", "efeitos.js", "fx.js", "extras.js", "config.js", "estilo.css", "manifest.webmanifest", "icone-192.png", "icone-512.png"];
self.addEventListener("install", (e) => { e.waitUntil(caches.open(CACHE).then((c) => c.addAll(BASE))); self.skipWaiting(); });
self.addEventListener("activate", (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE && !k.startsWith("novely-vozes") && !k.startsWith("novely-motor")).map((k) => caches.delete(k))))); self.clients.claim(); });
self.addEventListener("fetch", (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== "GET") return;
  // vozes do Novely: são grandes e já ficam guardadas à parte (vozneural.js)
  if (u.origin === location.origin && u.pathname.includes("/vozes/")) return;
  // motor das vozes (30 MB, só muda com a versão): guarda e não baixa de novo a cada visita
  if (u.origin === location.origin && u.pathname.includes("/voz/") && !u.pathname.endsWith("-worker.js")) {
    e.respondWith(caches.open("novely-motor-sherpa-1.13.8").then((c) => c.match(e.request).then((r) => r || fetch(e.request).then((resp) => { if (resp.ok) c.put(e.request, resp.clone()); return resp; }))));
    return;
  }
  // bibliotecas do Firebase e fontes: cache primeiro
  if (u.hostname.includes("gstatic.com") || u.hostname.includes("fonts.g")) {
    e.respondWith(caches.match(e.request).then((r) => r || fetch(e.request).then((resp) => { const c = resp.clone(); caches.open(CACHE).then((k) => k.put(e.request, c)); return resp; })));
    return;
  }
  // arquivos do site: rede primeiro (pega versão nova), cache se estiver offline
  if (u.origin === location.origin) {
    e.respondWith(fetch(e.request).then((resp) => { const c = resp.clone(); caches.open(CACHE).then((k) => k.put(e.request, c)); return resp; }).catch(() => caches.match(e.request)));
  }
});
