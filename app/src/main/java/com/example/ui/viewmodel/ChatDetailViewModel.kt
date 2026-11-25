package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.data.model.UiState
import com.example.data.remote.ChatRepository
import com.example.data.remote.unwrapFirestoreError
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DecryptedChatMessage(
    val raw: ChatMessage,
    val displayText: String
)

class ChatDetailViewModel(
    private val chatId: String,
    private val currentUserId: String,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _replyTo = MutableStateFlow<ChatMessage?>(null)
    val replyTo: StateFlow<ChatMessage?> = _replyTo.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = chatRepo.observeMessages(chatId, currentUserId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load messages"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun setReplyTo(msg: ChatMessage?) {
        _replyTo.value = msg
        if (msg != null) _editingMessage.value = null
    }

    fun setEditingMessage(msg: ChatMessage?) {
        _editingMessage.value = msg
        if (msg != null) _replyTo.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resolveDisplayText(
        msg: ChatMessage,
        myPrivateKey: String,
        peerPublicKey: String
    ): String {
        if (msg.deletedForEveryone) return "This message was deleted"
        if (!msg.e2ee || msg.cipherText.isBlank()) return msg.text
        return CryptoManager.decryptMessage(msg.cipherText, msg.iv, myPrivateKey, peerPublicKey)
            ?: msg.text.ifBlank { "[Encrypted message]" }
    }

    fun sendTextOrSaveEdit(
        chat: Chat,
        senderName: String,
        input: String,
        myPrivateKey: String,
        peerPublicKey: String,
        onError: (String) -> Unit
    ) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val editing = _editingMessage.value
        val reply = _replyTo.value
        viewModelScope.launch {
            if (editing != null) {
                val res = chatRepo.editMessage(chat.id, editing, trimmed)
                if (res.isSuccess) _editingMessage.value = null
                else onError(res.exceptionOrNull()?.localizedMessage ?: "Edit failed")
            } else {
                val res = chatRepo.sendMessage(
                    chat = chat,
                    senderName = senderName,
                    messageType = MessageType.TEXT,
                    text = trimmed,
                    myPrivateKey = myPrivateKey,
                    peerPublicKey = peerPublicKey,
                    replyTo = reply
                )
                if (res.isSuccess) _replyTo.value = null
                else onError(res.exceptionOrNull()?.localizedMessage ?: "Send failed")
            }
        }
    }

    fun sendMediaMessage(
        chat: Chat,
        senderName: String,
        messageType: MessageType,
        text: String = "",
        mediaData: String = "",
        mediaName: String = "",
        mediaMime: String = "",
        mediaDurationSec: Long = 0L,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String = "",
        sharedContactName: String = "",
        sharedContactPhone: String = "",
        onError: (String) -> Unit
    ) {
        val reply = _replyTo.value
        viewModelScope.launch {
            val res = chatRepo.sendMessage(
                chat = chat,
                senderName = senderName,
                messageType = messageType,
                text = text,
                mediaData = mediaData,
                mediaName = mediaName,
                mediaMime = mediaMime,
                mediaDurationSec = mediaDurationSec,
                latitude = latitude,
                longitude = longitude,
                locationLabel = locationLabel,
                sharedContactName = sharedContactName,
                sharedContactPhone = sharedContactPhone,
                replyTo = reply
            )
            if (res.isSuccess) _replyTo.value = null
            else onError(res.exceptionOrNull()?.localizedMessage ?: "Failed to send attachment")
        }
    }

    fun forwardMessage(targetChat: Chat, senderName: String, sourceDisplayText: String, source: ChatMessage) {
        viewModelScope.launch {
            chatRepo.sendMessage(
                chat = targetChat,
                senderName = senderName,
                messageType = MessageType.entries.firstOrNull { it.name == source.messageType } ?: MessageType.TEXT,
                text = sourceDisplayText,
                mediaData = source.mediaData,
                mediaName = source.mediaName,
                mediaMime = source.mediaMime,
                forwarded = true
            )
        }
    }

    fun markRead(chat: Chat, messages: List<ChatMessage>) {
        viewModelScope.launch { chatRepo.markChatRead(chat, messages) }
    }

    fun setTyping(chat: Chat, typing: Boolean) {
        viewModelScope.launch { chatRepo.setTyping(chat, typing) }
    }

    fun toggleReaction(msg: ChatMessage, emoji: String) {
        viewModelScope.launch { chatRepo.toggleReaction(chatId, msg, emoji) }
    }

    fun toggleStar(msg: ChatMessage) {
        viewModelScope.launch { chatRepo.toggleStarMessage(chatId, msg) }
    }

    fun deleteMessage(msg: ChatMessage, forEveryone: Boolean) {
        viewModelScope.launch { chatRepo.deleteMessage(chatId, msg, forEveryone) }
    }

    fun updateDisappearingTimer(hours: Long) {
        viewModelScope.launch { chatRepo.updateDisappearingTimer(chatId, hours) }
    }

    fun updateGroupSettings(title: String, description: String, onlyAdminsCanSend: Boolean, announcementOnly: Boolean) {
        viewModelScope.launch {
            chatRepo.updateGroupDetailsAndPermissions(chatId, title, description, onlyAdminsCanSend, announcementOnly)
        }
    }

    fun updateGroupMembers(memberUids: List<String>, coAdminUids: List<String>) {
        viewModelScope.launch {
            chatRepo.updateGroupMembers(chatId, memberUids, coAdminUids)
        }
    }
}
