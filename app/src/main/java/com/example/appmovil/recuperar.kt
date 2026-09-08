package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

class recuperar : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etCodigo1: EditText
    private lateinit var etCodigo2: EditText
    private lateinit var etCodigo3: EditText
    private lateinit var etCodigo4: EditText
    private lateinit var btnEnviar: Button
    private lateinit var btnValidar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Vinculación exacta con los IDs de tu XML
        etEmail = findViewById(R.id.rEmail)
        etCodigo1 = findViewById(R.id.codigo1)
        etCodigo2 = findViewById(R.id.codigo2)
        etCodigo3 = findViewById(R.id.codigo3)
        etCodigo4 = findViewById(R.id.codigo4)
        btnEnviar = findViewById(R.id.rEnviar)
        btnValidar = findViewById(R.id.rValidar)

        // Acción al presionar "Enviar" (Valida email)
        btnEnviar.setOnClickListener {
            validarEmailYEnviar()
        }

        // Acción al presionar "Validar" (Valida el código)
        btnValidar.setOnClickListener {
            validarCodigoEIngresar()
        }
    }

    private fun validarEmailYEnviar() {
        val email = etEmail.text.toString().trim()

        when {
            email.isEmpty() -> {
                mostrarAlerta("Campo requerido", "Debes ingresar tu email.", SweetAlertDialog.WARNING_TYPE)
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mostrarAlerta("Formato incorrecto", "Ingresa un correo electrónico válido.", SweetAlertDialog.ERROR_TYPE)
            }
            else -> {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Código Enviado!")
                    .setContentText("Se ha enviado el código de verificación a $email.")
                    .setConfirmText("Entendido")
                    .show()
            }
        }
    }

    private fun validarCodigoEIngresar() {
        val c1 = etCodigo1.text.toString().trim()
        val c2 = etCodigo2.text.toString().trim()
        val c3 = etCodigo3.text.toString().trim()
        val c4 = etCodigo4.text.toString().trim()

        // Verifica que los 4 cuadros tengan un número
        if (c1.isEmpty() || c2.isEmpty() || c3.isEmpty() || c4.isEmpty()) {
            mostrarAlerta("Código incompleto", "Debes ingresar los 4 dígitos del código.", SweetAlertDialog.WARNING_TYPE)
        } else {
            // Código validado correctamente, pasa a la creación de nueva contraseña
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Código Correcto!")
                .setContentText("El código ha sido verificado con éxito.")
                .setConfirmText("Continuar")
                .setConfirmClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    val intent = Intent(this, claves::class.java)
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }
}