package com.example.appmovil

import android.content.Intent
import android.graphics.Color
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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Listado de usuarios (imagen N° 9).
 * Con Internet la lista viene de MySQL (listar.php) y se guarda en SQLite;
 * sin Internet se muestra lo guardado en SQLite. El usuario que inició sesión no aparece.
 */
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
            abrirDatos(listaFiltrada[position])
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarUsuarios(s.toString())
            }
        })
    }

    // Se ejecuta al abrir y al volver desde el detalle
    override fun onStart() {
        super.onStart()
        estabaEnLinea = ConexionMonitor.enLinea
        cargarLocal()               // muestra al tiro lo que hay en el teléfono
        consultarServidor()           // y si hay Internet, lo actualiza desde MySQL
    }

    /** Al volver la conexión o terminar una sincronización, la lista se refresca sola. */
    override fun alCambiarEstado(evento: Int) {
        val recienConectado = ConexionMonitor.enLinea && !estabaEnLinea
        estabaEnLinea = ConexionMonitor.enLinea
        when {
            evento == ConexionMonitor.EVENTO_SINCRONIZADO || evento == ConexionMonitor.EVENTO_CONFLICTO ||
                    recienConectado -> consultarServidor()
            else -> cargarLocal()
        }
    }

    private var estabaEnLinea = true

    private fun consultarServidor() {
        RepositorioUsuarios.actualizarDesdeServidor(this) { error ->
            if (isFinishing || isDestroyed) return@actualizarDesdeServidor
            cargarLocal()
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun cargarLocal() {
        val yo = Sesion.email(this)
        listaCompleta = ConexionDbHelper(this).listar()
            .filter { yo == null || !it.email.equals(yo, ignoreCase = true) }   // no mostrarse a sí mismo
        filtrarUsuarios(etBuscar.text.toString())
    }

    /** Sin distinguir mayúsculas ni tildes: "jose" encuentra "José". */
    private fun filtrarUsuarios(query: String) {
        val q = Validar.limpiar(query).normalizado()
        listaFiltrada = if (q.isEmpty()) {
            listaCompleta.toMutableList()
        } else {
            listaCompleta.filter {
                val completo = "${it.nombres} ${it.apellidos}".normalizado()
                completo.contains(q) ||
                        it.nombres.normalizado().contains(q) ||
                        it.apellidos.normalizado().contains(q)
            }.toMutableList()
        }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun abrirDatos(usuario: Usuario) {
        startActivity(Intent(this, datosbuscar::class.java).putExtra("rut", usuario.rut))
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
        view.findViewById<TextView>(R.id.tvNombreCompleto).text = "${usuario.nombres} ${usuario.apellidos}"
        view.findViewById<TextView>(R.id.tvRut).text = "${usuario.rut} · ${usuario.email}"

        val tvEstado = view.findViewById<TextView>(R.id.tvEstado)
        when {
            Estado.esConflicto(usuario.estado) -> {
                tvEstado.visibility = View.VISIBLE
                tvEstado.setTextColor(Color.parseColor("#D32F2F"))
                tvEstado.text = "⚠ No sincronizado: ${usuario.error ?: "dato duplicado"}. Toca para corregir."
            }
            Estado.esPendiente(usuario.estado) -> {
                tvEstado.visibility = View.VISIBLE
                tvEstado.setTextColor(Color.parseColor("#F57C00"))
                tvEstado.text = "⏳ Pendiente de sincronizar"
            }
            else -> tvEstado.visibility = View.GONE
        }
        return view
    }
}
