package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import android.util.Patterns
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import android.net.Uri

private lateinit var etEmail: EditText
private lateinit var etClave: EditText
private lateinit var btningresar: Button
private lateinit var cRegistro: TextView
private lateinit var cOlvide: TextView

private lateinit var datosui: RequestQueue

class MainActivity : BaseConexion() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        etEmail = findViewById(R.id.cEmail)
        etClave = findViewById(R.id.cClave)
        btningresar = findViewById(R.id.cIngresar)
        cRegistro = findViewById(R.id.cRegistrar)
        cOlvide = findViewById(R.id.cOlvide)
        datosui= Volley.newRequestQueue(this);

        btningresar.setOnClickListener {
            if (validarCampos()) {
                btningresar.isEnabled = false // Bloquea el botón según la rúbrica
                consultarDatos(etEmail.text.toString().trim(), etClave.text.toString().trim())
            }
        }
        cRegistro.setOnClickListener {
            val intent = Intent(this, registro::class.java)
            startActivity(intent)
        }
        cOlvide.setOnClickListener {
            val intent = Intent(this, recuperar::class.java)
            startActivity(intent)
        }
        SyncWorker.programar(this)
    }
    private fun validarCampos(): Boolean {
        val email = etEmail.text.toString().trim()
        val clave = etClave.text.toString().trim()

        return when {
            email.isEmpty() -> {
                mostrarCampoVacio("Debes ingresar tu email.")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Formato incorrecto")
                    .setContentText("Debes ingresar un email válido (ej: usuario@dominio.com).")
                    .setConfirmText("Entendido")
                    .show()
                false
            }
            clave.isEmpty() -> {
                mostrarCampoVacio("Debes ingresar una contraseña.")
                false
            }
            else -> true
        }
    }
    fun consultarDatos(usu: String, pass: String) {
        val url = "http://13.223.171.108/apiconsultausu.php?usu=${Uri.encode(usu)}&pass=${Uri.encode(pass)}"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                btningresar.isEnabled = true
                try {
                    val estado = response.getString("estado")
                    when (estado) {
                        "1" -> mostrarLoginExitoso()
                        "2" -> SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Cuenta bloqueada")
                            .setContentText("Tu cuenta está en proceso de recuperación de contraseña.")
                            .setConfirmText("Cerrar")
                            .show()
                        else -> SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("Usuario o contraseña incorrectos")
                            .setConfirmText("Cerrar")
                            .setConfirmClickListener { dialog ->
                                dialog.dismissWithAnimation()
                            }
                            .show()
                    }
                } catch (e: JSONException) {
                    SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("Respuesta inesperada del servidor.")
                        .setConfirmText("Cerrar")
                        .show()
                }
            },
            { error ->
                btningresar.isEnabled = true
                error.printStackTrace()
                // Sin conexión con el servidor: se intenta con la base local
                if (ConexionDbHelper(this).validarLogin(usu, pass)) {
                    mostrarLoginExitoso()
                } else {
                    SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Sin conexión")
                        .setContentText("No se pudo conectar con el servidor y el usuario no existe en este dispositivo.")
                        .setConfirmText("Cerrar")
                        .setConfirmClickListener { dialog ->
                            dialog.dismissWithAnimation()
                        }
                        .show()
                }
            }
        )
        datosui.add(request)
    }
    private fun mostrarCampoVacio(mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Campo requerido")
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    private fun mostrarLoginExitoso() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Inicio exitoso!")
            .setContentText("Tu cuenta ha iniciado sesión correctamente.")
            .setConfirmText("Aceptar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this, principal::class.java)
                startActivity(intent)
            }
            .show()
    }
}