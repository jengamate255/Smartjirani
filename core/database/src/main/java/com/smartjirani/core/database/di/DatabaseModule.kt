package com.smartjirani.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartjirani.core.database.SmartJiraniDatabase
import com.smartjirani.core.database.dao.CommentDao
import com.smartjirani.core.database.dao.ConversationDao
import com.smartjirani.core.database.dao.MessageDao
import com.smartjirani.core.database.dao.NeighborhoodDao
import com.smartjirani.core.database.dao.NotificationDao
import com.smartjirani.core.database.dao.PostDao
import com.smartjirani.core.database.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT,
                        `isGroup` INTEGER NOT NULL DEFAULT 0,
                        `createdBy` TEXT NOT NULL,
                        `lastMessageAt` INTEGER NOT NULL,
                        `lastMessageContent` TEXT,
                        `lastMessageSenderId` TEXT,
                        `unreadCount` INTEGER NOT NULL DEFAULT 0,
                        `participantIds` TEXT NOT NULL DEFAULT '',
                        `participantNames` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `messages` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `conversationId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL,
                        `messageType` TEXT NOT NULL DEFAULT 'TEXT',
                        `mediaUrl` TEXT,
                        `replyToId` TEXT,
                        `isEdited` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
            }
        }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SmartJiraniDatabase =
        Room
            .databaseBuilder(
                context,
                SmartJiraniDatabase::class.java,
                "smartjirani.db",
            ).addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun providePostDao(db: SmartJiraniDatabase): PostDao = db.postDao()

    @Provides fun provideCommentDao(db: SmartJiraniDatabase): CommentDao = db.commentDao()

    @Provides fun provideProfileDao(db: SmartJiraniDatabase): ProfileDao = db.profileDao()

    @Provides fun provideNeighborhoodDao(db: SmartJiraniDatabase): NeighborhoodDao = db.neighborhoodDao()

    @Provides fun provideNotificationDao(db: SmartJiraniDatabase): NotificationDao = db.notificationDao()

    @Provides fun provideConversationDao(db: SmartJiraniDatabase): ConversationDao = db.conversationDao()

    @Provides fun provideMessageDao(db: SmartJiraniDatabase): MessageDao = db.messageDao()
}
