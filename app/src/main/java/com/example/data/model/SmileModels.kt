package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class VisibilityOption(val label: String) {
    EVERYONE("Everyone"),
    CONTACTS("My Contacts"),
    NOBODY("Nobody")
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, DOCUMENT, VOICE, CONTACT, LOCATION, STICKER, SYSTEM
}

enum class CallType {
    VOICE, VIDEO
}

enum class CallStatus {
    RINGING, CONNECTED, ENDED, MISSED, DECLINED
}

enum class StatusPrivacyMode {
    ALL_CONTACTS, SELECTED, EXCEPT
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val smileId: String = "",
    val smileIdLower: String = "",
    val phoneNumber: String = "",
    val phoneHash: String = "",
    val email: String = "",
    val about: String = "बात करें, मुस्कुराएं!",
    val avatarData: String = "",
    val publicKey: String = "",
    val online: Boolean = false,
    val lastSeenVisibility: String = VisibilityOption.EVERYONE.name,
    val photoVisibility: String = VisibilityOption.EVERYONE.name,
    val aboutVisibility: String = VisibilityOption.EVERYONE.name,
    val readReceiptsEnabled: Boolean = true,
    val twoStepEnabled: Boolean = false,
    val twoStepPinHash: String = "",
    val suspended: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastSeenAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val smileId: String = "",
    val smileIdLower: String = "",
    val phoneHash: String = "",
    val about: String = "",
    val avatarData: String = "",
    val publicKey: String = "",
    val online: Boolean = false,
    val lastSeenVisibility: String = VisibilityOption.EVERYONE.name,
    val photoVisibility: String = VisibilityOption.EVERYONE.name,
    val aboutVisibility: String = VisibilityOption.EVERYONE.name,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectoryLookup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val smileId: String = "",
    val displayName: String = "",
    val avatarData: String = "",
    val about: String = "",
    val publicKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedContact(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val contactUid: String = "",
    val displayName: String = "",
    val smileId: String = "",
    val phoneHint: String = "",
    val avatarData: String = "",
    val about: String = "",
    val publicKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val blockedUid: String = "",
    val displayName: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Chat(
    @DocumentId val id: String = "",
    val chatType: String = "DIRECT",
    val title: String = "",
    val description: String = "",
    val avatarData: String = "",
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val coAdminUids: List<String> = emptyList(),
    val createdBy: String = "",
    val onlyAdminsCanSend: Boolean = false,
    val onlyAdminsCanEditInfo: Boolean = true,
    val announcementOnly: Boolean = false,
    val inviteCode: String = "",
    val communityId: String = "",
    val disappearingHours: Long = 0L,
    val lastMessageText: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val lastMessageSenderId: String = "",
    val lastMessageSenderName: String = "",
    val pinnedByUids: List<String> = emptyList(),
    val archivedByUids: List<String> = emptyList(),
    val mutedByUids: List<String> = emptyList(),
    val typingUids: List<String> = emptyList(),
    val unreadCounts: Map<String, Long> = emptyMap(),
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val messageType: String = MessageType.TEXT.name,
    val text: String = "",
    val cipherText: String = "",
    val iv: String = "",
    val e2ee: Boolean = false,
    val mediaData: String = "",
    val mediaName: String = "",
    val mediaMime: String = "",
    val mediaDurationSec: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "",
    val sharedContactName: String = "",
    val sharedContactPhone: String = "",
    val replyToMessageId: String = "",
    val replyToText: String = "",
    val replyToSenderName: String = "",
    val forwarded: Boolean = false,
    val edited: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val deletedForUids: List<String> = emptyList(),
    val starredByUids: List<String> = emptyList(),
    val deliveredToUids: List<String> = emptyList(),
    val readByUids: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    val expiresAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallRecord(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val chatId: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = CallType.VOICE.name,
    @get:com.google.firebase.firestore.PropertyName("isGroup")
    @com.google.firebase.firestore.PropertyName("isGroup")
    val isGroup: Boolean = false,
    val status: String = CallStatus.RINGING.name,
    val durationSeconds: Long = 0L,
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StatusUpdate(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerAvatar: String = "",
    val mediaType: String = "TEXT",
    val text: String = "",
    val caption: String = "",
    val backgroundColorHex: String = "#FFD600",
    val mediaData: String = "",
    val privacyMode: String = StatusPrivacyMode.ALL_CONTACTS.name,
    val allowedViewerUids: List<String> = emptyList(),
    val mutedByUids: List<String> = emptyList(),
    val viewedByUids: List<String> = emptyList(),
    val expiresAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StatusView(
    @DocumentId val id: String = "",
    val statusOwnerId: String = "",
    val viewerId: String = "",
    val viewerName: String = "",
    val viewerAvatar: String = "",
    @ServerTimestamp val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconData: String = "",
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val linkedChatIds: List<String> = emptyList(),
    val announcementText: String = "",
    val inviteCode: String = "",
    val mutedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val active: Boolean = true,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
