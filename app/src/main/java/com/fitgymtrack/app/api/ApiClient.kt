package com.fitgymtrack.app.api

import com.fitgymtrack.app.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.Gson

object ApiClient {

    //private const val BASE_URL = "http://192.168.1.113/api/" // Per emulatore che punta a localhost
    // oppure
    private const val BASE_URL = "https://fitgymtrack.com/api/" // Per il server remoto

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
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // Log completo di richieste e risposte
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Converter Factory personalizzato per gestire meglio gli errori di parsing
    class SafeConverterFactory(private val gson: Gson) : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            val delegate = GsonConverterFactory.create(gson).responseBodyConverter(type, annotations, retrofit)
            return if (delegate != null) {
                Converter<ResponseBody, Any> { body ->
                    try {
                        delegate.convert(body)
                    } catch (e: JsonSyntaxException) {
                        // Log dell'errore
                        android.util.Log.e("ApiClient", "Errore parsing JSON: ${e.message}")

                        // In caso di errore, restituisce un valore di default in base al tipo
                        null
                    }
                }
            } else {
                null
            }
        }
    }

    private val retrofit by lazy {
        // Crea un Gson più permissivo
        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // Servizi API disponibili
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val workoutApiService: WorkoutApiService by lazy {
        retrofit.create(WorkoutApiService::class.java)
    }

    val activeWorkoutApiService: ActiveWorkoutApiService by lazy {
        retrofit.create(ActiveWorkoutApiService::class.java)
    }

    val userExerciseApiService: UserExerciseApiService by lazy {
        retrofit.create(UserExerciseApiService::class.java)
    }

    val exerciseApiService: ExerciseApiService by lazy {
        retrofit.create(ExerciseApiService::class.java)
    }

    val workoutHistoryApiService: WorkoutHistoryApiService by lazy {
        retrofit.create(WorkoutHistoryApiService::class.java)
    }

    // Aggiungiamo il servizio per i pagamenti
    val paymentApiService: PaymentApiService by lazy {
        retrofit.create(PaymentApiService::class.java)
    }

    // Aggiungiamo il servizio per gli abbonamenti
    val subscriptionApiService: SubscriptionApiService by lazy {
        retrofit.create(SubscriptionApiService::class.java)
    }

    // Aggiungiamo il servizio per le statistiche
    val statsApiService: StatsApiService by lazy {
        retrofit.create(StatsApiService::class.java)
    }

    // Aggiungiamo il servizio per il feedback
    val feedbackApiService: FeedbackApiService by lazy {
        retrofit.create(FeedbackApiService::class.java)
    }

}