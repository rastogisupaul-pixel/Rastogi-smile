package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.UserAccount
import com.example.data.remote.AuthHelper
import com.example.localization.LocalSmileStrings
import com.example.security.CryptoManager
import com.example.ui.components.SmileLogoMark
import com.example.ui.components.UserAvatar
import com.example.ui.theme.NotoDevanagariFontFamily
import com.example.ui.theme.SmileGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileSlate
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.utils.MediaUtils
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.65f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        alpha.animateTo(1f, animationSpec = tween(350))
        delay(950)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmileYellow)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            SmileLogoMark(
                size = 132.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(34.dp))
                    .border(4.dp, SmileWhite, RoundedCornerShape(34.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SMILE",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = SmileInk
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "बात करें, मुस्कुराएं!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = NotoDevanagariFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = SmileInk.copy(alpha = 0.88f),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val strings = LocalSmileStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                SmileLogoMark(size = 84.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = NotoDevanagariFontFamily),
                    color = SmileSlate
                )
                Spacer(modifier = Modifier.height(20.dp))
                Image(
                    painter = painterResource(id = R.drawable.img_welcome_banner),
                    contentDescription = "SMILE messaging illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = strings.signInTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        AuthHelper.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmileYellow,
                        contentColor = SmileInk
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SmileInk,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = strings.signInButton,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SmileGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End-to-end encrypted messaging & +91 mobile discovery",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    existingAccount: UserAccount?,
    defaultName: String,
    onSave: (displayName: String, smileId: String, phoneE164: String, about: String, avatarData: String) -> Unit,
    isSaving: Boolean
) {
    val strings = LocalSmileStrings.current
    val context = LocalContext.current
    val countryCodes = listOf("+91" to "IN", "+1" to "US", "+44" to "UK", "+971" to "AE", "+65" to "SG")

    var selectedCountryCode by remember { mutableStateOf("+91") }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var name by remember(existingAccount) {
        mutableStateOf(existingAccount?.displayName?.ifBlank { defaultName } ?: defaultName)
    }
    var smileId by remember(existingAccount) {
        mutableStateOf(
            existingAccount?.smileId?.ifBlank {
                defaultName.lowercase().replace(Regex("[^a-z0-9]"), "").take(14).ifBlank { "smileuser" }
            } ?: defaultName.lowercase().replace(Regex("[^a-z0-9]"), "").take(14).ifBlank { "smileuser" }
        )
    }
    var phoneDigits by remember(existingAccount) {
        mutableStateOf(existingAccount?.phoneNumber?.removePrefix("+91").orEmpty())
    }
    var about by remember(existingAccount) {
        mutableStateOf(existingAccount?.about ?: "बात करें, मुस्कुराएं!")
    }
    var avatarData by remember(existingAccount) {
        mutableStateOf(existingAccount?.avatarData ?: "")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 320, quality = 70)?.let {
                avatarData = it
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Yellow branded top banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SmileYellow)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    text = strings.profileSetupTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SmileInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.profileSetupSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmileInk.copy(alpha = 0.85f)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("setup_avatar_button"),
                contentAlignment = Alignment.BottomEnd
            ) {
                UserAvatar(avatarData = avatarData, name = name.ifBlank { "S" }, size = 96.dp)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SmileYellow)
                        .border(2.dp, SmileWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Choose photo",
                        tint = SmileInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.fullNameLabel) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = smileId,
                onValueChange = { smileId = it.replace(" ", "") },
                label = { Text(strings.smileIdLabel) },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                supportingText = { Text("Unique handle e.g. rahul_99") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_smile_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        color = SmileYellowSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { countryDropdownExpanded = true }
                            .testTag("country_code_selector")
                    ) {
                        Text(
                            text = selectedCountryCode,
                            style = MaterialTheme.typography.titleMedium,
                            color = SmileInk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = countryDropdownExpanded,
                        onDismissRequest = { countryDropdownExpanded = false }
                    ) {
                        countryCodes.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text("$label ($code)") },
                                onClick = {
                                    selectedCountryCode = code
                                    countryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = phoneDigits,
                    onValueChange = { phoneDigits = it.filter { ch -> ch.isDigit() }.take(12) },
                    label = { Text(strings.mobileLabel) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("setup_phone_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = about,
                onValueChange = { about = it.take(140) },
                label = { Text(strings.aboutLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_about_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanId = smileId.trim().replace(Regex("[^a-zA-Z0-9._]"), "")
                    when {
                        cleanName.isEmpty() -> validationError = "Please enter your name."
                        cleanId.length < 3 -> validationError = "Smile ID must be at least 3 characters."
                        phoneDigits.isNotEmpty() && phoneDigits.length < 7 ->
                            validationError = "Enter a valid mobile number (e.g. 10 digits for +91)."
                        else -> {
                            validationError = null
                            val e164 = if (phoneDigits.isBlank()) "" else CryptoManager.normalizePhoneToE164(phoneDigits, selectedCountryCode)
                            onSave(cleanName, cleanId, e164, about.trim(), avatarData)
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("setup_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SmileInk)
                } else {
                    Text(
                        text = strings.saveContinue,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    expectedPinHash: String,
    userId: String,
    onUnlocked: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmileYellow)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = SmileInk,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SMILE App Lock",
            style = MaterialTheme.typography.headlineMedium,
            color = SmileInk
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (error) "Incorrect PIN. Try again." else "Enter your 4-digit PIN",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else SmileInk.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { idx ->
                val filled = idx < entered.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (filled) SmileInk else SmileWhite.copy(alpha = 0.65f))
                        .border(1.5.dp, SmileInk, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(68.dp))
                        "DEL" -> IconButton(
                            onClick = {
                                if (entered.isNotEmpty()) {
                                    entered = entered.dropLast(1)
                                    error = false
                                }
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(SmileWhite.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = SmileInk)
                        }
                        else -> Surface(
                            onClick = {
                                if (entered.length < 4) {
                                    val next = entered + key
                                    entered = next
                                    error = false
                                    if (next.length == 4) {
                                        if (CryptoManager.hashPinWithSalt(next, userId) == expectedPinHash) {
                                            onUnlocked()
                                        } else {
                                            error = true
                                            entered = ""
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = SmileWhite,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("pin_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SmileInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
