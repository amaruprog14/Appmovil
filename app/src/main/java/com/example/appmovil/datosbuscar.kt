package com.example.appmovil

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

class datosbuscar : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_datosbuscar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvRutDetalle = findViewById<TextView>(R.id.tvRutDetalle)
        val tvNombresDetalle = findViewById<TextView>(R.id.tvNombresDetalle)
        val tvApellidosDetalle = findViewById<TextView>(R.id.tvApellidosDetalle)
        val tvEmailDetalle = findViewById<TextView>(R.id.tvEmailDetalle)
        val btnModificar = findViewById<Button>(R.id.btnModificar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        val rut = intent.getStringExtra("rut") ?: ""
        val nombres = intent.getStringExtra("nombres") ?: ""
        val apellidos = intent.getStringExtra("apellidos") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        tvRutDetalle.text = "Rut: $rut"
        tvNombresDetalle.text = "Nombres: $nombres"
        tvApellidosDetalle.text = "Apellidos: $apellidos"
        tvEmailDetalle.text = "Email: $email"

        btnModificar.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("Modificar")
                .setContentText("Aquí iría la edición de $nombres $apellidos.")
                .setConfirmText("Aceptar")
                .show()
        }

        btnEliminar.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("¿Eliminar usuario?")
                .setContentText("Se eliminará a $nombres $apellidos.")
                .setConfirmText("Sí, eliminar")
                .setCancelText("Cancelar")
                .setConfirmClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    finish()
                }
                .setCancelClickListener { dialog ->
                    dialog.dismissWithAnimation()
                }
                .show()
        }
    }
}