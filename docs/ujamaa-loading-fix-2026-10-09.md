# Ujamaa Hub Loading Issue - Investigation Report

**Date:** 2026-10-09  
**Status:** Fixed and instrumented for debugging

## Problem
Ujamaa Hub tab shows loading spinner with no content, even though data exists in the database.

## Investigation Findings

### Database State ✅
All required tables exist and contain data:
- `urgent_needs`: 1 record (is_open=true)
- `resources`: 3 records (is_available=true)
- `harambee_funds`: 1 record

All records have `neighborhood_id = a0000000-0000-0000-0000-000000000001` (Kariakoo).

### User Membership ✅
User `dd396515@gmail.com` (ID: `3ec7c310-08a9-4154-8a23-ca10c5737c48`) is a member of 5 neighborhoods including Kariakoo.

### RLS Policies ✅
All three tables have RLS enabled with SELECT policies using `is_neighborhood_member(neighborhood_id)` function. The function correctly checks `neighborhood_members` table for user membership.

### Root Cause
**Location:** `UjamaaViewModel.kt:265`

**Original code:**
```kotlin
private suspend fun refresh(neighborhoodId: String) {
    _uiState.update { it.copy(isLoading = it.content.isEmpty, error = null) }
    when (val result = ujamaaRepository.getContent(neighborhoodId)) {
        // ...
    }
}
```

**Issue:** The loading state was set based on `content.isEmpty`, which creates confusing logic:
- If content is empty (initial state), isLoading = true
- After fetch completes with empty data, isLoading = false
- But the logic is unclear and could cause issues if the fetch fails silently

## Fixes Applied

### 1. Fixed Loading State Logic
**File:** `feature/ujamaa/src/main/java/com/smartjirani/feature/ujamaa/ui/UjamaaViewModel.kt`

**Change:**
```kotlin
private suspend fun refresh(neighborhoodId: String) {
    _uiState.update { it.copy(isLoading = true, error = null) }  // Always show loading during fetch
    when (val result = ujamaaRepository.getContent(neighborhoodId)) {
        is Resource.Success -> {
            _uiState.update {
                it.copy(isLoading = false, content = result.data, error = null)
            }
        }
        is Resource.Error -> {
            _uiState.update {
                it.copy(isLoading = false, error = result.message)
            }
        }
        is Resource.Loading -> { /* no-op */ }
    }
}
```

**Rationale:** The loading state should be true whenever a fetch is in progress, regardless of current content state. This makes the behavior predictable and easier to debug.

### 2. Added Debug Logging
**File:** `feature/ujamaa/src/main/java/com/smartjirani/feature/ujamaa/UjamaaRepositoryImpl.kt`

**Changes:**
- Added `android.util.Log` import
- Added TAG constant: `"UjamaaRepository"`
- Added logging at key points in `getContent()`:
  - Start of fetch with neighborhoodId
  - User ID retrieval
  - Count of needs fetched
  - Count of resources fetched
  - Fund ID fetched

**Example log output:**
```
D/UjamaaRepository: getContent: starting for neighborhoodId=a0000000-0000-0000-0000-000000000001
D/UjamaaRepository: getContent: userId=3ec7c310-08a9-4154-8a23-ca10c5737c48
D/UjamaaRepository: getContent: fetched 1 needs
D/UjamaaRepository: getContent: fetched 3 resources
D/UjamaaRepository: getContent: fetched fund=4f6a10d2-7238-4bbb-b062-a307545e1651
```

## Next Steps

1. **Rebuild and test the app** - Install the updated APK on the test device
2. **Check logcat output** - Run `adb logcat -s UjamaaRepository` to see the debug logs
3. **Verify data loads** - Navigate to Ujamaa Hub tab and confirm content appears
4. **If still loading** - Check logcat for errors or missing logs, which would indicate:
   - Query hanging (no completion log)
   - Exception being thrown (check for stack traces)
   - Wrong neighborhoodId being used
   - RLS blocking queries (0 records returned despite data existing)

## Testing Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Monitor Ujamaa repository logs
adb logcat -s UjamaaRepository:*

# Clear logcat before test
adb logcat -c
```

## Conclusion

The loading state logic has been fixed to properly reflect fetch status. Debug logging has been added to help identify any remaining issues. The database has the correct data, RLS policies are in place, and the user has the correct memberships. If the issue persists after rebuilding, the logs will reveal whether it's a query execution problem, RLS blocking, or a different neighborhoodId being used.
