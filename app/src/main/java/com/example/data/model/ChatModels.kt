package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val photoUri: String,
    val status: String, // "Online", "Offline", "Away"
    val lastSeen: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val isFriend: Boolean = false,
    val isFriendRequestSent: Boolean = false,
    val isFriendRequestReceived: Boolean = false,
    val isMuted: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val type: String, // "private", "group"
    val name: String,
    val description: String = "",
    val avatarUri: String = "",
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isTypingUserId: String? = null,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderPhoto: String,
    val content: String,
    val type: String, // "text", "image", "video", "document", "voice"
    val mediaUri: String? = null,
    val mediaName: String? = null,
    val mediaSize: String? = null,
    val durationSec: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean = true,
    val isRead: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val isEdited: Boolean = false,
    val reactionCsv: String = "" // e.g. "👍,❤️,😀,😮,😢,🙏" with count or simply list
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val id: String,
    val callerId: String,
    val callerName: String,
    val callerPhoto: String,
    val calleeId: String,
    val calleeName: String,
    val calleePhoto: String,
    val type: String, // "voice", "video"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSec: Int = 0,
    val status: String // "Missed", "Completed", "Active", "Declined"
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val reporterName: String,
    val reportedUserId: String,
    val reportedUserName: String,
    val reason: String,
    val messageText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)
