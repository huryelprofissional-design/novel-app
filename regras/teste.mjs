// Prova, no emulador, que as brechas da auditoria (FB-02, FB-04, FB-05, FB-06) fecharam
// e que o uso normal do app continua passando.  Rodar: npx firebase emulators:exec --only firestore --project demo-novely "node --test teste.mjs"
import { test, before, after } from "node:test";
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { doc, getDoc, getDocs, collection, setDoc, updateDoc, increment, deleteDoc } from "firebase/firestore";

let env;
const DIA = 86400000;
before(async () => {
  env = await initializeTestEnvironment({ projectId: "demo-novely", firestore: { rules: readFileSync("../firestore.rules", "utf8"), host: "127.0.0.1", port: 8080 } });
  await env.withSecurityRulesDisabled(async (c) => {
    const db = c.firestore();
    await setDoc(doc(db, "publicas/p1"), { autorUid: "autor", titulo: "Obra", leituras: 10, leiturasSemana: 3, semanaId: "2026-45", seguidores: 2, somaEstrelas: 8, numEstrelas: 2, desbloqueios: 0 });
    await setDoc(doc(db, "publicas/p1/caps/c1"), { titulo: "Cap", curtidas: 0, comentarios: 0, leituras: 0, reacoes: { fogo: 1 } });
    await setDoc(doc(db, "colabs/o1"), { autorUid: "autor" });
    await setDoc(doc(db, "users/autor/obras/o1"), { titulo: "Privada" });
    await setDoc(doc(db, "users/atacante/obras/o2"), { titulo: "Minha" });
    await setDoc(doc(db, "convites/ABC234"), { pid: "o1", papel: "REVISOR", autorUid: "autor", criadoEm: Date.now() });
    await setDoc(doc(db, "convites/VELHO2"), { pid: "o1", papel: "COAUTOR", autorUid: "autor", criadoEm: Date.now() - 8 * DIA });
    await setDoc(doc(db, "perfis/autor"), { nome: "Autor", seguidores: 5 });
  });
});
after(async () => { await env.cleanup(); });

const como = (uid) => env.authenticatedContext(uid).firestore();
const pub = (db) => doc(db, "publicas/p1");
const cap = (db) => doc(db, "publicas/p1/caps/c1");

test("FB-02: ninguém grava contador com valor qualquer", async () => {
  const a = como("atacante");
  await assertFails(updateDoc(pub(a), { leituras: 999999999 }));
  await assertFails(updateDoc(pub(a), { seguidores: increment(5) }));
  await assertFails(updateDoc(pub(a), { somaEstrelas: increment(100) }));
  await assertFails(updateDoc(pub(a), { numEstrelas: increment(3) }));
  await assertFails(updateDoc(pub(a), { titulo: "hackeado" }));
  await assertFails(updateDoc(cap(a), { curtidas: increment(50) }));
  await assertFails(updateDoc(cap(a), { "reacoes.fogo": increment(10) }));
  await assertFails(updateDoc(cap(a), { "reacoes.xss": increment(1) }));
});

test("FB-02: o uso normal continua funcionando (+1 / -1)", async () => {
  const l = como("leitor");
  await assertSucceeds(updateDoc(pub(l), { leituras: increment(1), leiturasSemana: increment(1) }));
  await assertSucceeds(updateDoc(pub(l), { seguidores: increment(1) }));
  await assertSucceeds(updateDoc(pub(l), { seguidores: increment(-1) }));
  await assertSucceeds(updateDoc(pub(l), { somaEstrelas: increment(4), numEstrelas: increment(1) }));
  await assertSucceeds(updateDoc(pub(l), { leituras: increment(1), leiturasSemana: 1, semanaId: "2026-46" }));
  await assertSucceeds(updateDoc(cap(l), { curtidas: increment(1) }));
  await assertSucceeds(updateDoc(cap(l), { "reacoes.fogo": increment(-1), "reacoes.risada": increment(1) }));
  await assertSucceeds(updateDoc(cap(l), { comentarios: increment(1) }));
});

test("FB-02: zerar a semana sem leitura de verdade é recusado", async () => {
  await assertFails(updateDoc(pub(como("atacante")), { leiturasSemana: 1, semanaId: "9999-1" }));
});

test("FB-02: o autor não infla os próprios números, mas edita a obra", async () => {
  const au = como("autor");
  await assertFails(updateDoc(pub(au), { leituras: increment(100) }));
  await assertSucceeds(updateDoc(pub(au), { titulo: "Novo título", sinopse: "..." }));
  await assertFails(setDoc(doc(au, "publicas/p9"), { autorUid: "autor", titulo: "X", leituras: 5000 }));
  await assertSucceeds(setDoc(doc(au, "publicas/p9"), { autorUid: "autor", titulo: "X", leituras: 0, seguidores: 0, somaEstrelas: 0, numEstrelas: 0 }));
  await assertFails(updateDoc(pub(au), { autorUid: "outro" }));
});

test("FB-04: convites não podem ser listados; pegar pelo código pode", async () => {
  const a = como("atacante");
  await assertFails(getDocs(collection(a, "convites")));
  await assertSucceeds(getDoc(doc(a, "convites/ABC234")));
});

test("FB-04: convite vencido não vale; papel não pode ser forjado", async () => {
  const r = como("revisor");
  await assertFails(setDoc(doc(r, "colabs/o1/membros/revisor"), { papel: "COAUTOR", codigo: "VELHO2", nome: "x", em: Date.now() }));
  await assertFails(setDoc(doc(r, "colabs/o1/membros/revisor"), { papel: "COAUTOR", codigo: "ABC234", nome: "x", em: Date.now() }));
  await assertSucceeds(setDoc(doc(r, "colabs/o1/membros/revisor"), { papel: "REVISOR", codigo: "ABC234", nome: "x", em: Date.now() }));
  await assertSucceeds(deleteDoc(doc(r, "convites/ABC234"))); // uso único: quem entrou apaga o convite
});

test("FB-05: não dá para ocupar a sala de colaboração de obra alheia", async () => {
  const a = como("atacante");
  await assertFails(setDoc(doc(a, "colabs/p1"), { autorUid: "atacante" }));
  await assertSucceeds(setDoc(doc(a, "colabs/o2"), { autorUid: "atacante" }));
  await assertFails(setDoc(doc(a, "convites/ZZZ999"), { pid: "o1", papel: "COAUTOR", autorUid: "atacante", criadoEm: Date.now() }));
});

test("FB-06: comentário só com os campos do app, tamanho e data válidos", async () => {
  const l = como("leitor");
  const col = (n) => doc(l, `publicas/p1/caps/c1/comentarios/${n}`);
  const ok = { uid: "leitor", nome: "Ana", texto: "Que capítulo!", em: Date.now(), bloco: 3, trecho: "..." };
  await assertSucceeds(setDoc(col("c1"), ok));
  await assertFails(setDoc(col("c2"), { ...ok, uid: "outro" }));
  await assertFails(setDoc(col("c3"), { ...ok, lixo: "x".repeat(500000) }));
  await assertFails(setDoc(col("c4"), { ...ok, em: Date.now() + 365 * DIA }));
  await assertFails(setDoc(col("c5"), { ...ok, nome: "x".repeat(500) }));
});

test("FB-06: avaliação só de 1 a 5", async () => {
  const l = como("leitor");
  await assertFails(setDoc(doc(l, "publicas/p1/avaliacoes/leitor"), { estrelas: 50, em: Date.now() }));
  await assertSucceeds(setDoc(doc(l, "publicas/p1/avaliacoes/leitor"), { estrelas: 4, em: Date.now() }));
});

test("FB-02: seguidores do perfil", async () => {
  await assertFails(updateDoc(doc(como("atacante"), "perfis/autor"), { seguidores: increment(100) }));
  await assertSucceeds(updateDoc(doc(como("fa"), "perfis/autor"), { seguidores: increment(1) }));
  await assertFails(updateDoc(doc(como("fa"), "perfis/autor"), { nome: "trocado" }));
  await assertFails(setDoc(doc(como("novo"), "perfis/novo"), { nome: "Novo", seguidores: 1000 }));
  await assertSucceeds(setDoc(doc(como("novo"), "perfis/novo"), { nome: "Novo" }));
  await assertSucceeds(updateDoc(doc(como("autor"), "perfis/autor"), { nome: "Autor 2", bio: "oi" }));
});

test("privado continua privado", async () => {
  await assertFails(getDoc(doc(como("atacante"), "users/autor/obras/o1")));
  await assertSucceeds(getDoc(doc(como("autor"), "users/autor/obras/o1")));
});
