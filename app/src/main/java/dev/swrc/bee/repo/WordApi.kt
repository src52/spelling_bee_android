package dev.swrc.bee.repo

import dev.swrc.bee.network.WordApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object WordApi {
    private const val BASE_URL = "https://krinqq5p7h.execute-api.us-east-2.amazonaws.com/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val retrofitService: WordApiService by lazy {
        retrofit.create(WordApiService::class.java)
    }
}