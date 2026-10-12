package com.example.appmovil

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Estado de cada fila local respecto de MySQL.
 * La columna VERSION sube con cada cambio local: así el Worker sabe si la fila cambió
 * mientras la estaba enviando, y no la marca como sincronizada por error.
 */
object Estado {
    const val SINCRONIZADO = 0
    const val INSERTAR = 1          // creado sin conexión, falta enviarlo
    const val ELIMINAR = 2          // borrado sin conexión, falta enviarlo
    const val MODIFICAR = 3         // editado sin conexión, falta enviarlo
    const val CONFLICTO_INSERTAR = 4  // el servidor lo rechazó (ej. correo duplicado): no se pierde, espera corrección
    const val CONFLICTO_MODIFICAR = 5

    fun esPendiente(e: Int) = e == INSERTAR || e == ELIMINAR || e == MODIFICAR
    fun esConflicto(e: Int) = e == CONFLICTO_INSERTAR || e == CONFLICTO_MODIFICAR
}

data class Usuario(
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val email: String,
    val estado: Int = Estado.SINCRONIZADO,
    val error: String? = null
)

data class Pendiente(
    val rut: String, val nombres: String, val apellidos: String,
    val email: String, val clave: String?, val estado: Int, val version: Int
)

/**
 * Una sola instancia para toda la app (UI y Worker): evita errores "database is locked"
 * cuando ambos escriben a la vez. Se sigue usando como ConexionDbHelper(contexto).
 */
class ConexionDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "CRUD", null, 2) {

    companion object {
        @Volatile private var instancia: ConexionDbHelper? = null

        operator fun invoke(ctx: Context): ConexionDbHelper =
            instancia ?: synchronized(this) {
                instancia ?: ConexionDbHelper(ctx).also { instancia = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE USUARIOS (RUT TEXT PRIMARY KEY, NOMBRES TEXT, APELLIDOS TEXT, " +
                    "EMAIL TEXT, CLAVE TEXT, ESTADO INTEGER DEFAULT 0, " +
                    "VERSION INTEGER DEFAULT 0, ERROR TEXT)"
        )
        crearCredenciales(db)
    }

    /** Credenciales para iniciar sesión sin conexión. Solo se guarda el hash, nunca la clave. */
    private fun crearCredenciales(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS CREDENCIALES (EMAIL TEXT PRIMARY KEY COLLATE NOCASE, HASH TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE USUARIOS ADD COLUMN VERSION INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE USUARIOS ADD COLUMN ERROR TEXT")
            crearCredenciales(db)
            // Las claves de la versión anterior estaban en texto plano: se pasan a hash
            val c = db.rawQuery("SELECT EMAIL, CLAVE FROM USUARIOS WHERE CLAVE IS NOT NULL AND ESTADO != 2", null)
            while (c.moveToNext()) {
                db.insertWithOnConflict("CREDENCIALES", null, ContentValues().apply {
                    put("EMAIL", c.getString(0)); put("HASH", Seguridad.hash(c.getString(1)))
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            c.close()
            // Ya sincronizados: la clave en texto plano no se necesita más
            db.execSQL("UPDATE USUARIOS SET CLAVE=NULL WHERE ESTADO=0")
        }
    }

    // ------------------------------------------------------------------ consultas

    fun listar(): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        val c = readableDatabase.rawQuery(
            "SELECT RUT, NOMBRES, APELLIDOS, EMAIL, ESTADO, ERROR FROM USUARIOS " +
                    "WHERE ESTADO != ${Estado.ELIMINAR} ORDER BY NOMBRES COLLATE NOCASE, APELLIDOS COLLATE NOCASE",
            null
        )
        while (c.moveToNext()) {
            lista.add(Usuario(c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                c.getInt(4), c.getString(5)))
        }
        c.close()
        return lista
    }

    fun obtener(rut: String): Usuario? {
        val c = readableDatabase.rawQuery(
            "SELECT RUT, NOMBRES, APELLIDOS, EMAIL, ESTADO, ERROR FROM USUARIOS WHERE RUT=?", arrayOf(rut)
        )
        val u = if (c.moveToFirst())
            Usuario(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getString(5))
        else null
        c.close()
        return u
    }

    /** Incluye filas pendientes de eliminar: ese RUT no se puede reutilizar hasta sincronizar. */
    fun estadoRut(rut: String): Int? {
        val c = readableDatabase.rawQuery("SELECT ESTADO FROM USUARIOS WHERE RUT=? COLLATE NOCASE", arrayOf(rut))
        val e = if (c.moveToFirst()) c.getInt(0) else null
        c.close()
        return e
    }

    /** true si otro usuario (distinto de [excluirRut]) ya usa ese correo. */
    fun emailEnUso(email: String, excluirRut: String = ""): Boolean {
        val c = readableDatabase.rawQuery(
            "SELECT 1 FROM USUARIOS WHERE EMAIL=? COLLATE NOCASE AND RUT != ? AND ESTADO != ${Estado.ELIMINAR} LIMIT 1",
            arrayOf(email, excluirRut)
        )
        val existe = c.moveToFirst()
        c.close()
        return existe
    }

    fun pendientes(): List<Pendiente> {
        val lista = mutableListOf<Pendiente>()
        val c = readableDatabase.rawQuery(
            "SELECT RUT, NOMBRES, APELLIDOS, EMAIL, CLAVE, ESTADO, VERSION FROM USUARIOS " +
                    "WHERE ESTADO IN (${Estado.INSERTAR},${Estado.ELIMINAR},${Estado.MODIFICAR})",
            null
        )
        while (c.moveToNext()) {
            lista.add(Pendiente(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "",
                c.getString(3) ?: "", c.getString(4), c.getInt(5), c.getInt(6)))
        }
        c.close()
        return lista
    }

    fun contarPendientes(): Int = contar("ESTADO IN (${Estado.INSERTAR},${Estado.ELIMINAR},${Estado.MODIFICAR})")
    fun contarConflictos(): Int = contar("ESTADO IN (${Estado.CONFLICTO_INSERTAR},${Estado.CONFLICTO_MODIFICAR})")

    private fun contar(where: String): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM USUARIOS WHERE $where", null)
        val n = if (c.moveToFirst()) c.getInt(0) else 0
        c.close()
        return n
    }

    // ------------------------------------------------------------------ cambios hechos en el teléfono

    /** Registro sin conexión: queda pendiente de enviar. */
    fun insertarPendiente(rut: String, nombres: String, apellidos: String, email: String, clave: String): Boolean {
        val v = ContentValues().apply {
            put("RUT", rut); put("NOMBRES", nombres); put("APELLIDOS", apellidos)
            put("EMAIL", email); put("CLAVE", clave)   // se borra apenas el servidor confirma
            put("ESTADO", Estado.INSERTAR); put("VERSION", 1)
        }
        val ok = writableDatabase.insert("USUARIOS", null, v) != -1L
        if (ok) guardarCredencial(email, clave)
        return ok
    }

    /** Registro confirmado por el servidor: se guarda como copia local ya sincronizada. */
    fun guardarSincronizado(u: Usuario) {
        writableDatabase.insertWithOnConflict("USUARIOS", null, ContentValues().apply {
            put("RUT", u.rut); put("NOMBRES", u.nombres); put("APELLIDOS", u.apellidos)
            put("EMAIL", u.email); putNull("CLAVE"); put("ESTADO", Estado.SINCRONIZADO)
            put("VERSION", 0); putNull("ERROR")
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Edición sin conexión. Si la fila aún no existía en el servidor sigue siendo una inserción
     * (con los datos nuevos); si ya existía, queda como modificación pendiente.
     */
    fun modificarLocal(rut: String, nombres: String, apellidos: String, email: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val actual = obtener(rut) ?: return
            val nuevoEstado = when (actual.estado) {
                Estado.INSERTAR, Estado.CONFLICTO_INSERTAR -> Estado.INSERTAR
                else -> Estado.MODIFICAR
            }
            db.execSQL(
                "UPDATE USUARIOS SET NOMBRES=?, APELLIDOS=?, EMAIL=?, ESTADO=?, ERROR=NULL, " +
                        "VERSION=VERSION+1 WHERE RUT=?",
                arrayOf<Any>(nombres, apellidos, email, nuevoEstado, rut)
            )
            cambiarEmailCredencial(actual.email, email)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Edición que el servidor ya confirmó (se hizo con conexión). */
    fun aplicarModificacionSincronizada(rut: String, nombres: String, apellidos: String, email: String) {
        val anterior = obtener(rut)?.email
        writableDatabase.execSQL(
            "UPDATE USUARIOS SET NOMBRES=?, APELLIDOS=?, EMAIL=?, ESTADO=${Estado.SINCRONIZADO}, ERROR=NULL, " +
                    "VERSION=VERSION+1 WHERE RUT=?",
            arrayOf(nombres, apellidos, email, rut)
        )
        if (anterior != null) cambiarEmailCredencial(anterior, email)
    }

    /**
     * Eliminación sin conexión. Si el usuario nunca llegó al servidor se borra de inmediato
     * (no hay nada que enviar); si existía allá, queda marcado para eliminarlo al sincronizar.
     */
    fun eliminarLocal(rut: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val actual = obtener(rut) ?: return
            if (actual.estado == Estado.INSERTAR || actual.estado == Estado.CONFLICTO_INSERTAR) {
                db.execSQL("DELETE FROM USUARIOS WHERE RUT=?", arrayOf(rut))
            } else {
                db.execSQL(
                    "UPDATE USUARIOS SET ESTADO=${Estado.ELIMINAR}, ERROR=NULL, VERSION=VERSION+1 WHERE RUT=?",
                    arrayOf(rut)
                )
            }
            borrarCredencial(actual.email)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Eliminación ya confirmada por el servidor. */
    fun borrarDefinitivo(rut: String) {
        obtener(rut)?.let { borrarCredencial(it.email) }
        writableDatabase.execSQL("DELETE FROM USUARIOS WHERE RUT=?", arrayOf(rut))
    }

    // ------------------------------------------------------------------ usados por el Worker

    /**
     * Marca como sincronizado SOLO si la fila no cambió mientras se enviaba (misma VERSION).
     * Casos borde:
     *  - se editó durante el envío de una inserción: pasa a MODIFICAR para enviar los datos nuevos.
     *  - se editó durante el envío de una modificación: sigue pendiente y se reenvía.
     *  - se borró durante el envío de una inserción: se deja una marca ELIMINAR para borrarlo del servidor.
     */
    fun confirmarSincronizado(p: Pendiente) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (p.estado == Estado.ELIMINAR) {
                db.execSQL("DELETE FROM USUARIOS WHERE RUT=? AND ESTADO=${Estado.ELIMINAR}", arrayOf(p.rut))
            } else {
                val filas = db.compileStatement(
                    "UPDATE USUARIOS SET ESTADO=${Estado.SINCRONIZADO}, CLAVE=NULL, ERROR=NULL " +
                            "WHERE RUT=? AND VERSION=?"
                ).apply { bindString(1, p.rut); bindLong(2, p.version.toLong()) }.executeUpdateDelete()

                if (filas == 0 && p.estado == Estado.INSERTAR) {
                    val ahora = obtener(p.rut)
                    when {
                        ahora == null -> db.insert("USUARIOS", null, ContentValues().apply {
                            put("RUT", p.rut); put("NOMBRES", p.nombres); put("APELLIDOS", p.apellidos)
                            put("EMAIL", p.email); put("ESTADO", Estado.ELIMINAR); put("VERSION", 1)
                        })
                        ahora.estado == Estado.INSERTAR ->
                            db.execSQL("UPDATE USUARIOS SET ESTADO=${Estado.MODIFICAR}, CLAVE=NULL WHERE RUT=?",
                                arrayOf(p.rut))
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** El servidor rechazó el cambio para siempre (ej. correo duplicado): se conserva y se avisa. */
    fun marcarConflicto(p: Pendiente, mensaje: String) {
        val nuevo = if (p.estado == Estado.INSERTAR) Estado.CONFLICTO_INSERTAR else Estado.CONFLICTO_MODIFICAR
        val st = writableDatabase.compileStatement(
            "UPDATE USUARIOS SET ESTADO=?, ERROR=? WHERE RUT=? AND VERSION=?"
        )
        st.bindLong(1, nuevo.toLong()); st.bindString(2, mensaje)
        st.bindString(3, p.rut); st.bindLong(4, p.version.toLong())
        st.executeUpdateDelete()   // si la versión cambió, el usuario ya lo corrigió: se reenviará
    }

    /**
     * Copia local de lo que hay en MySQL. Las filas con cambios pendientes o en conflicto
     * no se tocan, para no perder lo hecho sin conexión.
     */
    fun reemplazarDesdeServidor(lista: List<Usuario>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val delServidor = lista.associateBy { it.rut }
            val locales = mutableMapOf<String, Pair<Int, String>>()   // rut -> (estado, email)
            val c = db.rawQuery("SELECT RUT, ESTADO, EMAIL FROM USUARIOS", null)
            while (c.moveToNext()) locales[c.getString(0)] = c.getInt(1) to (c.getString(2) ?: "")
            c.close()

            // Borrados en el servidor por otro dispositivo
            for ((rut, datos) in locales) {
                if (datos.first == Estado.SINCRONIZADO && rut !in delServidor) {
                    db.execSQL("DELETE FROM USUARIOS WHERE RUT=?", arrayOf(rut))
                    borrarCredencial(datos.second)
                }
            }
            for (u in lista) {
                val local = locales[u.rut]
                if (local == null) {
                    db.insert("USUARIOS", null, ContentValues().apply {
                        put("RUT", u.rut); put("NOMBRES", u.nombres); put("APELLIDOS", u.apellidos)
                        put("EMAIL", u.email); put("ESTADO", Estado.SINCRONIZADO); put("VERSION", 0)
                    })
                } else if (local.first == Estado.SINCRONIZADO) {
                    db.execSQL("UPDATE USUARIOS SET NOMBRES=?, APELLIDOS=?, EMAIL=? WHERE RUT=?",
                        arrayOf(u.nombres, u.apellidos, u.email, u.rut))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ------------------------------------------------------------------ login sin conexión

    fun guardarCredencial(email: String, clave: String) {
        writableDatabase.insertWithOnConflict("CREDENCIALES", null, ContentValues().apply {
            put("EMAIL", email.lowercase()); put("HASH", Seguridad.hash(clave))
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun borrarCredencial(email: String) {
        writableDatabase.execSQL("DELETE FROM CREDENCIALES WHERE EMAIL=?", arrayOf(email))
    }

    private fun cambiarEmailCredencial(anterior: String, nuevo: String) {
        if (anterior.equals(nuevo, ignoreCase = true)) return
        writableDatabase.execSQL("DELETE FROM CREDENCIALES WHERE EMAIL=?", arrayOf(nuevo))
        writableDatabase.execSQL("UPDATE CREDENCIALES SET EMAIL=? WHERE EMAIL=?", arrayOf(nuevo.lowercase(), anterior))
    }

    /** Login offline: compara contra el hash guardado. */
    fun validarLogin(email: String, clave: String): Boolean {
        val c = readableDatabase.rawQuery("SELECT HASH FROM CREDENCIALES WHERE EMAIL=?", arrayOf(email))
        val hash = if (c.moveToFirst()) c.getString(0) else null
        c.close()
        return Seguridad.verificar(clave, hash)
    }
}
