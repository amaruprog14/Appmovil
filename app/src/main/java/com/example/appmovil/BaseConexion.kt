package com.example.appmovil

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Activity base: muestra un aviso (Snackbar) con el estado de conexión y sincronización:
 *  - sin conexión (modo local), con cuántos cambios hay pendientes
 *  - cambios pendientes mientras se sincroniza
 *  - registros rechazados por el servidor (conflictos) que el usuario debe revisar
 *  - sincronización completada
 */
open class BaseConexion : AppCompatActivity() {

    private var aviso: Snackbar? = null
    private var textoActual: String? = null

    private val escucha: (Int) -> Unit = { evento ->
        actualizarAviso(evento)
        alCambiarEstado(evento)
    }

    /** Las pantallas que muestran datos lo sobrescriben para refrescarse. */
    protected open fun alCambiarEstado(evento: Int) {}

    override fun onStart() {
        super.onStart()
        ConexionMonitor.iniciar(this)
        ConexionMonitor.agregar(escucha)
        actualizarAviso(ConexionMonitor.EVENTO_CONEXION)
    }

    override fun onStop() {
        ConexionMonitor.quitar(escucha)
        aviso?.dismiss()
        aviso = null
        textoActual = null
        super.onStop()
    }

    private fun actualizarAviso(evento: Int) {
        val db = ConexionDbHelper(this)
        val pendientes = db.contarPendientes()
        val conflictos = db.contarConflictos()
        val plural = if (pendientes == 1) "cambio pendiente" else "cambios pendientes"

        when {
            !ConexionMonitor.enLinea && pendientes > 0 ->
                mostrar("Sin conexión: modo local. $pendientes $plural de sincronizar",
                    Snackbar.LENGTH_INDEFINITE)

            !ConexionMonitor.enLinea ->
                mostrar("Sin conexión: trabajando en modo local", Snackbar.LENGTH_INDEFINITE)

            conflictos > 0 -> {
                val txt = if (conflictos == 1) "1 registro no se pudo sincronizar"
                else "$conflictos registros no se pudieron sincronizar"
                mostrar("$txt (dato duplicado). Revísalo en Listar usuarios",
                    Snackbar.LENGTH_INDEFINITE, conAccionRevisar = this is principal || this is usuarios)
            }

            pendientes > 0 ->
                mostrar("$pendientes $plural, sincronizando...", Snackbar.LENGTH_INDEFINITE)

            evento == ConexionMonitor.EVENTO_SINCRONIZADO ->
                mostrar("Sincronización completada", Snackbar.LENGTH_LONG)

            else -> {
                aviso?.dismiss()
                aviso = null
                textoActual = null
            }
        }
    }

    private fun mostrar(texto: String, duracion: Int, conAccionRevisar: Boolean = false) {
        // Evita parpadeos si el mensaje no cambió
        if (texto == textoActual && aviso?.isShownOrQueued == true) return
        aviso?.dismiss()
        textoActual = texto
        aviso = Snackbar.make(findViewById<View>(android.R.id.content), texto, duracion).also {
            if (conAccionRevisar) {
                it.setAction("Revisar") { startActivity(Intent(this, buscar::class.java)) }
            }
            it.show()
        }
    }
}
