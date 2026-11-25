# SMILE — Deployment, Infrastructure & Play Store Guide

**App Name:** SMILE (`बात करें, मुस्कुराएं!`)  
**Android Package / Application ID:** `com.aistudio.smile.vqmzkp`  
**Firebase Project ID:** `gen-lang-client-0079366378`  
**Firestore Database ID:** `ai-studio-android-smile-0f5ce38c-b39c-4abc-a026-9d63783bf188`

---

## 1. What Works Out of the Box vs. External Cloud Services Requiring Setup

| Capability | Status in Current Build | External / Manual Setup Notes |
|---|---|---|
| Google Sign-In (Credential Manager) | Provisioned & ready | Debug SHA-1/SHA-256 registered. Add Play Store App Signing SHA-1/SHA-256 before production release. |
| Cloud Firestore + Zero-Trust Rules | Provisioned & rules tested (7/7 tests) | Security rules enforced for users, chats, messages, calls, statuses, communities, reports, and admins. |
| E2EE (ECDH `secp256r1` + AES-256-GCM) | Implemented on-device | Keys generated locally via `CryptoManager`; private key never leaves DataStore. |
| +91 Phone Discovery & Contact Sync | Implemented via SHA-256 lookup | Requires user-granted `READ_CONTACTS` consent. |
| Real-time Voice Notes, Photos, Docs, Location | Implemented | Images/voice notes/docs sync directly via Firestore size-checked Base64 payloads (up to ~450 KB). |
| Call Signaling + Live CameraX & Audio Routing | Implemented | Signaling uses `/calls/{callId}` in Firestore; local mic mute/speakerphone/front-rear camera switch work immediately. |
| Phone Number SMS OTP (`PhoneAuthProvider`) | Requires manual Firebase Console setup | Enable **Phone** sign-in provider in Firebase Console & add production Play Integrity SHA-256. |
| Large Video/File Media (`Firebase Storage`) | Requires manual Firebase Console setup | Upgrade project to Blaze if needed, enable Cloud Storage, and swap `MediaUtils` payload storage to Storage download URLs. |
| Background Call/Chat Push (`FCM`) | Requires manual Firebase Console + server setup | Enable Firebase Cloud Messaging and deploy a Cloud Function trigger on `/chats/{chatId}/messages` and `/calls/{callId}` to send high-priority FCM data messages. |
| Production WebRTC NAT Traversal (`TURN` / `coturn`) | Requires external TURN server | Configure `coturn` (credentials below) and attach an `org.webrtc:google-webrtc` / `io.github.webrtc-sdk:android` `PeerConnectionFactory` to the existing `/calls/{callId}` `sdpOffer`/`sdpAnswer` documents. |

---

## 2. Firebase Console Setup Steps (Phone Auth, Storage & FCM)

1. Open [Firebase Console](https://console.firebase.google.com/) and select project **`gen-lang-client-0079366378`**.
2. **Phone Authentication (SMS OTP):**
   - Go to **Authentication → Sign-in method → Phone** and toggle **Enable**.
   - Add your Play App Signing **SHA-1** and **SHA-256** fingerprints under **Project Settings → Your apps → `com.aistudio.smile.vqmzkp`** so Play Integrity verification succeeds on physical devices.
3. **Firebase Cloud Messaging (Background Calls & Notifications):**
   - Deploy a Firebase Cloud Function (`onDocumentCreated` on `chats/{chatId}/messages/{messageId}` and `calls/{callId}`) using `firebase-admin` `messaging().sendEachForMulticast(...)` with `android: { priority: "high" }`.
   - Register the device FCM token on `/users/{uid}` and add a `FirebaseMessagingService` subclass in Android to wake the incoming-call notification (`NotificationCompat.CATEGORY_CALL`) when the app is killed.
4. **Firebase Storage (Large HD Video & >1 MB Documents):**
   - Enable **Storage** in Firebase Console, deploy storage rules restricting `/chat_media/{chatId}/*` to chat members, and upload large files before saving the `gs://` or signed URL in `ChatMessage.mediaData`.

---

## 3. WebRTC STUN / TURN Server Setup (`coturn`)

For reliable 1:1 and group voice/video calls behind carrier-grade NAT (4G/5G in India), deploy `coturn` on a small VPS (Ubuntu 22.04+, ports `3478 UDP/TCP`, `5349 TLS`, `49152-65535 UDP`):

```bash
sudo apt update && sudo apt install -y coturn
```

Edit `/etc/turnserver.conf`:
```ini
listening-port=3478
tls-listening-port=5349
fingerprint
lt-cred-mech
use-auth-secret
static-auth-secret=REPLACE_WITH_64_CHAR_SECRET
realm=turn.yourdomain.com
total-quota=100
bps-capacity=0
stale-nonce=600
cert=/etc/letsencrypt/live/turn.yourdomain.com/fullchain.pem
pkey=/etc/letsencrypt/live/turn.yourdomain.com/privkey.pem
no-stdout-log
```

Generate time-limited TURN credentials on your backend API (`HMAC-SHA1` of `expiryTimestamp:uid` using `static-auth-secret`) and pass `stun:stun.l.google.com:19302` + `turn:turn.yourdomain.com:3478` into `PeerConnection.RTCConfiguration`. Never embed static TURN passwords inside the APK.

---

## 4. Owner-Only Web Admin Dashboard (`/admin-dashboard/`)

The Admin Console lives in `/admin-dashboard/` completely separate from the Android APK:
- Restricted by `firestore.rules` (`isAdmin()` checks verified email `bhatiyaofficial@gmail.com` or an entry in `/admins/{uid}`).
- Host privately on Firebase Hosting or an internal domain (run `firebase deploy --only hosting` pointing to `admin-dashboard`).
- Grab your **Web API Key** from **Firebase Console → Project Settings → General** and paste it on the Admin login gate.

---

## 5. Building Signed Release APK / AAB & Play Store Checklist

1. **Generate Release Keystore (outside repo):**
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Build Signed AAB:**
   ```bash
   STORE_PASSWORD=... KEY_PASSWORD=... KEYSTORE_PATH=/path/to/my-upload-key.jks gradle :app:bundleRelease
   ```
3. **Google Play Console Compliance Checklist:**
   - App title <= 30 chars (`SMILE`), no emojis/buzzwords.
   - Zero broad storage permissions (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` omitted; uses Android Photo Picker & SAF).
   - Data Safety Form: declare phone number (hashed lookup), Google account email, user messages (E2EE), optional contacts & foreground location.
   - Link hosted Privacy Policy & Terms of Service URLs.

---

## 6. Privacy Policy & Terms Templates

- **Privacy Policy Summary:** SMILE collects your Google Sign-In account identifier, display name, unique `@smileId`, and optional mobile number (stored as a salted SHA-256 hash for contact discovery). Direct 1-to-1 text messages are encrypted end-to-end on device using ECDH (`secp256r1`) + AES-256-GCM. Contacts and location are accessed strictly upon explicit user action. You may export chats or permanently delete your account at any time inside **Settings → Delete Account**.
- **Terms of Service Summary:** Users must not send spam, harassment, unlawful content, or impersonate others. Admins review abuse reports in the isolated Operations Console and may suspend violating accounts.

---

## 7. GitHub Actions CI/CD Workflows (`.github/workflows/`)

- **`android-ci.yml` (`SMILE Android CI`):** Runs automatically on `push` / `pull_request` to `main` or `master` (and manual trigger). Restores `debug.keystore`, runs unit tests, builds the Debug APK (`app/build/outputs/apk/debug/*.apk`), uploads build/test artifacts, and runs the Firestore Security Rules test suite (`firestore.test.js`) against the Firebase Emulator.
- **`android-release.yml` (`SMILE Release APK & AAB`):** Runs on `v*` git tags or manual `workflow_dispatch` to assemble signed `.apk` and `.aab` release artifacts. Before running it, add these 3 secrets under **GitHub Repo → Settings → Secrets and variables → Actions**:
  - `ANDROID_KEYSTORE_BASE64`: Output of `base64 -w 0 my-upload-key.jks` (macOS: `base64 -i my-upload-key.jks`)
  - `STORE_PASSWORD`: Your keystore password
  - `KEY_PASSWORD`: Your `upload` key alias password

