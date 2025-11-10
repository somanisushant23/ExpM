# Retrofit API Integration

## Overview

This project has been integrated with Retrofit to communicate with the Expense Manager API.

**Base URL:** `https://api.expensemanager.org`

## Project Structure

```
app/src/main/java/com/example/expm/network/
├── ApiService.kt                    # API endpoint definitions
├── RetrofitClient.kt                # Retrofit singleton configuration
├── Resource.kt                      # Generic wrapper for API responses
├── models/
│   └── ApiModels.kt                 # Request/Response data models
├── repository/
│   └── ApiRepository.kt             # Repository pattern for API calls
├── utils/
│   └── ModelMappers.kt              # Extension functions for model conversion
└── examples/
    └── ApiUsageExamples.kt          # Detailed usage examples
```

## Dependencies

The following dependencies have been added to `app/build.gradle.kts`:

```kotlin
// Retrofit for API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

## Features

### 1. **API Endpoints**

The following endpoints are implemented:

#### Authentication
- `POST /auth/login` - User login
- `POST /auth/register` - User registration
- `POST /auth/logout` - User logout
- `GET /auth/profile` - Get user profile
- `PUT /auth/profile` - Update user profile

#### Entries
- `GET /entries` - Get all entries (paginated)
- `GET /entries/{id}` - Get specific entry
- `POST /entries` - Create new entry
- `PUT /entries/{id}` - Update entry
- `DELETE /entries/{id}` - Delete entry
- `POST /entries/sync` - Sync multiple entries

#### Categories
- `GET /categories` - Get all categories

#### Analytics
- `GET /analytics/summary` - Get analytics summary

### 2. **Retrofit Client Configuration**

The `RetrofitClient` includes:
- **Logging Interceptor**: Logs all HTTP requests and responses (for debugging)
- **Auth Interceptor**: Automatically adds Bearer token to requests
- **Timeout Configuration**: 30 seconds for connect/read/write operations
- **GSON Converter**: JSON serialization/deserialization

### 3. **Resource Wrapper**

All API responses are wrapped in a `Resource<T>` sealed class with three states:
- `Resource.Loading` - Request in progress
- `Resource.Success` - Request succeeded with data
- `Resource.Error` - Request failed with error message

### 4. **Repository Pattern**

`ApiRepository` provides a clean interface for making API calls with automatic error handling.

### 5. **Model Mappers**

Extension functions to convert between local Room database models and API models:
- `Entry.toEntryRequest()` - Convert local Entry to API request
- `EntryData.toEntry()` - Convert API response to local Entry

## Quick Start

### Step 1: Sync Gradle

After integration, sync your Gradle files to download the new dependencies:
- In Android Studio: File → Sync Project with Gradle Files
- Or run: `./gradlew build`

### Step 2: Basic Usage in ViewModel

```kotlin
class MyViewModel : ViewModel() {
    private val apiService = RetrofitClient.getApiService()
    private val repository = ApiRepository(apiService)
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            when (result) {
                is Resource.Loading -> { /* Show loading */ }
                is Resource.Success -> { 
                    val token = result.data?.data?.token
                    // Save token and navigate
                }
                is Resource.Error -> { 
                    // Show error message
                }
            }
        }
    }
}
```

### Step 3: Using with Authentication Token

For authenticated requests, pass the token:

```kotlin
viewModelScope.launch {
    val token = getTokenFromPreferences() // Your method
    val result = repository.getEntries(token)
    when (result) {
        is Resource.Success -> {
            val entries = result.data?.data?.entries
            // Update UI
        }
        is Resource.Error -> {
            // Handle error
        }
        is Resource.Loading -> {
            // Show loading
        }
    }
}
```

### Step 4: Syncing Local Data

Convert and sync local Room database entries:

```kotlin
viewModelScope.launch {
    val localEntries = entryDao.getAllEntries()
    val token = getTokenFromPreferences()
    
    val entryRequests = localEntries.map { it.toEntryRequest() }
    val result = repository.syncEntries(token, entryRequests)
    
    when (result) {
        is Resource.Success -> {
            // Mark entries as persisted in local database
            result.data?.data?.entries?.forEach { entryData ->
                entryDao.update(entryData.toEntry())
            }
        }
        is Resource.Error -> {
            // Handle sync error
        }
        is Resource.Loading -> {
            // Show loading
        }
    }
}
```

## Configuration Options

### Changing Base URL

Edit `RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "https://your-api-url.com/"
```

### Disabling Logging in Production

In `RetrofitClient.kt`, modify the logging interceptor:

```kotlin
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}
```

### Automatic Token Injection

Configure RetrofitClient with a token provider:

```kotlin
val apiService = RetrofitClient.getApiService {
    // This lambda is called for each request
    getTokenFromPreferences()
}
```

## Error Handling

All API calls are wrapped in try-catch blocks and return appropriate error messages:

- Network errors (no internet, timeout)
- HTTP errors (4xx, 5xx status codes)
- Parsing errors (invalid JSON)

Errors are propagated through the `Resource.Error` state with descriptive messages.

## Testing

### Testing API Calls

You can create mock instances for testing:

```kotlin
class MockApiService : ApiService {
    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return Response.success(
            LoginResponse(
                success = true,
                data = LoginData(
                    token = "mock_token",
                    user = UserProfile(1, "Test User", "test@example.com")
                )
            )
        )
    }
    // ... implement other methods
}
```

## Additional Resources

- See `ApiUsageExamples.kt` for detailed usage examples
- See `ApiModels.kt` for all request/response data structures
- See `ModelMappers.kt` for data conversion utilities

## Permissions

The following permission is already added in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Next Steps

1. **Sync Gradle** to download dependencies
2. **Implement ViewModels** for your screens that need API calls
3. **Add token storage** using SharedPreferences or DataStore
4. **Implement UI observers** to react to API response states
5. **Add offline-first strategy** by syncing local Room data with the server

## Support

For questions or issues with the API integration, refer to:
- API Service interface: `ApiService.kt`
- Usage examples: `ApiUsageExamples.kt`
- Repository implementation: `ApiRepository.kt`

