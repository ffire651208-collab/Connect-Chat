package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondarySky
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConnectChatApp(
    viewModel: ChatViewModel = viewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val inAppNotification by viewModel.inAppNotification.collectAsState()
    val callState by viewModel.callState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Core Content Router ---
        Crossfade(targetState = isLoggedIn, label = "AuthRouter") { loggedIn ->
            if (loggedIn) {
                MainLayout(viewModel)
            } else {
                AuthScreen(viewModel)
            }
        }

        // --- Active Full Screen Cover: Chat Conversation Window ---
        val activeChatId by viewModel.activeChatId.collectAsState()
        AnimatedVisibility(
            visible = isLoggedIn && activeChatId != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            ActiveChatScreen(viewModel)
        }

        // --- Active Full Screen Cover: WebRTC Call Logic Overlay ---
        AnimatedVisibility(
            visible = callState != "IDLE",
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            WebRTCCallOverlay(viewModel)
        }

        // --- In-App Push / Status Banner Alert ---
        AnimatedVisibility(
            visible = inAppNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alert",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = inAppNotification ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. AUTHENTICATION SCREENS & SIMULATION
// ==========================================
@Composable
fun AuthScreen(viewModel: ChatViewModel) {
    val isSignUpMode by viewModel.isSignUpMode.collectAsState()
    val isPhoneLoginMode by viewModel.isPhoneLoginMode.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var showResetPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(SecondarySky, PrimaryBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = "Connect Logo",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Connect Chat",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Production Ready Secure Messenger",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- PROACTIVE PREVIEW HELPER CARD ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌟 Instant Guest Access",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bypass manual registration to instantly explore all interactive chats, mock WebRTC calls, and the admin panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.loginGoogleSimulated() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Instant Guest Login", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (authError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = authError ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Forms Router
        if (showResetPassword) {
            ForgotPasswordForm(viewModel) { showResetPassword = false }
        } else if (isPhoneLoginMode) {
            PhoneOTPLoginForm(viewModel) { viewModel.isPhoneLoginMode.value = false }
        } else if (isSignUpMode) {
            SignUpForm(viewModel) { viewModel.isSignUpMode.value = false }
        } else {
            LoginForm(
                viewModel = viewModel,
                onForgotPasswordClick = { showResetPassword = true },
                onSignUpRequest = { viewModel.isSignUpMode.value = true },
                onPhoneLoginRequest = { viewModel.isPhoneLoginMode.value = true }
            )
        }
    }
}

@Composable
fun LoginForm(
    viewModel: ChatViewModel,
    onForgotPasswordClick: () -> Unit,
    onSignUpRequest: () -> Unit,
    onPhoneLoginRequest: () -> Unit
) {
    val email by viewModel.emailInput.collectAsState()
    val password by viewModel.passwordInput.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SimpleTextField(
            value = email,
            onValueChange = { viewModel.emailInput.value = it },
            label = "Email Address",
            leadingIcon = Icons.Default.Email
        )

        SimpleTextField(
            value = password,
            onValueChange = { viewModel.passwordInput.value = it },
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = { viewModel.handleLogin() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign In Securely", fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
            Text(
                text = "Register Free Account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSignUpRequest() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Aux login methods
        OutlinedButton(
            onClick = { onPhoneLoginRequest() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Default.Phone, "Phone OTP")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Login via Phone OTP")
        }

        OutlinedButton(
            onClick = { viewModel.loginGoogleSimulated() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Default.AccountCircle, "Google SSO")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue with Google")
        }
    }
}

@Composable
fun SignUpForm(viewModel: ChatViewModel, onCancel: () -> Unit) {
    val email by viewModel.emailInput.collectAsState()
    val password by viewModel.passwordInput.collectAsState()
    val username by viewModel.usernameInput.collectAsState()
    val displayName by viewModel.displayNameInput.collectAsState()
    val bio by viewModel.bioInput.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SimpleTextField(
            value = email,
            onValueChange = { viewModel.emailInput.value = it },
            label = "Email Address",
            leadingIcon = Icons.Default.Email
        )
        SimpleTextField(
            value = username,
            onValueChange = { viewModel.usernameInput.value = it },
            label = "Choose @Username",
            leadingIcon = Icons.Default.AlternateEmail
        )
        SimpleTextField(
            value = displayName,
            onValueChange = { viewModel.displayNameInput.value = it },
            label = "Full Name",
            leadingIcon = Icons.Default.Person
        )
        SimpleTextField(
            value = bio,
            onValueChange = { viewModel.bioInput.value = it },
            label = "Short Bio",
            leadingIcon = Icons.Default.Info
        )
        SimpleTextField(
            value = password,
            onValueChange = { viewModel.passwordInput.value = it },
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = { viewModel.handleSignUp() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Production Account", fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Standard Login")
        }
    }
}

@Composable
fun PhoneOTPLoginForm(viewModel: ChatViewModel, onCancel: () -> Unit) {
    val phone by viewModel.phoneInput.collectAsState()
    val otp by viewModel.otpInput.collectAsState()
    val isOtpSent by viewModel.isOtpSent.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SimpleTextField(
            value = phone,
            onValueChange = { viewModel.phoneInput.value = it },
            label = "Phone Number (+1...)",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        if (isOtpSent) {
            SimpleTextField(
                value = otp,
                onValueChange = { viewModel.otpInput.value = it },
                label = "OTP SMS Verification Code",
                leadingIcon = Icons.Default.Key,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = { viewModel.verifyOTP() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Code & Logging", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = { viewModel.requestOTP() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send Interactive OTP via SMS", fontWeight = FontWeight.Bold)
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Standard Login")
        }
    }
}

@Composable
fun ForgotPasswordForm(viewModel: ChatViewModel, onCancel: () -> Unit) {
    val email by viewModel.emailInput.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SimpleTextField(
            value = email,
            onValueChange = { viewModel.emailInput.value = it },
            label = "Your Email Address",
            leadingIcon = Icons.Default.Email
        )
        Button(
            onClick = { viewModel.triggerForgotPassword() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Send Instructions", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Login")
        }
    }
}

// ==========================================
// 2. MAIN NAVIGATIONAL CONTENT
// ==========================================
@Composable
fun MainLayout(viewModel: ChatViewModel) {
    var activeTab by remember { mutableStateOf("CHATS") } // CHATS, CALLS, CONTACTS, ADMIN
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == "CHATS",
                    onClick = { activeTab = "CHATS" },
                    icon = { Icon(Icons.Default.Chat, "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = activeTab == "CALLS",
                    onClick = { activeTab = "CALLS" },
                    icon = { Icon(Icons.Default.Call, "Calls") },
                    label = { Text("Calls") }
                )
                NavigationBarItem(
                    selected = activeTab == "CONTACTS",
                    onClick = { activeTab = "CONTACTS" },
                    icon = { Icon(Icons.Default.People, "Friends") },
                    label = { Text("People") }
                )
                NavigationBarItem(
                    selected = activeTab == "ADMIN",
                    onClick = { activeTab = "ADMIN" },
                    icon = { Icon(Icons.Default.Security, "Moderation") },
                    label = { Text("Admin") }
                )
            }
        },
        floatingActionButton = {
            if (activeTab == "CHATS") {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.GroupAdd, "New Group")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "CHATS" -> ChatsDashboard(viewModel)
                "CALLS" -> CallsHistoryTab(viewModel)
                "CONTACTS" -> ContactsTabScreen(viewModel)
                "ADMIN" -> AdminDashboardScreen(viewModel)
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(viewModel) { showCreateGroupDialog = false }
    }
}

// ==========================================
// 3. CHATS LIST SCREEN (DASHBOARD)
// ==========================================
@Composable
fun ChatsDashboard(viewModel: ChatViewModel) {
    val chats by viewModel.allChats.collectAsState()
    val searchQuery by viewModel.chatSearchQuery.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showProfileEditDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // App top header
        HeaderBar(
            title = "Connect Chat",
            subtitle = "@" + (currentUser?.username ?: "connected"),
            avatarUri = currentUser?.photoUri,
            navigationIcon = {
                IconButton(onClick = { showProfileEditDialog = true }) {
                    Icon(Icons.Default.AccountCircle, "Profile", tint = MaterialTheme.colorScheme.primary)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Default.ExitToApp, "Logout")
                }
            }
        )

        // Search Bar with Margin
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.chatSearchQuery.value = it },
            placeholder = { Text("Search conversations...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Chats items list
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No chats matching found",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Add a friend in the 'People' tab or launch a Group from the Action Button.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Pin list separation
                val pinned = chats.filter { it.isPinned }
                val normal = chats.filter { !it.isPinned }

                if (pinned.isNotEmpty()) {
                    item {
                        Text(
                            "PINNED CHATS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(pinned) { chat ->
                        ConversationRow(chat, viewModel)
                    }
                }

                if (normal.isNotEmpty()) {
                    item {
                        Text(
                            "ALL MESSAGES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(normal) { chat ->
                        ConversationRow(chat, viewModel)
                    }
                }
            }
        }
    }

    if (showProfileEditDialog && currentUser != null) {
        ProfileEditDialog(currentUser!!, viewModel) { showProfileEditDialog = false }
    }
}

@Composable
fun ConversationRow(chat: ChatEntity, viewModel: ChatViewModel) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeLabel = formatter.format(Date(chat.lastMessageTime))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectChat(chat.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            ProfileImage(photoUri = chat.avatarUri, displayName = chat.name, size = 50)
            if (chat.type == "private") {
                // Try and discover status from profiles
                val profiles by viewModel.allUserProfiles.collectAsState()
                val profile = profiles.find { it.id == chat.id }
                if (profile != null) {
                    StatusBadge(
                        status = profile.status,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 4.dp)
                    )
                }

                val hasTyping = chat.isTypingUserId != null
                val displayText = if (hasTyping) "Typing..." else chat.lastMessageText
                val displayColor = if (hasTyping) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text = displayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = displayColor,
                    modifier = Modifier.weight(1f)
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. ACTIVE CONVERSATION SCREEN
// ==========================================
@Composable
fun ActiveChatScreen(viewModel: ChatViewModel) {
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val draft by viewModel.draftMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val typingUser by viewModel.activeTypingUser.collectAsState()
    val selectedMessageForAction by viewModel.selectedMessageForAction.collectAsState()
    val isEditingMessage by viewModel.isEditingMessage.collectAsState()

    var showAttachmentsSheet by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    if (activeChat == null) return

    BackHandler {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.selectChat(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // App bar for active chat
        HeaderBar(
            title = activeChat!!.name,
            subtitle = if (typingUser != null) "Typing..." else "Online encrypted stream",
            avatarUri = activeChat!!.avatarUri,
            navigationIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.selectChat(null)
                }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            },
            actions = {
                // Call buttons trigger calling workflow
                IconButton(onClick = { viewModel.initiateWebRTCCall(activeChat!!.id, "voice") }) {
                    Icon(Icons.Default.Call, "Voice WebRTC Codec")
                }
                IconButton(onClick = { viewModel.initiateWebRTCCall(activeChat!!.id, "video") }) {
                    Icon(Icons.Default.VideoCall, "Video WebRTC WebCam")
                }
                IconButton(onClick = { viewModel.togglePin(activeChat!!.id, activeChat!!.isPinned) }) {
                    Icon(
                        imageVector = if (activeChat!!.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        contentDescription = "Pin Chat",
                        tint = if (activeChat!!.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Message List Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                val isSelf = currentUser != null && message.senderId == currentUser!!.id
                MessageBubble(
                    message = message,
                    isCurrentUser = isSelf,
                    onLongClick = { viewModel.selectedMessageForAction.value = message },
                    onReactionClick = { emoji ->
                        viewModel.addReaction(message.id, emoji)
                    },
                    onReplyMode = {
                        viewModel.selectedMessageForAction.value = message
                        viewModel.isEditingMessage.value = false
                    }
                )
            }
        }

        // Typing dynamic banner
        if (typingUser != null) {
            Text(
                "Typing...",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
        }

        // Action Reply / Edit reference strip
        if (selectedMessageForAction != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isEditingMessage) "Editing Message" else "Replying to ${selectedMessageForAction!!.senderName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedMessageForAction!!.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        if (!isEditingMessage) {
                            IconButton(onClick = {
                                viewModel.isEditingMessage.value = true
                                viewModel.draftMessage.value = selectedMessageForAction!!.content
                            }) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                        }
                        IconButton(onClick = { viewModel.selectedMessageForAction.value = null }) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                }
            }
        }

        // Input Deck (Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showAttachmentsSheet = true }) {
                Icon(Icons.Default.AddCircle, "Attachments Menu", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = {
                // Simulated Emoji reactions panel trigger
                viewModel.draftMessage.value = draft + " 😂"
            }) {
                Icon(Icons.Default.Mood, "Emojis Codec", tint = MaterialTheme.colorScheme.primary)
            }

            // Text input bar
            TextField(
                value = draft,
                onValueChange = { viewModel.draftMessage.value = it },
                placeholder = { Text("Message...") },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp),
                trailingIcon = {
                    if (draft.isNotEmpty()) {
                        IconButton(onClick = { viewModel.handleSendMessage() }) {
                            Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        // Simulated voice recording capture
                        IconButton(onClick = {
                            viewModel.handleSendMedia(
                                type = "voice",
                                uri = "sim_voice.aac",
                                fileName = "Voice Note.aac",
                                sizeString = "250 KB",
                                duration = 12
                            )
                        }) {
                            Icon(Icons.Default.Mic, "Record Audio note", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }

    if (showAttachmentsSheet) {
        AttachmentsSheet(viewModel) { showAttachmentsSheet = false }
    }
}

// Attachment selection sheet mockup with accurate attachments triggers
@Composable
fun AttachmentsSheet(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Simulated Media", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select an interactive real-time media upload stream:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            viewModel.handleSendMedia(
                                type = "image",
                                uri = "https://images.unsplash.com/photo-1543269865-cbf427effbad?w=400",
                                fileName = "team_conference.jpg",
                                sizeString = "1.2 MB"
                            )
                            onDismiss()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Photo, null, tint = Color.White)
                        }
                        Text("Gallery Photo", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            viewModel.handleSendMedia(
                                type = "document",
                                uri = "specs_draft.pdf",
                                fileName = "connect_chat_architecture.pdf",
                                sizeString = "2.8 MB"
                            )
                            onDismiss()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.InsertDriveFile, null, tint = Color.White)
                        }
                        Text("Document PDF", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    )
}

// ==========================================
// 5. CALL HISTORY SCREEN TAB
// ==========================================
@Composable
fun CallsHistoryTab(viewModel: ChatViewModel) {
    val logs by viewModel.callLogs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderBar(
            title = "Calling Records",
            subtitle = "Active low latency WebRTC stream history"
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No calls recorded", fontWeight = FontWeight.Bold)
                    Text("WebRTC logs appear when calling friends directly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { call ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(
                                photoUri = if (call.callerName == "You") call.calleePhoto else call.callerPhoto,
                                displayName = if (call.callerName == "You") call.calleeName else call.callerName,
                                size = 44
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (call.callerName == "You") call.calleeName else call.callerName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (call.status == "Missed") Icons.Default.CallMissed else Icons.Default.CallMade,
                                        contentDescription = call.status,
                                        tint = if (call.status == "Missed") Color.Red else Color.Green,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val durationText = if (call.durationSec > 0) "${call.durationSec}s" else "No answer"
                                    Text(
                                        text = "${call.type.uppercase()} • $durationText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.initiateWebRTCCall(if (call.callerName == "You") call.calleeId else call.callerId, call.type) }) {
                            Icon(
                                imageVector = if (call.type == "video") Icons.Default.VideoCall else Icons.Default.Call,
                                contentDescription = "Dial-back peer connection",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. CONTACTS & RELATIONS SCREEN TAB
// ==========================================
@Composable
fun ContactsTabScreen(viewModel: ChatViewModel) {
    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.friendRequests.collectAsState()
    val allProfiles by viewModel.allUserProfiles.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderBar(
            title = "People & Channels",
            subtitle = "${friends.size} friends secured"
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (requests.isNotEmpty()) {
                item {
                    Text(
                        "PENDING FRIEND REQUESTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(requests) { request ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(photoUri = request.photoUri, displayName = request.displayName, size = 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(request.displayName, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Button(
                                onClick = { viewModel.respondToFriend(request.id, true) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Accept", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { viewModel.respondToFriend(request.id, false) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Decline", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "CONNECTED CONTACTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (friends.isEmpty()) {
                item {
                    Text(
                        "No contacts added. Explore users list below to start secure channels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(friends) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectChat(friend.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                ProfileImage(photoUri = friend.photoUri, displayName = friend.displayName, size = 44)
                                StatusBadge(status = friend.status, modifier = Modifier.align(Alignment.BottomEnd))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(friend.displayName, fontWeight = FontWeight.Bold)
                                Text(friend.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.initiateWebRTCCall(friend.id, "voice") }) {
                                Icon(Icons.Default.Call, "Call", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.handleBlockUser(friend.id, friend.isBlocked) }) {
                                Icon(
                                    imageVector = if (friend.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                    contentDescription = "Block options",
                                    tint = if (friend.isBlocked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Discover list
            val nonFriends = allProfiles.filter { !it.isFriend && !it.isFriendRequestReceived && it.id != viewModel.currentUser.value?.id }
            if (nonFriends.isNotEmpty()) {
                item {
                    Text(
                        "DISCOVER CHANNELS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(nonFriends) { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(photoUri = profile.photoUri, displayName = profile.displayName, size = 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(profile.displayName, fontWeight = FontWeight.Bold)
                                Text(profile.bio, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (profile.isFriendRequestSent) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sent", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.triggerAddFriend(profile.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add Friend", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. WEBRTC MEDIA CALL MODULE SCREEN OVERLAY
// ==========================================
@Composable
fun WebRTCCallOverlay(viewModel: ChatViewModel) {
    val activeCall by viewModel.activeCall.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val durationSec by viewModel.callDurationSec.collectAsState()
    
    val muted by viewModel.isMuted.collectAsState()
    val videoDisabled by viewModel.isVideoDisabled.collectAsState()
    val speakerOn by viewModel.isSpeakerphoneOn.collectAsState()

    val displayMin = durationSec / 60
    val displaySec = durationSec % 60
    val timerText = String.format("%02d:%02d", displayMin, displaySec)

    if (activeCall == null) return

    val peerName = if (activeCall!!.callerName == "You") activeCall!!.calleeName else activeCall!!.callerName
    val peerPhoto = if (activeCall!!.callerName == "You") activeCall!!.calleePhoto else activeCall!!.callerPhoto

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        // --- WebRTC Video Stream Simulation Renderer ---
        if (activeCall!!.type == "video" && !videoDisabled && callState == "CONNECTED") {
            // Simulated Peer Full Screen Cam Stream
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.VideoCameraFront, contentDescription = "Peer camera active", modifier = Modifier.size(96.dp), tint = AccentTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Simulated low-latency 1080p peer stream",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }

                // PiP (Local Self Stream)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 24.dp)
                        .size(100.dp, 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, "Your video input", tint = Color.LightGray)
                }
            }
        } else {
            // Voice Call Gradient Theme
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                        )
                    )
            )
        }

        // Caller Details layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileImage(photoUri = peerPhoto, displayName = peerName, size = 96)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = peerName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = if (callState == "CONNECTED") timerText else "WebRTC Secure: $callState",
                        color = if (callState == "CONNECTED") AccentTeal else Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Controls Row (Audio, Cam, Speaker, Mute)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    // Mute Button
                    FilledIconButton(
                        onClick = { viewModel.toggleMute() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (muted) Color.Red else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute audio stream",
                            tint = Color.White
                        )
                    }

                    // Hangup Button
                    FilledIconButton(
                        onClick = { viewModel.hangUpCall() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Red
                        ),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, "Terminate WebRTC RTC socket", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Video Camera Toggle
                    if (activeCall!!.type == "video") {
                        FilledIconButton(
                            onClick = { viewModel.toggleVideo() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (videoDisabled) Color.Red else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (videoDisabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "Toggle screen share video",
                                tint = Color.White
                            )
                        }
                    } else {
                        // Speaker switch for Voice calls
                        FilledIconButton(
                            onClick = { viewModel.toggleSpeakerphone() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (speakerOn) AccentTeal else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (speakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Speakerphone logic",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, "Lock secure encrypted codec", tint = Color.LightGray.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "End-to-End Encrypted Codec Stream",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. ADMIN MODERATION PANEL & DASHBOARD
// ==========================================
@Composable
fun AdminDashboardScreen(viewModel: ChatViewModel) {
    val reports by viewModel.reportsList.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val profiles by viewModel.allUserProfiles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderBar(
            title = "Moderator Center",
            subtitle = "Active moderation, reports logging, rates, safety rules"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analytics Dashboard widgets
            item {
                Text(
                    "ANALYTICS SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCard(
                        title = "Subscribers",
                        value = "${profiles.size + 10} accounts", // Simulates growth offline
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = "Reports Active",
                        value = "${reports.filter { !it.isResolved }.size} pending",
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Moderation list logs
            item {
                Text(
                    "SUBMITTED COMPLAINT TICKETS (${reports.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (reports.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Connect Safety Protocol active. No safety incidents reported in database.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(reports) { report ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Report ID: " + report.id.take(6).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatusResolvedBadge(isResolved = report.isResolved)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reporter: ${report.reporterName}", style = MaterialTheme.typography.bodyMedium)
                            Text("Reported user: ${report.reportedUserName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Reason details: ${report.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            )

                            if (!report.messageText.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Flagged transcript: \"${report.messageText}\"",
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (!report.isResolved) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.resolveModerationReport(report.id, true, report.reportedUserId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("Restrict User", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { viewModel.resolveModerationReport(report.id, false, report.reportedUserId) }) {
                                        Text("Dismiss Ticket", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun StatusResolvedBadge(isResolved: Boolean) {
    val container = if (isResolved) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val text = if (isResolved) Color(0xFF065F46) else Color(0xFF991B1B)
    val label = if (isResolved) "Resolved" else "Open Alert"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, color = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// 9. AUXILIARY POPUPS & DIALOGS
// ==========================================
@Composable
fun CreateGroupDialog(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assemble Group Chat", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Start an interactive chat for the entire team:")
                SimpleTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = "Group Identity Name",
                    leadingIcon = Icons.Default.Group
                )
                SimpleTextField(
                    value = groupDesc,
                    onValueChange = { groupDesc = it },
                    label = "Brief Focus Context / description",
                    leadingIcon = Icons.Default.Info
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.trim().isNotEmpty()) {
                        viewModel.createGroup(groupName, groupDesc)
                        onDismiss()
                    }
                }
            ) {
                Text("Launch Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProfileEditDialog(profile: UserProfile, viewModel: ChatViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var photoUri by remember { mutableStateOf(profile.photoUri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Secure Personal Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ProfileImage(photoUri = photoUri, displayName = name, size = 64)
                }

                SimpleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Display Name",
                    leadingIcon = Icons.Default.Person
                )
                SimpleTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = "About Bio",
                    leadingIcon = Icons.Default.Badge
                )
                SimpleTextField(
                    value = photoUri,
                    onValueChange = { photoUri = it },
                    label = "Profile Image URL Asset",
                    leadingIcon = Icons.Default.AddPhotoAlternate
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.updateProfile(name, bio, photoUri)
                onDismiss()
            }) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
