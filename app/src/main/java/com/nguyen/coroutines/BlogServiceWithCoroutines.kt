package com.nguyen.coroutines

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface BlogServiceWithCoroutines {
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") postId: Int): Post

    @GET("users/{id}")
    suspend fun getUser(@Path("user") userId: Int): User

    @GET("users/{id}/posts")
    suspend fun getPostsByUser(@Path("user") userId: Int): List<Post>
}