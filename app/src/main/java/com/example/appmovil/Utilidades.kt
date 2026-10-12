package com.example.appmovil

import android.content.Context
import android.util.Base64
import android.util.Patterns
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cifrado (hash) de contraseñas para la base local.
 * En MySQL el hash lo hace el servidor con password_hash(); aquí se usa PBKDF2 con sal
 * aleatoria para poder validar el login sin conexión sin guardar la clave en texto plano.
 */
object Seguridad {
    private const val ITERACIONES = 10_000
    private const val LARGO_BITS = 256

    fun hash(clave: String): String {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return b64(sal) + ":" + b64(pbkdf2(clave, sal))
    }

    fun verificar(clave: String, guardado: String?): Boolean {
        val partes = guardado?.split(":") ?: return false
        if (partes.size != 2) return false
        return try {
            val sal = Base64.decode(partes[0], Base64.NO_WRAP)
            val esperado = Base64.decode(partes[1], Base64.NO_WRAP)
            MessageDigest.isEqual(esperado, pbkdf2(clave, sal))
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun pbkdf2(clave: String, sal: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")   // disponible desde API 10 (minSdk 24)
            .generateSecret(PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, LARGO_BITS))
            .encoded

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
}

/** Validaciones compartidas por registro, ingreso y modificación de usuarios. */
object Validar {
    private val SOLO_LETRAS = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+( [A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$")
    private val CLAVE_ROBUSTA = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#._-]).{8,}$")

    /** Quita espacios de los extremos y deja un solo espacio entre palabras. */
    fun limpiar(texto: String) = texto.trim().replace(Regex("\\s+"), " ")

    fun soloLetras(texto: String) = SOLO_LETRAS.matches(texto)
    fun formatoEmail(texto: String) = Patterns.EMAIL_ADDRESS.matcher(texto).matches()
    fun claveRobusta(clave: String) = CLAVE_ROBUSTA.matches(clave)

    const val MSG_CLAVE =
        "La contraseña debe tener mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial."

    /** Problema de validación: [vacio] = campo requerido (alerta amarilla), si no error (roja). */
    data class Problema(val msg: String, val vacio: Boolean = false)

    /** Nombres, apellidos y correo (se usa al registrar y al modificar). */
    fun datosPersonales(nombres: String, apellidos: String, email: String): Problema? = when {
        nombres.isEmpty() -> Problema("Debes ingresar los nombres.", vacio = true)
        !soloLetras(nombres) -> Problema("Los nombres solo pueden contener letras y espacios (se permiten tildes y ñ).")
        apellidos.isEmpty() -> Problema("Debes ingresar los apellidos.", vacio = true)
        !soloLetras(apellidos) -> Problema("Los apellidos solo pueden contener letras y espacios (se permiten tildes y ñ).")
        email.isEmpty() -> Problema("Debes ingresar el email.", vacio = true)
        !formatoEmail(email) -> Problema("El formato del correo electrónico no es válido (ej: usuario@dominio.com).")
        else -> null
    }

    /** Formulario completo de registro (validaciones de la primera entrega + repetir clave). */
    fun registro(
        rut: String, nombres: String, apellidos: String, email: String, clave: String, repetir: String
    ): Problema? = when {
        rut.isEmpty() -> Problema("Debes ingresar el Rut.", vacio = true)
        else -> datosPersonales(nombres, apellidos, email) ?: when {
            clave.isEmpty() -> Problema("Debes ingresar una contraseña.", vacio = true)
            !claveRobusta(clave) -> Problema(MSG_CLAVE)
            repetir.isEmpty() -> Problema("Debes repetir la contraseña.", vacio = true)
            clave != repetir -> Problema("Las contraseñas no coinciden.")
            else -> null
        }
    }
}

/** Texto sin tildes y en minúsculas: "José Núñez" -> "jose nunez". */
fun String.normalizado(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** Usuario que inició sesión (para no mostrarse a sí mismo en el CRUD). */
object Sesion {
    private const val PREFS = "sesion"

    fun guardar(ctx: Context, email: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("email", email.lowercase()).apply()
    }

    fun email(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("email", null)
}
