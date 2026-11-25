package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.data.model.PublicProfile
import com.example.data.model.SavedContact
import com.example.data.model.StatusUpdate
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.localization.LocalSmileStrings
import com.example.ui.components.DeliveryTicks
import com.example.ui.components.SmileLogoMark
import com.example.ui.components.UserAvatar
import com.example.ui.theme.NotoDevanagariFontFamily
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileRed
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.theme.SmileYellowTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    currentUserId: String,
    account: UserAccount?,
    chatsState: UiState<List<Chat>>,
    contactsState: UiState<List<SavedContact>>,
    callsState: UiState<List<CallRecord>>,
    statusState: UiState<List<StatusUpdate>>,
    communitiesState: UiState<List<Community>>,
    announcements: List<Announcement>,
    peerProfiles: Map<String, PublicProfile>,
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQrProfile: () -> Unit,
    onOpenStarred: () -> Unit,
    onInviteFriends: () -> Unit,
    onOpenNewChat: () -> Unit,
    onOpenNewGroup: () -> Unit,
    onOpenStatusCreator: () -> Unit,
    onViewStatus: (StatusUpdate) -> Unit,
    onToggleMuteStatus: (StatusUpdate) -> Unit,
    onOpenNewCommunity: () -> Unit,
    onOpenJoinCommunity: () -> Unit,
    onPostCommunityAnnouncement: (Community) -> Unit,
    onToggleCommunityMute: (Community) -> Unit,
    onStartCallFromHistory: (CallRecord) -> Unit,
    onToggleChatPin: (Chat) -> Unit,
    onToggleChatArchive: (Chat) -> Unit,
    onToggleChatMute: (Chat) -> Unit
) {
    val strings = LocalSmileStrings.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var chatActionTarget by remember { mutableStateOf<Chat?>(null) }

    val chatsList = (chatsState as? UiState.Success)?.data ?: emptyList()
    val totalUnread = remember(chatsList, currentUserId) {
        chatsList.count { (it.unreadCounts[currentUserId] ?: 0L) > 0L && currentUserId !in it.archivedByUids }
    }

    val tabs = listOf(
        strings.tabChats to Icons.AutoMirrored.Filled.Chat,
        strings.tabUpdates to Icons.Default.DonutLarge,
        strings.tabCalls to Icons.Default.Call,
        strings.tabCommunities to Icons.Default.Groups
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
            ) {
                if (!searchOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmileLogoMark(size = 38.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = SmileInk
                            )
                            Text(
                                text = strings.tagline,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NotoDevanagariFontFamily
                                ),
                                color = SmileInk.copy(alpha = 0.78f)
                            )
                        }
                        IconButton(
                            onClick = { searchOpen = true },
                            modifier = Modifier.testTag("home_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = SmileInk)
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("home_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = SmileInk)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.newGroup) },
                                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenNewGroup() }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.archivedChats) },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = { menuExpanded = false; showArchived = !showArchived; selectedTab = 0 }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.starredMessages) },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenStarred() }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.qrProfile) },
                                    leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenQrProfile() }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.inviteFriends) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = { menuExpanded = false; onInviteFriends() }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.settings) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenSettings() }
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchHint, color = SmileInk.copy(alpha = 0.6f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SmileWhite,
                                unfocusedContainerColor = SmileWhite,
                                focusedTextColor = SmileInk,
                                unfocusedTextColor = SmileInk,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                IconButton(onClick = { searchQuery = ""; searchOpen = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search", tint = SmileInk)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_field")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onOpenNewChat()
                        1 -> onOpenStatusCreator()
                        2 -> onOpenNewChat()
                        else -> onOpenNewCommunity()
                    }
                },
                containerColor = SmileYellow,
                contentColor = SmileInk,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("home_fab")
            ) {
                Icon(
                    imageVector = when (selectedTab) {
                        1 -> Icons.Default.Add
                        3 -> Icons.Default.Groups
                        else -> Icons.AutoMirrored.Filled.Chat
                    },
                    contentDescription = "Primary Action"
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 700.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = SmileYellowTint,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEachIndexed { idx, (label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Box {
                                        Icon(icon, contentDescription = label)
                                        if (idx == 0 && totalUnread > 0) {
                                            Badge(containerColor = SmileGreen) {
                                                Text("$totalUnread", color = SmileWhite)
                                            }
                                        }
                                    }
                                },
                                label = { Text(label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SmileInk,
                                    selectedTextColor = SmileInk,
                                    indicatorColor = SmileYellow
                                )
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        HomeTabBody(
                            selectedTab = selectedTab,
                            currentUserId = currentUserId,
                            account = account,
                            chatsState = chatsState,
                            contactsState = contactsState,
                            callsState = callsState,
                            statusState = statusState,
                            communitiesState = communitiesState,
                            announcements = announcements,
                            peerProfiles = peerProfiles,
                            searchQuery = searchQuery,
                            showArchived = showArchived,
                            onToggleShowArchived = { showArchived = !showArchived },
                            onOpenChat = onOpenChat,
                            onOpenNewChat = onOpenNewChat,
                            onOpenStatusCreator = onOpenStatusCreator,
                            onViewStatus = onViewStatus,
                            onToggleMuteStatus = onToggleMuteStatus,
                            onOpenNewCommunity = onOpenNewCommunity,
                            onOpenJoinCommunity = onOpenJoinCommunity,
                            onPostCommunityAnnouncement = onPostCommunityAnnouncement,
                            onToggleCommunityMute = onToggleCommunityMute,
                            onStartCallFromHistory = onStartCallFromHistory,
                            onLongPressChat = { chatActionTarget = it }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SmileYellow,
                        contentColor = SmileInk,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.5.dp,
                                color = SmileInk
                            )
                        }
                    ) {
                        tabs.forEachIndexed { idx, (label, _) ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                modifier = Modifier.testTag("home_tab_$idx"),
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = SmileInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (idx == 0 && totalUnread > 0) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(SmileGreen)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "$totalUnread",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SmileWhite
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    HomeTabBody(
                        selectedTab = selectedTab,
                        currentUserId = currentUserId,
                        account = account,
                        chatsState = chatsState,
                        contactsState = contactsState,
                        callsState = callsState,
                        statusState = statusState,
                        communitiesState = communitiesState,
                        announcements = announcements,
                        peerProfiles = peerProfiles,
                        searchQuery = searchQuery,
                        showArchived = showArchived,
                        onToggleShowArchived = { showArchived = !showArchived },
                        onOpenChat = onOpenChat,
                        onOpenNewChat = onOpenNewChat,
                        onOpenStatusCreator = onOpenStatusCreator,
                        onViewStatus = onViewStatus,
                        onToggleMuteStatus = onToggleMuteStatus,
                        onOpenNewCommunity = onOpenNewCommunity,
                        onOpenJoinCommunity = onOpenJoinCommunity,
                        onPostCommunityAnnouncement = onPostCommunityAnnouncement,
                        onToggleCommunityMute = onToggleCommunityMute,
                        onStartCallFromHistory = onStartCallFromHistory,
                        onLongPressChat = { chatActionTarget = it }
                    )
                }
            }
        }
    }

    chatActionTarget?.let { chat ->
        val isPinned = currentUserId in chat.pinnedByUids
        val isArchived = currentUserId in chat.archivedByUids
        val isMuted = currentUserId in chat.mutedByUids
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chatActionTarget = null },
            title = { Text(resolveChatDisplayTitle(chat, currentUserId, peerProfiles)) },
            text = {
                Column {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Unpin chat" else "Pin chat") },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        onClick = { onToggleChatPin(chat); chatActionTarget = null }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                        onClick = { onToggleChatMute(chat); chatActionTarget = null }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArchived) "Unarchive chat" else "Archive chat") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        onClick = { onToggleChatArchive(chat); chatActionTarget = null }
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { chatActionTarget = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HomeTabBody(
    selectedTab: Int,
    currentUserId: String,
    account: UserAccount?,
    chatsState: UiState<List<Chat>>,
    contactsState: UiState<List<SavedContact>>,
    callsState: UiState<List<CallRecord>>,
    statusState: UiState<List<StatusUpdate>>,
    communitiesState: UiState<List<Community>>,
    announcements: List<Announcement>,
    peerProfiles: Map<String, PublicProfile>,
    searchQuery: String,
    showArchived: Boolean,
    onToggleShowArchived: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    onOpenStatusCreator: () -> Unit,
    onViewStatus: (StatusUpdate) -> Unit,
    onToggleMuteStatus: (StatusUpdate) -> Unit,
    onOpenNewCommunity: () -> Unit,
    onOpenJoinCommunity: () -> Unit,
    onPostCommunityAnnouncement: (Community) -> Unit,
    onToggleCommunityMute: (Community) -> Unit,
    onStartCallFromHistory: (CallRecord) -> Unit,
    onLongPressChat: (Chat) -> Unit
) {
    when (selectedTab) {
        0 -> ChatsTab(
            currentUserId = currentUserId,
            chatsState = chatsState,
            peerProfiles = peerProfiles,
            searchQuery = searchQuery,
            showArchived = showArchived,
            onToggleShowArchived = onToggleShowArchived,
            onOpenChat = onOpenChat,
            onOpenNewChat = onOpenNewChat,
            onLongPressChat = onLongPressChat
        )
        1 -> UpdatesTab(
            currentUserId = currentUserId,
            account = account,
            statusState = statusState,
            onOpenStatusCreator = onOpenStatusCreator,
            onViewStatus = onViewStatus,
            onToggleMuteStatus = onToggleMuteStatus
        )
        2 -> CallsTab(
            currentUserId = currentUserId,
            callsState = callsState,
            onStartCallFromHistory = onStartCallFromHistory
        )
        else -> CommunitiesTab(
            currentUserId = currentUserId,
            communitiesState = communitiesState,
            announcements = announcements,
            onOpenNewCommunity = onOpenNewCommunity,
            onOpenJoinCommunity = onOpenJoinCommunity,
            onPostCommunityAnnouncement = onPostCommunityAnnouncement,
            onToggleCommunityMute = onToggleCommunityMute,
            onOpenChat = onOpenChat
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsTab(
    currentUserId: String,
    chatsState: UiState<List<Chat>>,
    peerProfiles: Map<String, PublicProfile>,
    searchQuery: String,
    showArchived: Boolean,
    onToggleShowArchived: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    onLongPressChat: (Chat) -> Unit
) {
    val strings = LocalSmileStrings.current
    when (chatsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(chatsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val allChats = chatsState.data
            val archivedCount = allChats.count { currentUserId in it.archivedByUids }
            val filtered = allChats.filter { chat ->
                val isArch = currentUserId in chat.archivedByUids
                val matchesArchive = if (showArchived) isArch else !isArch
                val title = resolveChatDisplayTitle(chat, currentUserId, peerProfiles)
                val matchesSearch = searchQuery.isBlank() ||
                    title.contains(searchQuery, ignoreCase = true) ||
                    chat.lastMessageText.contains(searchQuery, ignoreCase = true)
                matchesArchive && matchesSearch
            }

            if (filtered.isEmpty() && archivedCount == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_welcome_banner),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(strings.noChatsTitle, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.noChatsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onOpenNewChat) {
                        Text(strings.newChat)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    if (archivedCount > 0) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleShowArchived() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SmileYellowSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Archive, contentDescription = null, tint = SmileInk)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = if (showArchived) "Back to all chats" else "${strings.archivedChats} ($archivedCount)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    items(filtered, key = { it.id }) { chat ->
                        val peerUid = chat.memberUids.firstOrNull { it != currentUserId }
                        val peer = peerUid?.let { peerProfiles[it] }
                        val title = resolveChatDisplayTitle(chat, currentUserId, peerProfiles)
                        val avatar = if (chat.chatType == "GROUP") chat.avatarData else peer?.avatarData ?: chat.avatarData
                        val isOnline = chat.chatType == "DIRECT" && peer?.online == true
                        val unread = (chat.unreadCounts[currentUserId] ?: 0L).toInt()
                        val isPinned = currentUserId in chat.pinnedByUids
                        val isMuted = currentUserId in chat.mutedByUids
                        val isTyping = chat.typingUids.any { it != currentUserId }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpenChat(chat.id) },
                                    onLongClick = { onLongPressChat(chat) }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("chat_item_${chat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                avatarData = avatar,
                                name = title,
                                size = 52.dp,
                                isGroup = chat.chatType == "GROUP",
                                showOnlineDot = chat.chatType == "DIRECT",
                                online = isOnline
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isPinned) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (isMuted) {
                                        Icon(
                                            Icons.Default.NotificationsOff,
                                            contentDescription = "Muted",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = formatShortTime(chat.lastMessageAt?.toDate()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unread > 0) SmileGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isTyping && chat.lastMessageSenderId == currentUserId && chat.lastMessageText.isNotBlank()) {
                                        DeliveryTicks(isRead = false, isDelivered = true)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isTyping) strings.typing else chat.lastMessageText.ifBlank { "Tap to start chatting" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isTyping) SmileGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (unread > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(SmileYellow)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$unread",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SmileInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesTab(
    currentUserId: String,
    account: UserAccount?,
    statusState: UiState<List<StatusUpdate>>,
    onOpenStatusCreator: () -> Unit,
    onViewStatus: (StatusUpdate) -> Unit,
    onToggleMuteStatus: (StatusUpdate) -> Unit
) {
    val strings = LocalSmileStrings.current
    val updates = (statusState as? UiState.Success)?.data ?: emptyList()
    val myStatuses = updates.filter { it.ownerId == currentUserId }
    val others = updates.filter { it.ownerId != currentUserId }
    val muted = others.filter { currentUserId in it.mutedByUids }
    val unmuted = others.filter { currentUserId !in it.mutedByUids }
    val recent = unmuted.filter { currentUserId !in it.viewedByUids }
    val viewed = unmuted.filter { currentUserId in it.viewedByUids }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SmileYellowTint),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (myStatuses.isNotEmpty()) onViewStatus(myStatuses.first()) else onOpenStatusCreator()
                    }
                    .testTag("my_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatar(
                            avatarData = account?.avatarData.orEmpty(),
                            name = account?.displayName.orEmpty().ifBlank { "Me" },
                            size = 54.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(SmileYellow)
                                .border(1.5.dp, SmileWhite, CircleShape)
                                .clickable { onOpenStatusCreator() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add status", tint = SmileInk, modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.myStatus, style = MaterialTheme.typography.titleMedium, color = SmileInk)
                        Text(
                            text = if (myStatuses.isEmpty()) strings.tapToAddStatus
                            else "${myStatuses.size} active update(s) • Tap to view",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmileInk.copy(alpha = 0.72f)
                        )
                    }
                    OutlinedButton(onClick = onOpenStatusCreator) {
                        Text("Post", color = SmileInk)
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                Text(strings.recentUpdates, style = MaterialTheme.typography.labelLarge, color = SmileGreen)
            }
            items(recent, key = { it.id }) { status ->
                StatusRowItem(status = status, isViewed = false, onClick = { onViewStatus(status) }, onMute = { onToggleMuteStatus(status) })
            }
        }

        if (viewed.isNotEmpty()) {
            item {
                Text(strings.viewedUpdates, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(viewed, key = { it.id }) { status ->
                StatusRowItem(status = status, isViewed = true, onClick = { onViewStatus(status) }, onMute = { onToggleMuteStatus(status) })
            }
        }

        if (muted.isNotEmpty()) {
            item {
                Text(strings.mutedUpdates, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(muted, key = { it.id }) { status ->
                StatusRowItem(status = status, isViewed = true, onClick = { onViewStatus(status) }, onMute = { onToggleMuteStatus(status) })
            }
        }

        if (others.isEmpty() && myStatuses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Share a text or photo status that disappears automatically after 24 hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRowItem(
    status: StatusUpdate,
    isViewed: Boolean,
    onClick: () -> Unit,
    onMute: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.5.dp, if (isViewed) Color.Gray.copy(alpha = 0.4f) else SmileYellow, CircleShape)
                .padding(3.dp)
        ) {
            UserAvatar(avatarData = status.ownerAvatar, name = status.ownerName, size = 48.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(status.ownerName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatShortTime(status.createdAt?.toDate()) +
                    if (status.caption.isNotBlank()) " • ${status.caption}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMute) {
            Icon(Icons.Default.NotificationsOff, contentDescription = "Mute status", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CallsTab(
    currentUserId: String,
    callsState: UiState<List<CallRecord>>,
    onStartCallFromHistory: (CallRecord) -> Unit
) {
    val strings = LocalSmileStrings.current
    when (callsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(callsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val calls = callsState.data
            if (calls.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.callHistoryEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(calls, key = { it.id }) { call ->
                        val outgoing = call.callerId == currentUserId
                        val missed = call.status == CallStatus.MISSED.name || call.status == CallStatus.DECLINED.name
                        val peerName = if (outgoing) call.calleeName else call.callerName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartCallFromHistory(call) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(avatarData = call.callerAvatar, name = peerName, size = 48.dp, isGroup = call.isGroup)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (missed && !outgoing) SmileRed else MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            missed && !outgoing -> Icons.AutoMirrored.Filled.CallMissed
                                            outgoing -> Icons.AutoMirrored.Filled.CallMade
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        },
                                        contentDescription = null,
                                        tint = if (missed && !outgoing) SmileRed else SmileGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val dur = if (call.durationSeconds > 0) " • ${call.durationSeconds}s" else ""
                                    Text(
                                        text = "${formatShortTime(call.createdAt?.toDate())}$dur",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onStartCallFromHistory(call) }) {
                                Icon(
                                    imageVector = if (call.callType == CallType.VIDEO.name) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Call back",
                                    tint = SmileGreen
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitiesTab(
    currentUserId: String,
    communitiesState: UiState<List<Community>>,
    announcements: List<Announcement>,
    onOpenNewCommunity: () -> Unit,
    onOpenJoinCommunity: () -> Unit,
    onPostCommunityAnnouncement: (Community) -> Unit,
    onToggleCommunityMute: (Community) -> Unit,
    onOpenChat: (String) -> Unit
) {
    val strings = LocalSmileStrings.current
    val communities = (communitiesState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (announcements.isNotEmpty()) {
            items(announcements, key = { it.id }) { ann ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = SmileInk)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(ann.title, style = MaterialTheme.typography.titleMedium, color = SmileInk)
                            Text(ann.body, style = MaterialTheme.typography.bodySmall, color = SmileInk.copy(alpha = 0.85f))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onOpenNewCommunity,
                    modifier = Modifier.weight(1f).testTag("create_community_btn")
                ) {
                    Text(strings.newCommunity)
                }
                OutlinedButton(
                    onClick = onOpenJoinCommunity,
                    modifier = Modifier.weight(1f).testTag("join_community_btn")
                ) {
                    Text("Join Code")
                }
            }
        }

        if (communities.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Bring groups together with announcements, shared invite codes, and admin controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(communities, key = { it.id }) { comm ->
                val isAdmin = currentUserId in comm.adminUids
                val isMuted = currentUserId in comm.mutedByUids
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(avatarData = comm.iconData, name = comm.name, size = 46.dp, isGroup = true)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(comm.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${comm.memberUids.size} members • Code: ${comm.inviteCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onToggleCommunityMute(comm) }) {
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = if (isMuted) SmileRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (comm.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(comm.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = SmileYellowTint,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = SmileInk, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = comm.announcementText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SmileInk,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (comm.linkedChatIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            comm.linkedChatIds.forEach { linkedId ->
                                Text(
                                    text = "Open linked group ($linkedId)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SmileGreen,
                                    modifier = Modifier
                                        .clickable { onOpenChat(linkedId) }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { onPostCommunityAnnouncement(comm) }) {
                                Text("Post Announcement")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun resolveChatDisplayTitle(
    chat: Chat,
    currentUserId: String,
    peerProfiles: Map<String, PublicProfile>
): String {
    if (chat.chatType == "GROUP") return chat.title.ifBlank { "Group Chat" }
    val peerUid = chat.memberUids.firstOrNull { it != currentUserId }
    val peer = peerUid?.let { peerProfiles[it] }
    return peer?.displayName?.ifBlank { chat.title } ?: chat.title.ifBlank { "SMILE Contact" }
}

fun formatShortTime(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}
