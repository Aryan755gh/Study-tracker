const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "gen-lang-client-0408030816";
const DATABASE_ID = "ai-studio-android-studyos-b765f5f1-7d50-469a-b802-d1554045de5d";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

test("unauthenticated user cannot read or write user profiles", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users").doc("alice").get());
  await assertFails(unauthedDb.collection("users").doc("alice").set({
    userId: "alice",
    xp: 100,
    level: 1,
    streak: 1
  }));
});

test("authenticated user can read and write their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users").doc("alice").set({
    userId: "alice",
    xp: 100,
    level: 1,
    streak: 1
  }));
  await assertSucceeds(aliceDb.collection("users").doc("alice").get());
});

test("authenticated user cannot write another user profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertFails(aliceDb.collection("users").doc("bob").set({
    userId: "bob",
    xp: 100,
    level: 1,
    streak: 1
  }));
});

test("authenticated user can create sessions in their subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users").doc("alice").collection("sessions").doc("s1").set({
    id: "s1",
    userId: "alice",
    durationMinutes: 25,
    mode: "focus",
    soundUsed: "LOFI_SYNTH"
  }));
});
