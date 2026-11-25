import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import {
  getFirestore,
  collection,
  getDocs,
  doc,
  updateDoc,
  addDoc,
  serverTimestamp,
  orderBy,
  query,
  limit
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";

const PROJECT_ID = "gen-lang-client-0079366378";
const DATABASE_ID = "ai-studio-android-smile-0f5ce38c-b39c-4abc-a026-9d63783bf188";

const apiKeyInput = document.getElementById("web-api-key");
const savedKey = localStorage.getItem("smile_admin_web_api_key") || "";
if (savedKey) apiKeyInput.value = savedKey;

const btnSignIn = document.getElementById("btn-signin");
const btnSignOut = document.getElementById("btn-signout");
const adminBadge = document.getElementById("admin-badge");
const loginGate = document.getElementById("login-gate");
const dashboard = document.getElementById("dashboard");

let auth = null;
let db = null;
let cachedUsers = [];

function initFirebase(apiKey) {
  const app = initializeApp({
    apiKey,
    authDomain: `${PROJECT_ID}.firebaseapp.com`,
    projectId: PROJECT_ID
  });
  auth = getAuth(app);
  db = getFirestore(app, DATABASE_ID);
  onAuthStateChanged(auth, async (user) => {
    if (user) {
      btnSignIn.classList.add("hidden");
      btnSignOut.classList.remove("hidden");
      adminBadge.textContent = user.email || user.uid;
      adminBadge.classList.remove("hidden");
      loginGate.classList.add("hidden");
      dashboard.classList.remove("hidden");
      await loadDashboardData(user);
    } else {
      btnSignIn.classList.remove("hidden");
      btnSignOut.classList.add("hidden");
      adminBadge.classList.add("hidden");
      loginGate.classList.remove("hidden");
      dashboard.classList.add("hidden");
    }
  });
}

if (savedKey) {
  initFirebase(savedKey);
}

btnSignIn.addEventListener("click", async () => {
  const key = apiKeyInput.value.trim();
  if (!key) {
    alert("Enter your Firebase Web API key from Firebase Console -> Project Settings first.");
    return;
  }
  localStorage.setItem("smile_admin_web_api_key", key);
  if (!auth) initFirebase(key);
  try {
    await signInWithPopup(auth, new GoogleAuthProvider());
  } catch (err) {
    alert("Admin sign-in failed: " + err.message);
  }
});

btnSignOut.addEventListener("click", async () => {
  if (auth) await signOut(auth);
});

async function writeAuditLog(user, action, targetId) {
  await addDoc(collection(db, "audit_logs"), {
    actorUid: user.uid,
    action,
    targetId,
    createdAt: serverTimestamp()
  });
}

async function loadDashboardData(user) {
  try {
    const [usersSnap, chatsSnap, reportsSnap, callsSnap, annSnap, auditSnap] = await Promise.all([
      getDocs(collection(db, "users")),
      getDocs(collection(db, "chats")),
      getDocs(collection(db, "reports")),
      getDocs(collection(db, "calls")),
      getDocs(collection(db, "announcements")),
      getDocs(query(collection(db, "audit_logs"), orderBy("createdAt", "desc"), limit(20)))
    ]);

    cachedUsers = usersSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
    const reports = reportsSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
    const openReports = reports.filter((r) => r.status === "OPEN");

    document.getElementById("stat-users").textContent = String(cachedUsers.length);
    document.getElementById("stat-chats").textContent = String(chatsSnap.size);
    document.getElementById("stat-reports").textContent = String(openReports.length);
    document.getElementById("stat-calls").textContent = String(callsSnap.size);

    renderUsers(cachedUsers, user);
    renderReports(reports, user);
    renderAnnouncements(annSnap.docs.map((d) => ({ id: d.id, ...d.data() })));
    renderAuditLogs(auditSnap.docs.map((d) => d.data()));
  } catch (err) {
    alert("Access denied or query error (ensure you signed in as bhatiyaofficial@gmail.com): " + err.message);
  }
}

function renderUsers(users, adminUser) {
  const tbody = document.getElementById("users-tbody");
  tbody.innerHTML = "";
  users.forEach((u) => {
    const tr = document.createElement("tr");
    const statusLabel = u.suspended ? '<span class="pill-red">Suspended</span>' : '<span class="pill-green">Active</span>';
    tr.innerHTML = `
      <td>${escapeHtml(u.displayName || "")}</td>
      <td>@${escapeHtml(u.smileId || "")}</td>
      <td>${escapeHtml(u.phoneNumber || "—")}</td>
      <td>${statusLabel}</td>
      <td><button class="${u.suspended ? "btn-outline" : "btn-danger"}">${u.suspended ? "Restore" : "Suspend"}</button></td>
    `;
    tr.querySelector("button").addEventListener("click", async () => {
      await updateDoc(doc(db, "users", u.id), {
        suspended: !u.suspended,
        updatedAt: serverTimestamp()
      });
      await writeAuditLog(adminUser, u.suspended ? "RESTORE_USER" : "SUSPEND_USER", u.id);
      await loadDashboardData(adminUser);
    });
    tbody.appendChild(tr);
  });
}

function renderReports(reports, adminUser) {
  const container = document.getElementById("reports-list");
  container.innerHTML = reports.length ? "" : '<p class="hint">No moderation reports submitted yet.</p>';
  reports.forEach((r) => {
    const div = document.createElement("div");
    div.className = "list-item";
    div.innerHTML = `
      <div>
        <strong>[${escapeHtml(r.targetType)}] ${escapeHtml(r.reason)}</strong>
        <p class="hint">${escapeHtml(r.details || "")} • Status: <b>${escapeHtml(r.status)}</b></p>
      </div>
      ${r.status !== "RESOLVED" ? '<button class="btn-primary">Resolve</button>' : ""}
    `;
    const btn = div.querySelector("button");
    if (btn) {
      btn.addEventListener("click", async () => {
        await updateDoc(doc(db, "reports", r.id), { status: "RESOLVED", updatedAt: serverTimestamp() });
        await writeAuditLog(adminUser, "RESOLVE_REPORT", r.id);
        await loadDashboardData(adminUser);
      });
    }
    container.appendChild(div);
  });
}

function renderAnnouncements(items) {
  const list = document.getElementById("announcements-list");
  list.innerHTML = "";
  items.forEach((a) => {
    const div = document.createElement("div");
    div.className = "list-item";
    div.innerHTML = `<div><strong>${escapeHtml(a.title)}</strong><p class="hint">${escapeHtml(a.body)}</p></div>`;
    list.appendChild(div);
  });
}

function renderAuditLogs(logs) {
  const list = document.getElementById("audit-list");
  list.innerHTML = logs.length ? "" : '<p class="hint">No admin audit entries recorded yet.</p>';
  logs.forEach((l) => {
    const div = document.createElement("div");
    div.className = "list-item";
    div.innerHTML = `<div><strong>${escapeHtml(l.action)}</strong> → <code>${escapeHtml(l.targetId)}</code></div><span class="hint">by ${escapeHtml(l.actorUid)}</span>`;
    list.appendChild(div);
  });
}

document.getElementById("announcement-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!auth?.currentUser) return;
  const title = document.getElementById("ann-title").value.trim();
  const body = document.getElementById("ann-body").value.trim();
  if (!title || !body) return;
  const ref = await addDoc(collection(db, "announcements"), {
    title,
    body,
    active: true,
    authorId: auth.currentUser.uid,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  await writeAuditLog(auth.currentUser, "PUBLISH_ANNOUNCEMENT", ref.id);
  document.getElementById("ann-title").value = "";
  document.getElementById("ann-body").value = "";
  await loadDashboardData(auth.currentUser);
});

document.getElementById("user-search").addEventListener("input", (e) => {
  const q = e.target.value.toLowerCase();
  const filtered = cachedUsers.filter((u) =>
    (u.displayName || "").toLowerCase().includes(q) ||
    (u.smileId || "").toLowerCase().includes(q) ||
    (u.phoneNumber || "").toLowerCase().includes(q)
  );
  if (auth?.currentUser) renderUsers(filtered, auth.currentUser);
});

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
