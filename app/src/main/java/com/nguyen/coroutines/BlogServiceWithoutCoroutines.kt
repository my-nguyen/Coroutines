package com.nguyen.coroutines

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface BlogServiceWithoutCoroutines {
    @GET("posts/{id}")
    fun getPost(@Path("id") postId: Int): Call<Post>

    @GET("users/{id}")
    fun getUser(@Path("user") userId: Int): Call<User>

    @GET("users/{id}/posts")
    fun getPostsByUser(@Path("user") userId: Int): Call<List<Post>>
}