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

test("Unauthenticated user: cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").set({
      userId: ALICE_UID,
      localId: "habit_1",
      name: "Read",
      description: "",
      category: "LEARNING",
      habitType: "BUILD",
      trackingMode: "NUMERICAL",
      targetValue: 30,
      unit: "min",
      iconKey: "book",
      colorHex: "#27C6D9",
      scheduleType: "DAILY",
      scheduleDays: [1, 2, 3, 4, 5, 6, 7],
      reminderEnabled: false,
      reminderTime: "08:00",
      archived: false,
      createdEpochDay: 20400,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid habit, record, urge, goal, focus, and plan payloads succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ahmed",
      themeMode: "DARK",
      languageCode: "ar",
      notificationsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").set({
      userId: ALICE_UID,
      localId: "habit_1",
      name: "Reading",
      description: "Read 30 minutes daily",
      category: "LEARNING",
      habitType: "BUILD",
      trackingMode: "NUMERICAL",
      targetValue: 30,
      unit: "min",
      iconKey: "book",
      colorHex: "#35C98B",
      scheduleType: "DAILY",
      scheduleDays: [1, 2, 3, 4, 5, 6, 7],
      reminderEnabled: true,
      reminderTime: "20:00",
      archived: false,
      createdEpochDay: 20400,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").update({
      name: "Deep Reading",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habit_records").doc("habit_1_20400").set({
      userId: ALICE_UID,
      habitLocalId: "habit_1",
      epochDay: 20400,
      status: "COMPLETED",
      progressValue: 30,
      notes: "Finished chapter 2",
      recordedTimestampMs: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("urge_records").doc("urge_1").set({
      userId: ALICE_UID,
      epochDay: 20400,
      timeString: "21:15",
      recordKind: "URGE_RESISTED",
      intensity: 6,
      notes: "Used 5 min timer",
      recordedTimestampMs: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1").set({
      userId: ALICE_UID,
      title: "Finish 12 Books",
      description: "One per month",
      targetEpochDay: 20800,
      currentProgress: 7,
      targetTotal: 12,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("focus_sessions").doc("sess_1").set({
      userId: ALICE_UID,
      durationMinutes: 25,
      epochDay: 20400,
      completed: true,
      sessionType: "FOCUS",
      startedAtMs: Date.now() - 1500000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("if_then_plans").doc("plan_1").set({
      userId: ALICE_UID,
      triggerText: "If I feel an urge while studying",
      responseText: "I will continue studying without arguing with it",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("habit_records").get());
});

test("Security bounds: reject cross-user access, ghost fields, future timestamps, and @DocumentId field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("habits").doc("bob_h1").set({
      userId: BOB_UID,
      localId: "bob_h1",
      name: "Workout",
      description: "",
      category: "FITNESS",
      habitType: "BUILD",
      trackingMode: "BOOLEAN",
      targetValue: 1,
      unit: "",
      iconKey: "fitness",
      colorHex: "#27C6D9",
      scheduleType: "DAILY",
      scheduleDays: [1, 2, 3, 4, 5, 6, 7],
      reminderEnabled: false,
      reminderTime: "",
      archived: false,
      createdEpochDay: 20400,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read/write blocked
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("bob_h1").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("bob_h1").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field & @DocumentId 'id' field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_bad").set({
      id: "goal_bad",
      userId: ALICE_UID,
      title: "Bad Goal",
      description: "",
      targetEpochDay: 20800,
      currentProgress: 0,
      targetTotal: 10,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("if_then_plans").doc("plan_future").set({
      userId: ALICE_UID,
      triggerText: "If bored",
      responseText: "Read",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
