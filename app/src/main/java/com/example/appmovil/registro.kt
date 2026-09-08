package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

class registro : AppCompatActivity() {

    private lateinit var etRut: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etClave: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etRut = findViewById(R.id.editTextText3)
        etNombres = findViewById(R.id.editTextText4)
        etApellidos = findViewById(R.id.editTextText5)
        etEmail = findViewById(R.id.editTextText6)
        etClave = findViewById(R.id.editTextText7)
        btnGuardar = findViewById(R.id.button2)

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

        // Dentro de validarCampos() en registro.kt:
        when {
            rut.isEmpty() -> mostrarCampoVacio("Debes ingresar tu Rut.")
            nombres.isEmpty() -> mostrarCampoVacio("Debes ingresar tus nombres.")
            apellidos.isEmpty() -> mostrarCampoVacio("Debes ingresar tus apellidos.")
            email.isEmpty() -> mostrarCampoVacio("Debes ingresar tu email.")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mostrarError("Formato de email inválido.")
            }
            clave.isEmpty() -> mostrarCampoVacio("Debes ingresar una contraseña.")
            !esClaveRobusta(clave) -> {
                mostrarError("La contraseña debe tener mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.")
            }
            else -> mostrarRegistroExitoso()
        }
    }
    private fun mostrarError(mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error de validación")
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
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

    private fun mostrarRegistroExitoso() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Registro exitoso!")
            .setContentText("Tu cuenta ha sido creada correctamente.")
            .setConfirmText("Aceptar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .show()
    }
}