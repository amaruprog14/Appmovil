package com.example.appmovil

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Pendiente(
    val rut: String, val nombres: String, val apellidos: String,
    val email: String, val clave: String, val estado: Int
)

class ConexionDbHelper(context: Context) :
    SQLiteOpenHelper(context, "CRUD", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE USUARIOS (RUT TEXT PRIMARY KEY, NOMBRES TEXT, APELLIDOS TEXT, " +
                    "EMAIL TEXT, CLAVE TEXT, ESTADO INTEGER DEFAULT 1)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    /** Devuelve false si el RUT ya existe. */
    fun insertar(rut: String, nombres: String, apellidos: String, email: String, clave: String): Boolean {
        val db = writableDatabase
        // Si estaba marcado para eliminar, se reemplaza por el nuevo registro
        db.execSQL("DELETE FROM USUARIOS WHERE RUT=? AND ESTADO=2", arrayOf(rut))
        val v = ContentValues().apply {
            put("RUT", rut); put("NOMBRES", nombres); put("APELLIDOS", apellidos)
            put("EMAIL", email); put("CLAVE", clave); put("ESTADO", 1)
        }
        return db.insert("USUARIOS", null, v) != -1L
    }

    fun actualizar(rut: String, nombres: String, apellidos: String, email: String) {
        writableDatabase.execSQL(
            "UPDATE USUARIOS SET NOMBRES=?, APELLIDOS=?, EMAIL=?, ESTADO=1 WHERE RUT=?",
            arrayOf(nombres, apellidos, email, rut)
        )
    }

    fun marcarEliminado(rut: String) {
        writableDatabase.execSQL("UPDATE USUARIOS SET ESTADO=2 WHERE RUT=?", arrayOf(rut))
    }

    fun listar(): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        val c = readableDatabase.rawQuery(
            "SELECT RUT, NOMBRES, APELLIDOS, EMAIL FROM USUARIOS WHERE ESTADO != 2 ORDER BY NOMBRES", null
        )
        while (c.moveToNext()) {
            lista.add(Usuario(c.getString(0), c.getString(1), c.getString(2), c.getString(3)))
        }
        c.close()
        return lista
    }

    fun pendientes(): List<Pendiente> {
        val lista = mutableListOf<Pendiente>()
        val c = readableDatabase.rawQuery(
            "SELECT RUT, NOMBRES, APELLIDOS, EMAIL, CLAVE, ESTADO FROM USUARIOS WHERE ESTADO IN (1,2)", null
        )
        while (c.moveToNext()) {
            lista.add(
                Pendiente(c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getInt(5))
            )
        }
        c.close()
        return lista
    }

    fun marcarSincronizado(rut: String) {
        writableDatabase.execSQL("UPDATE USUARIOS SET ESTADO=0 WHERE RUT=?", arrayOf(rut))
    }

    fun borrarDefinitivo(rut: String) {
        writableDatabase.execSQL("DELETE FROM USUARIOS WHERE RUT=?", arrayOf(rut))
    }
    /** Login offline: true si existe un usuario local con ese email y clave. */
    fun validarLogin(email: String, clave: String): Boolean {
        val c = readableDatabase.rawQuery(
            "SELECT 1 FROM USUARIOS WHERE EMAIL=? AND CLAVE=? AND ESTADO != 2 LIMIT 1",
            arrayOf(email, clave)
        )
        val existe = c.moveToFirst()
        c.close()
        return existe
    }
}