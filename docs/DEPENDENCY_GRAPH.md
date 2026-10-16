# SmartJirani Module Dependency Graph

## Overview

SmartJirani follows a strict modular Clean Architecture. Dependencies flow in one direction: **app → feature → core**. Feature modules cannot depend on other feature modules.

## Module Overview

### Application Module

| Module | Purpose |
|--------|---------|
| `app` | Entry point, wires all modules, DI configuration |

### Core Modules (12)

| Module | Purpose | Key Dependencies |
|--------|---------|-----------------|
| `core:common` | Utilities, extensions, base classes | Kotlin stdlib |
| `core:network` | API clients, interceptors | Ktor, serialization |
| `core:database` | Room database, DAOs, entities | Room, Hilt |
| `core:datastore` | DataStore preferences | DataStore, Hilt |
| `core:designsystem` | UI components, themes | Compose, Material 3 |
| `core:navigation` | Navigation graph, routes | Navigation Compose |
| `core:location` | Location services | Play Services Location |
| `core:notifications` | Push notifications | Firebase Messaging |
| `core:audio` | Audio playback/recording | ExoPlayer, MediaRecorder |
| `core:video` | Video processing, camera | CameraX, MediaCodec |
| `core:realtime` | WebSocket, Supabase Realtime | Ktor WebSocket |
| `core:security` | Encryption, cert pinning | Tink, TLS |

### Feature Modules (10)

| Module | Purpose | Core Dependencies |
|--------|---------|-------------------|
| `feature:auth` | Login, registration | common, network, database, datastore, designsystem, navigation, security |
| `feature:profile` | User profile management | common, network, database, datastore, designsystem, navigation |
| `feature:neighborhood` | Neighborhood CRUD | common, network, database, datastore, designsystem, navigation, location |
| `feature:feed` | Community feed | common, network, database, designsystem, navigation |
| `feature:posts` | Post creation/viewing | common, network, database, designsystem, navigation |
| `feature:comments` | Comment threads | common, network, database, designsystem, navigation |
| `feature:voice` | Voice messaging, calls | common, network, database, designsystem, navigation, audio, realtime |
| `feature:calls` | 1-to-1 calls | common, network, database, designsystem, navigation, audio, video, realtime |
| `feature:conference` | Multi-party conferencing | common, network, database, designsystem, navigation, audio, video, realtime |
| `feature:notifications` | Notification center | common, network, database, designsystem, navigation, notifications |

## Dependency Graph

### High-Level View

```
                              ┌──────────────────────────────────┐
                              │               app                 │
                              └───────────────┬──────────────────┘
                                              │
            ┌─────────────────────────────────┼─────────────────────────────────┐
            │                                 │                                 │
            ▼                                 ▼                                 ▼
    ┌───────────────┐               ┌───────────────┐               ┌───────────────┐
    │ feature:auth  │               │feature:profile│               │feature:neighborhood
    └───────┬───────┘               └───────┬───────┘               └───────┬───────┘
            │                               │                               │
            ▼                               ▼                               ▼
    ┌───────────────────────────────────────────────────────────────────────────────┐
    │                            feature:feed                                      │
    └───────────────────────────────────────────────────────────────────────────────┘
                                              │
            ┌─────────────────────────────────┼─────────────────────────────────┐
            │                                 │                                 │
            ▼                                 ▼                                 ▼
    ┌───────────────┐               ┌───────────────┐               ┌───────────────┐
    │feature:posts  │               │feature:comments│              │ feature:voice │
    └───────┬───────┘               └───────┬───────┘               └───────┬───────┘
            │                               │                               │
            ▼                               ▼                               ▼
    ┌───────────────┐               ┌───────────────┐               ┌───────────────┐
    │feature:calls  │               │feature:conference│             │feature:notif  │
    └───────┬───────┘               └───────┬───────┘               └───────┬───────┘
            │                               │                               │
            └───────────────────────────────┼───────────────────────────────┘
                                            │
                                            ▼
                              ┌──────────────────────────────────┐
                              │          core modules             │
                              └──────────────────────────────────┘
```

### Detailed Dependencies

#### app Module

```
app
├── feature:auth
├── feature:profile
├── feature:neighborhood
├── feature:feed
├── feature:posts
├── feature:comments
├── feature:voice
├── feature:calls
├── feature:conference
├── feature:notifications
├── core:common
├── core:network
├── core:database
├── core:datastore
├── core:designsystem
├── core:navigation
├── core:location
├── core:notifications
├── core:audio
├── core:video
├── core:realtime
└── core:security
```

#### feature:auth

```
feature:auth
├── core:common
├── core:network
├── core:database
├── core:datastore
├── core:designsystem
├── core:navigation
└── core:security
```

#### feature:profile

```
feature:profile
├── core:common
├── core:network
├── core:database
├── core:datastore
├── core:designsystem
└── core:navigation
```

#### feature:neighborhood

```
feature:neighborhood
├── core:common
├── core:network
├── core:database
├── core:datastore
├── core:designsystem
├── core:navigation
└── core:location
```

#### feature:feed

```
feature:feed
├── core:common
├── core:network
├── core:database
├── core:designsystem
└── core:navigation
```

#### feature:posts

```
feature:posts
├── core:common
├── core:network
├── core:database
├── core:designsystem
└── core:navigation
```

#### feature:comments

```
feature:comments
├── core:common
├── core:network
├── core:database
├── core:designsystem
└── core:navigation
```

#### feature:voice

```
feature:voice
├── core:common
├── core:network
├── core:database
├── core:designsystem
├── core:navigation
├── core:audio
└── core:realtime
```

#### feature:calls

```
feature:calls
├── core:common
├── core:network
├── core:database
├── core:designsystem
├── core:navigation
├── core:audio
├── core:video
└── core:realtime
```

#### feature:conference

```
feature:conference
├── core:common
├── core:network
├── core:database
├── core:designsystem
├── core:navigation
├── core:audio
├── core:video
└── core:realtime
```

#### feature:notifications

```
feature:notifications
├── core:common
├── core:network
├── core:database
├── core:designsystem
├── core:navigation
└── core:notifications
```

### Core Module Dependencies

```
core:common          (no dependencies — leaf module)
    │
    ▼
core:network         → core:common
    │
    ▼
core:database        → core:common
    │
    ▼
core:datastore       → core:common
    │
    ▼
core:designsystem    → core:common
    │
    ▼
core:navigation      → core:common, core:designsystem
    │
    ▼
core:location        → core:common
    │
    ▼
core:notifications   → core:common
    │
    ▼
core:audio           → core:common
    │
    ▼
core:video           → core:common, core:audio
    │
    ▼
core:realtime        → core:common, core:network
    │
    ▼
core:security        → core:common
```

## Dependency Rules

### Allowed Dependencies

1. **Feature → Core**: Feature modules can depend on any core module
2. **Core → Core**: Core modules can depend on lower-level core modules
3. **App → All**: App module depends on all feature and core modules

### Forbidden Dependencies

1. **Feature → Feature**: Feature modules CANNOT depend on other feature modules
2. **Core → Feature**: Core modules CANNOT depend on feature modules
3. **Circular Dependencies**: No module can have circular dependencies

### Enforcing Dependencies

Dependencies are enforced via Gradle convention plugins:

```kotlin
// Convention plugin: smartjirani.android.feature
plugins {
    id("smartjirani.android.library")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
}
```

## Module Communication

### Navigation Contracts

Feature modules communicate via navigation routes, not direct dependencies:

```kotlin
// core:navigation module
object Routes {
    const val FEED = "feed"
    const val POST_DETAIL = "post/{postId}"
    const val PROFILE = "profile/{userId}"
    const val CALL = "call/{callId}"
}

// feature:posts - defines route
@Composable
fun PostDetailScreen(postId: String) { ... }

// feature:feed - navigates to post detail
navController.navigate("post/$postId")
```

### Shared Contracts

For cross-module communication, use interfaces in core modules:

```kotlin
// core:common module
interface SessionManager {
    fun getCurrentUserId(): String?
    fun isAuthenticated(): Boolean
    fun logout()
}

// feature:auth implements
class AuthSessionManager @Inject constructor(...) : SessionManager { ... }

// feature:profile uses interface
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager
) { ... }
```

## Build Impact Analysis

### When core:common Changes

| Module Type | Impact | Rebuild Required |
|-------------|--------|------------------|
| Core modules | High | All dependent cores |
| Feature modules | High | All features |
| App module | High | Full rebuild |

### When feature:feed Changes

| Module Type | Impact | Rebuild Required |
|-------------|--------|------------------|
| Core modules | None | No |
| Other features | None | No |
| App module | Medium | App only |

### When core:network Changes

| Module Type | Impact | Rebuild Required |
|-------------|--------|------------------|
| Core modules | Low | Dependent cores only |
| Feature modules | Medium | Features using network |
| App module | Medium | App + affected features |

## Performance Considerations

### Incremental Builds

- Core module changes trigger recompilation of dependent modules
- Feature module changes are isolated to that feature only
- App module changes only affect the final APK

### Parallel Compilation

- Independent modules compile in parallel
- Core modules with no interdependencies compile simultaneously
- Feature modules compile in parallel after core modules

### Dependency Caching

- Gradle build cache stores compilation outputs
- Configuration cache speeds up build configuration
- Remote build cache (optional) for CI/CD
