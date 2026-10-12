package com.example.appmovil

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Resultado de una llamada a la API. */
sealed class Resultado {
    /** El servidor confirmó la operación. */
    data class Ok(val json: JSONObject) : Resultado()

    /** El servidor respondió, pero rechazó la operación (correo duplicado, RUT duplicado, etc.). */
    data class Rechazado(val codigo: String, val msg: String) : Resultado() {
        /** Rechazos que no se arreglan reintentando: hay que avisar al usuario. */
        val esDefinitivo: Boolean get() = codigo in DEFINITIVOS
    }

    /** No hubo respuesta válida (sin Internet, servidor caído, error 500, JSON roto). */
    object FalloRed : Resultado()

    companion object {
        val DEFINITIVOS = setOf("EMAIL_DUPLICADO", "RUT_DUPLICADO", "NO_EXISTE", "DATOS_INVALIDOS")
    }
}

object Api {
    const val BASE = "http://13.223.171.108/crud/"
    private var queue: RequestQueue? = null

    private fun queue(ctx: Context): RequestQueue =
        queue ?: Volley.newRequestQueue(ctx.applicationContext).also { queue = it }

    /**
     * Sin reintentos automáticos de Volley: un reintento silencioso podría enviar dos veces
     * el mismo registro. Los reintentos los controla el SyncWorker.
     */
    private fun politica() = DefaultRetryPolicy(12_000, 0, 1f)

    private fun interpretar(texto: String?): Resultado = try {
        val json = JSONObject(texto ?: "")
        if (json.optBoolean("ok")) Resultado.Ok(json)
        else Resultado.Rechazado(
            json.optString("codigo", "ERROR"),
            json.optString("msg", "El servidor rechazó la operación.")
        )
    } catch (e: Exception) {
        Resultado.FalloRed
    }

    /** POST asíncrono: la respuesta llega en el hilo principal. */
    fun post(ctx: Context, endpoint: String, params: Map<String, String>, alTerminar: (Resultado) -> Unit) {
        val req = object : StringRequest(
            Request.Method.POST, BASE + endpoint,
            { texto -> alTerminar(interpretar(texto)) },
            { alTerminar(Resultado.FalloRed) }
        ) {
            override fun getParams(): MutableMap<String, String> = params.toMutableMap()
        }
        req.setRetryPolicy(politica())
        queue(ctx).add(req)
    }

    /** GET asíncrono. */
    fun get(ctx: Context, endpoint: String, alTerminar: (Resultado) -> Unit) {
        val req = StringRequest(
            Request.Method.GET, BASE + endpoint,
            { texto -> alTerminar(interpretar(texto)) },
            { alTerminar(Resultado.FalloRed) }
        )
        req.setRetryPolicy(politica())
        queue(ctx).add(req)
    }

    /** Bloqueante: solo llamar desde un hilo secundario (el Worker). */
    fun postSync(ctx: Context, endpoint: String, params: Map<String, String>): Resultado {
        val future = RequestFuture.newFuture<String>()
        val req = object : StringRequest(Request.Method.POST, BASE + endpoint, future, future) {
            override fun getParams(): MutableMap<String, String> = params.toMutableMap()
        }
        req.setRetryPolicy(politica())
        queue(ctx).add(req)
        return try {
            interpretar(future.get(15, TimeUnit.SECONDS))
        } catch (e: Exception) {
            Resultado.FalloRed
        }
    }
}
