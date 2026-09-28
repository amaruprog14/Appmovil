package com.example.appmovil

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.widget.Switch
import android.widget.Toast


class sensores : AppCompatActivity() {

    private lateinit var txtFecha: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtHumedad: TextView
    private lateinit var imgTemp: ImageView
    private lateinit var queue: RequestQueue

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var switchLuz: Switch

    private lateinit var cameraManager: CameraManager

    private var cameraId: String? = null

    companion object {
        private const val URL = "https://pnk.cl/muestra_datos.php"
        private const val REFRESH_MS = 5000L   // API

        private const val CLOCK_MS = 1000L     // reloj
        private const val LIMITE_TEMP = 25f
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

        txtFecha = findViewById(R.id.textView13)   // Fecha
        txtTemp = findViewById(R.id.textView15)    // valor de temperatura
        txtHumedad = findViewById(R.id.textView17) // valor de humedad
        imgTemp = findViewById(R.id.imageView20)   // termómetro

        queue = Volley.newRequestQueue(this)
        switchLuz = findViewById(R.id.switch1)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

// Busca la cámara que tiene flash
        cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        if (cameraId == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            switchLuz.isEnabled = false
            Toast.makeText(this, "Este dispositivo no tiene linterna", Toast.LENGTH_SHORT).show()
        }

        switchLuz.setOnCheckedChangeListener { _, encendido ->
            cambiarLinterna(encendido)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(actualizarReloj)
        handler.post(refrescar)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(actualizarReloj)
        handler.removeCallbacks(refrescar)
        switchLuz.isChecked = false
    }

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

    private fun cambiarLinterna(encendida: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, encendida)
        } catch (e: CameraAccessException) {
            Log.e("sensores", "No se pudo cambiar la linterna", e)
            Toast.makeText(this, "No se pudo controlar la linterna", Toast.LENGTH_SHORT).show()
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
                } catch (e: JSONException) {
                    Log.e("sensores", "JSON inválido", e)
                } catch (e: NumberFormatException) {
                    Log.e("sensores", "Valor no numérico", e)
                }
            },
            { error -> Log.e("sensores", "Error de red: ${error.message}") }
        )
        queue.add(request)
    }

    private fun cambiarImagen(temperatura: Float) {

        if (temperatura > LIMITE_TEMP) {
            imgTemp.setImageResource(R.drawable.hot)
        } else {
            imgTemp.setImageResource(R.drawable.cold)
        }
    }
}