package com.nguyen.coroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.nguyen.coroutines.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigInteger
import java.util.*
import kotlin.system.measureTimeMillis

data class Post(val id: Int, val userId: Int, val title: String)
data class User(val id: Int, val name: String, val email: String)

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val BASE_URL = "https://jsonplaceholder.typicode.com/"
    }

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDoApiRequests.setOnClickListener {
            // withoutCoroutines()
            // withCoroutines()
            withMainSafety()
        }
        binding.btnStartComputation.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                binding.progress.visibility = View.VISIBLE
                val time = doExpensiveWork()
                binding.progress.visibility = View.INVISIBLE
                binding.text.text = time
            }
        }
    }

    // this is a suspend function, which needs to be called from a coroutine
    private suspend fun doExpensiveWork(): String {
        // it's a suspend function aka a coroutine, so there's no need to create a CoroutineScope
        // so just call withContext()
        return withContext(Dispatchers.Default) {
            val time = measureTimeMillis {
                BigInteger.probablePrime(2200, Random())
            }
            // return is implied with a lambda
            "Time taken (ms): $time"
        }
    }

    private fun withMainSafety() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val blogService = retrofit.create(BlogServiceWithCoroutines::class.java)

        // Main safety: the complexity of launching a coroutine in a background thread should be
        // handled by the Retrofit library. so we can safely make the network calls from the main thread
        CoroutineScope(Dispatchers.Main).launch {
            Log.i(TAG, "coroutine thread ${Thread.currentThread().name}")
            // error handling is done in one place instead of scattering all over as in the callback
            // hell scenario
            try {
                val post = blogService.getPost(1)
                val user = blogService.getUser(post.userId)
                val postsByUser = blogService.getPostsByUser(user.id)

                // no need to switch back to the main thread anymore
                binding.text.text = "User ${user.name} made ${postsByUser.size} posts"
            } catch (e: Exception) {
                Log.e(TAG, "Exception $e")
            }
        }
    }

    private fun withCoroutines() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val blogService = retrofit.create(BlogServiceWithCoroutines::class.java)

        // create a coroutine with an IO context, which is where to launch the coroutine
        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "coroutine thread ${Thread.currentThread().name}")
            // data is fetched directly; no more callback.
            // all of the following calls take place on the background thread
            val post = blogService.getPost(1)
            val user = blogService.getUser(post.userId)
            val postsByUser = blogService.getPostsByUser(user.id)

            // switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                binding.text.text = "User ${user.name} made ${postsByUser.size} posts"
            }
        }
    }

    private fun withoutCoroutines() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val blogService = retrofit.create(BlogServiceWithoutCoroutines::class.java)

        // callback hell: getPost -> getUser -> getPostsByUser
        blogService.getPost(1).enqueue(object: Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                val post = response.body()
                if (post == null) {
                    Log.e(TAG, "Did not receive valid response body")
                } else {
                    blogService.getUser(post.userId).enqueue(object: Callback<User> {
                        override fun onResponse(call: Call<User>, response: Response<User>) {
                            val user = response.body()
                            if (user == null) {
                                Log.e(TAG, "Did not receive valid response body")
                            } else {
                                blogService.getPostsByUser(user.id).enqueue(object: Callback<List<Post>> {
                                    override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                                        val postsByUser = response.body()
                                        if (postsByUser == null) {
                                            Log.e(TAG, "Did not receive valid response body")
                                        } else {
                                            Log.i(TAG, "Done with all network requests")
                                            binding.text.text = "User ${user.name} made ${postsByUser.size} posts"
                                        }
                                    }

                                    override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                                        Log.e(TAG, "onFailure $t")
                                    }
                                })
                            }
                        }

                        override fun onFailure(call: Call<User>, t: Throwable) {
                            Log.e(TAG, "onFailure $t")
                        }
                    })
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })
    }
}