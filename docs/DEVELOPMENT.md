# SmartJirani Development Guide

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Android Studio | Ladybug (2024.2.1)+ | IDE |
| JDK | 21 | Compilation |
| Kotlin | 2.1.0 | Language |
| Gradle | 8.11+ | Build system |
| Android SDK | 35 (target) | Platform |
| Android SDK | 26 (min) | Compatibility |
| Supabase CLI | Latest | Backend management |

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/SmartJirani.git
cd SmartJirani
```

### 2. Configure Local Properties

Create `local.properties`:

```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
supabase.url=https://<project-ref>.supabase.co
supabase.anon.key=<your-anon-key>
supabase.service.role.key=<service-role-key>
```

### 3. Configure Supabase

```bash
# Install Supabase CLI
npm install -g supabase

# Login
supabase login

# Link to project
supabase link --project-ref <your-project-ref>

# Run migrations
supabase db push

# Start local development (optional)
supabase start
```

### 4. Build the Project

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Run all checks
./gradlew check

# Run tests
./gradlew test
```

### 5. Run the App

- Connect Android device or start emulator
- Click Run in Android Studio or:
```bash
./gradlew installDebug
```

## Project Configuration

### Gradle Setup

```kotlin
// build.gradle.kts (project level)
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.53.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}
```

### Module-Level Build

```kotlin
// Example: feature/feed/build.gradle.kts
plugins {
    id("smartjirani.android.feature")
    id("smartjirani.kotlin.compose")
    id("smartjirani.hilt")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
}
```

### Version Catalog

Defined in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.1.0"
compose-bom = "2024.12.01"
hilt = "2.53.1"
room = "2.6.1"
ktor = "3.0.3"
supabase = "3.0.2"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

## Convention Plugins

SmartJirani uses convention plugins for consistent module configuration:

### Available Plugins

| Plugin | Purpose |
|--------|---------|
| `smartjirani.android.application` | App module config |
| `smartjirani.android.library` | Library module config |
| `smartjirani.android.feature` | Feature module config |
| `smartjirani.kotlin.compose` | Compose compiler config |
| `smartjirani.hilt` | Hilt DI setup |
| `smartjirani.room` | Room database setup |
| `smartjirani.ktor` | Ktor client setup |

### Creating a New Module

1. Create module directory: `feature/<name>/`
2. Create `build.gradle.kts`:
```kotlin
plugins {
    id("smartjirani.android.feature")
    id("smartjirani.kotlin.compose")
    id("smartjirani.hilt")
}

dependencies {
    implementation(project(":core:common"))
    // Add other core dependencies as needed
}
```
3. Create `src/main/AndroidManifest.xml`
4. Register in `settings.gradle.kts`
5. Add module dependency in `app/build.gradle.kts`

## Code Style

### Kotlin Style Guide

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `ktlint` with project configuration
- Run lint: `./gradlew ktlintCheck`
- Auto-fix: `./gradlew ktlintFormat`

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Classes | PascalCase | `UserRepository` |
| Functions | camelCase | `getUserProfile()` |
| Variables | camelCase | `isLoading` |
| Constants | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| Composables | PascalCase | `ProfileScreen()` |
| Files | PascalCase | `UserProfile.kt` |
| Packages | lowercase | `com.smartjirani.feature.feed` |
| Resources | snake_case | `ic_send.xml` |

### Compose Conventions

```kotlin
// Screen composable pattern
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

// Content composable (stateless)
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}
```

### ViewModel Pattern

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val updateProfile: UpdateProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.Refresh -> loadProfile()
            is ProfileAction.UpdateName -> updateName(action.name)
            is ProfileAction.UpdateAvatar -> updateAvatar(action.uri)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserProfile()
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                    _effect.emit(ProfileEffect.ShowSnackbar(error.message ?: "Unknown error"))
                }
        }
    }
}
```

## Testing

### Test Structure

```
src/
├── main/           # Production code
├── test/           # Unit tests (JVM)
└── androidTest/    # Instrumented tests (device)
```

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific module tests
./gradlew :feature:feed:test

# Instrumented tests (requires connected device)
./gradlew connectedAndroidTest

# Code coverage
./gradlew jacocoTestReport
```

### Test Categories

| Type | Framework | Location | Purpose |
|------|-----------|----------|---------|
| Unit | JUnit5 + MockK | `test/` | Business logic |
| Integration | Turbine + Room | `test/` | Data layer |
| UI | Compose Testing | `androidTest/` | Screen rendering |
| E2E | Espresso | `androidTest/` | User flows |

### Writing Tests

```kotlin
// Unit test example
class GetUserProfileUseCaseTest {
    @Test
    fun `invoke returns profile from repository`() = runTest {
        // Given
        val repository = mockk<UserRepository>()
        val useCase = GetUserProfileUseCase(repository)
        val expectedProfile = UserProfile(id = "1", name = "Test")

        every { repository.getUserProfile("1") } returns flowOf(Result.success(expectedProfile))

        // When
        val result = useCase("1").first()

        // Then
        assertEquals(Result.success(expectedProfile), result)
    }
}
```

## Debugging

### Common Issues

#### Build Failures

```bash
# Clean and rebuild
./gradlew clean assembleDebug

# Clear Gradle cache
rm -rf ~/.gradle/caches

# Invalidate caches in Android Studio
File → Invalidate Caches → Invalidate and Restart
```

#### Dependency Conflicts

```bash
# Check dependency tree
./gradlew :app:dependencies

# Resolve conflicts
./gradlew :app:dependencyInsight --configuration runtimeClasspath --dependency compose-bom
```

#### Room Compilation Errors

```bash
# Verify schema export
ls app/schemas/

# Check migration SQL
./gradlew :core:database:assemble
```

### Debug Tools

- **Layout Inspector**: Compose preview debugging
- **Database Inspector**: Room data inspection
- **Network Inspector**: API request monitoring
- **LeakCanary**: Memory leak detection (debug builds)
- **Chucker**: HTTP inspector (debug builds)

## Git Workflow

### Branch Strategy

```
main          ← Production-ready code
  └── develop ← Integration branch
       ├── feature/auth     ← Feature branches
       ├── feature/feed
       ├── fix/login-bug    ← Bug fix branches
       └── release/1.0.0    ← Release branches
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add voice call feature
fix: resolve login crash on API 26
docs: update architecture documentation
refactor: extract use case from ViewModel
test: add unit tests for UserRepository
chore: update dependencies
```

### Pull Request Process

1. Create feature branch from `develop`
2. Implement changes with tests
3. Run `./gradlew check` locally
4. Create PR with description
5. Request code review
6. Address review feedback
7. Merge to `develop` after approval

## Performance

### Build Optimization

- Enable Gradle build cache
- Use configuration cache
- Parallel module compilation
- Incremental compilation

### Runtime Optimization

- Lazy loading for off-screen content
- Image caching with Coil
- Database query optimization
- Background work with WorkManager
- Compose recomposition monitoring

## Release Process

### Build Variants

| Variant | Build Type | Purpose |
|---------|-----------|---------|
| debug | debug | Development |
| release | release | Production |
| staging | release | Pre-production testing |

### Release Checklist

1. Update version in `build.gradle.kts`
2. Update changelog
3. Run full test suite: `./gradlew check`
4. Build release: `./gradlew assembleRelease`
5. Test release build on device
6. Upload to Play Console (internal testing)
7. Promote through testing tracks
8. Production release

### Version Naming

```kotlin
// build.gradle.kts (app module)
android {
    defaultConfig {
        versionCode = 1          // Increment with each release
        versionName = "1.0.0"    // Semantic versioning
    }
}
```

## Documentation

### Generated Docs

- **KDoc**: API documentation
- **Room Schema**: Auto-exported to `schemas/`
- **Navigation Graph**: Auto-generated from code

### Manual Documentation

- `docs/` directory for architecture and guides
- `README.md` for project overview
- Inline code comments for complex logic
