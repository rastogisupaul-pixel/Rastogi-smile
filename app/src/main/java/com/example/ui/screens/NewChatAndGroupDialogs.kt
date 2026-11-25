package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.data.model.DirectoryLookup
import com.example.data.model.SavedContact
import com.example.data.model.StatusPrivacyMode
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileYellow
import com.example.utils.MediaUtils

@Composable
fun NewChatDialog(
    contacts: List<SavedContact>,
    onSearchUser: (String, (DirectoryLookup?) -> Unit) -> Unit,
    onSelectLookup: (DirectoryLookup) -> Unit,
    onSelectContact: (SavedContact) -> Unit,
    onSyncContacts: () -> Unit,
    onInviteFriends: () -> Unit,
    onJoinGroupCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<DirectoryLookup?>(null) }
    var searched by remember { mutableStateOf(false) }
    var groupCode by remember { mutableStateOf("") }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onSyncContacts()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat & Find Friends") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; searched = false },
                    label = { Text("+91 phone or @smileId") },
                    singleLine = true,
                    trailingIcon = {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                onSearchUser(query) { res ->
                                    searchResult = res
                                    searched = true
                                }
                            },
                            modifier = Modifier.testTag("find_user_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Find user")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("find_user_input")
                )

                if (searched) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (searchResult != null) {
                        val found = searchResult!!
                        Surface(
                            color = SmileYellow.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLookup(found); onDismiss() }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(avatarData = found.avatarData, name = found.displayName, size = 40.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(found.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("@${found.smileId}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Chat", color = SmileInk, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            "No registered SMILE user matched \"$query\". Invite them below!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) onSyncContacts()
                            else contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onInviteFriends,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = groupCode,
                        onValueChange = { groupCode = it },
                        label = { Text("Group invite code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (groupCode.isNotBlank()) { onJoinGroupCode(groupCode); onDismiss() } },
                        colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                    ) {
                        Text("Join")
                    }
                }

                if (contacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Saved SMILE Contacts", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(contacts, key = { it.contactUid }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectContact(contact); onDismiss() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(avatarData = contact.avatarData, name = contact.displayName, size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(contact.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("@${contact.smileId}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreateGroupDialog(
    contacts: List<SavedContact>,
    onCreateGroup: (title: String, description: String, memberUids: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedUids = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Select Members (${selectedUids.size} selected)", style = MaterialTheme.typography.labelLarge)
                if (contacts.isEmpty()) {
                    Text(
                        "Find or sync at least 1 SMILE contact first to add them to a group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(contacts, key = { it.contactUid }) { contact ->
                            val checked = contact.contactUid in selectedUids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedUids.remove(contact.contactUid)
                                        else selectedUids.add(contact.contactUid)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it) selectedUids.add(contact.contactUid)
                                        else selectedUids.remove(contact.contactUid)
                                    }
                                )
                                Text(contact.displayName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedUids.isNotEmpty()) {
                        onCreateGroup(title, description, selectedUids.toList())
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && selectedUids.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateStatusDialog(
    onPostStatus: (mediaType: String, text: String, caption: String, bgHex: String, mediaData: String, privacy: StatusPrivacyMode) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf("") }
    var selectedHex by remember { mutableStateOf("#FFD600") }
    var privacy by remember { mutableStateOf(StatusPrivacyMode.ALL_CONTACTS) }
    val bgSwatches = listOf("#FFD600", "#16A34A", "#2563EB", "#9333EA", "#111111")

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 720, quality = 70)?.let {
                imageBase64 = it
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share 24h Status") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (imageBase64.isBlank()) "Pick Image from Gallery" else "Image Ready (Tap to change)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (imageBase64.isBlank()) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Status text...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("status_text_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgSwatches.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedHex == hex) 3.dp else 1.dp,
                                        color = SmileInk,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedHex = hex }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Add a caption...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Who can see this:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPrivacyMode.entries.forEach { mode ->
                        val selected = privacy == mode
                        Surface(
                            color = if (selected) SmileYellow else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { privacy = mode }
                        ) {
                            Text(
                                text = when (mode) {
                                    StatusPrivacyMode.ALL_CONTACTS -> "Contacts"
                                    StatusPrivacyMode.SELECTED -> "Selected"
                                    StatusPrivacyMode.EXCEPT -> "Except"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = SmileInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isImg = imageBase64.isNotBlank()
                    if (isImg || text.isNotBlank()) {
                        onPostStatus(
                            if (isImg) "IMAGE" else "TEXT",
                            text,
                            caption,
                            selectedHex,
                            imageBase64,
                            privacy
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateCommunityDialog(
    chats: List<Chat>,
    contacts: List<SavedContact>,
    onCreate: (name: String, desc: String, announcement: String, linkedChatIds: List<String>, memberUids: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var announcement by remember { mutableStateOf("") }
    val selectedChatIds = remember { mutableStateListOf<String>() }
    val groups = chats.filter { it.chatType == "GROUP" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Community") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Community Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it },
                    label = { Text("Welcome Announcement") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (groups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Link Groups", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(groups, key = { it.id }) { g ->
                            val checked = g.id in selectedChatIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedChatIds.remove(g.id) else selectedChatIds.add(g.id)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = {
                                    if (it) selectedChatIds.add(g.id) else selectedChatIds.remove(g.id)
                                })
                                Text(g.title)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(
                            name,
                            desc,
                            announcement,
                            selectedChatIds.toList(),
                            contacts.map { it.contactUid }
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun JoinCommunityDialog(
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Community") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Community Invite Code (e.g. COM_...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (code.isNotBlank()) { onJoin(code); onDismiss() } },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PostAnnouncementDialog(
    community: Community,
    onPost: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(community.announcementText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Community Announcement") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Announcement message") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) { onPost(text); onDismiss() } },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
