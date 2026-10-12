package com.example.appmovil

import android.content.Context

/**
 * Punto único para el CRUD de usuarios.
 *  - Con Internet: la operación se hace en MySQL mediante la API y se copia a SQLite.
 *  - Sin Internet (o si el servidor no responde): se hace en SQLite y queda pendiente
 *    de sincronización; el SyncWorker la envía al volver la conexión.
 */
object RepositorioUsuarios {

    sealed class Respuesta {
        /** MySQL confirmó la operación. */
        object EnServidor : Respuesta()

        /** Se guardó en el teléfono y quedó pendiente de sincronizar. */
        data class EnLocal(val motivo: String) : Respuesta()

        /** No se hizo: el dato no es válido (ej. correo ya registrado). */
        data class Rechazado(val msg: String) : Respuesta()
    }

    private const val SIN_CONEXION =
        "Estás sin conexión: se guardó en el teléfono y se sincronizará automáticamente al volver Internet."
    private const val SERVIDOR_NO_RESPONDE =
        "No se pudo contactar al servidor: se guardó en el teléfono y se sincronizará automáticamente."

    // ------------------------------------------------------------------ ingresar

    fun registrar(
        ctx: Context, rut: String, nombres: String, apellidos: String, email: String, clave: String,
        alTerminar: (Respuesta) -> Unit
    ) {
        val db = ConexionDbHelper(ctx)

        // 1) Duplicados contra lo que ya hay en el teléfono (incluye pendientes no enviados)
        when (db.estadoRut(rut)) {
            null -> {}
            Estado.ELIMINAR -> return alTerminar(Respuesta.Rechazado(
                "Ese RUT tiene una eliminación pendiente de sincronizar. Intenta cuando vuelva la conexión."))
            else -> return alTerminar(Respuesta.Rechazado("Ya existe un usuario registrado con el RUT $rut."))
        }
        if (db.emailEnUso(email)) {
            return alTerminar(Respuesta.Rechazado("El correo $email ya está registrado por otro usuario."))
        }

        val guardarLocal = { motivo: String ->
            if (db.insertarPendiente(rut, nombres, apellidos, email, clave)) {
                SyncWorker.programar(ctx)
                ConexionMonitor.notificarCambioLocal()
                alTerminar(Respuesta.EnLocal(motivo))
            } else {
                alTerminar(Respuesta.Rechazado("Ya existe un usuario registrado con el RUT $rut."))
            }
        }

        if (!ConexionMonitor.enLinea) return guardarLocal(SIN_CONEXION)

        // 2) Con conexión: MySQL decide (también revisa duplicados del lado del servidor)
        Api.post(ctx, "registrar.php", mapOf(
            "rut" to rut, "nombres" to nombres, "apellidos" to apellidos, "email" to email, "clave" to clave
        )) { r ->
            when (r) {
                is Resultado.Ok -> {
                    db.guardarSincronizado(Usuario(rut, nombres, apellidos, email))
                    db.guardarCredencial(email, clave)
                    alTerminar(Respuesta.EnServidor)
                }
                is Resultado.Rechazado ->
                    if (r.esDefinitivo) alTerminar(Respuesta.Rechazado(r.msg))
                    else guardarLocal(SERVIDOR_NO_RESPONDE)
                Resultado.FalloRed -> guardarLocal(SERVIDOR_NO_RESPONDE)
            }
        }
    }

    // ------------------------------------------------------------------ modificar

    fun modificar(
        ctx: Context, actual: Usuario, nombres: String, apellidos: String, email: String,
        alTerminar: (Respuesta) -> Unit
    ) {
        val db = ConexionDbHelper(ctx)
        if (db.emailEnUso(email, excluirRut = actual.rut)) {
            return alTerminar(Respuesta.Rechazado("El correo $email ya pertenece a otro usuario."))
        }

        val guardarLocal = { motivo: String ->
            db.modificarLocal(actual.rut, nombres, apellidos, email)
            SyncWorker.programar(ctx)
            ConexionMonitor.notificarCambioLocal()
            alTerminar(Respuesta.EnLocal(motivo))
        }

        // Si la fila tiene cambios sin enviar, se respeta el orden: va a la cola local
        if (!ConexionMonitor.enLinea) return guardarLocal(SIN_CONEXION)
        if (actual.estado != Estado.SINCRONIZADO) {
            return guardarLocal("Se guardó y se enviará al servidor junto con los cambios pendientes de este usuario.")
        }

        Api.post(ctx, "modificar.php", mapOf(
            "rut" to actual.rut, "nombres" to nombres, "apellidos" to apellidos, "email" to email
        )) { r ->
            when (r) {
                is Resultado.Ok -> {
                    db.aplicarModificacionSincronizada(actual.rut, nombres, apellidos, email)
                    alTerminar(Respuesta.EnServidor)
                }
                is Resultado.Rechazado -> when {
                    r.codigo == "NO_EXISTE" -> {
                        db.borrarDefinitivo(actual.rut)
                        alTerminar(Respuesta.Rechazado("Este usuario ya no existe en el servidor."))
                    }
                    r.esDefinitivo -> alTerminar(Respuesta.Rechazado(r.msg))
                    else -> guardarLocal(SERVIDOR_NO_RESPONDE)
                }
                Resultado.FalloRed -> guardarLocal(SERVIDOR_NO_RESPONDE)
            }
        }
    }

    // ------------------------------------------------------------------ eliminar

    fun eliminar(ctx: Context, actual: Usuario, alTerminar: (Respuesta) -> Unit) {
        val db = ConexionDbHelper(ctx)

        val guardarLocal = { motivo: String ->
            db.eliminarLocal(actual.rut)
            SyncWorker.programar(ctx)
            ConexionMonitor.notificarCambioLocal()
            alTerminar(Respuesta.EnLocal(motivo))
        }

        // Nunca llegó al servidor: basta con borrarlo del teléfono
        if (actual.estado == Estado.INSERTAR || actual.estado == Estado.CONFLICTO_INSERTAR) {
            db.eliminarLocal(actual.rut)
            ConexionMonitor.notificarCambioLocal()
            return alTerminar(Respuesta.EnServidor)
        }
        if (!ConexionMonitor.enLinea) return guardarLocal(SIN_CONEXION)
        if (actual.estado != Estado.SINCRONIZADO) {
            return guardarLocal("Se eliminará del servidor junto con los cambios pendientes.")
        }

        Api.post(ctx, "eliminar.php", mapOf("rut" to actual.rut)) { r ->
            when (r) {
                is Resultado.Ok -> {
                    db.borrarDefinitivo(actual.rut)
                    alTerminar(Respuesta.EnServidor)
                }
                else -> guardarLocal(SERVIDOR_NO_RESPONDE)
            }
        }
    }

    // ------------------------------------------------------------------ listar

    /**
     * Con conexión trae la lista desde MySQL y actualiza la copia local.
     * [alTerminar] recibe un mensaje de error si no se pudo consultar el servidor (null si todo bien).
     */
    fun actualizarDesdeServidor(ctx: Context, alTerminar: (String?) -> Unit) {
        if (!ConexionMonitor.enLinea) return alTerminar(null)   // el aviso de "sin conexión" ya se muestra

        Api.get(ctx, "listar.php") { r ->
            if (r is Resultado.Ok) {
                try {
                    val arr = r.json.getJSONArray("usuarios")
                    val lista = (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        Usuario(o.getString("rut"), o.optString("nombres"),
                            o.optString("apellidos"), o.optString("email"))
                    }
                    ConexionDbHelper(ctx).reemplazarDesdeServidor(lista)
                    alTerminar(null)
                } catch (e: Exception) {
                    alTerminar("El servidor envió una respuesta inválida. Se muestran los datos guardados en el teléfono.")
                }
            } else {
                alTerminar("No se pudo consultar MySQL. Se muestran los datos guardados en el teléfono.")
            }
        }
    }
}
