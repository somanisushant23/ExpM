package com.example.expm.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client singleton for API calls
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.expensemanager.org/"
    //private const val BASE_URL = "http://192.168.1.12:8080/"
    private const val TIMEOUT_SECONDS = 60L

    // Logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {

        level = HttpLoggingInterceptor.Level.BODY
    }

    // Auth interceptor to add authorization header
    private class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val originalRequest = chain.request()
            val token = tokenProvider()

            val newRequest = if (token != null && !originalRequest.header("Authorization").isNullOrEmpty().not()) {
                originalRequest.newBuilder()
                    .header("Authorization", "$token")
                    .build()
            } else {
                originalRequest
            }

            return chain.proceed(newRequest)
        }
    }

    // OkHttpClient with interceptors
    private fun createOkHttpClient(tokenProvider: () -> String?): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(tokenProvider))
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit instance
    private fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Get API service instance
     * @param tokenProvider Lambda to provide the current auth token
     */
    fun getApiService(tokenProvider: () -> String? = { null }): ApiService {
        val okHttpClient = createOkHttpClient(tokenProvider)
        val retrofit = createRetrofit(okHttpClient)
        return retrofit.create(ApiService::class.java)
    }
}

