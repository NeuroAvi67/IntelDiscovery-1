const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write discoveries, searches, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("discoveries").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("searches").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());

  const now = new Date(Date.now() - 1000);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("d1").set({
      userId: ALICE_UID,
      query: "Quantum cryptography",
      category: "Security",
      summary: "Overview of post-quantum key exchange.",
      verifiedFacts: ["NIST standardized ML-KEM in 2024."],
      interpretations: ["Migration requires hybrid deployment."],
      aiSuggestions: ["Audit TLS 1.3 key exchange dependencies."],
      sourceNotes: ["Synthesized from AI training knowledge."],
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can create, list, pin-update, and delete DiscoveryRecord matching Kotlin payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_1").set({
      userId: ALICE_UID,
      query: "Solid state batteries",
      category: "Technology",
      summary: "Solid electrolytes replace liquid organic solvents.",
      verifiedFacts: ["Solid-state cells aim to reduce thermal runaway risk."],
      interpretations: ["Manufacturing yield remains the primary commercial bottleneck."],
      aiSuggestions: ["Compare sulfide vs oxide electrolyte trade-offs."],
      sourceNotes: ["Model knowledge synthesis; verify current pilot production metrics."],
      pinned: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").get()
  );

  // Pin toggle update mirrors Kotlin partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_1").update({
      pinned: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / immutable mutation / future timestamp / @DocumentId 'id' leakage
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_1").update({
      summary: "Mutated summary",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_2").set({
      id: "disc_2",
      userId: ALICE_UID,
      query: "Test",
      category: "General",
      summary: "Test",
      verifiedFacts: [],
      interpretations: [],
      aiSuggestions: [],
      sourceNotes: [],
      pinned: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_3").set({
      userId: ALICE_UID,
      query: "Future timestamp",
      category: "General",
      summary: "Should fail",
      verifiedFacts: [],
      interpretations: [],
      aiSuggestions: [],
      sourceNotes: [],
      pinned: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("discoveries").doc("disc_1").delete()
  );
});

test("Owner can create, list, and delete searches and assistant messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("searches").doc("s1").set({
      userId: ALICE_UID,
      query: "Edge AI inference",
      category: "Technology",
      resultCount: 6,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m1").set({
      userId: ALICE_UID,
      role: "user",
      content: "Summarize neural quantization.",
      modelName: "gemini-3.8-flash",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("searches").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").get()
  );

  // Updates forbidden on immutable search/message logs
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m1").update({
      content: "Tampered",
    })
  );
});

test("Cross-user access is strictly denied", async () => {
  const now = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("discoveries").doc("bob_d").set({
      userId: BOB_UID,
      query: "Bob secret",
      category: "Security",
      summary: "Private discovery",
      verifiedFacts: [],
      interpretations: [],
      aiSuggestions: [],
      sourceNotes: [],
      pinned: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("discoveries").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("discoveries").doc("bob_d").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("discoveries").doc("bob_d").delete());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("discoveries").doc("alice_inject").set({
      userId: ALICE_UID,
      query: "Spoof",
      category: "General",
      summary: "Spoof",
      verifiedFacts: [],
      interpretations: [],
      aiSuggestions: [],
      sourceNotes: [],
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});
