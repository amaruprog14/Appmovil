package com.example.appmovil

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

/** Ingresar usuario desde Gestión de usuarios (imagen N° 8). Al terminar vuelve al menú (imagen N° 7). */
class ingresar : BaseConexion() {

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
        etRepetir = findViewById(R.id.etRepetirClave)
        btnGuardar = findViewById(R.id.button3)

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
                    mostrarIngresoExitoso("El usuario se registró correctamente en MySQL.")
                is RepositorioUsuarios.Respuesta.EnLocal ->
                    mostrarIngresoExitoso("El usuario se registró. ${r.motivo}")
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

    private fun mostrarIngresoExitoso(mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("¡Listo!")
            .setContentText(mensaje)
            .setConfirmText("Aceptar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                finish()   // vuelve a Gestión de usuarios
            }
            .show()
    }
}
