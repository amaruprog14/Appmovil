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

class MainActivity : AppCompatActivity() {
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
            validarCampos()

        }
        cRegistro.setOnClickListener {
            val intent = Intent(this, registro::class.java)
            startActivity(intent)
        }
        cOlvide.setOnClickListener {
            val intent = Intent(this, recuperar::class.java)
            startActivity(intent)
        }
    }
    private fun validarCampos() {
        val email = etEmail.text.toString().trim()
        val clave = etClave.text.toString().trim()

        when {
            email.isEmpty() -> mostrarCampoVacio("Debes ingresar tu email.")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Formato incorrecto")
                    .setContentText("Debes ingresar un email válido (ej: usuario@dominio.com).")
                    .setConfirmText("Entendido")
                    .show()
            }
            clave.isEmpty() -> mostrarCampoVacio("Debes ingresar una contraseña.")
            else -> {
                btningresar.isEnabled = false // Bloquea el botón según la rúbrica
                mostrarLoginExitoso()
            }
        }
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