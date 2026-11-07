package com.katchy.focuslive.ui.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fotos de notas: se guardan reducidas dentro de la app, así no dependen de la galería. */
object NoteImages {
    private const val MAX_SIDE = 1600

    private fun dir(context: Context) = File(context.filesDir, "note_images").apply { mkdirs() }

    /** Copia la imagen elegida, reducida a 1600 px como máximo, y devuelve su ruta; null si falla. */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val file = File(dir(context), "${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 86, it) }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** Borra una foto solo si vive en la carpeta de la app. */
    fun delete(context: Context, path: String) {
        val file = File(path)
        if (file.parentFile?.canonicalPath == dir(context).canonicalPath) file.delete()
    }
}
