package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileRed
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.utils.MediaUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun ActiveCallScreen(
    call: CallRecord,
    currentUserId: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: (durationSec: Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isIncoming = call.callerId != currentUserId && call.status == CallStatus.RINGING.name
    val isVideo = call.callType == CallType.VIDEO.name
    val peerName = if (call.callerId == currentUserId) call.calleeName else call.callerName

    var micMuted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(isVideo) }
    var cameraEnabled by remember { mutableStateOf(isVideo) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPerm = grants[Manifest.permission.CAMERA] == true || hasCameraPerm
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) needed.add(Manifest.permission.CAMERA)
        permLauncher.launch(needed.toTypedArray())
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    DisposableEffect(speakerOn, micMuted) {
        runCatching {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = speakerOn
            audioManager.isMicrophoneMute = micMuted
        }
        onDispose {
            runCatching {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = false
                audioManager.isMicrophoneMute = false
            }
        }
    }

    LaunchedEffect(call.status) {
        if (call.status == CallStatus.CONNECTED.name) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    BackHandler { onEndCall(elapsedSeconds) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isVideo && cameraEnabled && hasCameraPerm && call.status != CallStatus.ENDED.name) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                update = { previewView ->
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val selector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                UserAvatar(
                    avatarData = call.callerAvatar,
                    name = peerName,
                    size = 96.dp,
                    isGroup = call.isGroup
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SmileWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                val timerText = when (call.status) {
                    CallStatus.RINGING.name -> if (isIncoming) "Incoming ${call.callType.lowercase()} call..." else "Ringing..."
                    CallStatus.CONNECTED.name -> "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
                    else -> call.status
                }
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.titleMedium,
                    color = SmileYellow
                )
            }

            if (isIncoming) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Surface(
                        onClick = onDecline,
                        color = SmileRed,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp).testTag("call_decline_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = SmileWhite, modifier = Modifier.size(30.dp))
                        }
                    }
                    Surface(
                        onClick = onAnswer,
                        color = SmileGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp).testTag("call_answer_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Call, contentDescription = "Answer", tint = SmileWhite, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlBtn(
                        icon = if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        active = micMuted,
                        label = "Mute",
                        onClick = { micMuted = !micMuted }
                    )
                    CallControlBtn(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        active = speakerOn,
                        label = "Speaker",
                        onClick = { speakerOn = !speakerOn }
                    )
                    if (isVideo) {
                        CallControlBtn(
                            icon = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            active = !cameraEnabled,
                            label = "Camera",
                            onClick = { cameraEnabled = !cameraEnabled }
                        )
                        CallControlBtn(
                            icon = Icons.Default.Cameraswitch,
                            active = false,
                            label = "Flip",
                            onClick = { useFrontCamera = !useFrontCamera }
                        )
                    }
                    Surface(
                        onClick = { onEndCall(elapsedSeconds) },
                        color = SmileRed,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp).testTag("call_end_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = SmileWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            color = if (active) SmileYellow else Color.White.copy(alpha = 0.16f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = if (active) SmileInk else SmileWhite)
            }
        }
    }
}

@Composable
fun StatusViewerScreen(
    status: StatusUpdate,
    currentUserId: String,
    viewersFlow: Flow<List<StatusView>>,
    onReplyToStatus: (String) -> Unit,
    onMuteStatus: () -> Unit,
    onDeleteStatus: () -> Unit,
    onBack: () -> Unit
) {
    val isMine = status.ownerId == currentUserId
    val viewers by viewersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var showViewers by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(status.id) {
        val steps = 50
        repeat(steps) {
            delay(120L)
            progress = (it + 1) / steps.toFloat()
        }
    }

    BackHandler { onBack() }

    val bgColor = remember(status.backgroundColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(status.backgroundColorHex)) }
            .getOrDefault(SmileYellow)
    }
    val bmp = remember(status.mediaData) { MediaUtils.decodeBase64Bitmap(status.mediaData) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (status.mediaType == "IMAGE") Color.Black else bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Status media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (bgColor == SmileYellow) SmileInk else SmileWhite,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                color = SmileYellow,
                trackColor = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileWhite)
                }
                UserAvatar(avatarData = status.ownerAvatar, name = status.ownerName, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.ownerName, style = MaterialTheme.typography.titleMedium, color = SmileWhite)
                    Text(formatShortTime(status.createdAt?.toDate()), style = MaterialTheme.typography.labelSmall, color = SmileWhite.copy(alpha = 0.8f))
                }
                if (isMine) {
                    IconButton(onClick = { showViewers = true }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Viewers", tint = SmileWhite)
                    }
                    IconButton(onClick = { onDeleteStatus(); onBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete status", tint = SmileWhite)
                    }
                } else {
                    IconButton(onClick = { onMuteStatus(); onBack() }) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = "Mute status", tint = SmileWhite)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (status.caption.isNotBlank()) {
                Text(
                    text = status.caption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SmileWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isMine) {
                TextButton(onClick = { showViewers = true }) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = SmileYellow)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${viewers.size} view(s)", color = SmileWhite)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to status...", color = SmileWhite.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SmileWhite,
                            unfocusedTextColor = SmileWhite,
                            focusedBorderColor = SmileYellow,
                            unfocusedBorderColor = SmileWhite.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReplyToStatus(replyText)
                                replyText = ""
                                onBack()
                            }
                        },
                        color = SmileYellow,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = SmileInk)
                        }
                    }
                }
            }
        }
    }

    if (showViewers) {
        AlertDialog(
            onDismissRequest = { showViewers = false },
            title = { Text("Viewed by (${viewers.size})") },
            text = {
                if (viewers.isEmpty()) {
                    Text("No views yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(viewers, key = { it.viewerId }) { v ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(avatarData = v.viewerAvatar, name = v.viewerName, size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(v.viewerName, modifier = Modifier.weight(1f))
                                Text(formatShortTime(v.viewedAt?.toDate()), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showViewers = false }) { Text("Close") } }
        )
    }
}
