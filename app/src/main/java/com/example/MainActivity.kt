package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.SmilePreferences
import com.example.data.local.SmileSettingsState
import com.example.data.local.ThemeMode
import com.example.data.model.CallStatus
import com.example.data.model.Community
import com.example.data.model.MessageType
import com.example.data.model.UiState
import com.example.data.remote.AuthHelper
import com.example.data.remote.ChatRepository
import com.example.data.remote.SocialAndCallRepository
import com.example.data.remote.UserDirectoryRepository
import com.example.localization.AppLanguage
import com.example.localization.EnglishStrings
import com.example.localization.HindiStrings
import com.example.localization.LocalSmileStrings
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreateCommunityDialog
import com.example.ui.screens.CreateGroupDialog
import com.example.ui.screens.CreateStatusDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JoinCommunityDialog
import com.example.ui.screens.NewChatDialog
import com.example.ui.screens.PostAnnouncementDialog
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.QrProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StarredMessagesScreen
import com.example.ui.screens.StatusViewerScreen
import com.example.ui.screens.resolveChatDisplayTitle
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileTheme
import com.example.ui.theme.SmileYellow
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.ui.viewmodel.SmileMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_CHAT = "chat/{chatId}"
private const val ROUTE_CALL = "call/{callId}"
private const val ROUTE_STATUS = "status/{statusId}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_EDIT_PROFILE = "edit_profile"
private const val ROUTE_QR_PROFILE = "qr_profile"
private const val ROUTE_STARRED = "starred"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmileAppRoot()
        }
    }
}

@Composable
fun SmileAppRoot() {
    val context = LocalContext.current
    val preferences = remember(context) { SmilePreferences(context.applicationContext) }
    val settings by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = SmileSettingsState())

    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val strings = remember(settings.language) {
        if (settings.language == AppLanguage.HINDI) HindiStrings else EnglishStrings
    }

    var showSplash by remember { mutableStateOf(true) }
    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    SmileTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalSmileStrings provides strings) {
            when {
                showSplash -> SplashScreen(onFinished = { showSplash = false })
                firebaseUser == null -> AuthScreen(
                    onAuthSuccess = { firebaseUser = Firebase.auth.currentUser }
                )
                else -> key(firebaseUser!!.uid) {
                    SmileAuthenticatedHost(
                        user = firebaseUser!!,
                        preferences = preferences,
                        settings = settings,
                        onSignedOut = { firebaseUser = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmileAuthenticatedHost(
    user: FirebaseUser,
    preferences: SmilePreferences,
    settings: SmileSettingsState,
    onSignedOut: () -> Unit
) {
    val uid = user.uid
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val mainViewModel: SmileMainViewModel = viewModel(
        key = "main_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SmileMainViewModel(
                    currentUserId = uid,
                    userRepo = UserDirectoryRepository(db),
                    chatRepo = ChatRepository(db),
                    socialRepo = SocialAndCallRepository(db),
                    preferences = preferences
                )
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uid) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                mainViewModel.setPresenceOffline()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var unlockedInSession by rememberSaveable(uid) { mutableStateOf(false) }
    if (settings.appLockPinHash.isNotBlank() && !unlockedInSession) {
        AppLockScreen(
            expectedPinHash = settings.appLockPinHash,
            userId = uid,
            onUnlocked = { unlockedInSession = true }
        )
        return
    }

    val accountState by mainViewModel.accountState.collectAsStateWithLifecycle()
    val chatsState by mainViewModel.chatsState.collectAsStateWithLifecycle()
    val contactsState by mainViewModel.contactsState.collectAsStateWithLifecycle()
    val blockedUsers by mainViewModel.blockedState.collectAsStateWithLifecycle()
    val callsState by mainViewModel.callsState.collectAsStateWithLifecycle()
    val statusState by mainViewModel.statusState.collectAsStateWithLifecycle()
    val communitiesState by mainViewModel.communitiesState.collectAsStateWithLifecycle()
    val announcements by mainViewModel.announcementsState.collectAsStateWithLifecycle()
    val peerProfiles by mainViewModel.peerProfiles.collectAsStateWithLifecycle()
    val starredMessages by mainViewModel.starredMessages.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mainViewModel) {
        mainViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var isSavingProfile by remember { mutableStateOf(false) }

    when (val accState = accountState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmileYellow)
            }
        }
        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(accState.message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        AuthHelper.signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) {
                    Text("Sign Out & Retry")
                }
            }
        }
        is UiState.Success -> {
            val account = accState.data
            if (account == null) {
                ProfileSetupScreen(
                    existingAccount = null,
                    defaultName = user.displayName.orEmpty().ifBlank { "SMILE User" },
                    isSaving = isSavingProfile,
                    onSave = { name, smileId, phone, about, avatar ->
                        isSavingProfile = true
                        mainViewModel.saveUserProfile(name, smileId, phone, about, avatar, null) {
                            isSavingProfile = false
                        }
                    }
                )
            } else {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                val chatsList = (chatsState as? UiState.Success)?.data ?: emptyList()
                val contactsList = (contactsState as? UiState.Success)?.data ?: emptyList()
                val callsList = (callsState as? UiState.Success)?.data ?: emptyList()
                val statusList = (statusState as? UiState.Success)?.data ?: emptyList()

                var showNewChatDialog by remember { mutableStateOf(false) }
                var showCreateGroupDialog by remember { mutableStateOf(false) }
                var showCreateStatusDialog by remember { mutableStateOf(false) }
                var showCreateCommunityDialog by remember { mutableStateOf(false) }
                var showJoinCommunityDialog by remember { mutableStateOf(false) }
                var announcementCommunity by remember { mutableStateOf<Community?>(null) }
                var reportTargetUid by remember { mutableStateOf<String?>(null) }

                val shareInvite: () -> Unit = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Let's chat & video call on SMILE (बात करें, मुस्कुराएं!) Search my ID: @${account.smileId} or ${account.phoneNumber}"
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, "Invite to SMILE"))
                }

                // Auto-present incoming ringing call screen when an active incoming call arrives
                val incomingCall = callsList.firstOrNull {
                    it.status == CallStatus.RINGING.name && it.callerId != uid
                }
                LaunchedEffect(incomingCall?.id) {
                    if (incomingCall != null && currentRoute != ROUTE_CALL) {
                        navController.navigate("call/${incomingCall.id}") {
                            launchSingleTop = true
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { _ ->
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                currentUserId = uid,
                                account = account,
                                chatsState = chatsState,
                                contactsState = contactsState,
                                callsState = callsState,
                                statusState = statusState,
                                communitiesState = communitiesState,
                                announcements = announcements,
                                peerProfiles = peerProfiles,
                                onOpenChat = { chatId -> navController.navigate("chat/$chatId") },
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onOpenQrProfile = { navController.navigate(ROUTE_QR_PROFILE) },
                                onOpenStarred = {
                                    mainViewModel.loadStarredMessages(chatsList)
                                    navController.navigate(ROUTE_STARRED)
                                },
                                onInviteFriends = shareInvite,
                                onOpenNewChat = { showNewChatDialog = true },
                                onOpenNewGroup = { showCreateGroupDialog = true },
                                onOpenStatusCreator = { showCreateStatusDialog = true },
                                onViewStatus = { status -> navController.navigate("status/${status.id}") },
                                onToggleMuteStatus = { mainViewModel.toggleMuteStatus(it) },
                                onOpenNewCommunity = { showCreateCommunityDialog = true },
                                onOpenJoinCommunity = { showJoinCommunityDialog = true },
                                onPostCommunityAnnouncement = { announcementCommunity = it },
                                onToggleCommunityMute = { mainViewModel.toggleCommunityMute(it) },
                                onStartCallFromHistory = { rec ->
                                    val isGroup = rec.isGroup
                                    val peerId = if (rec.callerId == uid) rec.calleeId else rec.callerId
                                    val peerName = if (rec.callerId == uid) rec.calleeName else rec.callerName
                                    val cType = com.example.data.model.CallType.entries.firstOrNull {
                                        it.name == rec.callType
                                    } ?: com.example.data.model.CallType.VOICE
                                    mainViewModel.startCall(
                                        account = account,
                                        calleeId = peerId,
                                        calleeName = peerName,
                                        chatId = rec.chatId,
                                        participantUids = rec.participantUids,
                                        callType = cType,
                                        isGroup = isGroup
                                    ) { newCallId ->
                                        navController.navigate("call/$newCallId")
                                    }
                                },
                                onToggleChatPin = { mainViewModel.toggleChatPin(it) },
                                onToggleChatArchive = { mainViewModel.toggleChatArchive(it) },
                                onToggleChatMute = { mainViewModel.toggleChatMute(it) }
                            )
                        }

                        composable(
                            route = ROUTE_CHAT,
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { entry ->
                            val chatId = entry.arguments?.getString("chatId").orEmpty()
                            val chat = chatsList.firstOrNull { it.id == chatId }
                            if (chat == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = SmileYellow)
                                }
                            } else {
                                val chatVm: ChatDetailViewModel = viewModel(
                                    key = "chat_${uid}_$chatId",
                                    factory = viewModelFactory {
                                        initializer {
                                            ChatDetailViewModel(
                                                chatId = chatId,
                                                currentUserId = uid,
                                                chatRepo = mainViewModel.chatRepo
                                            )
                                        }
                                    }
                                )
                                ChatScreen(
                                    chat = chat,
                                    allChats = chatsList,
                                    currentUserId = uid,
                                    account = account,
                                    contacts = contactsList,
                                    peerProfiles = peerProfiles,
                                    myPrivateKey = settings.e2eePrivateKey,
                                    wallpaper = settings.wallpaper,
                                    viewModel = chatVm,
                                    onBack = { navController.popBackStack() },
                                    onStartCall = { callType ->
                                        val title = resolveChatDisplayTitle(chat, uid, peerProfiles)
                                        val peerUid = chat.memberUids.firstOrNull { it != uid }.orEmpty()
                                        mainViewModel.startCall(
                                            account = account,
                                            calleeId = peerUid,
                                            calleeName = title,
                                            chatId = chat.id,
                                            participantUids = chat.memberUids,
                                            callType = callType,
                                            isGroup = chat.chatType == "GROUP"
                                        ) { callId ->
                                            navController.navigate("call/$callId")
                                        }
                                    },
                                    onBlockPeer = { peerId, peerName ->
                                        mainViewModel.blockUser(peerId, peerName)
                                        navController.popBackStack()
                                    },
                                    onReportPeer = { peerId -> reportTargetUid = peerId },
                                    onExportChat = { txt ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "SMILE Chat Export")
                                            putExtra(Intent.EXTRA_TEXT, txt)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Export Chat"))
                                    },
                                    onToast = { mainViewModel.emitMessage(it) }
                                )
                            }
                        }

                        composable(
                            route = ROUTE_CALL,
                            arguments = listOf(navArgument("callId") { type = NavType.StringType })
                        ) { entry ->
                            val callId = entry.arguments?.getString("callId").orEmpty()
                            val call = callsList.firstOrNull { it.id == callId }
                            LaunchedEffect(call?.status) {
                                if (call != null && call.status in listOf(
                                        CallStatus.ENDED.name,
                                        CallStatus.MISSED.name,
                                        CallStatus.DECLINED.name
                                    )
                                ) {
                                    navController.popBackStack()
                                }
                            }
                            if (call != null) {
                                ActiveCallScreen(
                                    call = call,
                                    currentUserId = uid,
                                    onAnswer = {
                                        mainViewModel.updateCallStatus(call.id, CallStatus.CONNECTED)
                                    },
                                    onDecline = {
                                        mainViewModel.updateCallStatus(call.id, CallStatus.DECLINED)
                                        navController.popBackStack()
                                    },
                                    onEndCall = { duration ->
                                        val nextStatus = if (call.status == CallStatus.RINGING.name && call.callerId == uid) {
                                            CallStatus.MISSED
                                        } else {
                                            CallStatus.ENDED
                                        }
                                        mainViewModel.updateCallStatus(call.id, nextStatus, duration)
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = SmileYellow)
                                }
                            }
                        }

                        composable(
                            route = ROUTE_STATUS,
                            arguments = listOf(navArgument("statusId") { type = NavType.StringType })
                        ) { entry ->
                            val statusId = entry.arguments?.getString("statusId").orEmpty()
                            val status = statusList.firstOrNull { it.id == statusId }
                            if (status != null) {
                                LaunchedEffect(status.id) {
                                    mainViewModel.markStatusViewed(status, account)
                                }
                                StatusViewerScreen(
                                    status = status,
                                    currentUserId = uid,
                                    viewersFlow = mainViewModel.observeStatusViews(status.id),
                                    onReplyToStatus = { replyText ->
                                        scope.launch {
                                            val chatRes = mainViewModel.chatRepo.getOrCreateDirectChat(
                                                peerUid = status.ownerId,
                                                peerName = status.ownerName,
                                                peerAvatar = status.ownerAvatar
                                            )
                                            chatRes.getOrNull()?.let { chatId ->
                                                val targetChat = chatsList.firstOrNull { it.id == chatId }
                                                    ?: com.example.data.model.Chat(
                                                        id = chatId,
                                                        chatType = "DIRECT",
                                                        title = status.ownerName,
                                                        memberUids = listOf(uid, status.ownerId).sorted()
                                                    )
                                                mainViewModel.chatRepo.sendMessage(
                                                    chat = targetChat,
                                                    senderName = account.displayName,
                                                    messageType = MessageType.TEXT,
                                                    text = "Replied to status: $replyText"
                                                )
                                                mainViewModel.emitMessage("Reply sent to ${status.ownerName}")
                                            }
                                        }
                                    },
                                    onMuteStatus = { mainViewModel.toggleMuteStatus(status) },
                                    onDeleteStatus = { mainViewModel.deleteStatus(status.id) },
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    TextButton(onClick = { navController.popBackStack() }) {
                                        Text("Status expired. Go back")
                                    }
                                }
                            }
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                currentUserId = uid,
                                account = account,
                                settings = settings,
                                blockedUsers = blockedUsers,
                                chats = chatsList,
                                preferences = preferences,
                                backupJson = mainViewModel.buildBackupJson(account, chatsList),
                                onEditProfile = { navController.navigate(ROUTE_EDIT_PROFILE) },
                                onOpenQrProfile = { navController.navigate(ROUTE_QR_PROFILE) },
                                onUpdatePrivacy = { ls, ph, ab, rr, ts, pinHash ->
                                    mainViewModel.updatePrivacy(ls, ph, ab, rr, ts, pinHash)
                                },
                                onUnblockUser = { mainViewModel.unblockUser(it) },
                                onSubmitBugReport = { reason, details ->
                                    mainViewModel.submitReport("", "BUG", reason, details)
                                },
                                onSignOut = {
                                    mainViewModel.setPresenceOffline()
                                    AuthHelper.signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
                                },
                                onDeleteAccount = {
                                    mainViewModel.deleteAccount(account) {
                                        AuthHelper.signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_EDIT_PROFILE) {
                            BackHandler { navController.popBackStack() }
                            ProfileSetupScreen(
                                existingAccount = account,
                                defaultName = account.displayName,
                                isSaving = isSavingProfile,
                                onSave = { name, smileId, phone, about, avatar ->
                                    isSavingProfile = true
                                    mainViewModel.saveUserProfile(name, smileId, phone, about, avatar, account) { ok ->
                                        isSavingProfile = false
                                        if (ok) navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(ROUTE_QR_PROFILE) {
                            QrProfileScreen(
                                account = account,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_STARRED) {
                            StarredMessagesScreen(
                                currentUserId = uid,
                                starredState = starredMessages,
                                peerProfiles = peerProfiles,
                                myPrivateKey = settings.e2eePrivateKey,
                                onOpenChat = { chatId -> navController.navigate("chat/$chatId") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }

                if (showNewChatDialog) {
                    NewChatDialog(
                        contacts = contactsList,
                        onSearchUser = { q, cb -> mainViewModel.searchUser(q, cb) },
                        onSelectLookup = { lookup ->
                            mainViewModel.startChatWithLookup(lookup) { chatId ->
                                navController.navigate("chat/$chatId")
                            }
                        },
                        onSelectContact = { contact ->
                            mainViewModel.startChatWithContact(contact) { chatId ->
                                navController.navigate("chat/$chatId")
                            }
                        },
                        onSyncContacts = { mainViewModel.syncContacts(context) },
                        onInviteFriends = shareInvite,
                        onJoinGroupCode = { code ->
                            mainViewModel.joinGroupByCode(code) { chatId ->
                                navController.navigate("chat/$chatId")
                            }
                        },
                        onDismiss = { showNewChatDialog = false }
                    )
                }

                if (showCreateGroupDialog) {
                    CreateGroupDialog(
                        contacts = contactsList,
                        onCreateGroup = { title, desc, members ->
                            mainViewModel.createGroup(title, desc, members) { chatId ->
                                navController.navigate("chat/$chatId")
                            }
                        },
                        onDismiss = { showCreateGroupDialog = false }
                    )
                }

                if (showCreateStatusDialog) {
                    CreateStatusDialog(
                        onPostStatus = { mediaType, text, caption, bgHex, mediaData, privacy ->
                            mainViewModel.postStatus(
                                account = account,
                                mediaType = mediaType,
                                text = text,
                                caption = caption,
                                bgHex = bgHex,
                                mediaData = mediaData,
                                privacyMode = privacy,
                                contacts = contactsList
                            )
                        },
                        onDismiss = { showCreateStatusDialog = false }
                    )
                }

                if (showCreateCommunityDialog) {
                    CreateCommunityDialog(
                        chats = chatsList,
                        contacts = contactsList,
                        onCreate = { name, desc, ann, linked, members ->
                            mainViewModel.createCommunity(name, desc, ann, linked, members)
                        },
                        onDismiss = { showCreateCommunityDialog = false }
                    )
                }

                if (showJoinCommunityDialog) {
                    JoinCommunityDialog(
                        onJoin = { code -> mainViewModel.joinCommunity(code) },
                        onDismiss = { showJoinCommunityDialog = false }
                    )
                }

                announcementCommunity?.let { comm ->
                    PostAnnouncementDialog(
                        community = comm,
                        onPost = { text -> mainViewModel.postCommunityAnnouncement(comm.id, text) },
                        onDismiss = { announcementCommunity = null }
                    )
                }

                reportTargetUid?.let { target ->
                    var reason by remember { mutableStateOf("Spam or Harassment") }
                    var details by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { reportTargetUid = null },
                        title = { Text("Report User") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    label = { Text("Reason") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = details,
                                    onValueChange = { details = it },
                                    label = { Text("Additional details (optional)") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    mainViewModel.submitReport(target, "USER", reason.ifBlank { "Abuse" }, details)
                                    reportTargetUid = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                            ) {
                                Text("Submit Report")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { reportTargetUid = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
