package com.example.expm.network.examples

/**
 * ============================================================================
 * RETROFIT API INTEGRATION - USAGE EXAMPLES
 * ============================================================================
 *
 * This file contains examples of how to use the Retrofit API integration
 * in your application.
 *
 * Base URL: https://api.expensemanager.org
 *
 * ============================================================================
 * SETUP
 * ============================================================================
 *
 * 1. The Retrofit client is already configured in RetrofitClient.kt
 * 2. All API endpoints are defined in ApiService.kt
 * 3. API models are in network/models/ApiModels.kt
 * 4. Repository pattern is implemented in network/repository/ApiRepository.kt
 *
 * ============================================================================
 * USAGE IN VIEWMODEL
 * ============================================================================
 *
 * Example: Login
 *
 * class LoginViewModel : ViewModel() {
 *     private val apiService = RetrofitClient.getApiService()
 *     private val repository = ApiRepository(apiService)
 *
 *     private val _loginState = MutableLiveData<Resource<LoginResponse>>()
 *     val loginState: LiveData<Resource<LoginResponse>> = _loginState
 *
 *     fun login(email: String, password: String) {
 *         viewModelScope.launch {
 *             _loginState.value = Resource.Loading()
 *             val result = repository.login(email, password)
 *             _loginState.value = result
 *         }
 *     }
 * }
 *
 * ============================================================================
 * USAGE IN ACTIVITY
 * ============================================================================
 *
 * class LoginActivity : AppCompatActivity() {
 *     private lateinit var viewModel: LoginViewModel
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
 *
 *         viewModel.loginState.observe(this) { result ->
 *             when (result) {
 *                 is Resource.Loading -> {
 *                     // Show loading indicator
 *                     showLoading()
 *                 }
 *                 is Resource.Success -> {
 *                     hideLoading()
 *                     val loginData = result.data?.data
 *                     if (loginData != null) {
 *                         // Save token
 *                         saveToken(loginData.token)
 *                         // Navigate to main screen
 *                         navigateToMain()
 *                     }
 *                 }
 *                 is Resource.Error -> {
 *                     hideLoading()
 *                     // Show error message
 *                     Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
 *                 }
 *             }
 *         }
 *
 *         btnLogin.setOnClickListener {
 *             val email = etEmail.text.toString()
 *             val password = etPassword.text.toString()
 *             viewModel.login(email, password)
 *         }
 *     }
 * }
 *
 * ============================================================================
 * WORKING WITH AUTH TOKEN
 * ============================================================================
 *
 * For authenticated requests, you need to pass the token:
 *
 * // Get entries with authentication
 * viewModelScope.launch {
 *     val token = getTokenFromPreferences() // Your method to retrieve saved token
 *     val result = repository.getEntries(token)
 *     when (result) {
 *         is Resource.Success -> {
 *             val entries = result.data?.data?.entries
 *             // Update UI with entries
 *         }
 *         is Resource.Error -> {
 *             // Handle error
 *         }
 *         is Resource.Loading -> {
 *             // Show loading
 *         }
 *     }
 * }
 *
 * ============================================================================
 * SYNCING LOCAL ENTRIES TO SERVER
 * ============================================================================
 *
 * // Convert local entries to API format and sync
 * viewModelScope.launch {
 *     val localEntries = entryDao.getAllEntries() // Get from Room database
 *     val token = getTokenFromPreferences()
 *
 *     val entryRequests = localEntries.map { it.toEntryRequest() }
 *
 *     val result = repository.syncEntries(token, entryRequests)
 *     when (result) {
 *         is Resource.Success -> {
 *             val syncData = result.data?.data
 *             Log.d("Sync", "Synced: ${syncData?.synced}, Failed: ${syncData?.failed}")
 *
 *             // Update local entries to mark them as persisted
 *             syncData?.entries?.forEach { entryData ->
 *                 val localEntry = entryData.toEntry()
 *                 entryDao.update(localEntry)
 *             }
 *         }
 *         is Resource.Error -> {
 *             Log.e("Sync", "Error: ${result.message}")
 *         }
 *         is Resource.Loading -> {
 *             // Show loading indicator
 *         }
 *     }
 * }
 *
 * ============================================================================
 * CREATING A NEW ENTRY
 * ============================================================================
 *
 * viewModelScope.launch {
 *     val token = getTokenFromPreferences()
 *     val entry = EntryRequest(
 *         title = "Grocery Shopping",
 *         amount = 50.0,
 *         type = "expense",
 *         category = "Food",
 *         createdOn = System.currentTimeMillis(),
 *         updatedOn = System.currentTimeMillis(),
 *         notes = "Weekly groceries"
 *     )
 *
 *     val result = repository.createEntry(token, entry)
 *     when (result) {
 *         is Resource.Success -> {
 *             val createdEntry = result.data?.data
 *             // Save to local database if needed
 *             createdEntry?.let {
 *                 entryDao.insert(it.toEntry())
 *             }
 *         }
 *         is Resource.Error -> {
 *             // Handle error
 *         }
 *         is Resource.Loading -> {
 *             // Show loading
 *         }
 *     }
 * }
 *
 * ============================================================================
 * GETTING ANALYTICS
 * ============================================================================
 *
 * viewModelScope.launch {
 *     val token = getTokenFromPreferences()
 *     val startDate = getStartOfMonth() // Your method
 *     val endDate = System.currentTimeMillis()
 *
 *     val result = repository.getAnalyticsSummary(token, startDate, endDate)
 *     when (result) {
 *         is Resource.Success -> {
 *             val analytics = result.data?.data
 *             analytics?.let {
 *                 Log.d("Analytics", "Income: ${it.totalIncome}")
 *                 Log.d("Analytics", "Expense: ${it.totalExpense}")
 *                 Log.d("Analytics", "Balance: ${it.balance}")
 *                 // Update UI with analytics data
 *             }
 *         }
 *         is Resource.Error -> {
 *             // Handle error
 *         }
 *         is Resource.Loading -> {
 *             // Show loading
 *         }
 *     }
 * }
 *
 * ============================================================================
 * ALTERNATIVE: USING WITH AUTOMATIC TOKEN INJECTION
 * ============================================================================
 *
 * You can configure RetrofitClient to automatically inject the token:
 *
 * // In your Application class or ViewModel
 * val apiService = RetrofitClient.getApiService {
 *     // This lambda is called for each request
 *     getTokenFromPreferences() // Your method to retrieve token
 * }
 *
 * val repository = ApiRepository(apiService)
 *
 * // Now you can make calls without passing token explicitly
 * // (if the endpoint doesn't require Authorization header to be passed)
 *
 * ============================================================================
 * ERROR HANDLING
 * ============================================================================
 *
 * All API calls return Resource<T> which has three states:
 *
 * 1. Resource.Loading - Request in progress
 * 2. Resource.Success - Request succeeded, data available in result.data
 * 3. Resource.Error - Request failed, error message in result.message
 *
 * Always handle all three states in your UI to provide good user experience.
 *
 * ============================================================================
 */

