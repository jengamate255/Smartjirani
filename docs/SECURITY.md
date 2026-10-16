# SmartJirani Security

## Overview

SmartJirani handles sensitive user data including authentication credentials, location data, voice/video communications, and personal messages. This document outlines the security practices implemented across the application.

## Authentication & Authorization

### Supabase Auth Integration

- **Providers**: Email/password, Google Sign-In, phone number (OTP)
- **Token Management**: JWT access tokens with automatic refresh
- **Session Persistence**: Tokens stored in EncryptedSharedPreferences
- **Token Refresh**: Transparent refresh via Ktor interceptor before API calls
- **Logout**: Full token revocation and local data clearing

### Token Storage Flow

```
Login → Supabase returns JWT + Refresh Token
         ↓
Store in EncryptedSharedPreferences (AES-256)
         ↓
Ktor Interceptor attaches Authorization header
         ↓
On 401 → Refresh token exchange → Update stored tokens
```

### Role-Based Access (RLS)

Supabase Row Level Security enforces:
- Users can only read/write their own profile
- Neighborhood content accessible only to members
- Call logs visible only to call participants
- Notifications visible only to recipient

## Data Encryption

### At Rest

| Data | Storage | Encryption |
|------|---------|-----------|
| Auth tokens | EncryptedSharedPreferences | AES-256-GCM |
| User passwords | Supabase (never stored locally) | bcrypt (server-side) |
| Media files | Device storage | Android file-based encryption |
| Database | Room (SQLite) | SQLCipher (if enabled) |
| Preferences | DataStore | Platform encryption |

### In Transit

- All network communication over TLS 1.2+
- Certificate pinning via `core:security` module
- Ktor TLS engine configuration:

```kotlin
install(HttpsRedirect) {
    sslPort = 443
}
install(TLSConfig) {
    trustManager = certificatePinningTrustManager
}
```

### Certificate Pinning

```kotlin
// core:security module
object CertificatePinning {
    // SHA-256 pins for Supabase endpoints
    val SUPABASE_PINS = listOf(
        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    )

    fun createTrustManager(): X509TrustManager { ... }
}
```

## Network Security

### Ktor Client Security Configuration

```kotlin
install(ContentNegotiation) { ... }
install(HttpTimeout) {
    requestTimeoutMillis = 30_000
    connectTimeoutMillis = 15_000
}
// Automatic token injection
install(Auth) {
    bearer {
        loadTokens {
            BearerTokens(
                accessToken = tokenStore.getAccessToken(),
                refreshToken = tokenStore.getRefreshToken()
            )
        }
        refreshTokens {
            val newTokens = supabaseClient.refreshSession()
            tokenStore.saveTokens(newTokens)
            newTokens.toBearerTokens()
        }
    }
}
```

### API Security

- **Input Validation**: All user inputs validated before API submission
- **SQL Injection**: Room parameterized queries prevent injection
- **XSS Prevention**: Content sanitization for user-generated text
- **Rate Limiting**: Client-side throttling for API calls
- **Request Signing**: Supabase handles API key validation

## Location Data Security

### Permission Model

```
┌─────────────────────────────────┐
│     Location Permission Flow     │
├─────────────────────────────────┤
│ 1. Request fine location        │
│ 2. User grants/denies           │
│ 3. Check permission status      │
│ 4. If denied → explain purpose  │
│ 5. If granted → proceed         │
│ 6. Background location optional │
└─────────────────────────────────┘
```

### Data Handling

- Location data used only for neighborhood discovery
- Coordinates rounded to 3 decimal places (~111m precision) before storage
- Location not shared with other users without explicit consent
- Background location only used when actively searching for neighborhoods
- Location data purged from local cache after 30 days

## Voice & Video Security

### Call Security

- **Signaling**: Supabase Realtime channel (encrypted WebSocket)
- **Media**: WebRTC with DTLS encryption
- **ICE Servers**: TURN servers for NAT traversal (encrypted)
- **No Recording**: Voice/video calls are not recorded by the app
- **Ephemeral**: Call data not persisted beyond call logs

### Conference Security

- Room IDs are cryptographically random (128-bit)
- Join requires authentication and neighborhood membership
- Host can eject participants
- End-to-end encryption via WebRTC DTLS

## Sensitive Data Handling

### Data Classification

| Classification | Examples | Handling |
|---------------|----------|----------|
| **Critical** | Passwords, auth tokens | Encrypted storage, never logged |
| **Sensitive** | Location, phone number | Encrypted storage, limited access |
| **Internal** | User ID, neighborhood ID | Standard storage, authenticated access |
| **Public** | Display name, avatar | Standard storage |

### Logging Rules

- **NEVER** log: passwords, tokens, full phone numbers, exact coordinates
- **NEVER** log: API keys, certificates, private keys
- **SAFE to log**: user ID (non-PII), neighborhood ID, timestamps, anonymized error messages

### Data Retention

| Data Type | Retention | Deletion |
|-----------|-----------|----------|
| Auth tokens | Until logout | Immediate on logout |
| User profiles | Until account deletion | Cascade delete |
| Posts | Indefinite (user can delete) | Soft delete, then purge |
| Call logs | 90 days | Automatic purge |
| Notifications | 30 days | Automatic purge |
| Media files | Until user deletes | User-initiated |
| Location cache | 30 days | Automatic purge |

## App Security Features

### Biometric Authentication

```kotlin
// Optional biometric lock for sensitive actions
BiometricPrompt authenticate(
    title = "Verify Identity",
    subtitle = "Authenticate to access sensitive data",
    negativeButtonText = "Cancel"
)
```

### Screen Security

- Sensitive screens use `FLAG_SECURE` to prevent screenshots
- Screen recording detection (API 33+)
- Task description cleared from recent apps for sensitive screens

### Root/Jailbreak Detection

```kotlin
object SecurityUtils {
    fun isDeviceCompromised(): Boolean {
        return checkRootBinaries() ||
               checkSuExists() ||
               checkTestKeys() ||
               checkDangerousProps()
    }
}
```

### ProGuard/R8 Obfuscation

- Release builds use R8 with full obfuscation
- Sensitive class names obfuscated
- String encryption for API keys and configuration
- Resource shrinking enabled

## Privacy

### Data Collection Transparency

- Clear privacy policy accessible from app settings
- Minimal data collection principle
- No third-party analytics without consent
- No advertising SDKs
- No data selling to third parties

### User Rights

- Users can export their data
- Users can delete their account and all associated data
- Users can revoke location access at any time
- Users can opt out of push notifications

## Security Auditing

### Automated Checks

- Dependency vulnerability scanning (Dependabot / OWASP)
- Static analysis with Detekt
- Lint security checks enabled
- R8/ProGuard verification in release builds

### Manual Reviews

- Security review for each major release
- Penetration testing for authentication flows
- Code review required for security-sensitive changes
- Incident response plan documented

## Incident Response

### Vulnerability Reporting

1. Report via GitHub Security Advisory or email
2. Acknowledge within 48 hours
3. Assess severity (Critical/High/Medium/Low)
4. Patch critical issues within 7 days
5. Release hotfix for critical vulnerabilities
6. Notify affected users if data was at risk

### Breach Response

1. Contain the breach (revoke compromised tokens)
2. Assess scope and impact
3. Notify affected users within 72 hours
4. Report to relevant authorities if required
5. Implement preventive measures
6. Post-incident review
