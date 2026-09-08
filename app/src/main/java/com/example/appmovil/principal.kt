package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
private lateinit var iconoCrud: ImageView
private lateinit var textoCrud: TextView

private lateinit var iconoSensores: ImageView
private lateinit var textoSensores: TextView

private lateinit var iconoDesarrolladores: ImageView
private lateinit var textoDesarrolladores: TextView
class principal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_principal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        iconoCrud = findViewById(R.id.iconCrud)
        textoCrud = findViewById(R.id.textCrud)

        iconoSensores = findViewById(R.id.iconSensores)
        textoSensores = findViewById(R.id.textSensores)

        iconoDesarrolladores = findViewById(R.id.iconInfo)
        textoDesarrolladores = findViewById(R.id.textInfo)

        // 3. Asigna el evento click a AMBOS (ícono y texto) para la misma acción
        iconoCrud.setOnClickListener {
            val intent = Intent(this, usuarios::class.java)
            startActivity(intent)
        }
        textoCrud.setOnClickListener {
            val intent = Intent(this, usuarios::class.java)
            startActivity(intent)
        }

        iconoSensores.setOnClickListener {
            val intent = Intent(this, sensores::class.java)
            startActivity(intent)
        }
        textoSensores.setOnClickListener {
            val intent = Intent(this, sensores::class.java)
            startActivity(intent)
        }

        iconoDesarrolladores.setOnClickListener {
            val intent = Intent(this, datos::class.java)
            startActivity(intent)
        }
        textoDesarrolladores.setOnClickListener { val intent = Intent(this, datos::class.java)
            startActivity(intent) }
    }
}