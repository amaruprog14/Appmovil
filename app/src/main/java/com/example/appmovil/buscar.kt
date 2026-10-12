package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

data class Usuario(
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val email: String
)

class buscar : BaseConexion() {

    private lateinit var etBuscar: EditText
    private lateinit var listViewUsuarios: ListView
    private lateinit var adapter: UsuarioAdapter

    private var listaCompleta: List<Usuario> = emptyList()
    private var listaFiltrada = mutableListOf<Usuario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buscar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etBuscar = findViewById(R.id.etBuscar)
        listViewUsuarios = findViewById(R.id.listViewUsuarios)

        adapter = UsuarioAdapter(listaFiltrada)
        listViewUsuarios.adapter = adapter

        listViewUsuarios.setOnItemClickListener { _, _, position, _ ->
            val usuarioSeleccionado = listaFiltrada[position]
            abrirDatos(usuarioSeleccionado)
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarUsuarios(s.toString())
            }
        })
    }

    // <-- AQUÍ: se ejecuta al abrir y al volver desde el detalle
    override fun onStart() {
        super.onStart()
        listaCompleta = ConexionDbHelper(this).listar()
        filtrarUsuarios(etBuscar.text.toString())
    }

    private fun filtrarUsuarios(query: String) {
        listaFiltrada = if (query.isEmpty()) {
            listaCompleta.toMutableList()
        } else {
            listaCompleta.filter {
                it.nombres.contains(query, ignoreCase = true) ||
                        it.apellidos.contains(query, ignoreCase = true) ||
                        it.rut.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun abrirDatos(usuario: Usuario) {
        val intent = Intent(this, datosbuscar::class.java)
        intent.putExtra("rut", usuario.rut)
        intent.putExtra("nombres", usuario.nombres)
        intent.putExtra("apellidos", usuario.apellidos)
        intent.putExtra("email", usuario.email)
        startActivity(intent)
    }
}

class UsuarioAdapter(private var usuarios: MutableList<Usuario>) : BaseAdapter() {

    override fun getCount(): Int = usuarios.size
    override fun getItem(position: Int): Any = usuarios[position]
    override fun getItemId(position: Int): Long = position.toLong()

    fun actualizarLista(nuevaLista: MutableList<Usuario>) {
        usuarios = nuevaLista
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)

        val usuario = usuarios[position]
        val tvNombreCompleto = view.findViewById<TextView>(R.id.tvNombreCompleto)
        val tvRut = view.findViewById<TextView>(R.id.tvRut)

        tvNombreCompleto.text = "${usuario.nombres} ${usuario.apellidos}"
        tvRut.text = usuario.rut

        return view
    }
}