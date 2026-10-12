package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

/** Registro público (imagen N° 5). Al terminar vuelve al login. */
class registro : BaseConexion() {

    private lateinit var etRut: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var etClave: EditText
    private lateinit var etRepetir: EditText
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
        etRepetir = findViewById(R.id.etRepetirClave)
        btnGuardar = findViewById(R.id.button2)

        btnGuardar.setOnClickListener { validarCampos() }
    }

    private fun validarCampos() {
        val rut = etRut.text.toString().trim().uppercase()
        val nombres = Validar.limpiar(etNombres.text.toString())
        val apellidos = Validar.limpiar(etApellidos.text.toString())
        val email = etEmail.text.toString().trim().lowercase()
        val clave = etClave.text.toString().trim()
        val repetir = etRepetir.text.toString().trim()

        val problema = Validar.registro(rut, nombres, apellidos, email, clave, repetir)
        if (problema != null) {
            if (problema.vacio) alerta("Campo requerido", problema.msg, SweetAlertDialog.WARNING_TYPE)
            else alerta("Error de validación", problema.msg, SweetAlertDialog.ERROR_TYPE)
            return
        }

        btnGuardar.isEnabled = false
        RepositorioUsuarios.registrar(this, rut, nombres, apellidos, email, clave) { r ->
            if (isFinishing || isDestroyed) return@registrar   // el usuario salió de la pantalla
            btnGuardar.isEnabled = true
            when (r) {
                RepositorioUsuarios.Respuesta.EnServidor ->
                    mostrarRegistroExitoso("Tu cuenta ha sido creada correctamente.")
                is RepositorioUsuarios.Respuesta.EnLocal ->
                    mostrarRegistroExitoso("Tu cuenta fue creada. ${r.motivo}")
                is RepositorioUsuarios.Respuesta.Rechazado ->
                    alerta("No se pudo registrar", r.msg, SweetAlertDialog.ERROR_TYPE)
            }
        }
    }

    private fun alerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }

    private fun mostrarRegistroExitoso(mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Registro exitoso!")
            .setContentText(mensaje)
            .setConfirmText("Aceptar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                startActivity(
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                finish()
            }
            .show()
    }
}
