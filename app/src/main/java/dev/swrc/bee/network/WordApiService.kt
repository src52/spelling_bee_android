package dev.swrc.bee.network

import retrofit2.http.Body
import retrofit2.http.POST

data class WordRequest(
    val word: String
)

data class WordResponse(
    val valid: Boolean
)

interface WordApiService {
    @POST("default")
    fun checkWord(@Body request: WordRequest): WordResponse
}