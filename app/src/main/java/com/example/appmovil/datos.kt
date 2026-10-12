package com.example.appmovil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

data class Desarrollador(
    val nombres: String,
    val apellidos: String,
    val rol: String,          // Frontend / Backend / QA / PM
    val correo: String,       // correo institucional
    val institucion: String,
    val carrera: String,
    val seccion: String,
    val github: String,
    val linkedin: String
)

class datos : BaseConexion() {

    // Los campos vacíos ("") no se muestran. Completa los que faltan antes de entregar.
    private val equipo = listOf(
        Desarrollador(
            nombres = "Amaru",
            apellidos = "Marin",
            rol = "FrontEnd",                                   // COMPLETAR (ej: Backend)
            correo = "amaru.marin@inacapmail.cl",                                // COMPLETAR correo institucional
            institucion = "INACAP",
            carrera = "Ingeniería en Informática",
            seccion = "P2-C2",                               // COMPLETAR sección
            github = "https://github.com/amaruprog14",
            linkedin = ""                               // COMPLETAR (opcional)
        ),
        Desarrollador(
            nombres = "Sebastian",
            apellidos = "Alzamora",
            rol = "BackEnd",                                   // COMPLETAR (ej: Frontend)
            correo = "sebastian.alzamora02@inacapmail.cl",
            institucion = "INACAP",
            carrera = "Ingeniería en Informática",
            seccion = "P2-C2",                               // COMPLETAR sección
            github = "https://github.com/Alzauu",                                // COMPLETAR
            linkedin = ""                               // COMPLETAR (opcional)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_datos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val contenedor = findViewById<LinearLayout>(R.id.contenedorDesarrolladores)
        val inflater = LayoutInflater.from(this)

        for (d in equipo) {
            val tarjeta = inflater.inflate(R.layout.item_desarrollador, contenedor, false)
            tarjeta.findViewById<TextView>(R.id.tvNombre).text = "${d.nombres} ${d.apellidos}"
            tarjeta.findViewById<TextView>(R.id.tvRol).mostrar("", d.rol)
            tarjeta.findViewById<TextView>(R.id.tvCorreo).mostrar("Correo", d.correo)
            tarjeta.findViewById<TextView>(R.id.tvInstitucion).mostrar("Institución", d.institucion)
            tarjeta.findViewById<TextView>(R.id.tvCarrera).mostrar("Carrera", d.carrera)
            tarjeta.findViewById<TextView>(R.id.tvSeccion).mostrar("Sección", d.seccion)
            tarjeta.findViewById<TextView>(R.id.tvGithub).mostrar("GitHub", d.github)
            tarjeta.findViewById<TextView>(R.id.tvLinkedin).mostrar("LinkedIn", d.linkedin)
            contenedor.addView(tarjeta)
        }
    }

    /** Muestra "Etiqueta: valor" o esconde la fila si no hay valor. */
    private fun TextView.mostrar(etiqueta: String, valor: String) {
        if (valor.isBlank()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            text = if (etiqueta.isEmpty()) valor else "$etiqueta: $valor"
        }
    }
}