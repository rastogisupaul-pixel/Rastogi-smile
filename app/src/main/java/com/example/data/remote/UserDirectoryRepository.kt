package com.example.data.remote

import android.content.Context
import android.provider.ContactsContract
import com.example.data.model.Announcement
import com.example.data.model.BlockedUser
import com.example.data.model.DirectoryLookup
import com.example.data.model.PublicProfile
import com.example.data.model.SavedContact
import com.example.data.model.UserAccount
import com.example.data.model.VisibilityOption
import com.example.security.CryptoManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserDirectoryRepository(private val db: FirebaseFirestore) {

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeOwnAccount(userId: String): Flow<UserAccount?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw root
                }
        )
    }

    fun observeContacts(userId: String): Flow<List<SavedContact>> = flow {
        val path = "users/$userId/contacts"
        emitAll(
            db.collection("users").document(userId).collection("contacts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val path = "users/$userId/blocked"
        emitAll(
            db.collection("users").document(userId).collection("blocked")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val path = "announcements"
        emitAll(
            db.collection("announcements")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    handleFirestoreError(root, OperationType.LIST, path)
                    throw root
                }
        )
    }

    suspend fun saveUserProfile(
        displayName: String,
        smileId: String,
        phoneNumberE164: String,
        about: String,
        avatarData: String,
        publicKey: String,
        existingAccount: UserAccount?
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(50)
            val cleanSmileId = smileId.trim().replace(Regex("[^a-zA-Z0-9._]"), "").take(30)
            require(cleanName.isNotEmpty()) { "Name is required" }
            require(cleanSmileId.length >= 3) { "Smile ID must be at least 3 characters" }
            val smileIdLower = cleanSmileId.lowercase()
            val phoneHash = CryptoManager.hashPhoneForLookup(phoneNumberE164)
            val cleanAbout = about.trim().take(140).ifBlank { "बात करें, मुस्कुराएं!" }
            val safeAvatar = avatarData.take(350_000)

            // Check Smile ID uniqueness if new or changed
            val existingSmileLookup = db.collection("lookup_smile_ids").document(smileIdLower).get().await()
            if (existingSmileLookup.exists()) {
                val ownerUid = existingSmileLookup.getString("userId").orEmpty()
                if (ownerUid != uid) {
                    return Result.failure(IllegalArgumentException("Smile ID @$cleanSmileId is already taken."))
                }
            }

            // Remove old lookup entries if Smile ID or phone changed
            if (existingAccount != null) {
                if (existingAccount.smileIdLower.isNotBlank() && existingAccount.smileIdLower != smileIdLower) {
                    runCatching { db.collection("lookup_smile_ids").document(existingAccount.smileIdLower).delete().await() }
                }
                if (existingAccount.phoneHash.isNotBlank() && existingAccount.phoneHash != phoneHash) {
                    runCatching { db.collection("lookup_phones").document(existingAccount.phoneHash).delete().await() }
                }
            }

            val email = auth.currentUser?.email.orEmpty().take(120)
            val batch = db.batch()
            val userRef = db.collection("users").document(uid)
            val publicRef = db.collection("public_profiles").document(uid)
            val smileLookupRef = db.collection("lookup_smile_ids").document(smileIdLower)
            docPath = userRef.path

            if (existingAccount == null) {
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "smileId" to cleanSmileId,
                        "smileIdLower" to smileIdLower,
                        "phoneNumber" to phoneNumberE164.take(20),
                        "phoneHash" to phoneHash,
                        "email" to email,
                        "about" to cleanAbout,
                        "avatarData" to safeAvatar,
                        "publicKey" to publicKey.take(512),
                        "online" to true,
                        "lastSeenVisibility" to VisibilityOption.EVERYONE.name,
                        "photoVisibility" to VisibilityOption.EVERYONE.name,
                        "aboutVisibility" to VisibilityOption.EVERYONE.name,
                        "readReceiptsEnabled" to true,
                        "twoStepEnabled" to false,
                        "twoStepPinHash" to "",
                        "suspended" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastSeenAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    publicRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "smileId" to cleanSmileId,
                        "smileIdLower" to smileIdLower,
                        "phoneHash" to phoneHash,
                        "about" to cleanAbout,
                        "avatarData" to safeAvatar,
                        "publicKey" to publicKey.take(512),
                        "online" to true,
                        "lastSeenVisibility" to VisibilityOption.EVERYONE.name,
                        "photoVisibility" to VisibilityOption.EVERYONE.name,
                        "aboutVisibility" to VisibilityOption.EVERYONE.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastSeenAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    userRef,
                    mapOf(
                        "displayName" to cleanName,
                        "smileId" to cleanSmileId,
                        "smileIdLower" to smileIdLower,
                        "phoneNumber" to phoneNumberE164.take(20),
                        "phoneHash" to phoneHash,
                        "about" to cleanAbout,
                        "avatarData" to safeAvatar,
                        "publicKey" to publicKey.take(512),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    publicRef,
                    mapOf(
                        "displayName" to cleanName,
                        "smileId" to cleanSmileId,
                        "smileIdLower" to smileIdLower,
                        "phoneHash" to phoneHash,
                        "about" to cleanAbout,
                        "avatarData" to safeAvatar,
                        "publicKey" to publicKey.take(512),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val lookupPayload = mapOf(
                "userId" to uid,
                "smileId" to cleanSmileId,
                "displayName" to cleanName,
                "avatarData" to safeAvatar,
                "about" to cleanAbout,
                "publicKey" to publicKey.take(512),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSmileLookup.exists()) {
                batch.update(
                    smileLookupRef,
                    mapOf(
                        "smileId" to cleanSmileId,
                        "displayName" to cleanName,
                        "avatarData" to safeAvatar,
                        "about" to cleanAbout,
                        "publicKey" to publicKey.take(512),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(smileLookupRef, lookupPayload)
            }

            if (phoneHash.isNotBlank()) {
                val phoneRef = db.collection("lookup_phones").document(phoneHash)
                val existingPhone = phoneRef.get().await()
                if (existingPhone.exists() && existingPhone.getString("userId") == uid) {
                    batch.update(
                        phoneRef,
                        mapOf(
                            "smileId" to cleanSmileId,
                            "displayName" to cleanName,
                            "avatarData" to safeAvatar,
                            "about" to cleanAbout,
                            "publicKey" to publicKey.take(512),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (!existingPhone.exists()) {
                    batch.set(phoneRef, lookupPayload)
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePresence(online: Boolean): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val batch = db.batch()
            val update = mapOf(
                "online" to online,
                "lastSeenAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.update(userRef, update)
            batch.update(db.collection("public_profiles").document(uid), update)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        lastSeenVisibility: VisibilityOption,
        photoVisibility: VisibilityOption,
        aboutVisibility: VisibilityOption,
        readReceiptsEnabled: Boolean,
        twoStepEnabled: Boolean,
        twoStepPinHash: String
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "lastSeenVisibility" to lastSeenVisibility.name,
                    "photoVisibility" to photoVisibility.name,
                    "aboutVisibility" to aboutVisibility.name,
                    "readReceiptsEnabled" to readReceiptsEnabled,
                    "twoStepEnabled" to twoStepEnabled,
                    "twoStepPinHash" to twoStepPinHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "lastSeenVisibility" to lastSeenVisibility.name,
                    "photoVisibility" to photoVisibility.name,
                    "aboutVisibility" to aboutVisibility.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun findUserByPhoneOrSmileId(query: String): Result<DirectoryLookup?> {
        val clean = query.trim()
        if (clean.isBlank()) return Result.success(null)
        return try {
            requireUserId()
            // Try Smile ID first if starts with @ or contains letters
            val looksLikePhone = clean.replace(Regex("[\\s\\-+()]"), "").all { it.isDigit() } &&
                clean.count { it.isDigit() } >= 7
            val docSnap = if (looksLikePhone) {
                val hash = CryptoManager.hashPhoneForLookup(clean, "+91")
                db.collection("lookup_phones").document(hash).get().await()
            } else {
                val idLower = clean.removePrefix("@").lowercase()
                db.collection("lookup_smile_ids").document(idLower).get().await()
            }
            if (docSnap.exists()) {
                Result.success(
                    docSnap.toObject(DirectoryLookup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "lookup")
            Result.failure(e)
        }
    }

    suspend fun getPublicProfile(userId: String): PublicProfile? {
        return try {
            val snap = db.collection("public_profiles").document(userId).get().await()
            if (snap.exists()) {
                snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "public_profiles/$userId")
            null
        }
    }

    suspend fun saveContact(lookup: DirectoryLookup, phoneHint: String = ""): Result<Unit> {
        var docPath = "users/contacts"
        return try {
            val uid = requireUserId()
            require(lookup.userId.isNotBlank() && lookup.userId != uid) { "Invalid contact user" }
            val ref = db.collection("users").document(uid)
                .collection("contacts").document(lookup.userId)
            docPath = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "displayName" to lookup.displayName,
                        "smileId" to lookup.smileId,
                        "phoneHint" to phoneHint.take(30),
                        "avatarData" to lookup.avatarData,
                        "about" to lookup.about,
                        "publicKey" to lookup.publicKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "ownerId" to uid,
                        "contactUid" to lookup.userId,
                        "displayName" to lookup.displayName,
                        "smileId" to lookup.smileId,
                        "phoneHint" to phoneHint.take(30),
                        "avatarData" to lookup.avatarData,
                        "about" to lookup.about,
                        "publicKey" to lookup.publicKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncDeviceContacts(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uid = requireUserId()
            val phoneNumbers = linkedSetOf<String>()
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )
            cursor?.use {
                val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && phoneNumbers.size < 40) {
                    val num = if (idx >= 0) it.getString(idx).orEmpty() else ""
                    if (num.count { ch -> ch.isDigit() } >= 7) {
                        phoneNumbers.add(num)
                    }
                }
            }
            var synced = 0
            for (raw in phoneNumbers) {
                val hash = CryptoManager.hashPhoneForLookup(raw, "+91")
                if (hash.isBlank()) continue
                val snap = db.collection("lookup_phones").document(hash).get().await()
                if (snap.exists()) {
                    val lookup = snap.toObject(DirectoryLookup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (lookup != null && lookup.userId != uid) {
                        saveContact(lookup, CryptoManager.normalizePhoneToE164(raw, "+91"))
                        synced++
                    }
                }
            }
            Result.success(synced)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "contacts_sync")
            Result.failure(e)
        }
    }

    suspend fun blockUser(blockedUid: String, displayName: String, reason: String = "Blocked by user"): Result<Unit> {
        var docPath = "users/blocked"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("blocked").document(blockedUid)
            docPath = ref.path
            ref.set(
                mapOf(
                    "ownerId" to uid,
                    "blockedUid" to blockedUid,
                    "displayName" to displayName.take(50).ifBlank { "User" },
                    "reason" to reason.take(200),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(blockedUid: String): Result<Unit> {
        var docPath = "users/blocked"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("blocked").document(blockedUid)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetUserId: String,
        targetType: String,
        reason: String,
        details: String
    ): Result<Unit> {
        var docPath = "reports"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reports").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "reporterId" to uid,
                    "targetUserId" to targetUserId.take(128),
                    "targetType" to targetType,
                    "reason" to reason.trim().take(120).ifBlank { "Report" },
                    "details" to details.trim().take(1000),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(account: UserAccount?): Result<Unit> {
        var docPath = "users"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
            val uid = user.uid
            docPath = "users/$uid"
            if (account != null) {
                if (account.smileIdLower.isNotBlank()) {
                    runCatching { db.collection("lookup_smile_ids").document(account.smileIdLower).delete().await() }
                }
                if (account.phoneHash.isNotBlank()) {
                    runCatching { db.collection("lookup_phones").document(account.phoneHash).delete().await() }
                }
            }
            runCatching { db.collection("public_profiles").document(uid).delete().await() }
            runCatching { db.collection("users").document(uid).delete().await() }
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
