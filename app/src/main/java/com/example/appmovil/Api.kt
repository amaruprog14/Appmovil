package com.example.appmovil

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Api {
    const val BASE = "http://13.223.171.108/crud/"
    private var queue: RequestQueue? = null

    private fun queue(ctx: Context): RequestQueue =
        queue ?: Volley.newRequestQueue(ctx.applicationContext).also { queue = it }

    /** Bloqueante: solo llamar desde un hilo secundario (el Worker). */
    fun postSync(ctx: Context, endpoint: String, params: Map<String, String>): Boolean {
        val future = RequestFuture.newFuture<String>()
        val req = object : StringRequest(Request.Method.POST, BASE + endpoint, future, future) {
            override fun getParams(): MutableMap<String, String> = params.toMutableMap()
        }
        queue(ctx).add(req)
        return try {
            JSONObject(future.get(15, TimeUnit.SECONDS)).optBoolean("ok")
        } catch (e: Exception) {
            false
        }
    }
}