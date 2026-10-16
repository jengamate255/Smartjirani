package com.smartjirani.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartjirani.core.database.dao.CommentDao
import com.smartjirani.core.database.dao.ConversationDao
import com.smartjirani.core.database.dao.MessageDao
import com.smartjirani.core.database.dao.NeighborhoodDao
import com.smartjirani.core.database.dao.NotificationDao
import com.smartjirani.core.database.dao.PostDao
import com.smartjirani.core.database.dao.ProfileDao
import com.smartjirani.core.database.entity.CommentEntity
import com.smartjirani.core.database.entity.ConversationEntity
import com.smartjirani.core.database.entity.MessageEntity
import com.smartjirani.core.database.entity.NeighborhoodEntity
import com.smartjirani.core.database.entity.NotificationEntity
import com.smartjirani.core.database.entity.PostEntity
import com.smartjirani.core.database.entity.ProfileEntity

@Database(
    entities = [
        PostEntity::class,
        CommentEntity::class,
        ProfileEntity::class,
        NeighborhoodEntity::class,
        NotificationEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SmartJiraniDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    abstract fun commentDao(): CommentDao

    abstract fun profileDao(): ProfileDao

    abstract fun neighborhoodDao(): NeighborhoodDao

    abstract fun notificationDao(): NotificationDao

    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao
}
