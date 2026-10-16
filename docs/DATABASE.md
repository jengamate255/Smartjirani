# SmartJirani Database

## Overview

SmartJirani uses a dual-database architecture:
- **Supabase PostgreSQL** — Remote database with Row Level Security (RLS)
- **Room (SQLite)** — Local database for offline-first caching

All data flows: Remote → Room Cache → UI. Writes flow: UI → Room → Remote Sync.

## Local Database (Room)

### Database Configuration

- **Database Class**: `SmartJiraniDatabase`
- **Version**: Starts at 1, incremented with each migration
- **Export Schema**: `true` (auto-exported to `schemas/` directory)
- **Type Converters**: For Date, UUID, enums, and complex types

### Core Entities

#### User
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val phoneNumber: String?,
    val neighborhoodId: String?,
    val isOnline: Boolean = false,
    val lastSeenAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Neighborhood
```kotlin
@Entity(tableName = "neighborhoods")
data class NeighborhoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val createdBy: String,
    val memberCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Post
```kotlin
@Entity(
    tableName = "posts",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["authorId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val neighborhoodId: String,
    val content: String,
    val imageUrl: String?,
    val category: PostCategory,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Comment
```kotlin
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(entity = PostEntity::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["authorId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val content: String,
    val parentCommentId: String? = null,
    val likeCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Call Log
```kotlin
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val callerId: String,
    val calleeId: String,
    val callType: CallType, // VOICE, VIDEO
    val status: CallStatus, // MISSED, DECLINED, COMPLETED
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Int?
)
```

#### Notification
```kotlin
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val referenceId: String?,
    val referenceType: String?,
    val isRead: Boolean = false,
    val createdAt: Long
)
```

### DAOs

Each entity has a corresponding DAO with Flow-based queries:

```kotlin
@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE neighborhoodId = :neighborhoodId ORDER BY createdAt DESC")
    fun observePosts(neighborhoodId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun observePost(postId: String): Flow<PostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: String)

    @Query("SELECT * FROM posts WHERE neighborhoodId = :neighborhoodId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaginated(neighborhoodId: String, limit: Int, offset: Int): List<PostEntity>
}
```

### Indexes

```sql
-- Posts
CREATE INDEX idx_posts_neighborhood ON posts(neighborhoodId);
CREATE INDEX idx_posts_author ON posts(authorId);
CREATE INDEX idx_posts_created ON posts(createdAt DESC);
CREATE INDEX idx_posts_category ON posts(category);

-- Comments
CREATE INDEX idx_comments_post ON comments(postId);
CREATE INDEX idx_comments_author ON comments(authorId);
CREATE INDEX idx_comments_parent ON comments(parentCommentId);

-- Call Logs
CREATE INDEX idx_calllogs_caller ON call_logs(callerId);
CREATE INDEX idx_calllogs_callee ON call_logs(calleeId);
CREATE INDEX idx_calllogs_started ON call_logs(startedAt DESC);

-- Notifications
CREATE INDEX idx_notifications_user ON notifications(userId);
CREATE INDEX idx_notifications_read ON notifications(isRead);
CREATE INDEX idx_notifications_created ON notifications(createdAt DESC);
```

## Migrations

### Migration Strategy

- Room auto-generates migration code via `@Database` annotation
- Schema files exported to `schemas/` for version control
- Test migrations with `MigrationTestHelper`
- Production migrations run in a transaction (atomic)

### Migration Example

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `call_logs` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `callerId` TEXT NOT NULL,
                `calleeId` TEXT NOT NULL,
                `callType` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `durationSeconds` INTEGER
            )
        """)
        db.execSQL("CREATE INDEX idx_calllogs_caller ON call_logs(callerId)")
        db.execSQL("CREATE INDEX idx_calllogs_callee ON call_logs(calleeId)")
    }
}
```

### Pre-Migration Validation

```kotlin
override fun onCreate(db: SupportSQLiteDatabase) {
    super.onCreate(db)
    // Validate schema integrity after migration
}
```

## Remote Database (Supabase PostgreSQL)

### Table Structure

Mirrors Room entities with Supabase-specific additions:

| Table | Purpose | RLS Policy |
|-------|---------|------------|
| `users` | User profiles | Read: authenticated; Update: own profile |
| `neighborhoods` | Neighborhood data | Read: members; Create: authenticated |
| `neighborhood_members` | Membership junction | Read: own memberships |
| `posts` | Community posts | Read: neighborhood members; CRUD: author |
| `comments` | Post comments | Read: neighborhood members; CRUD: author |
| `call_logs` | Call history | Read: participants only |
| `notifications` | Push notifications | Read: recipient only |
| `media` | Uploaded files | Read: neighborhood members |

### Row Level Security (RLS) Examples

```sql
-- Posts: only neighborhood members can read
CREATE POLICY "members_can_read_posts" ON posts
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM neighborhood_members
            WHERE neighborhood_members.neighborhood_id = posts.neighborhood_id
            AND neighborhood_members.user_id = auth.uid()
        )
    );

-- Posts: only author can update/delete
CREATE POLICY "author_can_modify_post" ON posts
    FOR ALL
    USING (author_id = auth.uid());

-- Comments: only neighborhood members can read
CREATE POLICY "members_can_read_comments" ON comments
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM posts
            JOIN neighborhood_members ON neighborhood_members.neighborhood_id = posts.neighborhood_id
            WHERE posts.id = comments.post_id
            AND neighborhood_members.user_id = auth.uid()
        )
    );
```

## Synchronization

### Sync Strategy

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Network    │────▶│  Room Cache  │────▶│     UI      │
│  (Supabase)  │◀────│   (SQLite)   │◀────│  (Compose)  │
└─────────────┘     └──────────────┘     └─────────────┘
```

1. **Pull**: On app launch or refresh, fetch latest from Supabase → upsert into Room
2. **Push**: On local write, update Room immediately → queue remote sync
3. **Realtime**: Supabase Realtime pushes updates → update Room → UI recomposes
4. **Conflict**: Last-write-wins using `updatedAt` timestamp

### Sync States

```kotlin
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val exception: Exception) : SyncState()
    object Synced : SyncState()
}
```

## DataStore Preferences

Used for non-relational key-value data:

| Key | Type | Purpose |
|-----|------|---------|
| `auth_token` | String (encrypted) | Supabase JWT |
| `refresh_token` | String (encrypted) | Token refresh |
| `user_id` | String | Current user ID |
| `selected_neighborhood_id` | String | Active neighborhood |
| `theme_mode` | Enum | Dark/Light/System |
| `notification_enabled` | Boolean | Push notification toggle |
| `last_sync_timestamp` | Long | Last full sync time |
| `onboarding_completed` | Boolean | First-run flag |

### Encrypted DataStore

Security-sensitive values stored via `EncryptedSharedPreferences`:
- Authentication tokens
- User credentials
- API keys (if any client-side)
