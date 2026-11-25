package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SmilePreferences
import com.example.data.local.SmileSettingsState
import com.example.data.model.Announcement
import com.example.data.model.BlockedUser
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.data.model.DirectoryLookup
import com.example.data.model.PublicProfile
import com.example.data.model.SavedContact
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.VisibilityOption
import com.example.data.remote.ChatRepository
import com.example.data.remote.SocialAndCallRepository
import com.example.data.remote.UserDirectoryRepository
import com.example.data.remote.unwrapFirestoreError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StarredMessageItem(
    val chat: Chat,
    val message: com.example.data.model.ChatMessage
)

class SmileMainViewModel(
    private val currentUserId: String,
    private val userRepo: UserDirectoryRepository,
    val chatRepo: ChatRepository,
    private val socialRepo: SocialAndCallRepository,
    val preferences: SmilePreferences
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _peerProfiles = MutableStateFlow<Map<String, PublicProfile>>(emptyMap())
    val peerProfiles: StateFlow<Map<String, PublicProfile>> = _peerProfiles.asStateFlow()

    private val _starredMessages = MutableStateFlow<UiState<List<StarredMessageItem>>>(UiState.Loading)
    val starredMessages: StateFlow<UiState<List<StarredMessageItem>>> = _starredMessages.asStateFlow()

    val settings: StateFlow<SmileSettingsState> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SmileSettingsState())

    val accountState: StateFlow<UiState<UserAccount?>> = userRepo.observeOwnAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chatsState: StateFlow<UiState<List<Chat>>> = chatRepo.observeChats(currentUserId)
        .map<List<Chat>, UiState<List<Chat>>> { chats ->
            prefetchPeersForChats(chats)
            UiState.Success(chats)
        }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contactsState: StateFlow<UiState<List<SavedContact>>> = userRepo.observeContacts(currentUserId)
        .map<List<SavedContact>, UiState<List<SavedContact>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load contacts"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val blockedState: StateFlow<List<BlockedUser>> = userRepo.observeBlockedUsers(currentUserId)
        .catch { error -> unwrapFirestoreError(error); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callsState: StateFlow<UiState<List<CallRecord>>> = socialRepo.observeCalls(currentUserId)
        .map<List<CallRecord>, UiState<List<CallRecord>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load calls"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val statusState: StateFlow<UiState<List<StatusUpdate>>> = socialRepo.observeStatusUpdates(currentUserId)
        .map<List<StatusUpdate>, UiState<List<StatusUpdate>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load updates"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communitiesState: StateFlow<UiState<List<Community>>> = socialRepo.observeCommunities(currentUserId)
        .map<List<Community>, UiState<List<Community>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load communities"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val announcementsState: StateFlow<List<Announcement>> = userRepo.observeAnnouncements()
        .catch { error -> unwrapFirestoreError(error); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            preferences.ensureE2eeKeyPair()
            userRepo.updatePresence(true)
        }
    }

    private fun prefetchPeersForChats(chats: List<Chat>) {
        val peerIds = chats.flatMap { it.memberUids }
            .distinct()
            .filter { it != currentUserId && !_peerProfiles.value.containsKey(it) }
        if (peerIds.isEmpty()) return
        viewModelScope.launch {
            val loaded = mutableMapOf<String, PublicProfile>()
            for (uid in peerIds.take(30)) {
                userRepo.getPublicProfile(uid)?.let { loaded[uid] = it }
            }
            if (loaded.isNotEmpty()) {
                _peerProfiles.value = _peerProfiles.value + loaded
            }
        }
    }

    fun emitMessage(msg: String) {
        _snackbarEvents.tryEmit(msg)
    }

    fun saveUserProfile(
        displayName: String,
        smileId: String,
        phoneE164: String,
        about: String,
        avatarData: String,
        existing: UserAccount?,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val keys = preferences.ensureE2eeKeyPair()
            val res = userRepo.saveUserProfile(
                displayName = displayName,
                smileId = smileId,
                phoneNumberE164 = phoneE164,
                about = about,
                avatarData = avatarData,
                publicKey = keys.publicKeyBase64,
                existingAccount = existing
            )
            if (res.isSuccess) {
                emitMessage("Profile saved!")
                onDone(true)
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Could not save profile")
                onDone(false)
            }
        }
    }

    fun searchUser(query: String, onResult: (DirectoryLookup?) -> Unit) {
        viewModelScope.launch {
            val res = userRepo.findUserByPhoneOrSmileId(query)
            if (res.isSuccess) {
                onResult(res.getOrNull())
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Search failed")
                onResult(null)
            }
        }
    }

    fun startChatWithLookup(lookup: DirectoryLookup, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            userRepo.saveContact(lookup)
            val res = chatRepo.getOrCreateDirectChat(lookup.userId, lookup.displayName, lookup.avatarData)
            if (res.isSuccess) {
                onChatReady(res.getOrThrow())
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Could not open chat")
            }
        }
    }

    fun startChatWithContact(contact: SavedContact, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val res = chatRepo.getOrCreateDirectChat(contact.contactUid, contact.displayName, contact.avatarData)
            if (res.isSuccess) {
                onChatReady(res.getOrThrow())
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Could not open chat")
            }
        }
    }

    fun createGroup(
        title: String,
        description: String,
        memberUids: List<String>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = chatRepo.createGroupChat(title, description, "", memberUids)
            if (res.isSuccess) {
                emitMessage("Group created")
                onCreated(res.getOrThrow())
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to create group")
            }
        }
    }

    fun joinGroupByCode(code: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            val res = chatRepo.joinGroupByInviteCode(code)
            if (res.isSuccess) {
                emitMessage("Joined group!")
                onJoined(res.getOrThrow())
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Invalid group invite")
            }
        }
    }

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            val res = userRepo.syncDeviceContacts(context)
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                emitMessage("Synced $count SMILE contact(s)")
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Contacts sync failed")
            }
        }
    }

    fun startCall(
        account: UserAccount?,
        calleeId: String,
        calleeName: String,
        chatId: String,
        participantUids: List<String>,
        callType: CallType,
        isGroup: Boolean,
        onStarted: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = socialRepo.startCall(
                callerName = account?.displayName ?: "SMILE User",
                callerAvatar = account?.avatarData ?: "",
                calleeId = calleeId,
                calleeName = calleeName,
                chatId = chatId,
                participantUids = participantUids,
                callType = callType,
                isGroup = isGroup
            )
            if (res.isSuccess) {
                onStarted(res.getOrThrow())
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Call could not connect")
            }
        }
    }

    fun updateCallStatus(callId: String, status: CallStatus, durationSeconds: Long = 0L) {
        viewModelScope.launch {
            val answer = if (status == CallStatus.CONNECTED) "v=0\r\no=smile-ans 1 1 IN IP4 0.0.0.0" else null
            socialRepo.updateCallStatus(callId, status, durationSeconds, answer)
        }
    }

    fun postStatus(
        account: UserAccount?,
        mediaType: String,
        text: String,
        caption: String,
        bgHex: String,
        mediaData: String,
        privacyMode: StatusPrivacyMode,
        contacts: List<SavedContact>
    ) {
        viewModelScope.launch {
            val allowed = contacts.map { it.contactUid }.distinct()
            val res = socialRepo.postStatus(
                ownerName = account?.displayName ?: "SMILE User",
                ownerAvatar = account?.avatarData ?: "",
                mediaType = mediaType,
                text = text,
                caption = caption,
                backgroundColorHex = bgHex,
                mediaData = mediaData,
                privacyMode = privacyMode,
                allowedViewerUids = allowed
            )
            if (res.isSuccess) emitMessage("Status shared!")
            else emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Status upload failed")
        }
    }

    fun markStatusViewed(status: StatusUpdate, account: UserAccount?) {
        viewModelScope.launch {
            socialRepo.markStatusViewed(
                status = status,
                viewerName = account?.displayName ?: "SMILE User",
                viewerAvatar = account?.avatarData ?: ""
            )
        }
    }

    fun observeStatusViews(statusId: String) = socialRepo.observeStatusViews(statusId, currentUserId)

    fun toggleMuteStatus(status: StatusUpdate) {
        viewModelScope.launch { socialRepo.toggleMuteStatus(status) }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch { socialRepo.deleteStatus(statusId) }
    }

    fun createCommunity(
        name: String,
        description: String,
        announcement: String,
        linkedChatIds: List<String>,
        memberUids: List<String>
    ) {
        viewModelScope.launch {
            val res = socialRepo.createCommunity(name, description, announcement, linkedChatIds, memberUids)
            if (res.isSuccess) emitMessage("Community created!")
            else emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to create community")
        }
    }

    fun postCommunityAnnouncement(communityId: String, text: String) {
        viewModelScope.launch {
            val res = socialRepo.postCommunityAnnouncement(communityId, text)
            if (res.isSuccess) emitMessage("Announcement posted")
            else emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed")
        }
    }

    fun toggleCommunityMute(community: Community) {
        viewModelScope.launch { socialRepo.toggleCommunityMute(community) }
    }

    fun joinCommunity(code: String) {
        viewModelScope.launch {
            val res = socialRepo.joinCommunityByInvite(code)
            if (res.isSuccess) emitMessage("Joined community!")
            else emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Invite not found")
        }
    }

    fun toggleChatPin(chat: Chat) {
        viewModelScope.launch { chatRepo.toggleChatFlag(chat, "PIN") }
    }

    fun toggleChatArchive(chat: Chat) {
        viewModelScope.launch { chatRepo.toggleChatFlag(chat, "ARCHIVE") }
    }

    fun toggleChatMute(chat: Chat) {
        viewModelScope.launch { chatRepo.toggleChatFlag(chat, "MUTE") }
    }

    fun blockUser(uid: String, name: String) {
        viewModelScope.launch {
            val res = userRepo.blockUser(uid, name)
            if (res.isSuccess) emitMessage("$name blocked")
        }
    }

    fun unblockUser(uid: String) {
        viewModelScope.launch {
            val res = userRepo.unblockUser(uid)
            if (res.isSuccess) emitMessage("User unblocked")
        }
    }

    fun submitReport(targetUid: String, type: String, reason: String, details: String) {
        viewModelScope.launch {
            val res = userRepo.submitReport(targetUid, type, reason, details)
            if (res.isSuccess) emitMessage("Report submitted. Thank you.")
            else emitMessage("Could not submit report")
        }
    }

    fun updatePrivacy(
        lastSeen: VisibilityOption,
        photo: VisibilityOption,
        about: VisibilityOption,
        readReceipts: Boolean,
        twoStepEnabled: Boolean,
        twoStepPinHash: String
    ) {
        viewModelScope.launch {
            val res = userRepo.updatePrivacySettings(lastSeen, photo, about, readReceipts, twoStepEnabled, twoStepPinHash)
            if (res.isSuccess) emitMessage("Privacy settings updated")
            else emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to update privacy")
        }
    }

    fun setPresenceOffline() {
        viewModelScope.launch { userRepo.updatePresence(false) }
    }

    fun deleteAccount(account: UserAccount?, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = userRepo.deleteAccount(account)
            if (res.isSuccess) {
                emitMessage("Account deleted")
                onDeleted()
            } else {
                emitMessage(res.exceptionOrNull()?.localizedMessage ?: "Re-sign in required before deleting account")
            }
        }
    }

    fun buildBackupJson(account: UserAccount?, chats: List<Chat>): String {
        val safeName = (account?.displayName ?: "").replace("\"", "'")
        val safeId = (account?.smileId ?: "").replace("\"", "'")
        return buildString {
            appendLine("{")
            appendLine("  \"app\": \"SMILE\",")
            appendLine("  \"displayName\": \"$safeName\",")
            appendLine("  \"smileId\": \"$safeId\",")
            appendLine("  \"chatCount\": ${chats.size},")
            appendLine("  \"chats\": [")
            chats.forEachIndexed { idx, chat ->
                val comma = if (idx < chats.lastIndex) "," else ""
                val title = chat.title.ifBlank { chat.id }.replace("\"", "'")
                val last = chat.lastMessageText.replace("\"", "'")
                appendLine("    {\"id\": \"${chat.id}\", \"type\": \"${chat.chatType}\", \"title\": \"$title\", \"last\": \"$last\"}$comma")
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    fun loadStarredMessages(chats: List<Chat>) {
        viewModelScope.launch {
            _starredMessages.value = UiState.Loading
            try {
                val items = chatRepo.getStarredMessagesForUser(chats, currentUserId)
                    .map { (chat, msg) -> StarredMessageItem(chat, msg) }
                _starredMessages.value = UiState.Success(items)
            } catch (e: Exception) {
                val root = unwrapFirestoreError(e)
                _starredMessages.value = UiState.Error(root.localizedMessage ?: "Failed to load starred messages")
            }
        }
    }
}
