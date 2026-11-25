const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const CHARLIE_UID = "charlie_uid_3";
const ADMIN_EMAIL = "bhatiyaofficial@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 1500) {
  return new Date(Date.now() - msOffset);
}

function validUserPayload(uid, smileId = "alice_smile", phoneHash = "hash_alice_919876543210") {
  const now = pastDate();
  return {
    userId: uid,
    displayName: "Alice",
    smileId,
    smileIdLower: smileId.toLowerCase(),
    phoneNumber: "+919876543210",
    phoneHash,
    email: "alice@example.com",
    about: "बात करें, मुस्कुराएं!",
    avatarData: "",
    publicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest",
    online: true,
    lastSeenVisibility: "EVERYONE",
    photoVisibility: "EVERYONE",
    aboutVisibility: "EVERYONE",
    readReceiptsEnabled: true,
    twoStepEnabled: false,
    twoStepPinHash: "",
    suspended: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validPublicProfilePayload(uid, smileId = "alice_smile", phoneHash = "hash_alice_919876543210") {
  const now = pastDate();
  return {
    userId: uid,
    displayName: "Alice",
    smileId,
    smileIdLower: smileId.toLowerCase(),
    phoneHash,
    about: "बात करें, मुस्कुराएं!",
    avatarData: "",
    publicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest",
    online: true,
    lastSeenVisibility: "EVERYONE",
    photoVisibility: "EVERYONE",
    aboutVisibility: "EVERYONE",
    createdAt: now,
    updatedAt: now,
  };
}

function validChatPayload(creatorUid, memberUids, chatType = "DIRECT") {
  const now = pastDate();
  return {
    chatType,
    title: chatType === "GROUP" ? "SMILE Friends" : "",
    description: chatType === "GROUP" ? "Weekend plans" : "",
    avatarData: "",
    memberUids,
    adminUids: [creatorUid],
    coAdminUids: [],
    createdBy: creatorUid,
    onlyAdminsCanSend: false,
    onlyAdminsCanEditInfo: true,
    announcementOnly: false,
    inviteCode: chatType === "GROUP" ? "INVITE123" : "",
    communityId: "",
    disappearingHours: 0,
    lastMessageText: "",
    lastMessageType: "TEXT",
    lastMessageSenderId: "",
    lastMessageSenderName: "",
    pinnedByUids: [],
    archivedByUids: [],
    mutedByUids: [],
    typingUids: [],
    unreadCounts: {},
    lastMessageAt: now,
    createdAt: now,
    updatedAt: now,
  };
}

function validMessagePayload(chatId, senderId, memberUids) {
  const now = pastDate();
  return {
    chatId,
    senderId,
    senderName: "Alice",
    memberUids,
    messageType: "TEXT",
    text: "Namaste!",
    cipherText: "",
    iv: "",
    e2ee: false,
    mediaData: "",
    mediaName: "",
    mediaMime: "",
    mediaDurationSec: 0,
    locationLabel: "",
    sharedContactName: "",
    sharedContactPhone: "",
    replyToMessageId: "",
    replyToText: "",
    replyToSenderName: "",
    forwarded: false,
    edited: false,
    deletedForEveryone: false,
    deletedForUids: [],
    starredByUids: [],
    deliveredToUids: [senderId],
    readByUids: [senderId],
    reactions: {},
    expiresAtMs: 0,
    createdAt: now,
    updatedAt: now,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("chats").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
});

test("User PII isolation, self-suspension guard, and ghost field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid owner create succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Shadow Update / Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID),
      isVerified: true,
    })
  );

  // Self-suspension rejected on create and update
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "bob_smile", "hash_bob"),
      suspended: true,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      suspended: true,
      updatedAt: pastDate(500),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      about: "Always smiling!",
      updatedAt: pastDate(500),
    })
  );

  // Cross-user PII read rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      about: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Exact-key lookup directories allow get but forbid list scraping", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("lookup_smile_ids").doc("alice_smile").set({
      userId: ALICE_UID,
      smileId: "alice_smile",
      displayName: "Alice",
      avatarData: "",
      about: "Hi",
      publicKey: "key123",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("lookup_smile_ids").doc("alice_smile").get());
  await assertFails(bobDb.collection("lookup_smile_ids").get());
  await assertFails(bobDb.collection("lookup_phones").get());
});

test("Chats and Messages enforce membership, parent chat existence, and query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const chatId = "chat_alice_bob";
  const members = [ALICE_UID, BOB_UID];

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set(validChatPayload(ALICE_UID, members))
  );

  // Filtered member query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("chats").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).get());

  // Send message inside existing chat
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1")
      .set(validMessagePayload(chatId, ALICE_UID, members))
  );

  // Bob can read/list messages with memberUids filter and add reaction
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages")
      .where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      reactions: { [BOB_UID]: "😊" },
      updatedAt: pastDate(500),
    })
  );

  // Charlie cannot read or send messages in Alice-Bob chat
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_2")
      .set(validMessagePayload(chatId, CHARLIE_UID, [ALICE_UID, BOB_UID, CHARLIE_UID]))
  );
});

test("Calls enforce participant scope and terminal status locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice",
      callerAvatar: "",
      calleeId: BOB_UID,
      calleeName: "Bob",
      chatId: "chat_alice_bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      isGroup: false,
      status: "RINGING",
      durationSeconds: 0,
      sdpOffer: "offer-sdp",
      sdpAnswer: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(charlieDb.collection("calls").doc("call_1").get());
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "CONNECTED",
      sdpAnswer: "answer-sdp",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").update({
      status: "ENDED",
      durationSeconds: 42,
      updatedAt: pastDate(300),
    })
  );
  // Terminal state lock: cannot reopen ENDED call
  await assertFails(
    bobDb.collection("calls").doc("call_1").update({
      status: "CONNECTED",
      updatedAt: pastDate(100),
    })
  );
});

test("Status updates & views enforce allowedViewerUids and owner-only viewer list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("status_updates").doc("st_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      ownerAvatar: "",
      mediaType: "TEXT",
      text: "Hello SMILE!",
      caption: "",
      backgroundColorHex: "#FFD600",
      mediaData: "",
      privacyMode: "SELECTED",
      allowedViewerUids: [ALICE_UID, BOB_UID],
      mutedByUids: [],
      viewedByUids: [],
      expiresAtMs: Date.now() + 86400000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("status_updates").where("allowedViewerUids", "array-contains", BOB_UID).get()
  );
  await assertFails(charlieDb.collection("status_updates").doc("st_1").get());

  // Bob records view receipt
  await assertSucceeds(
    bobDb.collection("status_updates").doc("st_1").collection("views").doc(BOB_UID).set({
      statusOwnerId: ALICE_UID,
      viewerId: BOB_UID,
      viewerName: "Bob",
      viewerAvatar: "",
      viewedAt: pastDate(400),
    })
  );

  // Alice can list views; Bob cannot list other viewers
  await assertSucceeds(
    aliceDb.collection("status_updates").doc("st_1").collection("views")
      .where("statusOwnerId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("status_updates").doc("st_1").collection("views")
      .where("statusOwnerId", "==", ALICE_UID).get()
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext("real_admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = pastDate();

  await assertFails(
    spoofAdminDb.collection("announcements").doc("ann_1").set({
      title: "Spoof",
      body: "Fake notice",
      active: true,
      authorId: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("announcements").doc("ann_1").set({
      title: "Welcome to SMILE",
      body: "Official launch announcement",
      active: true,
      authorId: "real_admin_uid",
      createdAt: now,
      updatedAt: now,
    })
  );
});
