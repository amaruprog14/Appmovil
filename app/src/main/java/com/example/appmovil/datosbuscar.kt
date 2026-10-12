package com.example.appmovil

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog

/**
 * Modificar / eliminar usuario (imagen N° 10).
 * Con Internet se hace en MySQL (modificar.php / eliminar.php); sin Internet queda en SQLite
 * como cambio pendiente de sincronizar.
 */
class datosbuscar : BaseConexion() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnModificar: Button
    private lateinit var btnEliminar: Button
    private lateinit var usuario: Usuario

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
        etNombres = findViewById(R.id.tvNombresDetalle)
        etApellidos = findViewById(R.id.tvApellidosDetalle)
        etEmail = findViewById(R.id.tvEmailDetalle)
        btnModificar = findViewById(R.id.btnModificar)
        btnEliminar = findViewById(R.id.btnEliminar)

        // Siempre se leen los datos actuales de la base (no los que venían en el Intent)
        val rut = intent.getStringExtra("rut") ?: ""
        val encontrado = ConexionDbHelper(this).obtener(rut)
        if (encontrado == null) {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Usuario no encontrado")
                .setContentText("El usuario ya no existe.")
                .setConfirmText("Volver")
                .setConfirmClickListener { d -> d.dismissWithAnimation(); finish() }
                .show()
            btnModificar.isEnabled = false
            btnEliminar.isEnabled = false
            return
        }
        usuario = encontrado

        tvRut.text = "Rut: ${usuario.rut}"
        etNombres.setText(usuario.nombres)
        etApellidos.setText(usuario.apellidos)
        etEmail.setText(usuario.email)

        // Registro rechazado al sincronizar: se explica el motivo para que lo corrija
        if (Estado.esConflicto(usuario.estado)) {
            alerta("No se pudo sincronizar",
                "${usuario.error ?: "El servidor rechazó este registro."}\n\nCorrige el dato y presiona Modificar.",
                SweetAlertDialog.WARNING_TYPE)
        }

        btnModificar.setOnClickListener { modificar() }
        btnEliminar.setOnClickListener { confirmarEliminar() }
    }

    private fun modificar() {
        val n = Validar.limpiar(etNombres.text.toString())
        val a = Validar.limpiar(etApellidos.text.toString())
        val e = etEmail.text.toString().trim().lowercase()

        val problema = Validar.datosPersonales(n, a, e)
        if (problema != null) {
            if (problema.vacio) alerta("Campo requerido", problema.msg, SweetAlertDialog.WARNING_TYPE)
            else alerta("Error de validación", problema.msg, SweetAlertDialog.ERROR_TYPE)
            return
        }

        bloquear(true)
        RepositorioUsuarios.modificar(this, usuario, n, a, e) resp@{ r ->
            if (isFinishing || isDestroyed) return@resp
            bloquear(false)
            when (r) {
                RepositorioUsuarios.Respuesta.EnServidor ->
                    exitoYVolver("¡Modificado!", "Los datos se actualizaron en MySQL.")
                is RepositorioUsuarios.Respuesta.EnLocal ->
                    exitoYVolver("¡Modificado!", r.motivo)
                is RepositorioUsuarios.Respuesta.Rechazado ->
                    alerta("No se pudo modificar", r.msg, SweetAlertDialog.ERROR_TYPE)
            }
        }
    }

    private fun confirmarEliminar() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Eliminar usuario?")
            .setContentText("Se eliminará a ${usuario.nombres} ${usuario.apellidos}. Esta acción no se puede deshacer.")
            .setConfirmText("Sí, eliminar")
            .setCancelText("Cancelar")
            .setConfirmClickListener { d ->
                d.dismissWithAnimation()
                eliminar()
            }
            .setCancelClickListener { d -> d.dismissWithAnimation() }
            .show()
    }

    private fun eliminar() {
        bloquear(true)
        RepositorioUsuarios.eliminar(this, usuario) resp@{ r ->
            if (isFinishing || isDestroyed) return@resp
            bloquear(false)
            when (r) {
                RepositorioUsuarios.Respuesta.EnServidor ->
                    exitoYVolver("¡Eliminado!", "El usuario fue eliminado.")
                is RepositorioUsuarios.Respuesta.EnLocal ->
                    exitoYVolver("¡Eliminado!", r.motivo)
                is RepositorioUsuarios.Respuesta.Rechazado ->
                    alerta("No se pudo eliminar", r.msg, SweetAlertDialog.ERROR_TYPE)
            }
        }
    }

    private fun bloquear(si: Boolean) {
        btnModificar.isEnabled = !si
        btnEliminar.isEnabled = !si
    }

    private fun exitoYVolver(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Aceptar")
            .setConfirmClickListener { d ->
                d.dismissWithAnimation()
                finish()
            }
            .show()
    }

    private fun alerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }
}
