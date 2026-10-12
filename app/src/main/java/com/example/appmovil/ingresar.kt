package com.example.appmovil

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

class ingresar : BaseConexion() {

    private lateinit var etRut: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etClave: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ingresar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etRut = findViewById(R.id.editTextText11)
        etNombres = findViewById(R.id.editTextText8)
        etApellidos = findViewById(R.id.editTextText9)
        etEmail = findViewById(R.id.editTextText10)
        etClave = findViewById(R.id.editTextText12)
        btnGuardar = findViewById(R.id.button3)

        btnGuardar.setOnClickListener {
            validarCampos()
        }
    }

    private fun validarCampos() {
        val rut = etRut.text.toString().trim()
        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val clave = etClave.text.toString().trim()

        when {
            rut.isEmpty() -> mostrarCampoVacio("Debes ingresar tu Rut.")
            nombres.isEmpty() -> mostrarCampoVacio("Debes ingresar tus nombres.")
            apellidos.isEmpty() -> mostrarCampoVacio("Debes ingresar tus apellidos.")
            email.isEmpty() -> mostrarCampoVacio("Debes ingresar tu email.")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mostrarError("El formato del correo electrónico no es válido.")
            }
            clave.isEmpty() -> mostrarCampoVacio("Debes ingresar una contraseña.")
            !esClaveRobusta(clave) -> {
                mostrarError("La contraseña debe contener mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.")
            }
            else -> {
                if (ConexionDbHelper(this).insertar(rut, nombres, apellidos, email, clave)) {
                    SyncWorker.programar(this)
                    mostrarIngresoExitoso()
                } else {
                    mostrarError("Ya existe un usuario con ese Rut.")
                }
            }
        }
    }

    private fun esClaveRobusta(clave: String): Boolean {
        val regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#._-]).{8,}\$".toRegex()
        return regex.matches(clave)
    }

    private fun mostrarCampoVacio(mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Campo requerido")
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    private fun mostrarError(mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error de validación")
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    private fun mostrarIngresoExitoso() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Listo!")
            .setContentText("Los datos se guardaron correctamente.")
            .setConfirmText("Aceptar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                finish()
            }
            .show()
    }
}