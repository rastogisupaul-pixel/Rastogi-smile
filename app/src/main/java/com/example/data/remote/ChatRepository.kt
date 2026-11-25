package com.example.data.remote

import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.security.CryptoManager
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

class ChatRepository(private val db: FirebaseFirestore) {

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeChats(userId: String): Flow<List<Chat>> = flow {
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<Chat> { userId in it.pinnedByUids }
                                .thenByDescending { (it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
                        )
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

    fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { userId !in it.deletedForUids }
                        .filter { msg ->
                            msg.expiresAtMs == 0L || msg.expiresAtMs > System.currentTimeMillis()
                        }
                        .sortedBy { (it.createdAt ?: Timestamp(0, 0)).toDate().time }
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

    suspend fun getOrCreateDirectChat(peerUid: String, peerName: String, peerAvatar: String): Result<String> {
        var docPath = "chats"
        return try {
            val uid = requireUserId()
            require(peerUid.isNotBlank() && peerUid != uid) { "Invalid chat peer" }
            val sorted = listOf(uid, peerUid).sorted()
            val chatId = "dm_${sorted[0]}_${sorted[1]}".take(128)
            val chatRef = db.collection("chats").document(chatId)
            docPath = chatRef.path
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "chatType" to "DIRECT",
                    "title" to peerName.take(80),
                    "description" to "",
                    "avatarData" to peerAvatar.take(350_000),
                    "memberUids" to sorted,
                    "adminUids" to listOf(uid),
                    "coAdminUids" to emptyList<String>(),
                    "createdBy" to uid,
                    "onlyAdminsCanSend" to false,
                    "onlyAdminsCanEditInfo" to true,
                    "announcementOnly" to false,
                    "inviteCode" to "",
                    "communityId" to "",
                    "disappearingHours" to 0L,
                    "lastMessageText" to "",
                    "lastMessageType" to MessageType.TEXT.name,
                    "lastMessageSenderId" to "",
                    "lastMessageSenderName" to "",
                    "pinnedByUids" to emptyList<String>(),
                    "archivedByUids" to emptyList<String>(),
                    "mutedByUids" to emptyList<String>(),
                    "typingUids" to emptyList<String>(),
                    "unreadCounts" to emptyMap<String, Long>(),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(payload).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        title: String,
        description: String,
        avatarData: String,
        memberUidsInput: List<String>,
        communityId: String = ""
    ): Result<String> {
        var docPath = "chats"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Group name is required" }
            val members = (listOf(uid) + memberUidsInput).distinct().take(100)
            // Ensure minimum 2 members if needed; if creator creates solo group, rules require size >= 2,
            // OR we allow creating with at least 1 selected contact. Guard clearly:
            require(members.size >= 2) { "Select at least 1 contact to create a group." }
            val chatRef = db.collection("chats").document()
            docPath = chatRef.path
            val inviteCode = "GRP_${UUID.randomUUID().toString().replace("-", "").take(10).uppercase()}"
            val batch = db.batch()
            batch.set(
                chatRef,
                mapOf(
                    "chatType" to "GROUP",
                    "title" to cleanTitle,
                    "description" to description.trim().take(300),
                    "avatarData" to avatarData.take(350_000),
                    "memberUids" to members,
                    "adminUids" to listOf(uid),
                    "coAdminUids" to emptyList<String>(),
                    "createdBy" to uid,
                    "onlyAdminsCanSend" to false,
                    "onlyAdminsCanEditInfo" to true,
                    "announcementOnly" to false,
                    "inviteCode" to inviteCode,
                    "communityId" to communityId.take(128),
                    "disappearingHours" to 0L,
                    "lastMessageText" to "Group \"$cleanTitle\" created",
                    "lastMessageType" to MessageType.SYSTEM.name,
                    "lastMessageSenderId" to uid,
                    "lastMessageSenderName" to "SMILE",
                    "pinnedByUids" to emptyList<String>(),
                    "archivedByUids" to emptyList<String>(),
                    "mutedByUids" to emptyList<String>(),
                    "typingUids" to emptyList<String>(),
                    "unreadCounts" to emptyMap<String, Long>(),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("group_invites").document(inviteCode),
                mapOf(
                    "chatId" to chatRef.id,
                    "chatTitle" to cleanTitle,
                    "description" to description.trim().take(300),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(chatRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinGroupByInviteCode(code: String): Result<String> {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return Result.failure(IllegalArgumentException("Enter an invite code"))
        return try {
            val uid = requireUserId()
            val inviteSnap = db.collection("group_invites").document(cleanCode).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invite code not found"))
            }
            val chatId = inviteSnap.getString("chatId").orEmpty()
            require(chatId.isNotBlank()) { "Invalid invite" }
            val chatRef = db.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            val currentMembers = (chatSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (uid !in currentMembers) {
                chatRef.update(
                    mapOf(
                        "memberUids" to (currentMembers + uid).distinct(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "group_invites/$cleanCode")
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chat: Chat,
        senderName: String,
        messageType: MessageType,
        text: String,
        myPrivateKey: String = "",
        peerPublicKey: String = "",
        mediaData: String = "",
        mediaName: String = "",
        mediaMime: String = "",
        mediaDurationSec: Long = 0L,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String = "",
        sharedContactName: String = "",
        sharedContactPhone: String = "",
        replyTo: ChatMessage? = null,
        forwarded: Boolean = false
    ): Result<Unit> {
        var docPath = "chats/${chat.id}/messages"
        return try {
            val uid = requireUserId()
            val chatRef = db.collection("chats").document(chat.id)
            val msgRef = chatRef.collection("messages").document()
            docPath = msgRef.path

            val rawText = text.trim().take(4000)
            var storedText = rawText
            var cipherText = ""
            var iv = ""
            var isE2ee = false
            if (chat.chatType == "DIRECT" && messageType == MessageType.TEXT && rawText.isNotBlank() &&
                myPrivateKey.isNotBlank() && peerPublicKey.isNotBlank()
            ) {
                val enc = CryptoManager.encryptMessage(rawText, myPrivateKey, peerPublicKey)
                if (enc != null) {
                    cipherText = enc.cipherTextBase64.take(8000)
                    iv = enc.ivBase64.take(64)
                    storedText = "" // Plaintext stripped when E2EE active!
                    isE2ee = true
                }
            }

            val expiresAtMs = if (chat.disappearingHours > 0L) {
                System.currentTimeMillis() + chat.disappearingHours * 3_600_000L
            } else 0L

            val msgPayload = mutableMapOf<String, Any?>(
                "chatId" to chat.id,
                "senderId" to uid,
                "senderName" to senderName.take(60).ifBlank { "SMILE User" },
                "memberUids" to chat.memberUids,
                "messageType" to messageType.name,
                "text" to storedText,
                "cipherText" to cipherText,
                "iv" to iv,
                "e2ee" to isE2ee,
                "mediaData" to mediaData.take(450_000),
                "mediaName" to mediaName.take(180),
                "mediaMime" to mediaMime.take(100),
                "mediaDurationSec" to mediaDurationSec.coerceIn(0L, 3600L),
                "latitude" to latitude,
                "longitude" to longitude,
                "locationLabel" to locationLabel.take(200),
                "sharedContactName" to sharedContactName.take(80),
                "sharedContactPhone" to sharedContactPhone.take(30),
                "replyToMessageId" to (replyTo?.id ?: "").take(128),
                "replyToText" to (replyTo?.text?.ifBlank { replyTo.messageType } ?: "").take(200),
                "replyToSenderName" to (replyTo?.senderName ?: "").take(60),
                "forwarded" to forwarded,
                "edited" to false,
                "deletedForEveryone" to false,
                "deletedForUids" to emptyList<String>(),
                "starredByUids" to emptyList<String>(),
                "deliveredToUids" to listOf(uid),
                "readByUids" to listOf(uid),
                "reactions" to emptyMap<String, String>(),
                "expiresAtMs" to expiresAtMs,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val summaryText = when (messageType) {
                MessageType.TEXT -> if (isE2ee) "Encrypted message" else rawText
                MessageType.IMAGE -> "Photo"
                MessageType.VIDEO -> "Video"
                MessageType.DOCUMENT -> "Document: ${mediaName.ifBlank { "File" }}"
                MessageType.VOICE -> "Voice note (${mediaDurationSec}s)"
                MessageType.CONTACT -> "Contact: $sharedContactName"
                MessageType.LOCATION -> "Location shared"
                MessageType.STICKER -> rawText.ifBlank { "Sticker" }
                MessageType.SYSTEM -> rawText
            }.take(500)

            val updatedUnread = chat.unreadCounts.toMutableMap()
            chat.memberUids.forEach { memberId ->
                if (memberId == uid) {
                    updatedUnread[memberId] = 0L
                } else {
                    updatedUnread[memberId] = (updatedUnread[memberId] ?: 0L) + 1L
                }
            }

            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessageText" to summaryText,
                    "lastMessageType" to messageType.name,
                    "lastMessageSenderId" to uid,
                    "lastMessageSenderName" to senderName.take(60).ifBlank { "SMILE User" },
                    "unreadCounts" to updatedUnread,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
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

    suspend fun markChatRead(chat: Chat, messages: List<ChatMessage>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val batch = db.batch()
            var hasChanges = false
            val unread = chat.unreadCounts[uid] ?: 0L
            if (unread > 0L) {
                val updatedUnread = chat.unreadCounts.toMutableMap().apply { put(uid, 0L) }
                batch.update(
                    db.collection("chats").document(chat.id),
                    mapOf("unreadCounts" to updatedUnread, "updatedAt" to FieldValue.serverTimestamp())
                )
                hasChanges = true
            }
            messages.filter { it.senderId != uid && uid !in it.readByUids }.takeLast(20).forEach { msg ->
                val ref = db.collection("chats").document(chat.id).collection("messages").document(msg.id)
                batch.update(
                    ref,
                    mapOf(
                        "deliveredToUids" to (msg.deliveredToUids + uid).distinct(),
                        "readByUids" to (msg.readByUids + uid).distinct(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                hasChanges = true
            }
            if (hasChanges) batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
            Result.failure(e)
        }
    }

    suspend fun setTyping(chat: Chat, isTypingNow: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val current = chat.typingUids
            val next = if (isTypingNow) (current + uid).distinct().take(20) else current.filter { it != uid }
            if (next != current) {
                db.collection("chats").document(chat.id).update(
                    mapOf("typingUids" to next, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    suspend fun toggleReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> {
        var docPath = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val nextReactions = message.reactions.toMutableMap()
            if (nextReactions[uid] == emoji) nextReactions.remove(uid) else nextReactions[uid] = emoji
            db.collection("chats").document(chatId).collection("messages").document(message.id)
                .update(mapOf("reactions" to nextReactions, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleStarMessage(chatId: String, message: ChatMessage): Result<Unit> {
        var docPath = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val next = if (uid in message.starredByUids) message.starredByUids - uid else message.starredByUids + uid
            db.collection("chats").document(chatId).collection("messages").document(message.id)
                .update(mapOf("starredByUids" to next.distinct(), "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun editMessage(chatId: String, message: ChatMessage, newText: String): Result<Unit> {
        var docPath = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            require(message.senderId == uid) { "Only sender can edit message" }
            db.collection("chats").document(chatId).collection("messages").document(message.id)
                .update(
                    mapOf(
                        "text" to newText.trim().take(4000),
                        "cipherText" to "",
                        "iv" to "",
                        "edited" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(chatId: String, message: ChatMessage, forEveryone: Boolean): Result<Unit> {
        var docPath = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("chats").document(chatId).collection("messages").document(message.id)
            if (forEveryone && message.senderId == uid) {
                ref.update(
                    mapOf(
                        "text" to "This message was deleted",
                        "cipherText" to "",
                        "iv" to "",
                        "deletedForEveryone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "deletedForUids" to (message.deletedForUids + uid).distinct(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleChatFlag(chat: Chat, flag: String): Result<Unit> {
        var docPath = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            val (field, list) = when (flag) {
                "PIN" -> "pinnedByUids" to chat.pinnedByUids
                "ARCHIVE" -> "archivedByUids" to chat.archivedByUids
                else -> "mutedByUids" to chat.mutedByUids
            }
            val next = if (uid in list) list - uid else list + uid
            db.collection("chats").document(chat.id)
                .update(mapOf(field to next.distinct(), "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDisappearingTimer(chatId: String, hours: Long): Result<Unit> {
        val allowed = if (hours in listOf(0L, 24L, 168L)) hours else 0L
        return try {
            requireUserId()
            db.collection("chats").document(chatId)
                .update(mapOf("disappearingHours" to allowed, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun updateGroupDetailsAndPermissions(
        chatId: String,
        title: String,
        description: String,
        onlyAdminsCanSend: Boolean,
        announcementOnly: Boolean
    ): Result<Unit> {
        return try {
            requireUserId()
            db.collection("chats").document(chatId).update(
                mapOf(
                    "title" to title.trim().take(80),
                    "description" to description.trim().take(300),
                    "onlyAdminsCanSend" to onlyAdminsCanSend,
                    "announcementOnly" to announcementOnly,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun updateGroupMembers(
        chatId: String,
        memberUids: List<String>,
        coAdminUids: List<String>
    ): Result<Unit> {
        return try {
            requireUserId()
            require(memberUids.size >= 2) { "A group must keep at least 2 members." }
            db.collection("chats").document(chatId).update(
                mapOf(
                    "memberUids" to memberUids.distinct().take(100),
                    "coAdminUids" to coAdminUids.distinct().take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun getStarredMessagesForUser(chats: List<Chat>, userId: String): List<Pair<Chat, ChatMessage>> {
        val results = mutableListOf<Pair<Chat, ChatMessage>>()
        for (chat in chats.take(25)) {
            try {
                val snap = db.collection("chats").document(chat.id).collection("messages")
                    .whereArrayContains("memberUids", userId)
                    .get()
                    .await()
                val msgs = snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { userId in it.starredByUids && userId !in it.deletedForUids && !it.deletedForEveryone }
                msgs.forEach { msg -> results.add(chat to msg) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Skip individual inaccessible chat without crashing starred list
            }
        }
        return results.sortedByDescending { (_, m) -> (m.createdAt ?: Timestamp(0, 0)).seconds }
    }
}
