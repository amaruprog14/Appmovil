package com.example.appmovil

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper

/**
 * Vigila el acceso real a Internet (Wi-Fi o datos móviles) y avisa a las pantallas.
 * Al recuperar la conexión programa la sincronización de los cambios pendientes.
 */
object ConexionMonitor {

    const val EVENTO_CONEXION = 0       // cambió la conexión o los pendientes
    const val EVENTO_SINCRONIZADO = 1   // el Worker confirmó la sincronización

    @Volatile
    var enLinea: Boolean = true
        private set

    private val main = Handler(Looper.getMainLooper())
    private val escuchas = mutableListOf<(Int) -> Unit>()   // solo se toca desde el hilo principal
    private var iniciado = false

    fun iniciar(ctx: Context) {
        if (iniciado) return
        iniciado = true
        val app = ctx.applicationContext
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        enLinea = tieneInternet(cm.getNetworkCapabilities(cm.activeNetwork))

        // Red por defecto: cubre Wi-Fi y datos móviles, y avisa cuando una reemplaza a la otra
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                cambio(app, tieneInternet(caps))
            }

            override fun onLost(network: Network) {
                cambio(app, false)
            }
        })
    }

    private fun tieneInternet(caps: NetworkCapabilities?): Boolean =
        caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    private fun cambio(app: Context, nuevo: Boolean) {
        if (nuevo == enLinea) return
        enLinea = nuevo
        if (nuevo) SyncWorker.programar(app)   // recuperó Internet: subir lo pendiente
        main.post { avisar(EVENTO_CONEXION) }
    }

    /** Lo llama el Worker (hilo secundario) cuando el servidor confirmó todo. */
    fun sincronizacionCompletada() {
        main.post { avisar(EVENTO_SINCRONIZADO) }
    }

    fun agregar(escucha: (Int) -> Unit) {
        if (!escuchas.contains(escucha)) escuchas.add(escucha)
    }

    fun quitar(escucha: (Int) -> Unit) {
        escuchas.remove(escucha)
    }

    private fun avisar(evento: Int) {
        escuchas.toList().forEach { it(evento) }
    }
}