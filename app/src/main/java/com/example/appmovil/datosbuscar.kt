package com.example.appmovil

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

class datosbuscar : BaseConexion() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_datosbuscar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvRut = findViewById<TextView>(R.id.tvRutDetalle)
        val etNombres = findViewById<EditText>(R.id.tvNombresDetalle)
        val etApellidos = findViewById<EditText>(R.id.tvApellidosDetalle)
        val etEmail = findViewById<EditText>(R.id.tvEmailDetalle)
        val btnModificar = findViewById<Button>(R.id.btnModificar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        val rut = intent.getStringExtra("rut") ?: ""
        tvRut.text = "Rut: $rut"
        etNombres.setText(intent.getStringExtra("nombres") ?: "")
        etApellidos.setText(intent.getStringExtra("apellidos") ?: "")
        etEmail.setText(intent.getStringExtra("email") ?: "")

        val db = ConexionDbHelper(this)

        btnModificar.setOnClickListener {
            val n = etNombres.text.toString().trim()
            val a = etApellidos.text.toString().trim()
            val e = etEmail.text.toString().trim()

            when {
                n.isEmpty() || a.isEmpty() || e.isEmpty() ->
                    alerta("Campos incompletos", "Completa todos los campos.", SweetAlertDialog.WARNING_TYPE)
                !Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                    alerta("Email inválido", "Ingresa un correo válido.", SweetAlertDialog.ERROR_TYPE)
                else -> {
                    db.actualizar(rut, n, a, e)
                    SyncWorker.programar(this)
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("¡Modificado!")
                        .setContentText("Los datos se guardaron correctamente.")
                        .setConfirmText("Aceptar")
                        .setConfirmClickListener { d ->
                            d.dismissWithAnimation()
                            finish()
                        }
                        .show()
                }
            }
        }

        btnEliminar.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("¿Eliminar usuario?")
                .setContentText("Se eliminará a ${etNombres.text} ${etApellidos.text}.")
                .setConfirmText("Sí, eliminar")
                .setCancelText("Cancelar")
                .setConfirmClickListener { d ->
                    db.marcarEliminado(rut)
                    SyncWorker.programar(this)
                    d.dismissWithAnimation()
                    finish()
                }
                .setCancelClickListener { d -> d.dismissWithAnimation() }
                .show()
        }
    }

    private fun alerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }
}