package com.example.whatsubb.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Neemt een gesprek op als AAC (.aac, mono, 16 kHz, 32 kbps: ca. 15 MB per uur).
 * Opnames staan in de privé-opslag van de app en worden bij het starten van de app opgeruimd.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    fun start(): File {
        val file = File(recordingsDir(context), "gesprek_${System.currentTimeMillis()}.aac")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(16_000)
            r.setAudioEncodingBitRate(32_000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            file.delete()
            throw e
        }
        recorder = r
        return file
    }

    /** @return false als de opname mislukt is (bijv. te kort). */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            false
        } finally {
            r.release()
        }
    }

    companion object {
        const val MIME_TYPE = "audio/aac"

        fun recordingsDir(context: Context): File = File(context.filesDir, "opnames").apply { mkdirs() }

        fun deleteAll(context: Context) {
            recordingsDir(context).listFiles()?.forEach { it.delete() }
        }
    }
}
