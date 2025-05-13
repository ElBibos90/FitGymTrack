package com.fitgymtrack.app.api

import com.fitgymtrack.app.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://192.168.1.113/api/" // Per emulatore che punta a localhost
    // oppure
    //private const val BASE_URL = "https://fitgymtrack.com/api/" // Per il server remoto

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var sessionManager: SessionManager

    // Inizializza il sessionManager
    fun initialize(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    // Interceptor personalizzato per aggiungere token di autorizzazione
    private val authInterceptor = Interceptor { chain ->
        // Se sessionManager non è stato inizializzato, procedi senza autenticazione
        if (!::sessionManager.isInitialized) {
            return@Interceptor chain.proceed(chain.request())
        }

        // Ottieni token di autenticazione usando runBlocking
        val token = runBlocking {
            sessionManager.getAuthToken().first()
        }

        // Se il token è null o vuoto, procedi senza autenticazione
        if (token.isNullOrEmpty()) {
            return@Interceptor chain.proceed(chain.request())
        }

        // Crea una nuova request con l'header Authorization
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        // Procedi con la request modificata
        chain.proceed(request)
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)  // Aggiungi l'interceptor di autenticazione
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}