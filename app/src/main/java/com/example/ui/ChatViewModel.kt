package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(
        application,
        db.userProfileDao(),
        db.chatDao(),
        db.messageDao(),
        db.callDao(),
        db.reportDao()
    )

    // --- Authentication State ---
    val currentUser = repository.currentUserState
    
    private val _isLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Auth Textfield inputs
    var emailInput = MutableStateFlow("")
    var passwordInput = MutableStateFlow("")
    var displayNameInput = MutableStateFlow("")
    var usernameInput = MutableStateFlow("")
    var bioInput = MutableStateFlow("")
    var photoUriInput = MutableStateFlow("")
    
    var phoneInput = MutableStateFlow("")
    var otpInput = MutableStateFlow("")
    var isOtpSent = MutableStateFlow(false)
    var isPhoneLoginMode = MutableStateFlow(false)
    var isSignUpMode = MutableStateFlow(false)
    var authError = MutableStateFlow<String?>(null)

    // --- Active Chat Context ---
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    val activeMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessages(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var chatSearchQuery = MutableStateFlow("")
    var messageSearchQuery = MutableStateFlow("")
    var draftMessage = MutableStateFlow("")

    // Reply / Edit state
    var selectedMessageForAction = MutableStateFlow<MessageEntity?>(null)
    var isEditingMessage = MutableStateFlow(false)

    // typing status
    val activeTypingUser: StateFlow<String?> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getChats().map { list -> list.find { it.id == id }?.isTypingUserId }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- General Dashboard States ---
    val allChats: StateFlow<List<ChatEntity>> = repository.getChats()
        .combine(chatSearchQuery) { list, query ->
            if (query.isEmpty()) list else list.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallEntity>> = repository.getCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendRequests = repository.getFriendRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends = repository.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserProfiles = repository.getUserProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Call Simulation WebRTC State ---
    private val _activeCall = MutableStateFlow<CallEntity?>(null)
    val activeCall: StateFlow<CallEntity?> = _activeCall.asStateFlow()

    private val _callState = MutableStateFlow<String>("IDLE") // IDLE, DIALING, CONNECTED, DISCONNECTED
    val callState: StateFlow<String> = _callState.asStateFlow()

    var callDurationSec = MutableStateFlow(0)
    var isMuted = MutableStateFlow(false)
    var isVideoDisabled = MutableStateFlow(false)
    var isSpeakerphoneOn = MutableStateFlow(true)
    private var callTimerJob: Job? = null

    // --- Admin state ---
    val reportsList = repository.getReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Notification & Toast alert states ---
    private val _inAppNotification = MutableStateFlow<String?>(null)
    val inAppNotification: StateFlow<String?> = _inAppNotification.asStateFlow()

    fun showToast(message: String) {
        _inAppNotification.value = message
        viewModelScope.launch {
            delay(3000)
            if (_inAppNotification.value == message) {
                _inAppNotification.value = null
            }
        }
    }

    // --- Action Handlers ---

    // Auth Actions
    fun handleLogin() {
        if (emailInput.value.isEmpty() || passwordInput.value.isEmpty()) {
            authError.value = "Email and Password cannot be empty."
            return
        }
        viewModelScope.launch {
            val success = repository.loginUser(emailInput.value, passwordInput.value)
            if (success) {
                _isLoggedIn.value = true
                authError.value = null
                showToast("Welcome back to Connect Chat!")
            } else {
                authError.value = "Authentication failed. Clear details and try again."
            }
        }
    }

    fun handleSignUp() {
        if (emailInput.value.isEmpty() || usernameInput.value.isEmpty() || displayNameInput.value.isEmpty()) {
            authError.value = "Please fill in all standard details."
            return
        }
        viewModelScope.launch {
            val success = repository.signUpUser(
                emailInput.value,
                usernameInput.value,
                displayNameInput.value,
                bioInput.value,
                photoUriInput.value
            )
            if (success) {
                _isLoggedIn.value = true
                isSignUpMode.value = false
                authError.value = null
                showToast("Account successfully registered!")
            }
        }
    }

    fun requestOTP() {
        if (phoneInput.value.isEmpty() || phoneInput.value.length < 8) {
            authError.value = "Please enter a valid phone number."
            return
        }
        viewModelScope.launch {
            repository.loginWithPhoneOTP(phoneInput.value)
            isOtpSent.value = true
            otpInput.value = "123456" // Automatically input mockOTP
            showToast("OTP Code 123456 sent securely via phone network.")
        }
    }

    fun verifyOTP() {
        viewModelScope.launch {
            val success = repository.verifyOTPAndLogin(phoneInput.value, otpInput.value)
            if (success) {
                _isLoggedIn.value = true
                isOtpSent.value = false
                isPhoneLoginMode.value = false
                authError.value = null
                showToast("Phone authenticated successfully!")
            } else {
                authError.value = "Invalid OTP verification, try again."
            }
        }
    }

    fun loginGoogleSimulated() {
        viewModelScope.launch {
            val success = repository.loginWithGoogle(
                "siddiqullah@google.com",
                "Siddiqullah",
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            )
            if (success) {
                _isLoggedIn.value = true
                authError.value = null
                showToast("Authenticated via Google SSO account.")
            }
        }
    }

    fun triggerForgotPassword() {
        if (emailInput.value.isEmpty()) {
            authError.value = "Please enter your email to send reset instructions."
            return
        }
        showToast("Secure password reset link sent to ${emailInput.value}.")
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
            _activeChatId.value = null
            _activeChat.value = null
            showToast("Successfully logged out securely.")
        }
    }

    fun updateProfile(displayName: String, bio: String, photoUri: String) {
        viewModelScope.launch {
            repository.updateCurrentUserProfile(displayName, bio, photoUri)
            showToast("Profile securely saved!")
        }
    }

    // Chat management
    fun selectChat(chatId: String?) {
        _activeChatId.value = chatId
        if (chatId != null) {
            viewModelScope.launch {
                val chat = db.chatDao().getChatById(chatId)
                _activeChat.value = chat
                repository.markChatAsRead(chatId)
            }
        } else {
            _activeChat.value = null
        }
    }

    fun togglePin(chatId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.pinChat(chatId, !currentPinned)
            showToast(if (!currentPinned) "Conversation pinned to top" else "Conversation unpinned")
        }
    }

    fun toggleArchive(chatId: String, currentArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveChat(chatId, !currentArchived)
            showToast(if (!currentArchived) "Conversation archived" else "Conversation unarchived")
        }
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            val newId = repository.createGroupChat(name, description)
            showToast("Group Chat created! Start adding team members.")
            selectChat(newId)
        }
    }

    // Message sending and managing
    fun handleSendMessage() {
        val input = draftMessage.value.trim()
        val chatId = _activeChatId.value ?: return
        if (input.isEmpty()) return

        viewModelScope.launch {
            val actionMessage = selectedMessageForAction.value
            if (isEditingMessage.value && actionMessage != null) {
                repository.editMessage(actionMessage.id, input)
                isEditingMessage.value = false
                selectedMessageForAction.value = null
                showToast("Message updated successfully")
            } else if (actionMessage != null) {
                // Reply to message
                repository.sendMessage(
                    chatId = chatId,
                    content = input,
                    type = "text",
                    replyToId = actionMessage.id,
                    replyToText = actionMessage.content
                )
                selectedMessageForAction.value = null
            } else {
                // Direct message
                repository.sendTextMessage(chatId, input)
            }
            draftMessage.value = ""
        }
    }

    fun handleSendMedia(type: String, uri: String, fileName: String, sizeString: String, duration: Int = 0) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                content = "[${type.uppercase()}] $fileName",
                type = type,
                mediaUri = uri,
                mediaName = fileName,
                mediaSize = sizeString,
                durationSec = duration
            )
            showToast("Sent simulated ${type} media attachment.")
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            showToast("Message deleted")
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.addReaction(messageId, emoji)
        }
    }

    fun forwardMessage(messageId: String, targetChatId: String) {
        viewModelScope.launch {
            val message = db.messageDao().getMessageById(messageId) ?: return@launch
            repository.sendMessage(
                chatId = targetChatId,
                content = message.content,
                type = message.type,
                mediaUri = message.mediaUri,
                mediaName = message.mediaName,
                mediaSize = message.mediaSize,
                durationSec = message.durationSec
            )
            showToast("Message securely forwarded.")
        }
    }

    // Contacts and Relations
    fun triggerAddFriend(userId: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(userId)
            showToast("Secure friend request dispatched.")
        }
    }

    fun respondToFriend(userId: String, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToFriendRequest(userId, accept)
            showToast(if (accept) "Friend request accepted!" else "Request declined")
        }
    }

    fun handleBlockUser(userId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.blockUser(userId, !isBlocked)
            showToast(if (!isBlocked) "User blocked securely" else "User unblocked")
        }
    }

    fun handleReport(reportedUserId: String, reason: String, textSample: String?) {
        viewModelScope.launch {
            repository.reportUser(reportedUserId, reason, textSample)
            showToast("Safetynet: Report submitted successfully to moderation.")
        }
    }

    // Administration Panel Action Control
    fun resolveModerationReport(reportId: String, penaltyApply: Boolean, targetUserId: String) {
        viewModelScope.launch {
            repository.resolveReport(reportId, true)
            if (penaltyApply) {
                repository.adminToggleUserLock(targetUserId, true)
                showToast("Moderation: Report resolved and offending user restricted.")
            } else {
                showToast("Moderation: Report approved and archived.")
            }
        }
    }

    // --- WebRTC Calling Engine ---

    fun initiateWebRTCCall(calleeId: String, type: String) {
        viewModelScope.launch {
            val callId = repository.startCall(calleeId, type)
            val log = db.callDao().getAllCallsFlow().first().firstOrNull { it.id == callId }
            _activeCall.value = log
            _callState.value = "DIALING"
            showToast("Initializing WebRTC secure signal logic...")

            // Simulate ringing tone delay, then connect call automatically!
            delay(2500)
            if (_callState.value == "DIALING") {
                _callState.value = "CONNECTED"
                startCallTimer()
                showToast("Encrypted peer connection established via WebRTC.")
            }
        }
    }

    fun startCallTimer() {
        callDurationSec.value = 0
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_callState.value == "CONNECTED") {
                delay(1000)
                callDurationSec.value += 1
            }
        }
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
    }

    fun toggleVideo() {
        isVideoDisabled.value = !isVideoDisabled.value
    }

    fun toggleSpeakerphone() {
        isSpeakerphoneOn.value = !isSpeakerphoneOn.value
    }

    fun hangUpCall() {
        val currentCall = _activeCall.value
        val duration = callDurationSec.value
        callTimerJob?.cancel()
        _callState.value = "DISCONNECTED"
        viewModelScope.launch {
            if (currentCall != null) {
                repository.endCall(currentCall.id, duration, "Completed")
            }
            delay(1500)
            _activeCall.value = null
            _callState.value = "IDLE"
            callDurationSec.value = 0
        }
    }
}
