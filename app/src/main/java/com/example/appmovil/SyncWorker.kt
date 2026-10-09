package com.example.appmovil

import android.content.Context
import androidx.work.*

class SyncWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val db = ConexionDbHelper(applicationContext)
        var huboFallo = false

        for (p in db.pendientes()) {
            if (p.estado == 1) {
                val ok = Api.postSync(applicationContext, "registrar.php", mapOf(
                    "rut" to p.rut, "nombres" to p.nombres, "apellidos" to p.apellidos,
                    "email" to p.email, "clave" to p.clave
                ))
                if (ok) db.marcarSincronizado(p.rut) else huboFallo = true
            } else {
                val ok = Api.postSync(applicationContext, "eliminar.php", mapOf("rut" to p.rut))
                if (ok) db.borrarDefinitivo(p.rut) else huboFallo = true
            }
        }
        return if (huboFallo) Result.retry() else Result.success()
    }

    companion object {
        fun programar(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(ctx.applicationContext).enqueueUniqueWork(
                "sync_usuarios", ExistingWorkPolicy.APPEND_OR_REPLACE, req
            )
        }
    }
}