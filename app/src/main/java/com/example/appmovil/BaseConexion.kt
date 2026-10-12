package com.example.appmovil

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Activity base: muestra un aviso (Snackbar) con el estado de conexión y sincronización:
 *  - sin conexión (modo local), con cuántos cambios hay pendientes
 *  - cambios pendientes mientras se sincroniza
 *  - sincronización completada
 */
open class BaseConexion : AppCompatActivity() {

    private var aviso: Snackbar? = null
    private var textoActual: String? = null

    private val escucha: (Int) -> Unit = { evento -> actualizarAviso(evento) }

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
        val pendientes = ConexionDbHelper(this).pendientes().size
        val plural = if (pendientes == 1) "cambio pendiente" else "cambios pendientes"

        when {
            evento == ConexionMonitor.EVENTO_SINCRONIZADO && pendientes == 0 ->
                mostrar("Sincronización completada", Snackbar.LENGTH_LONG)

            !ConexionMonitor.enLinea && pendientes > 0 ->
                mostrar("Sin conexión: modo local. $pendientes $plural de sincronizar",
                    Snackbar.LENGTH_INDEFINITE)

            !ConexionMonitor.enLinea ->
                mostrar("Sin conexión: trabajando en modo local", Snackbar.LENGTH_INDEFINITE)

            pendientes > 0 ->
                mostrar("$pendientes $plural, sincronizando...", Snackbar.LENGTH_INDEFINITE)

            else -> {
                aviso?.dismiss()
                aviso = null
                textoActual = null
            }
        }
    }

    private fun mostrar(texto: String, duracion: Int) {
        // Evita parpadeos si el mensaje no cambió
        if (texto == textoActual && aviso?.isShownOrQueued == true) return
        aviso?.dismiss()
        textoActual = texto
        aviso = Snackbar.make(findViewById<View>(android.R.id.content), texto, duracion).also { it.show() }
    }
}