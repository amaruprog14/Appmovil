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

private lateinit var etEmail: EditText
private lateinit var etClave: EditText
private lateinit var btningresar: Button
private lateinit var cRegistro: TextView
private lateinit var cOlvide: TextView

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
    /**
     * Login contra MySQL (login.php compara con password_verify).
     * Se envía por POST para que la clave no quede en la URL ni en los logs del servidor.
     * Si no hay conexión, se valida contra el hash guardado en el teléfono.
     */
    fun consultarDatos(usu: String, pass: String) {
        val db = ConexionDbHelper(this)

        if (!ConexionMonitor.enLinea) {
            btningresar.isEnabled = true
            loginSinConexion(usu, pass)
            return
        }

        Api.post(this, "login.php", mapOf("email" to usu, "clave" to pass)) { r ->
            btningresar.isEnabled = true
            when (r) {
                is Resultado.Ok -> when (r.json.optString("estado")) {
                    "1" -> {
                        db.guardarCredencial(usu, pass)   // permite entrar después sin conexión
                        Sesion.guardar(this, usu)
                        mostrarLoginExitoso()
                    }
                    "2" -> {
                        db.borrarCredencial(usu)          // bloqueada: tampoco entra sin conexión
                        SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Cuenta bloqueada")
                            .setContentText("Tu cuenta está en proceso de recuperación de contraseña.")
                            .setConfirmText("Cerrar")
                            .show()
                    }
                    else -> credencialesIncorrectas()
                }
                is Resultado.Rechazado -> credencialesIncorrectas()
                Resultado.FalloRed -> loginSinConexion(usu, pass)
            }
        }
    }

    private fun loginSinConexion(usu: String, pass: String) {
        if (ConexionDbHelper(this).validarLogin(usu, pass)) {
            Sesion.guardar(this, usu)
            mostrarLoginExitoso()
        } else {
            SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Sin conexión")
                .setContentText("No se pudo conectar con el servidor y no hay credenciales válidas guardadas en este dispositivo.")
                .setConfirmText("Cerrar")
                .setConfirmClickListener { dialog -> dialog.dismissWithAnimation() }
                .show()
        }
    }

    private fun credencialesIncorrectas() {
        SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error")
            .setContentText("Usuario o contraseña incorrectos")
            .setConfirmText("Cerrar")
            .setConfirmClickListener { dialog -> dialog.dismissWithAnimation() }
            .show()
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