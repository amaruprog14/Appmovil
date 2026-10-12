package com.example.appmovil

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class sensores : BaseConexion() {

    private lateinit var txtFecha: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtHumedad: TextView
    private lateinit var txtEstadoApi: TextView
    private lateinit var imgTemp: ImageView
    private lateinit var imgLinterna: ImageView
    private lateinit var imgAmpolleta: ImageView
    private lateinit var queue: RequestQueue
    private lateinit var cameraManager: CameraManager

    private val handler = Handler(Looper.getMainLooper())

    private var cameraId: String? = null
    private var linternaEncendida = false     // refleja el estado REAL de la linterna
    private var ampolletaEncendida = false    // solo visual
    private var falloAvisado = false          // para avisar una sola vez por racha de errores

    companion object {
        private const val URL = "https://pnk.cl/muestra_datos.php"
        private const val TAG_REQ = "sensores"
        private const val REFRESH_MS = 5000L   // API
        private const val CLOCK_MS = 1000L     // reloj
        private const val LIMITE_TEMP = 25f
        private const val KEY_AMPOLLETA = "ampolleta"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensores)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtFecha = findViewById(R.id.textView13)
        txtTemp = findViewById(R.id.textView15)
        txtHumedad = findViewById(R.id.textView17)
        txtEstadoApi = findViewById(R.id.tvEstadoApi)
        imgTemp = findViewById(R.id.imageView20)
        imgLinterna = findViewById(R.id.imgLinterna)
        imgAmpolleta = findViewById(R.id.imgAmpolleta)

        queue = Volley.newRequestQueue(this)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        // ---- Ampolleta (solo visual) ----
        ampolletaEncendida = savedInstanceState?.getBoolean(KEY_AMPOLLETA) ?: false
        actualizarAmpolleta()
        imgAmpolleta.setOnClickListener {
            ampolletaEncendida = !ampolletaEncendida
            actualizarAmpolleta()
        }

        // ---- Linterna real ----
        cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            Log.e("sensores", "No se pudo listar las cámaras", e)
            null
        }

        imgLinterna.setImageResource(R.drawable.linterna_apagada)
        if (cameraId == null) imgLinterna.alpha = 0.4f

        imgLinterna.setOnClickListener { alternarLinterna() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AMPOLLETA, ampolletaEncendida)
    }

    override fun onResume() {
        super.onResume()
        handler.post(actualizarReloj)
        handler.post(refrescar)
        if (cameraId != null) cameraManager.registerTorchCallback(torchCallback, handler)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(actualizarReloj)
        handler.removeCallbacks(refrescar)
        queue.cancelAll(TAG_REQ)

        // Al salir de la pantalla se apaga la linterna
        val id = cameraId
        if (id != null) {
            if (linternaEncendida) {
                try {
                    cameraManager.setTorchMode(id, false)
                } catch (e: Exception) {
                    Log.e("sensores", "No se pudo apagar la linterna", e)
                }
            }
            cameraManager.unregisterTorchCallback(torchCallback)
        }
    }

    // ---------------- Linterna ----------------

    /** La imagen solo cambia cuando el sistema confirma el estado real de la linterna. */
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            linternaEncendida = enabled
            imgLinterna.alpha = 1f
            imgLinterna.setImageResource(
                if (enabled) R.drawable.linterna_encendida else R.drawable.linterna_apagada
            )
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id != cameraId) return
            imgLinterna.alpha = 0.4f   // otra app está usando la cámara
        }
    }

    private fun alternarLinterna() {
        val id = cameraId
        if (id == null) {
            Toast.makeText(this, "Este dispositivo no tiene linterna", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            cameraManager.setTorchMode(id, !linternaEncendida)
        } catch (e: CameraAccessException) {
            Log.e("sensores", "No se pudo cambiar la linterna", e)
            Toast.makeText(this, "No se pudo controlar la linterna", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e("sensores", "Linterna no disponible", e)
            Toast.makeText(this, "La linterna no está disponible ahora", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Ampolleta ----------------

    private fun actualizarAmpolleta() {
        imgAmpolleta.setImageResource(
            if (ampolletaEncendida) R.drawable.prendida else R.drawable.apagada
        )
    }

    // ---------------- Reloj y API ----------------

    private val refrescar = object : Runnable {
        override fun run() {
            obtenerDatos()
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    private val actualizarReloj = object : Runnable {
        override fun run() {
            txtFecha.text = "Fecha: " + fechaHora()
            handler.postDelayed(this, CLOCK_MS)
        }
    }

    private fun fechaHora(): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    private fun obtenerDatos() {
        val request = JsonObjectRequest(
            Request.Method.GET, URL, null,
            { response ->
                try {
                    val temperatura = response.getString("temperatura").toFloat()
                    val humedad = response.getString("humedad").toFloat()

                    txtTemp.text = "${temperatura.toInt()}°"
                    txtHumedad.text = "${humedad.toInt()}%"
                    cambiarImagen(temperatura)
                    ocultarError()
                } catch (e: Exception) {
                    // JSON incompleto, valores no numéricos, etc.
                    Log.e("sensores", "Datos inválidos", e)
                    mostrarError("Los datos del sensor llegaron en un formato inválido.")
                }
            },
            { error ->
                Log.e("sensores", "Error de red: ${error.message}")
                mostrarError(mensajeDe(error))
            }
        )
        request.tag = TAG_REQ
        queue.add(request)
    }

    private fun mensajeDe(error: VolleyError): String {
        val codigo = error.networkResponse?.statusCode
        return when {
            error is NoConnectionError || error is TimeoutError ->
                "No hay conexión con el servidor de sensores. Reintentando..."
            codigo != null ->
                "El servidor de sensores respondió con error ($codigo). Reintentando..."
            else ->
                "No se pudo leer la respuesta del servidor de sensores. Reintentando..."
        }
    }

    private fun mostrarError(mensaje: String) {
        txtEstadoApi.text = mensaje
        txtEstadoApi.visibility = View.VISIBLE
        if (!falloAvisado) {
            falloAvisado = true
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun ocultarError() {
        txtEstadoApi.visibility = View.INVISIBLE
        falloAvisado = false
    }

    private fun cambiarImagen(temperatura: Float) {
        if (temperatura > LIMITE_TEMP) {
            imgTemp.setImageResource(R.drawable.hot)
        } else {
            imgTemp.setImageResource(R.drawable.cold)
        }
    }
}