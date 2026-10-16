## SmartJirani - Smart Neighborhood Community Platform

# SmartJirani

A native Android neighborhood/community platform designed for Tanzania and African markets.

## Features (Phase 1)

- Authentication (Email/Password)
- User Profiles
- Neighborhoods & Membership
- Home Feed
- Posts (General, Announcement, Alert, Lost/Found, Buy/Sell, Service, Event)
- Comments & Likes
- Image Uploads
- Voice Messages
- Voice Calls Architecture
- Video Conference Architecture
- Notifications Foundation
- Offline Caching (Room)
- Supabase Backend

## Architecture

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Clean Architecture** with modular structure
- **Hilt** dependency injection
- **Room** for offline caching
- **Supabase** for backend (Auth, Database, Storage, Realtime)
- **Coroutines** + **Flow** for reactive programming

## Project Structure

```
app/                          # Main application module
core/
  common/                     # Shared utilities, Result wrapper
  network/                    # Supabase client
  database/                   # Room database, DAOs, entities
  datastore/                  # User preferences
  designsystem/               # Theme, colors, components
  navigation/                 # Screen definitions
  location/                   # Location services
  notifications/              # Notification infrastructure
  audio/                      # Audio recording & playback
  video/                      # Call & conference services
  realtime/                   # Supabase realtime events
  security/                   # Encrypted storage
feature/
  auth/                       # Authentication
  profile/                    # User profiles
  neighborhood/               # Neighborhood management
  feed/                       # Home feed
  posts/                      # Posts CRUD
  comments/                   # Comments CRUD
  voice/                      # Voice messages
  calls/                      # Voice/video calls
  conference/                 # Video conferencing
  notifications/              # Notifications
```

## Setup

1. Clone the repository
2. Copy `.env.example` to configure Supabase credentials
3. Add to `local.properties`:
   ```
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   ```
4. Open in Android Studio
5. Sync Gradle and build

## Building

```bash
./gradlew assembleDebug
```

## Testing

```bash
./gradlew test
./gradlew lint
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Database](docs/DATABASE.md)
- [Security](docs/SECURITY.md)
- [Communications](docs/COMMUNICATIONS.md)
- [Development](docs/DEVELOPMENT.md)
