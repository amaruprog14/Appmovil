package com.example.appmovil

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class recuperar : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var cajas: List<EditText>
    private lateinit var btnEnviar: Button
    private lateinit var btnValidar: Button
    private lateinit var tvTimer: TextView
    private lateinit var queue: RequestQueue

    private var timer: CountDownTimer? = null
    private var emailEnviado: String? = null
    private var codigoVigente = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etEmail = findViewById(R.id.rEmail)
        btnEnviar = findViewById(R.id.rEnviar)
        btnValidar = findViewById(R.id.rValidar)
        tvTimer = findViewById(R.id.tvTimer)
        cajas = listOf(
            findViewById(R.id.codigo1), findViewById(R.id.codigo2), findViewById(R.id.codigo3),
            findViewById(R.id.codigo4), findViewById(R.id.codigo5), findViewById(R.id.codigo6)
        )
        queue = Volley.newRequestQueue(this)

        configurarCajas()
        btnEnviar.setOnClickListener { solicitarCodigo() }
        btnValidar.setOnClickListener { validarCodigo() }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    // ---------- Cajas del código: avanzan solas y retroceden con borrar ----------
    private fun configurarCajas() {
        cajas.forEachIndexed { i, caja ->
            caja.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < cajas.size - 1) cajas[i + 1].requestFocus()
                }
            })
            caja.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN &&
                    caja.text.isEmpty() && i > 0
                ) {
                    cajas[i - 1].apply { setText(""); requestFocus() }
                    true
                } else false
            }
        }
    }

    private fun codigoIngresado(): String = cajas.joinToString("") { it.text.toString().trim() }

    private fun limpiarCajas() {
        cajas.forEach { it.setText("") }
        cajas[0].requestFocus()
    }

    // ---------- Paso 1: pedir el código (también sirve para pedir otro) ----------
    private fun solicitarCodigo() {
        val email = etEmail.text.toString().trim()
        when {
            email.isEmpty() ->
                alerta("Campo requerido", "Debes ingresar tu email.", SweetAlertDialog.WARNING_TYPE)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                alerta("Formato incorrecto", "Ingresa un correo electrónico válido.", SweetAlertDialog.ERROR_TYPE)
            else -> {
                btnEnviar.isEnabled = false
                post("rec_enviar.php", mapOf("email" to email),
                    onRespuesta = { json ->
                        if (json.optBoolean("ok")) {
                            emailEnviado = email
                            limpiarCajas()
                            iniciarCuenta()
                            alerta("¡Código enviado!", "Revisa tu correo. El código vale 1 minuto.",
                                SweetAlertDialog.SUCCESS_TYPE)
                        } else {
                            btnEnviar.isEnabled = true
                            alerta("No se pudo enviar", json.optString("msg", "Error desconocido"),
                                SweetAlertDialog.ERROR_TYPE)
                        }
                    },
                    onFallo = {
                        btnEnviar.isEnabled = true
                        alerta("Sin conexión", "No se pudo conectar con el servidor. Revisa tu internet.",
                            SweetAlertDialog.ERROR_TYPE)
                    })
            }
        }
    }

    // ---------- Cuenta regresiva de 60 s ----------
    private fun iniciarCuenta() {
        timer?.cancel()
        codigoVigente = true
        btnEnviar.isEnabled = false
        timer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(msRestantes: Long) {
                tvTimer.text = "${(msRestantes + 999) / 1000} Segundos"
            }

            override fun onFinish() {
                codigoVigente = false
                tvTimer.text = "El código venció"
                btnEnviar.text = "Reenviar código"
                btnEnviar.isEnabled = true
            }
        }.start()
    }

    // ---------- Paso 2: validar el código en el servidor ----------
    private fun validarCodigo() {
        val email = emailEnviado
        val codigo = codigoIngresado()
        when {
            email == null ->
                alerta("Primero solicita el código", "Ingresa tu email y presiona «Enviar código».",
                    SweetAlertDialog.WARNING_TYPE)
            !codigoVigente ->
                alerta("Código vencido", "El código venció. Solicita uno nuevo.", SweetAlertDialog.ERROR_TYPE)
            codigo.length != 6 ->
                alerta("Código incompleto", "Debes ingresar los 6 dígitos del código.",
                    SweetAlertDialog.WARNING_TYPE)
            else -> {
                btnValidar.isEnabled = false
                post("rec_validar.php", mapOf("email" to email, "codigo" to codigo),
                    onRespuesta = { json ->
                        btnValidar.isEnabled = true
                        if (json.optBoolean("ok")) {
                            timer?.cancel()
                            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("¡Código correcto!")
                                .setContentText("Ahora crea tu nueva contraseña.")
                                .setConfirmText("Continuar")
                                .setConfirmClickListener { d ->
                                    d.dismissWithAnimation()
                                    startActivity(
                                        Intent(this, claves::class.java)
                                            .putExtra("email", email)
                                            .putExtra("codigo", codigo)
                                    )
                                    finish()
                                }
                                .show()
                        } else {
                            alerta("Código rechazado", json.optString("msg", "Código incorrecto"),
                                SweetAlertDialog.ERROR_TYPE)
                        }
                    },
                    onFallo = {
                        btnValidar.isEnabled = true
                        alerta("Sin conexión", "No se pudo conectar con el servidor.",
                            SweetAlertDialog.ERROR_TYPE)
                    })
            }
        }
    }

    // ---------- Utilidades ----------
    private fun post(
        endpoint: String,
        params: Map<String, String>,
        onRespuesta: (JSONObject) -> Unit,
        onFallo: () -> Unit
    ) {
        val req = object : StringRequest(
            Request.Method.POST, Api.BASE + endpoint,
            { texto ->
                try {
                    onRespuesta(JSONObject(texto))
                } catch (e: Exception) {
                    onFallo()
                }
            },
            { onFallo() }
        ) {
            override fun getParams(): MutableMap<String, String> = params.toMutableMap()
        }
        queue.add(req)
    }

    private fun alerta(titulo: String, mensaje: String, tipo: Int) {
        SweetAlertDialog(this, tipo)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Entendido")
            .show()
    }
}