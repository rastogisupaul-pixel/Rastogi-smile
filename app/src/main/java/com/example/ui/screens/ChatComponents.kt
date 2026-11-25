package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.data.model.PublicProfile
import com.example.data.model.SavedContact
import com.example.ui.components.DeliveryTicks
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileSlate
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.utils.MediaUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    displayText: String,
    isMine: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    isPlayingVoice: Boolean,
    onPlayVoiceToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val bubbleColor = if (isMine) SmileYellow else MaterialTheme.colorScheme.surface
    val textColor = if (isMine) SmileInk else MaterialTheme.colorScheme.onSurface
    val isRead = message.readByUids.any { it != currentUserId }
    val isDelivered = message.deliveredToUids.any { it != currentUserId }
    val isStarred = currentUserId in message.starredByUids

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(
                    onClick = {
                        if (message.messageType == MessageType.LOCATION.name &&
                            message.latitude != null && message.longitude != null
                        ) {
                            val uri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}(${Uri.encode(message.locationLabel)})")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                    },
                    onLongClick = onLongPress
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmileGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                if (message.forwarded) {
                    Text(
                        text = "Forwarded",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
                if (message.replyToMessageId.isNotBlank()) {
                    Surface(
                        color = SmileInk.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = message.replyToSenderName.ifBlank { "Reply" },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                        }
                    }
                }

                when (message.messageType) {
                    MessageType.IMAGE.name -> {
                        val bmp = remember(message.mediaData) { MediaUtils.decodeBase64Bitmap(message.mediaData) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Photo message",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (displayText.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    MessageType.DOCUMENT.name -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SmileInk.copy(alpha = 0.08f))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = textColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.mediaName.ifBlank { "Document" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor
                                )
                                Text(
                                    text = message.mediaMime.ifBlank { "File" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    MessageType.VOICE.name -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SmileInk.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(onClick = onPlayVoiceToggle, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (isPlayingVoice) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingVoice) "Stop" else "Play voice note",
                                    tint = textColor
                                )
                            }
                            Text(
                                text = "Voice Note (${message.mediaDurationSec}s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                    }
                    MessageType.LOCATION.name -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SmileGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Shared Location", style = MaterialTheme.typography.labelLarge, color = textColor)
                                Text(
                                    text = message.locationLabel.ifBlank { "${message.latitude}, ${message.longitude}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    MessageType.CONTACT.name -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = textColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(message.sharedContactName, style = MaterialTheme.typography.labelLarge, color = textColor)
                                Text(message.sharedContactPhone, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
                            }
                        }
                    }
                    else -> {}
                }

                if (displayText.isNotBlank()) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = if (message.deletedForEveryone) FontStyle.Italic else FontStyle.Normal
                        ),
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.e2ee) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = textColor.copy(alpha = 0.65f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    if (isStarred) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = textColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    if (message.edited) {
                        Text(
                            text = "edited • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.65f)
                        )
                    }
                    Text(
                        text = formatShortTime(message.createdAt?.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (isMine) {
                        DeliveryTicks(
                            isRead = isRead,
                            isDelivered = isDelivered,
                            tintDefault = textColor.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = message.reactions.values.distinct().joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MessageActionsDialog(
    message: ChatMessage,
    isMine: Boolean,
    isStarred: Boolean,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("❤️", "😂", "😮", "😢", "🙏", "👍")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                emojis.forEach { e ->
                    Text(
                        text = e,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReact(e); onDismiss() }
                            .padding(4.dp)
                    )
                }
            }
        },
        text = {
            Column {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                    onClick = { onReply(); onDismiss() }
                )
                DropdownMenuItem(
                    text = { Text("Forward") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null) },
                    onClick = { onForward(); onDismiss() }
                )
                DropdownMenuItem(
                    text = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = { onCopy(); onDismiss() }
                )
                DropdownMenuItem(
                    text = { Text(if (isStarred) "Unstar" else "Star message") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    onClick = { onToggleStar(); onDismiss() }
                )
                if (isMine && message.messageType == MessageType.TEXT.name && !message.deletedForEveryone) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { onEdit(); onDismiss() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete for me") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = { onDeleteForMe(); onDismiss() }
                )
                if (isMine && !message.deletedForEveryone) {
                    DropdownMenuItem(
                        text = { Text("Delete for everyone") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { onDeleteForEveryone(); onDismiss() }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun GroupInfoDialog(
    chat: Chat,
    currentUserId: String,
    contacts: List<SavedContact>,
    peerProfiles: Map<String, PublicProfile>,
    onUpdateSettings: (title: String, desc: String, onlyAdminsCanSend: Boolean, announcementOnly: Boolean) -> Unit,
    onUpdateMembers: (memberUids: List<String>, coAdminUids: List<String>) -> Unit,
    onStartGroupCall: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAdmin = currentUserId in chat.adminUids || currentUserId in chat.coAdminUids
    var title by remember(chat) { mutableStateOf(chat.title) }
    var description by remember(chat) { mutableStateOf(chat.description) }
    var onlyAdminsSend by remember(chat) { mutableStateOf(chat.onlyAdminsCanSend) }
    var announcementOnly by remember(chat) { mutableStateOf(chat.announcementOnly) }
    var members by remember(chat) { mutableStateOf(chat.memberUids) }
    var coAdmins by remember(chat) { mutableStateOf(chat.coAdminUids) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Info & Permissions") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onStartGroupCall(false); onDismiss() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voice Call")
                        }
                        OutlinedButton(onClick = { onStartGroupCall(true); onDismiss() }, modifier = Modifier.weight(1f)) {
                            Text("Video Call")
                        }
                    }
                    if (chat.inviteCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = SmileYellowSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Invite Code: ${chat.inviteCode}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SmileInk,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Join my SMILE group \"${chat.title}\" using invite code: ${chat.inviteCode}")
                                    }
                                    context.startActivity(Intent.createChooser(share, "Share Group Invite"))
                                }) {
                                    Text("Share")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        enabled = isAdmin,
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        enabled = isAdmin,
                        label = { Text("Group Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Only admins can send", modifier = Modifier.weight(1f))
                        Switch(checked = onlyAdminsSend, onCheckedChange = { if (isAdmin) onlyAdminsSend = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Announcement mode", modifier = Modifier.weight(1f))
                        Switch(checked = announcementOnly, onCheckedChange = { if (isAdmin) announcementOnly = it })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Members (${members.size})", style = MaterialTheme.typography.labelLarge)
                }

                items(members, key = { it }) { uid ->
                    val name = if (uid == currentUserId) "You" else peerProfiles[uid]?.displayName ?: uid.take(8)
                    val isMainAdmin = uid in chat.adminUids
                    val isCoAdmin = uid in coAdmins
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name + when {
                                isMainAdmin -> " (Admin)"
                                isCoAdmin -> " (Co-Admin)"
                                else -> ""
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isAdmin && !isMainAdmin && uid != currentUserId) {
                            TextButton(onClick = {
                                coAdmins = if (isCoAdmin) coAdmins - uid else coAdmins + uid
                            }) {
                                Text(if (isCoAdmin) "Demote" else "Co-Admin")
                            }
                            if (members.size > 2) {
                                TextButton(onClick = {
                                    members = members - uid
                                    coAdmins = coAdmins - uid
                                }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (isAdmin) {
                    val addable = contacts.filter { it.contactUid !in members }
                    if (addable.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Add Contacts", style = MaterialTheme.typography.labelLarge)
                        }
                        items(addable, key = { "add_${it.contactUid}" }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { members = (members + contact.contactUid).distinct() }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = false, onCheckedChange = {
                                    members = (members + contact.contactUid).distinct()
                                })
                                Text(contact.displayName)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAdmin) {
                Button(
                    onClick = {
                        onUpdateSettings(title, description, onlyAdminsSend, announcementOnly)
                        onUpdateMembers(members, coAdmins)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) {
                    Text("Save")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (isAdmin) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
