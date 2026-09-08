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

private lateinit var etClave: EditText
private lateinit var etRepetir: EditText
private lateinit var btnGuardar: Button
class claves : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_claves)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        etClave = findViewById(R.id.cContra)
        etRepetir = findViewById(R.id.cRepetir)
        btnGuardar = findViewById(R.id.btnGuardar)

        btnGuardar.setOnClickListener {
            validarCampos()
        }
    }
    private fun validarCampos() {
        val clave = etClave.text.toString().trim()
        val repetirClave = etRepetir.text.toString().trim()

        when {
            clave.isEmpty() -> mostrarAlerta("Campo requerido", "Ingresa tu nueva contraseña.", SweetAlertDialog.WARNING_TYPE)
            repetirClave.isEmpty() -> mostrarAlerta("Campo requerido", "Confirma tu contraseña nueva.", SweetAlertDialog.WARNING_TYPE)
            clave != repetirClave -> mostrarAlerta("Error", "Las contraseñas no coinciden.", SweetAlertDialog.ERROR_TYPE)
            !esClaveRobusta(clave) -> mostrarAlerta("Contraseña débil", "Debe tener al menos 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.", SweetAlertDialog.ERROR_TYPE)
            else -> mostrarExito()
        }
    }
    private fun esClaveRobusta(clave: String): Boolean {
        val regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#._-]).{8,}\$".toRegex()
        return regex.matches(clave)
    }

    private fun mostrarAlerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    private fun mostrarExito() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Contraseña actualizada!")
            .setContentText("La contraseña se ha actualizado correctamente.")
            .setConfirmText("Acepta")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                // Aquí puedes agregar un Intent para ir al Login, o finish() para cerrar
            }
            .show()
    }
}
