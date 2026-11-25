package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatWallpaper
import com.example.data.model.CallType
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.data.model.PublicProfile
import com.example.data.model.SavedContact
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.localization.LocalSmileStrings
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.theme.SmileYellowTint
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.utils.MediaUtils
import com.google.android.gms.location.LocationServices
import java.io.File

@Composable
fun ChatScreen(
    chat: Chat,
    allChats: List<Chat>,
    currentUserId: String,
    account: UserAccount?,
    contacts: List<SavedContact>,
    peerProfiles: Map<String, PublicProfile>,
    myPrivateKey: String,
    wallpaper: ChatWallpaper,
    viewModel: ChatDetailViewModel,
    onBack: () -> Unit,
    onStartCall: (CallType) -> Unit,
    onBlockPeer: (String, String) -> Unit,
    onReportPeer: (String) -> Unit,
    onExportChat: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val strings = LocalSmileStrings.current
    val context = LocalContext.current
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val replyTo by viewModel.replyTo.collectAsStateWithLifecycle()
    val editingMsg by viewModel.editingMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val peerUid = chat.memberUids.firstOrNull { it != currentUserId }
    val peerProfile = peerUid?.let { peerProfiles[it] }
    val chatTitle = resolveChatDisplayTitle(chat, currentUserId, peerProfiles)
    val peerPublicKey = if (chat.chatType == "DIRECT") peerProfile?.publicKey.orEmpty() else ""

    var inputText by remember { mutableStateOf("") }
    var showEmojiStrip by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showGroupInfo by remember { mutableStateOf(false) }
    var showContactShareDialog by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardSource by remember { mutableStateOf<ChatMessage?>(null) }

    // Voice recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordStartMs by remember { mutableLongStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }

    // Voice playback state
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.release() }
            runCatching { player?.release() }
            viewModel.setTyping(chat, false)
        }
    }

    LaunchedEffect(editingMsg) {
        if (editingMsg != null) {
            inputText = viewModel.resolveDisplayText(editingMsg!!, myPrivateKey, peerPublicKey)
        }
    }

    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            viewModel.markRead(chat, messages)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 720, quality = 68)
            if (base64 != null) {
                viewModel.sendMediaMessage(
                    chat = chat,
                    senderName = account?.displayName ?: "SMILE",
                    messageType = MessageType.IMAGE,
                    mediaData = base64,
                    mediaMime = "image/jpeg",
                    onError = onToast
                )
            } else {
                onToast("Could not read selected image")
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val doc = MediaUtils.readDocumentUri(context, uri)
            if (doc != null) {
                viewModel.sendMediaMessage(
                    chat = chat,
                    senderName = account?.displayName ?: "SMILE",
                    messageType = MessageType.DOCUMENT,
                    text = doc.name,
                    mediaData = doc.base64Data,
                    mediaName = doc.name,
                    mediaMime = doc.mimeType,
                    onError = onToast
                )
            } else {
                onToast("Unable to attach document")
            }
        }
    }

    fun startVoiceRecording() {
        val file = File(context.cacheDir, "smile_voice_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(32000)
            mr.setAudioSamplingRate(16000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            recordFile = file
            recordStartMs = System.currentTimeMillis()
            isRecording = true
        } catch (e: Exception) {
            runCatching { mr.release() }
            onToast("Microphone unavailable")
        }
    }

    fun stopAndSendVoiceRecording() {
        val mr = recorder ?: return
        val file = recordFile
        val durSec = ((System.currentTimeMillis() - recordStartMs) / 1000L).coerceAtLeast(1L)
        isRecording = false
        recorder = null
        runCatching {
            mr.stop()
            mr.release()
        }
        if (file != null && file.exists()) {
            val bytes = file.readBytes()
            file.delete()
            if (bytes.size <= 320_000) {
                viewModel.sendMediaMessage(
                    chat = chat,
                    senderName = account?.displayName ?: "SMILE",
                    messageType = MessageType.VOICE,
                    mediaData = MediaUtils.encodeBytesToBase64(bytes),
                    mediaMime = "audio/mp4",
                    mediaDurationSec = durSec,
                    onError = onToast
                )
            } else {
                onToast("Voice note too long for direct sync (keep under 45s)")
            }
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecording() else onToast("Microphone permission required for voice notes")
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            runCatching {
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.sendMediaMessage(
                            chat = chat,
                            senderName = account?.displayName ?: "SMILE",
                            messageType = MessageType.LOCATION,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            locationLabel = "Current Location",
                            onError = onToast
                        )
                    } else {
                        onToast("Location unavailable right now; ensure GPS is on.")
                    }
                }
            }
        } else {
            onToast("Location permission is needed to share your location.")
        }
    }

    val bgColor = when (wallpaper) {
        ChatWallpaper.WHITE -> SmileWhite
        ChatWallpaper.CREAM -> Color(0xFFFFFBF0)
        ChatWallpaper.SUNNY -> SmileYellowTint
        ChatWallpaper.MINT -> Color(0xFFF1FBF4)
        ChatWallpaper.SLATE -> Color(0xFF1A1D24)
    }

    val canSendInGroup = chat.chatType == "DIRECT" ||
        ((!chat.onlyAdminsCanSend && !chat.announcementOnly) ||
            currentUserId in chat.adminUids || currentUserId in chat.coAdminUids)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileInk)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (chat.chatType == "GROUP") showGroupInfo = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarData = if (chat.chatType == "GROUP") chat.avatarData else peerProfile?.avatarData.orEmpty(),
                            name = chatTitle,
                            size = 40.dp,
                            isGroup = chat.chatType == "GROUP",
                            showOnlineDot = chat.chatType == "DIRECT",
                            online = peerProfile?.online == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chatTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SmileInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val peerTyping = chat.typingUids.any { it != currentUserId }
                            val subtitle = when {
                                peerTyping -> strings.typing
                                chat.chatType == "GROUP" -> "${chat.memberUids.size} members"
                                peerProfile?.online == true -> strings.online
                                peerProfile?.lastSeenAt != null -> "Last seen ${formatShortTime(peerProfile.lastSeenAt.toDate())}"
                                else -> "Tap for info"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (peerTyping || peerProfile?.online == true) SmileGreen else SmileInk.copy(alpha = 0.72f)
                            )
                        }
                    }
                    IconButton(onClick = { onStartCall(CallType.VOICE) }, modifier = Modifier.testTag("chat_voice_call_btn")) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = SmileInk)
                    }
                    IconButton(onClick = { onStartCall(CallType.VIDEO) }, modifier = Modifier.testTag("chat_video_call_btn")) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = SmileInk)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Chat options", tint = SmileInk)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (chat.chatType == "GROUP") {
                                DropdownMenuItem(
                                    text = { Text("Group Info & Members") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = { showMenu = false; showGroupInfo = true }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Search messages") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = { showMenu = false; showSearchBar = !showSearchBar }
                            )
                            DropdownMenuItem(
                                text = { Text("Disappearing messages") },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                onClick = { showMenu = false; showDisappearingDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Export chat (.txt)") },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val exported = messages.joinToString("\n") { m ->
                                        "[${formatShortTime(m.createdAt?.toDate())}] ${m.senderName}: ${viewModel.resolveDisplayText(m, myPrivateKey, peerPublicKey)}"
                                    }
                                    onExportChat(exported)
                                }
                            )
                            if (peerUid != null && chat.chatType == "DIRECT") {
                                DropdownMenuItem(
                                    text = { Text("Block $chatTitle") },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                    onClick = { showMenu = false; onBlockPeer(peerUid, chatTitle) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report user") },
                                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                                    onClick = { showMenu = false; onReportPeer(peerUid) }
                                )
                            }
                        }
                    }
                }
                if (showSearchBar) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search in chat...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SmileWhite,
                            unfocusedContainerColor = SmileWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.setSearchQuery(""); showSearchBar = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (replyTo != null || editingMsg != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SmileYellowSoft)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMsg != null) "Editing message" else "Replying to ${replyTo!!.senderName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SmileInk
                            )
                            Text(
                                text = if (editingMsg != null) editingMsg!!.text else replyTo!!.text.ifBlank { replyTo!!.messageType },
                                style = MaterialTheme.typography.bodySmall,
                                color = SmileInk.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.setReplyTo(null); viewModel.setEditingMessage(null); inputText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = SmileInk)
                        }
                    }
                }

                if (showEmojiStrip) {
                    val quickStickers = listOf("😊", "😂", "❤️", "🙏", "🎉", "🔥", "👍", "🥳", "🌟", "😎")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(quickStickers) { emoji ->
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { inputText += emoji }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                if (showAttachSheet) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SmileYellowTint)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AttachOption(Icons.Default.Image, "Photo") {
                            showAttachSheet = false
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        AttachOption(Icons.Default.Description, "Document") {
                            showAttachSheet = false
                            docPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*"))
                        }
                        AttachOption(Icons.Default.Person, "Contact") {
                            showAttachSheet = false
                            showContactShareDialog = true
                        }
                        AttachOption(Icons.Default.LocationOn, "Location") {
                            showAttachSheet = false
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            } else {
                                locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                    }
                }

                if (!canSendInGroup) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Only group admins can send messages here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmojiStrip = !showEmojiStrip; showAttachSheet = false }) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji & Stickers")
                        }
                        IconButton(onClick = { showAttachSheet = !showAttachSheet; showEmojiStrip = false }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.setTyping(chat, it.isNotBlank())
                            },
                            placeholder = { Text(if (isRecording) "Recording voice note..." else strings.typeMessageHint) },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (inputText.isNotBlank()) {
                            Surface(
                                color = SmileYellow,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_send_button")
                            ) {
                                IconButton(onClick = {
                                    val msg = inputText
                                    inputText = ""
                                    viewModel.setTyping(chat, false)
                                    viewModel.sendTextOrSaveEdit(
                                        chat = chat,
                                        senderName = account?.displayName ?: "SMILE User",
                                        input = msg,
                                        myPrivateKey = myPrivateKey,
                                        peerPublicKey = peerPublicKey,
                                        onError = onToast
                                    )
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = SmileInk)
                                }
                            }
                        } else {
                            Surface(
                                color = if (isRecording) Color.Red else SmileYellow,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_voice_button")
                            ) {
                                IconButton(onClick = {
                                    if (isRecording) {
                                        stopAndSendVoiceRecording()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) startVoiceRecording()
                                        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (isRecording) "Stop & Send Voice" else "Record Voice Note",
                                        tint = if (isRecording) SmileWhite else SmileInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (messagesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SmileYellow)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text((messagesState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val visibleMessages = remember(messages, searchQuery, myPrivateKey, peerPublicKey) {
                        if (searchQuery.isBlank()) messages
                        else messages.filter {
                            viewModel.resolveDisplayText(it, myPrivateKey, peerPublicKey)
                                .contains(searchQuery, ignoreCase = true)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        if (chat.chatType == "DIRECT") {
                            item {
                                Surface(
                                    color = SmileYellowSoft.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = SmileInk, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.e2eeNotice,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SmileInk
                                        )
                                    }
                                }
                            }
                        }
                        items(visibleMessages, key = { it.id }) { msg ->
                            val resolved = remember(msg.id, msg.text, msg.cipherText, msg.deletedForEveryone, myPrivateKey, peerPublicKey) {
                                viewModel.resolveDisplayText(msg, myPrivateKey, peerPublicKey)
                            }
                            ChatMessageBubble(
                                message = msg,
                                displayText = resolved,
                                isMine = msg.senderId == currentUserId,
                                isGroup = chat.chatType == "GROUP",
                                currentUserId = currentUserId,
                                isPlayingVoice = playingMessageId == msg.id,
                                onPlayVoiceToggle = {
                                    if (playingMessageId == msg.id) {
                                        runCatching { player?.stop(); player?.release() }
                                        player = null
                                        playingMessageId = null
                                    } else {
                                        val bytes = MediaUtils.decodeBase64ToBytes(msg.mediaData)
                                        if (bytes != null) {
                                            runCatching {
                                                player?.release()
                                                val temp = File(context.cacheDir, "play_${msg.id}.m4a")
                                                temp.writeBytes(bytes)
                                                val mp = MediaPlayer().apply {
                                                    setDataSource(temp.absolutePath)
                                                    prepare()
                                                    setOnCompletionListener {
                                                        playingMessageId = null
                                                        it.release()
                                                    }
                                                    start()
                                                }
                                                player = mp
                                                playingMessageId = msg.id
                                            }
                                        }
                                    }
                                },
                                onLongPress = { selectedMessage = msg }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMessage?.let { msg ->
        val resolved = viewModel.resolveDisplayText(msg, myPrivateKey, peerPublicKey)
        MessageActionsDialog(
            message = msg,
            isMine = msg.senderId == currentUserId,
            isStarred = currentUserId in msg.starredByUids,
            onReact = { emoji -> viewModel.toggleReaction(msg, emoji) },
            onReply = { viewModel.setReplyTo(msg) },
            onForward = { forwardSource = msg },
            onCopy = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("SMILE", resolved))
                onToast("Copied")
            },
            onToggleStar = { viewModel.toggleStar(msg) },
            onEdit = { viewModel.setEditingMessage(msg) },
            onDeleteForMe = { viewModel.deleteMessage(msg, forEveryone = false) },
            onDeleteForEveryone = { viewModel.deleteMessage(msg, forEveryone = true) },
            onDismiss = { selectedMessage = null }
        )
    }

    forwardSource?.let { src ->
        val resolved = viewModel.resolveDisplayText(src, myPrivateKey, peerPublicKey)
        AlertDialog(
            onDismissRequest = { forwardSource = null },
            title = { Text("Forward message to") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(allChats, key = { it.id }) { target ->
                        Text(
                            text = resolveChatDisplayTitle(target, currentUserId, peerProfiles),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.forwardMessage(target, account?.displayName ?: "SMILE", resolved, src)
                                    forwardSource = null
                                    onToast("Forwarded")
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { forwardSource = null }) { Text("Cancel") } }
        )
    }

    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages") },
            text = {
                Column {
                    listOf(0L to "Off", 24L to "24 Hours", 168L to "7 Days").forEach { (hrs, label) ->
                        DropdownMenuItem(
                            text = { Text(label + if (chat.disappearingHours == hrs) " ✓" else "") },
                            onClick = {
                                viewModel.updateDisappearingTimer(hrs)
                                showDisappearingDialog = false
                                onToast("Disappearing timer: $label")
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDisappearingDialog = false }) { Text("Close") } }
        )
    }

    if (showContactShareDialog) {
        AlertDialog(
            onDismissRequest = { showContactShareDialog = false },
            title = { Text("Share a SMILE Contact") },
            text = {
                if (contacts.isEmpty()) {
                    Text("No saved contacts yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(contacts, key = { it.contactUid }) { c ->
                            Text(
                                text = "${c.displayName} (@${c.smileId})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendMediaMessage(
                                            chat = chat,
                                            senderName = account?.displayName ?: "SMILE",
                                            messageType = MessageType.CONTACT,
                                            sharedContactName = c.displayName,
                                            sharedContactPhone = c.phoneHint.ifBlank { "@${c.smileId}" },
                                            onError = onToast
                                        )
                                        showContactShareDialog = false
                                    }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContactShareDialog = false }) { Text("Close") } }
        )
    }

    if (showGroupInfo) {
        GroupInfoDialog(
            chat = chat,
            currentUserId = currentUserId,
            contacts = contacts,
            peerProfiles = peerProfiles,
            onUpdateSettings = { t, d, adminsSend, ann ->
                viewModel.updateGroupSettings(t, d, adminsSend, ann)
            },
            onUpdateMembers = { m, co ->
                viewModel.updateGroupMembers(m, co)
            },
            onStartGroupCall = { video -> onStartCall(if (video) CallType.VIDEO else CallType.VOICE) },
            onDismiss = { showGroupInfo = false }
        )
    }
}

@Composable
private fun AttachOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SmileYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = SmileInk)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
