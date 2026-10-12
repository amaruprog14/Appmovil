package com.example.appmovil

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Envía a MySQL los cambios hechos sin conexión.
 * Reglas de integridad:
 *  - Un cambio se marca sincronizado solo después de que el servidor responde ok.
 *  - Si falla la red, el cambio queda pendiente y WorkManager reintenta (con espera creciente).
 *  - Las inserciones se envían con sync=1: si el servidor ya tenía exactamente ese registro
 *    (porque se perdió la respuesta anterior) responde ok en vez de duplicarlo.
 *  - Si el servidor lo rechaza para siempre (correo/RUT duplicado) la fila queda en conflicto,
 *    con el motivo, para que el usuario la corrija. No se borra.
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val db = ConexionDbHelper(applicationContext)
        var falloRed = false
        var huboTrabajo = false
        var conflictos = 0

        // Varias pasadas: un cambio hecho durante el envío genera un nuevo pendiente
        pasadas@ for (pasada in 1..3) {
            val lista = db.pendientes()
            if (lista.isEmpty()) break
            huboTrabajo = true

            for (p in lista) {
                when (val r = enviar(p)) {
                    is Resultado.Ok -> db.confirmarSincronizado(p)
                    is Resultado.Rechazado ->
                        if (r.esDefinitivo && p.estado != Estado.ELIMINAR) {
                            db.marcarConflicto(p, r.msg)
                            conflictos++
                        } else {
                            falloRed = true     // error transitorio del servidor: reintentar
                            break@pasadas
                        }
                    Resultado.FalloRed -> {
                        falloRed = true         // sin red: no tiene sentido seguir con el resto
                        break@pasadas
                    }
                }
                ConexionMonitor.notificarCambioLocal()
            }
        }

        when {
            conflictos > 0 -> ConexionMonitor.conflictosDetectados()
            huboTrabajo && !falloRed && db.contarPendientes() == 0 -> ConexionMonitor.sincronizacionCompletada()
        }
        return if (falloRed) Result.retry() else Result.success()
    }

    private fun enviar(p: Pendiente): Resultado = when (p.estado) {
        Estado.INSERTAR -> Api.postSync(applicationContext, "registrar.php", mapOf(
            "rut" to p.rut, "nombres" to p.nombres, "apellidos" to p.apellidos,
            "email" to p.email, "clave" to (p.clave ?: ""), "sync" to "1"
        ))
        Estado.MODIFICAR -> Api.postSync(applicationContext, "modificar.php", mapOf(
            "rut" to p.rut, "nombres" to p.nombres, "apellidos" to p.apellidos, "email" to p.email
        ))
        else -> Api.postSync(applicationContext, "eliminar.php", mapOf("rut" to p.rut))
    }

    companion object {
        fun programar(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            // APPEND: si ya hay un envío en curso, este corre después (nunca dos a la vez)
            WorkManager.getInstance(ctx.applicationContext).enqueueUniqueWork(
                "sync_usuarios", ExistingWorkPolicy.APPEND_OR_REPLACE, req
            )
        }
    }
}
