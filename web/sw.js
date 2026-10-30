// Guarda o app para abrir sem internet (os dados ficam no cache do próprio Firestore).
const CACHE = "novely-v1";
const BASE = ["./", "index.html", "app.js", "config.js", "estilo.css", "manifest.webmanifest", "icone-192.png", "icone-512.png"];
self.addEventListener("install", (e) => { e.waitUntil(caches.open(CACHE).then((c) => c.addAll(BASE))); self.skipWaiting(); });
self.addEventListener("activate", (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k))))); self.clients.claim(); });
self.addEventListener("fetch", (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== "GET") return;
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
