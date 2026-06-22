package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfilesFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getProfileById(userId: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE isFriend = 1")
    fun getFriendsFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE isFriendRequestReceived = 1")
    fun getFriendRequestsFlow(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<UserProfile>)

    @Query("UPDATE user_profiles SET status = :status, lastSeen = :lastSeen WHERE id = :userId")
    suspend fun updateStatus(userId: String, status: String, lastSeen: Long = System.currentTimeMillis())

    @Query("UPDATE user_profiles SET isBlocked = :isBlocked WHERE id = :userId")
    suspend fun updateBlocked(userId: String, isBlocked: Boolean)

    @Query("UPDATE user_profiles SET isFriend = :isFriend, isFriendRequestSent = 0, isFriendRequestReceived = 0 WHERE id = :userId")
    suspend fun updateFriendship(userId: String, isFriend: Boolean)

    @Query("UPDATE user_profiles SET isFriendRequestSent = :sent WHERE id = :userId")
    suspend fun updateRequestSent(userId: String, sent: Boolean)

    @Query("DELETE FROM user_profiles WHERE id = :userId")
    suspend fun deleteProfile(userId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChat(chat: ChatEntity)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinned(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchived(chatId: String, isArchived: Boolean)

    @Query("UPDATE chats SET isTypingUserId = :typingUserId WHERE id = :chatId")
    suspend fun updateTyping(chatId: String, typingUserId: String?)

    @Query("UPDATE chats SET lastMessageText = :text, lastMessageTime = :time WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, text: String, time: Long)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMessages(query: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isEdited = 1, content = :newContent WHERE id = :messageId")
    suspend fun editMessage(messageId: String, newContent: String)

    @Query("UPDATE messages SET IsRead = 1 WHERE chatId = :chatId AND senderId != :currentUserId")
    suspend fun markChatAsRead(chatId: String, currentUserId: String)

    @Query("UPDATE messages SET reactionCsv = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactions: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)
}

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCallsFlow(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Query("UPDATE calls SET durationSec = :duration, status = :status WHERE id = :callId")
    suspend fun updateCallEnd(callId: String, duration: Int, status: String)

    @Query("DELETE FROM calls WHERE id = :callId")
    suspend fun deleteCall(callId: String)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET isResolved = :isResolved WHERE id = :reportId")
    suspend fun updateReportState(reportId: String, isResolved: Boolean)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReport(reportId: String)
}
