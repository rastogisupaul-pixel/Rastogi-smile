package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatWallpaper
import com.example.data.local.SmilePreferences
import com.example.data.local.SmileSettingsState
import com.example.data.local.ThemeMode
import com.example.data.model.BlockedUser
import com.example.data.model.Chat
import com.example.data.model.UserAccount
import com.example.data.model.VisibilityOption
import com.example.localization.AppLanguage
import com.example.localization.LocalSmileStrings
import com.example.security.CryptoManager
import com.example.ui.components.QrCodeMatrixCanvas
import com.example.ui.components.SmileLogoMark
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileRed
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowTint
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentUserId: String,
    account: UserAccount?,
    settings: SmileSettingsState,
    blockedUsers: List<BlockedUser>,
    chats: List<Chat>,
    preferences: SmilePreferences,
    backupJson: String,
    onEditProfile: () -> Unit,
    onOpenQrProfile: () -> Unit,
    onUpdatePrivacy: (VisibilityOption, VisibilityOption, VisibilityOption, Boolean, Boolean, String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSubmitBugReport: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalSmileStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showFingerprintDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(backupJson.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileInk)
                }
                Text(
                    text = strings.settings,
                    style = MaterialTheme.typography.titleLarge,
                    color = SmileInk
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmileYellowTint),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditProfile() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarData = account?.avatarData.orEmpty(),
                            name = account?.displayName.orEmpty().ifBlank { "S" },
                            size = 60.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account?.displayName.orEmpty().ifBlank { "SMILE User" }, style = MaterialTheme.typography.titleMedium, color = SmileInk)
                            Text("@${account?.smileId.orEmpty()} • ${account?.phoneNumber.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = SmileInk.copy(alpha = 0.75f))
                            Text(account?.about.orEmpty(), style = MaterialTheme.typography.bodySmall, color = SmileInk.copy(alpha = 0.7f), maxLines = 1)
                        }
                        IconButton(onClick = onOpenQrProfile) {
                            Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = SmileInk)
                        }
                    }
                }
            }

            item {
                SettingsRow(Icons.Default.Language, strings.languageLabel, settings.language.label) { showLanguageDialog = true }
                SettingsRow(Icons.Default.DarkMode, strings.themeLabel, settings.themeMode.label) { showThemeDialog = true }
                SettingsRow(Icons.Default.Wallpaper, strings.chatWallpaper, settings.wallpaper.label) { showWallpaperDialog = true }
                SettingsRow(
                    Icons.Default.Lock,
                    strings.appLock,
                    if (settings.appLockPinHash.isBlank()) "Off" else "4-Digit PIN Active"
                ) { showPinDialog = true }
                SettingsRow(
                    Icons.Default.Security,
                    strings.privacySecurity,
                    "Visibility, 2-step, Blocked (${blockedUsers.size})"
                ) { showPrivacyDialog = true }
                SettingsRow(
                    Icons.Default.Key,
                    "Encryption Key Fingerprint",
                    CryptoManager.keyFingerprint(settings.e2eePublicKey).take(19) + "..."
                ) { showFingerprintDialog = true }
            }

            item {
                HorizontalDivider()
                SettingsSwitchRow(
                    icon = Icons.Default.Notifications,
                    title = strings.notifications,
                    subtitle = "Message, call & status alerts",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { scope.launch { preferences.setNotificationsEnabled(it) } }
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Storage,
                    title = "Network Data Saver",
                    subtitle = "Reduce media auto-download on mobile data",
                    checked = settings.dataSaverEnabled,
                    onCheckedChange = { scope.launch { preferences.setDataSaverEnabled(it) } }
                )
            }

            item {
                HorizontalDivider()
                SettingsRow(
                    Icons.Default.Backup,
                    "Chat Backup Export (JSON)",
                    "${chats.size} chat(s) ready to export"
                ) { backupLauncher.launch("smile_backup.json") }
                SettingsRow(Icons.Default.BugReport, strings.helpSupport, "Report a technical issue") { showHelpDialog = true }
                SettingsRow(Icons.Default.Policy, strings.privacyPolicy, "Data & E2EE commitments") { showLegalDialog = "PRIVACY" }
                SettingsRow(Icons.Default.Fingerprint, strings.termsConditions, "Usage guidelines") { showLegalDialog = "TERMS" }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("settings_logout_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.logout)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().testTag("settings_delete_account_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SmileRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.deleteAccount, color = SmileRed)
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.languageLabel) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Text(
                            text = "${lang.label}" + if (settings.language == lang) " ✓" else "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { preferences.setLanguage(lang) }
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Close") } }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(strings.themeLabel) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Text(
                            text = mode.label + if (settings.themeMode == mode) " ✓" else "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { preferences.setThemeMode(mode) }
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
        )
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text(strings.chatWallpaper) },
            text = {
                Column {
                    ChatWallpaper.entries.forEach { wall ->
                        Text(
                            text = wall.label + if (settings.wallpaper == wall) " ✓" else "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { preferences.setWallpaper(wall) }
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWallpaperDialog = false }) { Text("Close") } }
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("App Lock PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Enter 4-digit PIN") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin.length == 4) {
                            scope.launch { preferences.setAppLockPinHash(CryptoManager.hashPinWithSalt(pin, currentUserId)) }
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Enable PIN") }
            },
            dismissButton = {
                Row {
                    if (settings.appLockPinHash.isNotBlank()) {
                        TextButton(onClick = {
                            scope.launch { preferences.setAppLockPinHash("") }
                            showPinDialog = false
                        }) { Text("Disable") }
                    }
                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showPrivacyDialog) {
        var lastSeen by remember {
            mutableStateOf(VisibilityOption.entries.firstOrNull { it.name == account?.lastSeenVisibility } ?: VisibilityOption.EVERYONE)
        }
        var photoVis by remember {
            mutableStateOf(VisibilityOption.entries.firstOrNull { it.name == account?.photoVisibility } ?: VisibilityOption.EVERYONE)
        }
        var aboutVis by remember {
            mutableStateOf(VisibilityOption.entries.firstOrNull { it.name == account?.aboutVisibility } ?: VisibilityOption.EVERYONE)
        }
        var readReceipts by remember { mutableStateOf(account?.readReceiptsEnabled ?: true) }
        var twoStep by remember { mutableStateOf(account?.twoStepEnabled ?: false) }

        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacySecurity) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilityCycleRow("Last Seen & Online", lastSeen) {
                        val idx = (VisibilityOption.entries.indexOf(lastSeen) + 1) % VisibilityOption.entries.size
                        lastSeen = VisibilityOption.entries[idx]
                    }
                    VisibilityCycleRow("Profile Photo", photoVis) {
                        val idx = (VisibilityOption.entries.indexOf(photoVis) + 1) % VisibilityOption.entries.size
                        photoVis = VisibilityOption.entries[idx]
                    }
                    VisibilityCycleRow("About", aboutVis) {
                        val idx = (VisibilityOption.entries.indexOf(aboutVis) + 1) % VisibilityOption.entries.size
                        aboutVis = VisibilityOption.entries[idx]
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Read Receipts", modifier = Modifier.weight(1f))
                        Switch(checked = readReceipts, onCheckedChange = { readReceipts = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Two-Step Verification", modifier = Modifier.weight(1f))
                        Switch(checked = twoStep, onCheckedChange = { twoStep = it })
                    }
                    if (blockedUsers.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Blocked (${blockedUsers.size})", style = MaterialTheme.typography.labelLarge)
                        blockedUsers.forEach { b ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(b.displayName, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onUnblockUser(b.blockedUid) }) { Text("Unblock") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePrivacy(lastSeen, photoVis, aboutVis, readReceipts, twoStep, account?.twoStepPinHash.orEmpty())
                        showPrivacyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") } }
        )
    }

    if (showFingerprintDialog) {
        AlertDialog(
            onDismissRequest = { showFingerprintDialog = false },
            title = { Text("E2EE Security Fingerprint") },
            text = {
                Column {
                    Text("Your 1-to-1 messages use ECDH (secp256r1) key agreement + AES-256-GCM encryption.")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = CryptoManager.keyFingerprint(settings.e2eePublicKey),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmileGreen
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showFingerprintDialog = false }) { Text("OK") } }
        )
    }

    if (showHelpDialog) {
        var reason by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(strings.helpSupport) },
            text = {
                Column {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Issue Summary") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reason.isNotBlank()) {
                            onSubmitBugReport(reason, details)
                            showHelpDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cancel") } }
        )
    }

    showLegalDialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { showLegalDialog = null },
            title = { Text(if (kind == "PRIVACY") strings.privacyPolicy else strings.termsConditions) },
            text = {
                Text(
                    if (kind == "PRIVACY") {
                        "SMILE encrypts 1-to-1 message contents end-to-end using ECDH + AES-256-GCM. Phone numbers are hashed with SHA-256 for contact discovery and are never exposed in public directory lists. You can delete your account and personal records at any time from Settings."
                    } else {
                        "By using SMILE (बात करें, मुस्कुराएं!), you agree to communicate respectfully, refrain from spam or abuse, and follow applicable laws. Reported abuse is reviewed by administrators to keep communities safe."
                    }
                )
            },
            confirmButton = { TextButton(onClick = { showLegalDialog = null }) { Text("Close") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = { Text("This permanently deletes your SMILE profile, contacts, and authentication session.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDeleteAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileRed, contentColor = SmileWhite)
                ) { Text("Delete Forever") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun VisibilityCycleRow(label: String, value: VisibilityOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value.label, color = SmileGreen, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QrProfileScreen(
    account: UserAccount?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileInk)
                }
                Text("My SMILE QR Code", style = MaterialTheme.typography.titleLarge, color = SmileInk)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SmileYellowTint),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SmileLogoMark(size = 44.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    UserAvatar(
                        avatarData = account?.avatarData.orEmpty(),
                        name = account?.displayName.orEmpty().ifBlank { "S" },
                        size = 72.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(account?.displayName.orEmpty(), style = MaterialTheme.typography.titleLarge, color = SmileInk)
                    Text("@${account?.smileId.orEmpty()}", style = MaterialTheme.typography.titleMedium, color = SmileInk.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(18.dp))
                    QrCodeMatrixCanvas(data = "smile://user/${account?.smileId.orEmpty()}", size = 210.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Scan or search @${account?.smileId.orEmpty()} to connect on SMILE",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmileInk.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Add me on SMILE! Search @${account?.smileId.orEmpty()} (${account?.phoneNumber.orEmpty()})")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share SMILE Profile"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share SMILE Profile")
            }
        }
    }
}

@Composable
fun StarredMessagesScreen(
    currentUserId: String,
    starredState: com.example.data.model.UiState<List<com.example.ui.viewmodel.StarredMessageItem>>,
    peerProfiles: Map<String, com.example.data.model.PublicProfile>,
    myPrivateKey: String,
    onOpenChat: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalSmileStrings.current
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("starred_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileInk)
                }
                Text(strings.starredMessages, style = MaterialTheme.typography.titleLarge, color = SmileInk)
            }
        }
    ) { innerPadding ->
        when (starredState) {
            is com.example.data.model.UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = SmileYellow)
                }
            }
            is com.example.data.model.UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(starredState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is com.example.data.model.UiState.Success -> {
                val items = starredState.data
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Long-press any message in a chat and tap Star to keep it handy here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { "${it.chat.id}_${it.message.id}" }) { item ->
                            val chatTitle = resolveChatDisplayTitle(item.chat, currentUserId, peerProfiles)
                            val peerUid = item.chat.memberUids.firstOrNull { it != currentUserId }
                            val peerPub = if (item.chat.chatType == "DIRECT") peerUid?.let { peerProfiles[it]?.publicKey }.orEmpty() else ""
                            val resolved = if (item.message.e2ee && item.message.cipherText.isNotBlank()) {
                                CryptoManager.decryptMessage(item.message.cipherText, item.message.iv, myPrivateKey, peerPub)
                                    ?: "[Encrypted message]"
                            } else {
                                item.message.text.ifBlank { item.message.messageType }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SmileYellowTint),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChat(item.chat.id) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${item.message.senderName} • $chatTitle",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SmileInk,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatShortTime(item.message.createdAt?.toDate()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SmileInk.copy(alpha = 0.65f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(resolved, style = MaterialTheme.typography.bodyMedium, color = SmileInk)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
