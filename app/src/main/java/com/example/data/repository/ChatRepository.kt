package com.example.data.repository

import android.content.Context
import android.text.format.DateUtils
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val callDao: CallDao,
    private val reportDao: ReportDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sharedPrefs = context.getSharedPreferences("connect_chat_prefs", Context.MODE_PRIVATE)

    // Current logged in user details state
    private val _currentUserState = MutableStateFlow<UserProfile?>(null)
    val currentUserState: StateFlow<UserProfile?> = _currentUserState.asStateFlow()

    init {
        // Load current user from preferences
        val storedUserId = sharedPrefs.getString("logged_in_user_id", null)
        if (storedUserId != null) {
            repositoryScope.launch {
                val profile = userProfileDao.getProfileById(storedUserId)
                if (profile != null) {
                    _currentUserState.value = profile
                    userProfileDao.updateStatus(storedUserId, "Online")
                } else {
                    // Seed standard accounts if missing
                    initializeDefaultData()
                }
            }
        } else {
            repositoryScope.launch {
                initializeDefaultData()
            }
        }
    }

    // --- Authentication Flow ---

    fun isUserLoggedIn(): Boolean {
        return sharedPrefs.getString("logged_in_user_id", null) != null
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        // Real logic for authentication simulation that persists standard user
        val targetUsername = email.substringBefore("@")
        val userId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
        var profile = userProfileDao.getProfileById(userId)
        
        if (profile == null) {
            profile = UserProfile(
                id = userId,
                username = targetUsername,
                displayName = targetUsername.replaceFirstChar { it.uppercase() },
                bio = "Hey there! I am using Connect Chat.",
                photoUri = "",
                status = "Online",
                isFriend = false
            )
            userProfileDao.insertOrUpdateProfile(profile)
        } else {
            userProfileDao.updateStatus(userId, "Online")
        }

        sharedPrefs.edit().putString("logged_in_user_id", userId).apply()
        _currentUserState.value = profile
        return true
    }

    suspend fun signUpUser(email: String, username: String, displayName: String, bio: String, photoUri: String): Boolean {
        val userId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
        val profile = UserProfile(
            id = userId,
            username = username,
            displayName = displayName,
            bio = bio,
            photoUri = photoUri,
            status = "Online",
            isFriend = false
        )
        userProfileDao.insertOrUpdateProfile(profile)
        sharedPrefs.edit().putString("logged_in_user_id", userId).apply()
        _currentUserState.value = profile
        return true
    }

    suspend fun loginWithPhoneOTP(phoneNumber: String): String {
        // Triggers calling verification and returns simulated confirmation code
        return "123456"
    }

    suspend fun verifyOTPAndLogin(phoneNumber: String, code: String): Boolean {
        if (code == "123456" || code.length == 6) {
            val userId = UUID.nameUUIDFromBytes(phoneNumber.toByteArray()).toString()
            var profile = userProfileDao.getProfileById(userId)
            if (profile == null) {
                profile = UserProfile(
                    id = userId,
                    username = "phone_${phoneNumber.takeLast(4)}",
                    displayName = "Member ${phoneNumber.takeLast(4)}",
                    bio = "Logged in via Phone OTP secure verification.",
                    photoUri = "",
                    status = "Online",
                    isFriend = false
                )
                userProfileDao.insertOrUpdateProfile(profile)
            } else {
                userProfileDao.updateStatus(userId, "Online")
            }
            sharedPrefs.edit().putString("logged_in_user_id", userId).apply()
            _currentUserState.value = profile
            return true
        }
        return false
    }

    suspend fun loginWithGoogle(email: String, displayName: String, photoUrl: String): Boolean {
        val userId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
        val profile = UserProfile(
            id = userId,
            username = email.substringBefore("@"),
            displayName = displayName,
            bio = "I logged in with Google securely.",
            photoUri = photoUrl,
            status = "Online",
            isFriend = false
        )
        userProfileDao.insertOrUpdateProfile(profile)
        sharedPrefs.edit().putString("logged_in_user_id", userId).apply()
        _currentUserState.value = profile
        return true
    }

    suspend fun updateCurrentUserProfile(displayName: String, bio: String, photoUri: String) {
        val current = _currentUserState.value ?: return
        val updated = current.copy(displayName = displayName, bio = bio, photoUri = photoUri)
        userProfileDao.insertOrUpdateProfile(updated)
        _currentUserState.value = updated
    }

    suspend fun logout() {
        val currentId = sharedPrefs.getString("logged_in_user_id", null)
        if (currentId != null) {
            userProfileDao.updateStatus(currentId, "Offline")
        }
        sharedPrefs.edit().remove("logged_in_user_id").apply()
        _currentUserState.value = null
    }

    // --- Core Chat Observables ---

    fun getChats(): Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()
    fun getCalls(): Flow<List<CallEntity>> = callDao.getAllCallsFlow()
    fun getReports(): Flow<List<ReportEntity>> = reportDao.getAllReportsFlow()
    fun getFriends(): Flow<List<UserProfile>> = userProfileDao.getFriendsFlow()
    fun getFriendRequests(): Flow<List<UserProfile>> = userProfileDao.getFriendRequestsFlow()
    fun getMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChatFlow(chatId)

    fun getUserProfiles(): Flow<List<UserProfile>> = userProfileDao.getAllProfilesFlow()

    // --- Message and Chat Actions ---

    suspend fun sendTextMessage(chatId: String, content: String, replyToId: String? = null, replyToText: String? = null) {
        sendMessage(chatId, content, "text", replyToId = replyToId, replyToText = replyToText)
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: String,
        mediaUri: String? = null,
        mediaName: String? = null,
        mediaSize: String? = null,
        durationSec: Int = 0,
        replyToId: String? = null,
        replyToText: String? = null
    ) {
        val currentUser = _currentUserState.value ?: return
        val messageId = UUID.randomUUID().toString()
        
        val message = MessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderPhoto = currentUser.photoUri,
            content = content,
            type = type,
            mediaUri = mediaUri,
            mediaName = mediaName,
            mediaSize = mediaSize,
            durationSec = durationSec,
            timestamp = System.currentTimeMillis(),
            isSent = true,
            isRead = false,
            replyToId = replyToId,
            replyToText = replyToText,
            reactionCsv = ""
        )

        messageDao.insertMessage(message)
        chatDao.updateLastMessage(chatId, if (type == "text") content else "[${type.replaceFirstChar { it.uppercase() }}]", System.currentTimeMillis())

        // Trigger real-time automatic smart simulated replies
        triggerAutoReply(chatId, content)
    }

    suspend fun editMessage(messageId: String, newContent: String) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null) {
            messageDao.editMessage(messageId, newContent)
            chatDao.updateLastMessage(msg.chatId, newContent, System.currentTimeMillis())
        }
    }

    suspend fun deleteMessage(messageId: String) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null) {
            messageDao.deleteMessage(messageId)
            chatDao.updateLastMessage(msg.chatId, "Message was deleted", System.currentTimeMillis())
        }
    }

    suspend fun addReaction(messageId: String, emoji: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        val currentReactions = msg.reactionCsv.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentReactions.contains(emoji)) {
            currentReactions.remove(emoji)
        } else {
            currentReactions.add(emoji)
        }
        messageDao.updateReactions(messageId, currentReactions.joinToString(","))
    }

    suspend fun searchMessages(query: String): List<MessageEntity> {
        return messageDao.searchMessages(query)
    }

    suspend fun pinChat(chatId: String, isPinned: Boolean) {
        chatDao.updatePinned(chatId, isPinned)
    }

    suspend fun archiveChat(chatId: String, isArchived: Boolean) {
        chatDao.updateArchived(chatId, isArchived)
    }

    suspend fun markChatAsRead(chatId: String) {
        val currentUser = _currentUserState.value ?: return
        messageDao.markChatAsRead(chatId, currentUser.id)
        chatDao.updateUnreadCount(chatId, 0)
    }

    suspend fun createGroupChat(name: String, description: String, initialAvatarUri: String = ""): String {
        val chatId = UUID.randomUUID().toString()
        val chat = ChatEntity(
            id = chatId,
            type = "group",
            name = name,
            description = description,
            avatarUri = initialAvatarUri,
            isArchived = false,
            isPinned = false,
            unreadCount = 0,
            lastMessageText = "Group Chat Created",
            lastMessageTime = System.currentTimeMillis()
        )
        chatDao.insertOrUpdateChat(chat)

        // Seed dynamic notification
        val welcomeMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            senderPhoto = "",
            content = "Welcome to $name! Start messaging securely.",
            type = "text",
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(welcomeMessage)
        return chatId
    }

    // --- Friends and User Actions ---

    suspend fun sendFriendRequest(userId: String) {
        userProfileDao.updateRequestSent(userId, true)
    }

    suspend fun respondToFriendRequest(userId: String, accept: Boolean) {
        userProfileDao.updateFriendship(userId, accept)
    }

    suspend fun blockUser(userId: String, block: Boolean) {
        userProfileDao.updateBlocked(userId, block)
    }

    suspend fun reportUser(reportedUserId: String, reason: String, sampleMessageText: String?) {
        val currentUser = _currentUserState.value ?: return
        val reportedProfile = userProfileDao.getProfileById(reportedUserId) ?: return
        val report = ReportEntity(
            id = UUID.randomUUID().toString(),
            reporterId = currentUser.id,
            reporterName = currentUser.displayName,
            reportedUserId = reportedUserId,
            reportedUserName = reportedProfile.displayName,
            reason = reason,
            messageText = sampleMessageText,
            timestamp = System.currentTimeMillis(),
            isResolved = false
        )
        reportDao.insertReport(report)
    }

    suspend fun resolveReport(reportId: String, resolve: Boolean) {
        reportDao.updateReportState(reportId, resolve)
    }

    suspend fun adminToggleUserLock(userId: String, isBlocked: Boolean) {
        userProfileDao.updateBlocked(userId, isBlocked)
    }

    // --- WebRTC Call Interface ---

    suspend fun startCall(calleeId: String, type: String): String {
        val currentUser = _currentUserState.value ?: return UUID.randomUUID().toString()
        val callee = userProfileDao.getProfileById(calleeId) ?: return UUID.randomUUID().toString()
        val callId = UUID.randomUUID().toString()

        val call = CallEntity(
            id = callId,
            callerId = currentUser.id,
            callerName = currentUser.displayName,
            callerPhoto = currentUser.photoUri,
            calleeId = callee.id,
            calleeName = callee.displayName,
            calleePhoto = callee.photoUri,
            type = type,
            timestamp = System.currentTimeMillis(),
            durationSec = 0,
            status = "Active"
        )
        callDao.insertCall(call)
        return callId
    }

    suspend fun endCall(callId: String, duration: Int, status: String) {
        callDao.updateCallEnd(callId, duration, status)
    }

    // --- Interactive Real-time Reply Engine Simulation ---

    private fun triggerAutoReply(chatId: String, userMessage: String) {
        repositoryScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            val currentUser = _currentUserState.value ?: return@launch
            
            // Wait 1.5 seconds to simulate networks
            delay(1000)

            if (chat.type == "private") {
                // Find contact profile
                val contactId = chatId // In standard direct chats, chatId is often the target userId
                val contact = userProfileDao.getProfileById(contactId) ?: return@launch

                if (contact.isBlocked) return@launch

                // Update status to online, trigger typing indicator
                userProfileDao.updateStatus(contactId, "Online")
                chatDao.updateTyping(chatId, contactId)

                // Simulate Typing duration
                val typingDelay = (userMessage.length * 30L).coerceIn(1000L, 3000L)
                delay(typingDelay)

                // Determine an intelligent reply based on message keywords
                val replyText = generateBotReply(contact.displayName, userMessage)

                val replyMessage = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = contactId,
                    senderName = contact.displayName,
                    senderPhoto = contact.photoUri,
                    content = replyText,
                    type = "text",
                    timestamp = System.currentTimeMillis(),
                    isSent = true,
                    isRead = false,
                    reactionCsv = ""
                )

                // Clear typing, insert message, mark chat is unread count updated
                chatDao.updateTyping(chatId, null)
                messageDao.insertMessage(replyMessage)
                chatDao.updateLastMessage(chatId, replyText, System.currentTimeMillis())
                chatDao.updateUnreadCount(chatId, chat.unreadCount + 1)
                
                // Keep online for 5 seconds then go Away/Offline
                delay(5000)
                userProfileDao.updateStatus(contactId, "Offline")
            } else {
                // Group Chat replies from different users in the group
                chatDao.updateTyping(chatId, "sophia_id")
                delay(1800)
                val response = "Hey ${currentUser.displayName}! That sounds like an awesome plan. Let's synchronize here."
                val responseMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = "sophia_id",
                    senderName = "Sophia Martinez",
                    senderPhoto = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = response,
                    type = "text",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.updateTyping(chatId, null)
                messageDao.insertMessage(responseMsg)
                chatDao.updateLastMessage(chatId, response, System.currentTimeMillis())
                chatDao.updateUnreadCount(chatId, chat.unreadCount + 1)
            }
        }
    }

    private fun generateBotReply(name: String, msg: String): String {
        val m = msg.lowercase()
        return when {
            m.contains("hello") || m.contains("hi") || m.contains("hey") -> {
                "Hey! Hope your day is going awesome. How can I help you today?"
            }
            m.contains("how are you") -> {
                "I'm keeping super active! Working on polished Jetpack Compose designs today. How are things on your end?"
            }
            m.contains("webrtc") || m.contains("call") || m.contains("video") -> {
                "Connect Chat's WebRTC implementation supports flawless live Calling! Press the Call or Video icon at the top right to launch the interactive peer session."
            }
            m.contains("admin") || m.contains("moderator") -> {
                "If you need to report anything, you can block or report me via the profile dropdown. You can also view reported logs inside the Admin Dashboard tab."
            }
            m.contains("photo") || m.contains("image") -> {
                "Awesome message! Feel free to send an image attachment by clicking the '+' plus symbol next to the input bar. It supports full media simulations."
            }
            else -> {
                "Perfect! That makes total sense. I'm excited to keep chatting here on Connect Chat."
            }
        }
    }

    // --- Seeding Default Database Entries ---

    private suspend fun initializeDefaultData() {
        val current = sharedPrefs.getString("logged_in_user_id", null)
        val hasProfiles = userProfileDao.getProfileById("sophia_id") != null
        if (hasProfiles) return

        // Insert Default Profiles (Contacts)
        val defaults = listOf(
            UserProfile(
                id = "sophia_id",
                username = "sophia",
                displayName = "Sophia Martinez",
                bio = "Designing elegant mobile spaces. Co-Founder @Connect.",
                photoUri = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                status = "Offline",
                isFriend = true
            ),
            UserProfile(
                id = "liam_id",
                username = "liam_miller",
                displayName = "Liam Miller",
                bio = "Android Engineer. Living in the terminal.",
                photoUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                status = "Online",
                isFriend = true
            ),
            UserProfile(
                id = "jessica_id",
                username = "jessica_d",
                displayName = "Jessica Davis",
                bio = "Curator of visual dreams. Say hi!",
                photoUri = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                status = "Away",
                isFriend = false,
                isFriendRequestReceived = true
            ),
            UserProfile(
                id = "admin_bot_id",
                username = "moderator_bot",
                displayName = "Safety Moderation Bot",
                bio = "Automatically keeps Connect Chat clean and moderated. Rate limit safe.",
                photoUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                status = "Online",
                isFriend = true
            )
        )
        userProfileDao.insertProfiles(defaults)

        // Seed default chat groups or private conversations
        val chatSophia = ChatEntity(
            id = "sophia_id",
            type = "private",
            name = "Sophia Martinez",
            description = "Direct chat with Sophia Martinez",
            avatarUri = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            isPinned = true,
            unreadCount = 2,
            lastMessageText = "Are we syncing on the interactive call specs?",
            lastMessageTime = System.currentTimeMillis() - 3600000
        )
        val chatLiam = ChatEntity(
            id = "liam_id",
            type = "private",
            name = "Liam Miller",
            description = "Android engineering conversations",
            avatarUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            isPinned = false,
            unreadCount = 0,
            lastMessageText = "WebRTC peer connection established!",
            lastMessageTime = System.currentTimeMillis() - 18000000
        )
        val chatGroup = ChatEntity(
            id = "group_connect_design",
            type = "group",
            name = "Connect Core Team",
            description = "Interactive team chat for the Connect application deployment.",
            avatarUri = "",
            isPinned = false,
            unreadCount = 0,
            lastMessageText = "Welcome to the group chat",
            lastMessageTime = System.currentTimeMillis() - 86400000
        )
        chatDao.insertOrUpdateChat(chatSophia)
        chatDao.insertOrUpdateChat(chatLiam)
        chatDao.insertOrUpdateChat(chatGroup)

        // Seed some history logs
        val messagesSophia = listOf(
            MessageEntity(
                id = "msg_s_1",
                chatId = "sophia_id",
                senderId = "sophia_id",
                senderName = "Sophia Martinez",
                senderPhoto = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                content = "Hey! Really excited to launch the production-ready Connect Chat app.",
                type = "text",
                timestamp = System.currentTimeMillis() - 7200000
            ),
            MessageEntity(
                id = "msg_s_2",
                chatId = "sophia_id",
                senderId = "sophia_id",
                senderName = "Sophia Martinez",
                senderPhoto = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                content = "Are we syncing on the interactive call specs?",
                type = "text",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = false
            )
        )
        val messagesLiam = listOf(
            MessageEntity(
                id = "msg_l_1",
                chatId = "liam_id",
                senderId = "liam_id",
                senderName = "Liam Miller",
                senderPhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                content = "Just tested the low latency WebRTC stream locally. It looks incredible!",
                type = "text",
                timestamp = System.currentTimeMillis() - 20000000
            ),
            MessageEntity(
                id = "msg_l_2",
                chatId = "liam_id",
                senderId = "liam_id",
                senderName = "Liam Miller",
                senderPhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                content = "WebRTC peer connection established!",
                type = "text",
                timestamp = System.currentTimeMillis() - 18000000
            )
        )
        messagesSophia.forEach { messageDao.insertMessage(it) }
        messagesLiam.forEach { messageDao.insertMessage(it) }

        // Seed calls history
        val someCalls = listOf(
            CallEntity(
                id = "call_1",
                callerId = "sophia_id",
                callerName = "Sophia Martinez",
                callerPhoto = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                calleeId = "current",
                calleeName = "You",
                calleePhoto = "",
                type = "video",
                timestamp = System.currentTimeMillis() - 43200000,
                durationSec = 245,
                status = "Completed"
            ),
            CallEntity(
                id = "call_2",
                callerId = "current",
                callerName = "You",
                callerPhoto = "",
                calleeId = "liam_id",
                calleeName = "Liam Miller",
                calleePhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                type = "voice",
                timestamp = System.currentTimeMillis() - 172800000,
                durationSec = 0,
                status = "Missed"
            )
        )
        someCalls.forEach { callDao.insertCall(it) }

        // Seed simulation reports logs for moderator panel
        val report_1 = ReportEntity(
            id = "report_1",
            reporterId = "sophia_id",
            reporterName = "Sophia Martinez",
            reportedUserId = "malicious_user",
            reportedUserName = "SpamMaster99",
            reason = "Spamming aggressive promo links in general chat.",
            messageText = "Click here to buy tokens cheap! http://scam.ru",
            timestamp = System.currentTimeMillis() - 86400000,
            isResolved = false
        )
        reportDao.insertReport(report_1)
    }
}
