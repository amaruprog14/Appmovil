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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class claves : AppCompatActivity() {

    private lateinit var etClave: EditText
    private lateinit var etRepetir: EditText
    private lateinit var btnGuardar: Button
    private lateinit var queue: RequestQueue

    private var email = ""
    private var codigo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_claves)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        email = intent.getStringExtra("email") ?: ""
        codigo = intent.getStringExtra("codigo") ?: ""

        etClave = findViewById(R.id.cContra)
        etRepetir = findViewById(R.id.cRepetir)
        btnGuardar = findViewById(R.id.btnGuardar)
        queue = Volley.newRequestQueue(this)

        // Si se llegó sin pasar por la verificación, no se puede cambiar nada
        if (email.isEmpty() || codigo.isEmpty()) {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Verificación requerida")
                .setContentText("Debes validar el código de recuperación primero.")
                .setConfirmText("Volver")
                .setConfirmClickListener { d -> d.dismissWithAnimation(); finish() }
                .show()
            btnGuardar.isEnabled = false
            return
        }

        btnGuardar.setOnClickListener { validarCampos() }
    }

    private fun validarCampos() {
        val clave = etClave.text.toString().trim()
        val repetir = etRepetir.text.toString().trim()

        when {
            clave.isEmpty() -> alerta("Campo requerido", "Ingresa tu nueva contraseña.", SweetAlertDialog.WARNING_TYPE)
            repetir.isEmpty() -> alerta("Campo requerido", "Confirma tu contraseña nueva.", SweetAlertDialog.WARNING_TYPE)
            clave != repetir -> alerta("Error", "Las contraseñas no coinciden.", SweetAlertDialog.ERROR_TYPE)
            !esClaveRobusta(clave) -> alerta(
                "Contraseña débil",
                "Debe tener al menos 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.",
                SweetAlertDialog.ERROR_TYPE
            )
            else -> guardarEnServidor(clave)
        }
    }

    private fun guardarEnServidor(clave: String) {
        btnGuardar.isEnabled = false
        val req = object : StringRequest(
            Request.Method.POST, Api.BASE + "rec_clave.php",
            { texto ->
                btnGuardar.isEnabled = true
                try {
                    val json = JSONObject(texto)
                    if (json.optBoolean("ok")) mostrarExito()
                    else alerta("No se pudo actualizar", json.optString("msg", "Error desconocido"),
                        SweetAlertDialog.ERROR_TYPE)
                } catch (e: Exception) {
                    alerta("Error", "Respuesta inesperada del servidor.", SweetAlertDialog.ERROR_TYPE)
                }
            },
            {
                btnGuardar.isEnabled = true
                alerta("Sin conexión", "No se pudo conectar con el servidor. Intenta de nuevo.",
                    SweetAlertDialog.ERROR_TYPE)
            }
        ) {
            override fun getParams(): MutableMap<String, String> =
                mutableMapOf("email" to email, "codigo" to codigo, "clave" to clave)
        }
        queue.add(req)
    }

    private fun esClaveRobusta(clave: String): Boolean {
        val regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#._-]).{8,}\$".toRegex()
        return regex.matches(clave)
    }

    private fun alerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    private fun mostrarExito() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Contraseña actualizada!")
            .setContentText("Tu cuenta quedó reactivada. Ya puedes iniciar sesión.")
            .setConfirmText("Iniciar sesión")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()
            }
            .show()
    }
}