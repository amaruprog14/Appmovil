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

private lateinit var etEmail: EditText
private lateinit var btnEnviar: Button
private lateinit var etCodigo1: EditText
private lateinit var etCodigo2: EditText
private lateinit var etCodigo3: EditText
private lateinit var etCodigo4: EditText
private lateinit var btnValidar: Button
private var correoEnviado = false
class recuperar : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        etEmail = findViewById(R.id.rEmail)
        btnEnviar = findViewById(R.id.rEnviar)
        etCodigo1 = findViewById(R.id.codigo1)
        etCodigo2 = findViewById(R.id.codigo2)
        etCodigo3 = findViewById(R.id.codigo3)
        etCodigo4 = findViewById(R.id.codigo4)
        btnValidar = findViewById(R.id.rValidar)

        // Evento Botón Enviar
        btnEnviar.setOnClickListener {
            validarEmail()
        }

        // Evento Botón Validar
        btnValidar.setOnClickListener {
            validarCodigo()
        }
    }
    private fun validarEmail() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            correoEnviado = false // Mantiene el candado cerrado
            mostrarAlerta("Campo requerido", "Debes ingresar tu email para buscar tu cuenta.", SweetAlertDialog.WARNING_TYPE)
        } else {
            correoEnviado = true // ABRE EL CANDADO
            mostrarAlerta("Enviado", "Te hemos enviado un código de recuperación al correo.", SweetAlertDialog.SUCCESS_TYPE)
        }
    }

    private fun validarCodigo() {
        // 1. Verifica si el candado está cerrado
        if (!correoEnviado) {
            mostrarAlerta("Acción requerida", "Primero debes presionar 'Enviar' para validar tu email.", SweetAlertDialog.ERROR_TYPE)
            return // Detiene la ejecución aquí mismo
        }

        // 2. Si el candado está abierto, procede a revisar los números
        val c1 = etCodigo1.text.toString().trim()
        val c2 = etCodigo2.text.toString().trim()
        val c3 = etCodigo3.text.toString().trim()
        val c4 = etCodigo4.text.toString().trim()

        if (c1.isEmpty() || c2.isEmpty() || c3.isEmpty() || c4.isEmpty()) {
            mostrarAlerta("Código incompleto", "Debes ingresar los 4 dígitos del código.", SweetAlertDialog.WARNING_TYPE)
        } else {
            mostrarValidacionExitosa()
        }
    }

    // Método reutilizable para mostrar alertas simples
    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    // Método específico para cuando el código es correcto y quieres cambiar de pantalla
    private fun mostrarValidacionExitosa() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Código validado!")
            .setContentText("Puedes proceder a crear tu nueva contraseña.")
            .setConfirmText("Continuar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this, claves::class.java)
                startActivity(intent)

            }
            .show()
    }
}