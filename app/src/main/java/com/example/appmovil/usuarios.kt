package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
private lateinit var ringresar: Button
private lateinit var rbuscar: Button
class usuarios : BaseConexion() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuarios)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ringresar = findViewById(R.id.Ingresar)
        rbuscar = findViewById(R.id.Buscar)
        ringresar.setOnClickListener {
            val intent = Intent(this, ingresar::class.java)
            startActivity(intent)
        }
        rbuscar.setOnClickListener {
            val intent = Intent(this, buscar::class.java)
            startActivity(intent)
        }
    }
}