package com.example.data.remote

import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.Community
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SocialAndCallRepository(private val db: FirebaseFirestore) {

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- CALLS ---
    fun observeCalls(userId: String): Flow<List<CallRecord>> = flow {
        val path = "calls"
        emitAll(
            db.collection("calls")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { (it.createdAt ?: Timestamp(0, 0)).seconds }
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

    suspend fun startCall(
        callerName: String,
        callerAvatar: String,
        calleeId: String,
        calleeName: String,
        chatId: String,
        participantUids: List<String>,
        callType: CallType,
        isGroup: Boolean
    ): Result<String> {
        var docPath = "calls"
        return try {
            val uid = requireUserId()
            val participants = (listOf(uid) + participantUids).distinct().take(32)
            require(participants.size >= 2) { "At least 2 participants required for a call." }
            val ref = db.collection("calls").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "callerId" to uid,
                    "callerName" to callerName.take(60).ifBlank { "SMILE User" },
                    "callerAvatar" to callerAvatar.take(350_000),
                    "calleeId" to calleeId.take(128),
                    "calleeName" to calleeName.take(60).ifBlank { "Contact" },
                    "chatId" to chatId.take(128),
                    "participantUids" to participants,
                    "callType" to callType.name,
                    "isGroup" to isGroup,
                    "status" to CallStatus.RINGING.name,
                    "durationSeconds" to 0L,
                    "sdpOffer" to "v=0\r\no=smile-webrtc 1 1 IN IP4 0.0.0.0\r\ns=SMILE-${callType.name}",
                    "sdpAnswer" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(
        callId: String,
        newStatus: CallStatus,
        durationSeconds: Long = 0L,
        sdpAnswer: String? = null
    ): Result<Unit> {
        var docPath = "calls/$callId"
        return try {
            requireUserId()
            val update = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (sdpAnswer != null) update["sdpAnswer"] = sdpAnswer.take(16000)
            db.collection("calls").document(callId).update(update).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- STATUS / UPDATES ---
    fun observeStatusUpdates(userId: String): Flow<List<StatusUpdate>> = flow {
        val path = "status_updates"
        emitAll(
            db.collection("status_updates")
                .whereArrayContains("allowedViewerUids", userId)
                .snapshots()
                .map { snap ->
                    val nowMs = System.currentTimeMillis()
                    snap.toObjects(StatusUpdate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.expiresAtMs > nowMs }
                        .sortedByDescending { (it.createdAt ?: Timestamp(0, 0)).seconds }
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

    fun observeStatusViews(statusId: String, ownerId: String): Flow<List<StatusView>> = flow {
        val path = "status_updates/$statusId/views"
        emitAll(
            db.collection("status_updates").document(statusId).collection("views")
                .whereEqualTo("statusOwnerId", ownerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StatusView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == ownerId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw root
                }
        )
    }

    suspend fun postStatus(
        ownerName: String,
        ownerAvatar: String,
        mediaType: String,
        text: String,
        caption: String,
        backgroundColorHex: String,
        mediaData: String,
        privacyMode: StatusPrivacyMode,
        allowedViewerUids: List<String>
    ): Result<Unit> {
        var docPath = "status_updates"
        return try {
            val uid = requireUserId()
            val ref = db.collection("status_updates").document()
            docPath = ref.path
            val viewers = (listOf(uid) + allowedViewerUids).distinct().take(100)
            val expiresAtMs = System.currentTimeMillis() + 24L * 3_600_000L
            ref.set(
                mapOf(
                    "ownerId" to uid,
                    "ownerName" to ownerName.take(60).ifBlank { "SMILE User" },
                    "ownerAvatar" to ownerAvatar.take(350_000),
                    "mediaType" to mediaType,
                    "text" to text.trim().take(500),
                    "caption" to caption.trim().take(300),
                    "backgroundColorHex" to backgroundColorHex.take(16),
                    "mediaData" to mediaData.take(450_000),
                    "privacyMode" to privacyMode.name,
                    "allowedViewerUids" to viewers,
                    "mutedByUids" to emptyList<String>(),
                    "viewedByUids" to emptyList<String>(),
                    "expiresAtMs" to expiresAtMs,
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

    suspend fun markStatusViewed(status: StatusUpdate, viewerName: String, viewerAvatar: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (status.ownerId == uid) return Result.success(Unit)
        return try {
            val batch = db.batch()
            val statusRef = db.collection("status_updates").document(status.id)
            if (uid !in status.viewedByUids) {
                batch.update(
                    statusRef,
                    mapOf(
                        "viewedByUids" to (status.viewedByUids + uid).distinct().take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val viewRef = statusRef.collection("views").document(uid)
            batch.set(
                viewRef,
                mapOf(
                    "statusOwnerId" to status.ownerId,
                    "viewerId" to uid,
                    "viewerName" to viewerName.take(60).ifBlank { "SMILE User" },
                    "viewerAvatar" to viewerAvatar.take(350_000),
                    "viewedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "status_updates/${status.id}/views")
            Result.failure(e)
        }
    }

    suspend fun toggleMuteStatus(status: StatusUpdate): Result<Unit> {
        return try {
            val uid = requireUserId()
            val next = if (uid in status.mutedByUids) status.mutedByUids - uid else status.mutedByUids + uid
            db.collection("status_updates").document(status.id)
                .update(mapOf("mutedByUids" to next.distinct(), "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "status_updates/${status.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("status_updates").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "status_updates/$statusId")
            Result.failure(e)
        }
    }

    // --- COMMUNITIES ---
    fun observeCommunities(userId: String): Flow<List<Community>> = flow {
        val path = "communities"
        emitAll(
            db.collection("communities")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Community::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
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

    suspend fun createCommunity(
        name: String,
        description: String,
        announcementText: String,
        linkedChatIds: List<String>,
        memberUids: List<String>
    ): Result<Unit> {
        var docPath = "communities"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Community name required" }
            val ref = db.collection("communities").document()
            docPath = ref.path
            val inviteCode = "COM_${UUID.randomUUID().toString().replace("-", "").take(10).uppercase()}"
            val batch = db.batch()
            batch.set(
                ref,
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(400),
                    "iconData" to "",
                    "ownerId" to uid,
                    "adminUids" to listOf(uid),
                    "memberUids" to (listOf(uid) + memberUids).distinct().take(100),
                    "linkedChatIds" to linkedChatIds.distinct().take(50),
                    "announcementText" to announcementText.trim().take(1000).ifBlank { "Welcome to $cleanName!" },
                    "inviteCode" to inviteCode,
                    "mutedByUids" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("community_invites").document(inviteCode),
                mapOf(
                    "communityId" to ref.id,
                    "communityName" to cleanName,
                    "description" to description.trim().take(400),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun postCommunityAnnouncement(communityId: String, text: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("communities").document(communityId).update(
                mapOf(
                    "announcementText" to text.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/$communityId")
            Result.failure(e)
        }
    }

    suspend fun linkGroupToCommunity(community: Community, chatId: String): Result<Unit> {
        return try {
            requireUserId()
            val next = (community.linkedChatIds + chatId).distinct().take(50)
            db.collection("communities").document(community.id).update(
                mapOf("linkedChatIds" to next, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/${community.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleCommunityMute(community: Community): Result<Unit> {
        return try {
            val uid = requireUserId()
            val next = if (uid in community.mutedByUids) community.mutedByUids - uid else community.mutedByUids + uid
            db.collection("communities").document(community.id).update(
                mapOf("mutedByUids" to next.distinct(), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/${community.id}")
            Result.failure(e)
        }
    }

    suspend fun joinCommunityByInvite(code: String): Result<Unit> {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return Result.failure(IllegalArgumentException("Enter a community invite code"))
        return try {
            val uid = requireUserId()
            val invSnap = db.collection("community_invites").document(cleanCode).get().await()
            if (!invSnap.exists()) return Result.failure(IllegalArgumentException("Community invite not found"))
            val commId = invSnap.getString("communityId").orEmpty()
            val commRef = db.collection("communities").document(commId)
            val commSnap = commRef.get().await()
            val members = (commSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (uid !in members) {
                commRef.update(
                    mapOf(
                        "memberUids" to (members + uid).distinct(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "community_invites/$cleanCode")
            Result.failure(e)
        }
    }
}
