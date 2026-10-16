# SmartJirani Architecture

## Overview

SmartJirani is a native Android application built with Kotlin and Jetpack Compose, following a modular Clean Architecture approach with MVVM presentation patterns. The app provides neighborhood management features including authentication, community feeds, messaging, voice/video calls, and real-time conferencing.

## Architecture Principles

- **Separation of Concerns**: Each module has a single responsibility
- **Dependency Inversion**: Higher-level modules depend on abstractions, not implementations
- **Single Source of Truth**: Room database as the local source of truth
- **Offline-First**: All data operations work offline with background synchronization
- **Unidirectional Data Flow**: UI → ViewModel → UseCase → Repository → DataSource

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Backend | Supabase (Auth, PostgreSQL, Storage, Realtime) |
| Networking | Ktor Client |
| Coroutines | Kotlin Coroutines + Flow |
| Language | Kotlin 2.1.0 |
| Build | Gradle (Kotlin DSL) |
| Target | Android 35, minSdk 26 |

## Project Structure

```
SmartJirani/
├── app/                          # Application module (entry point)
├── core/                         # Core modules (shared infrastructure)
│   ├── common/                   # Utilities, extensions, base classes
│   ├── network/                  # API clients, interceptors
│   ├── database/                 # Room database, DAOs, entities
│   ├── datastore/                # DataStore preferences
│   ├── designsystem/             # UI components, themes, tokens
│   ├── navigation/               # Navigation graph, routes, type-safe nav
│   ├── location/                 # Location services, permissions
│   ├── notifications/            # Push notifications, channels
│   ├── audio/                    # Audio playback, recording
│   ├── video/                    # Video processing, camera
│   ├── realtime/                 # WebSocket, Supabase Realtime
│   └── security/                 # Encryption, certificate pinning
└── feature/                      # Feature modules
    ├── auth/                     # Login, registration, password recovery
    ├── profile/                  # User profile management
    ├── neighborhood/             # Neighborhood creation, discovery
    ├── feed/                     # Community feed listing
    ├── posts/                    # Post creation, editing, viewing
    ├── comments/                 # Comment threads
    ├── voice/                    # Voice messaging, calls
    ├── calls/                    # 1-to-1 audio/video calls
    ├── conference/               # Multi-party conferencing
    └── notifications/            # In-app notification center
```

## Layer Architecture

### Presentation Layer (Feature Modules)

Each feature module follows MVVM:

```
screen/          # Composable UI screens
viewmodel/       # ViewModels with StateFlow
navigation/      # Module-specific routes and navigation
```

**Key Patterns:**
- UI state is represented as sealed classes/interfaces
- ViewModels expose single UI state via `StateFlow`
- Side effects use `SharedFlow` for one-shot events (navigation, snackbar)
- Screen composables receive only UI state and event lambdas

### Domain Layer (Shared across features)

```
usecase/         # Business logic, orchestration
model/           # Domain models (plain Kotlin data classes)
repository/      # Repository interfaces (contracts)
```

**Key Patterns:**
- Use cases are single-responsibility classes
- Repository interfaces defined in feature or shared modules
- Domain models are platform-agnostic
- Use cases return `Result<T>` or `Flow<Result<T>>`

### Data Layer (Core modules)

```
repository/      # Repository implementations
datasource/      # Remote (API) and local (Room, DataStore) sources
mapper/          # DTO ↔ Domain model mappers
```

**Key Patterns:**
- Local-first: read from cache, fetch from network, update cache
- Mappers isolate data layer from domain layer
- Repository implementations handle offline/online strategy

## Dependency Rules

```
app → feature/* → core/* → (no dependencies on feature modules)
feature/* → core/* (allowed)
feature/* → feature/* (NOT allowed — use navigation contracts)
```

**Dependency Flow:**
```
┌─────────────────────────────────────────────┐
│                   app                       │
│         (wires all modules together)        │
└──────────────────┬──────────────────────────┘
                   │
    ┌──────────────┼──────────────┐
    ▼              ▼              ▼
┌────────┐  ┌──────────┐  ┌──────────┐
│ feature │  │ feature  │  │ feature  │
│  auth   │  │  feed    │  │  calls   │  ...
└────┬───┘  └────┬─────┘  └────┬─────┘
     │           │              │
     ▼           ▼              ▼
┌─────────────────────────────────────────────┐
│              core modules                    │
│  common │ network │ database │ datastore    │
│  designsystem │ navigation │ location       │
│  notifications │ audio │ video │ realtime   │
│  security                                 │
└─────────────────────────────────────────────┘
```

## Backend Integration (Supabase)

### Authentication
- Supabase Auth with email/password and social providers
- JWT tokens stored securely via EncryptedSharedPreferences
- Automatic token refresh via Ktor interceptor

### Database
- Supabase PostgreSQL with Row Level Security (RLS)
- Real-time subscriptions via Supabase Realtime
- Local Room cache for offline access

### Storage
- Supabase Storage for user-uploaded media (avatars, post images, voice notes)
- Resumable uploads for large files
- CDN-backed delivery

### Realtime
- Supabase Realtime for live updates (new posts, comments, call signaling)
- WebSocket connection managed by core:realtime module
- Automatic reconnection with exponential backoff

## State Management

### UI State
```kotlin
data class FeedUiState(
    val posts: List<PostUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)
```

### Side Effects
```kotlin
sealed class FeedEffect {
    data class ShowSnackbar(val message: String) : FeedEffect()
    data class NavigateToPost(val postId: String) : FeedEffect()
    object NavigateBack : FeedEffect()
}
```

## Offline Strategy

1. **Cache-First**: UI reads from Room immediately
2. **Background Sync**: Coroutine workers fetch fresh data
3. **Optimistic Updates**: Local changes reflected immediately, synced later
4. **Conflict Resolution**: Last-write-wins with timestamp comparison
5. **Retry Logic**: Exponential backoff for failed network calls

## Testing Strategy

| Layer | Framework | Coverage Target |
|-------|-----------|-----------------|
| Unit | JUnit5 + MockK | Use cases, repositories |
| Integration | Turbine + Room in-memory | Data layer |
| UI | Compose Testing | Screen composables |
| E2E | Espresso / Compose Test | Critical user flows |
