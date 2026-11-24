package com.vibe.musicassistant.service.voice

import android.content.Context
import com.vibe.musicassistant.domain.model.WakeModelState
import com.vibe.musicassistant.service.VibeDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Downloads the small offline English speech model used only to spot "Hey VIBE".
 * HTTPS only, size-capped, SHA-256 verified, and unzipped with path-traversal checks.
 * Stored in no-backup app storage.
 */
class WakeModelManager(context: Context) {
    private val appContext = context.applicationContext
    private val modelDir = File(appContext.noBackupFilesDir, "wake-model")
    private val mutex = Mutex()

    private val _state = MutableStateFlow(if (isReady()) WakeModelState.Ready else WakeModelState.NotDownloaded)
    val state: StateFlow<WakeModelState> = _state.asStateFlow()

    val modelPath: String get() = modelDir.absolutePath

    fun isReady(): Boolean = File(modelDir, READY_MARKER).exists()

    suspend fun download(): Boolean = mutex.withLock {
        if (isReady()) {
            _state.value = WakeModelState.Ready
            return true
        }
        _state.value = WakeModelState.Downloading(0f)
        withContext(Dispatchers.IO) {
            val zip = File(appContext.cacheDir, "wake-model.zip")
            val staging = File(appContext.noBackupFilesDir, "wake-model-staging")
            try {
                fetch(zip)
                staging.deleteRecursively()
                unzip(zip, staging)
                modelDir.deleteRecursively()
                if (!staging.renameTo(modelDir)) throw IllegalStateException("move failed")
                File(modelDir, READY_MARKER).writeText(MODEL_SHA256)
                VibeDiagnostics.log("wake", "Model downloaded and verified")
                _state.value = WakeModelState.Ready
                true
            } catch (e: SecurityException) {
                VibeDiagnostics.log("wake", "Model download rejected: ${e.message}")
                _state.value = WakeModelState.Failed("The downloaded voice model failed its safety check, so it was deleted.")
                false
            } catch (e: Exception) {
                VibeDiagnostics.log("wake", "Model download failed: ${e.javaClass.simpleName} ${e.message}")
                _state.value = WakeModelState.Failed("Download failed. Check your internet and try again.")
                false
            } finally {
                zip.delete()
                staging.deleteRecursively()
            }
        }
    }

    fun delete() {
        modelDir.deleteRecursively()
        _state.value = WakeModelState.NotDownloaded
    }

    private fun fetch(target: File) {
        val connection = URL(MODEL_URL).openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true // HttpURLConnection never follows https -> http.
        try {
            if (connection.responseCode != HttpsURLConnection.HTTP_OK) throw IllegalStateException("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: EXPECTED_SIZE
            if (total > MAX_BYTES) throw SecurityException("too large")
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        read += n
                        if (read > MAX_BYTES) throw SecurityException("too large")
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                        _state.value = WakeModelState.Downloading((read.toFloat() / total).coerceIn(0f, 0.99f))
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (MODEL_SHA256.isNotEmpty() && !hash.equals(MODEL_SHA256, ignoreCase = true)) {
                throw SecurityException("checksum mismatch")
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Extracts [zip] into [dest], dropping the archive's top folder and refusing "../" tricks. */
    private fun unzip(zip: File, dest: File) {
        dest.mkdirs()
        val root = dest.canonicalPath + File.separator
        var unpacked = 0L
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val relative = entry.name.substringAfter('/', "")
                if (relative.isBlank()) continue
                val out = File(dest, relative)
                if (!out.canonicalPath.startsWith(root)) throw SecurityException("bad entry")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { o ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = zis.read(buffer)
                            if (n < 0) break
                            unpacked += n
                            if (unpacked > MAX_UNPACKED_BYTES) throw SecurityException("zip bomb")
                            o.write(buffer, 0, n)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        /** Pinned SHA-256 of vosk-model-small-en-us-0.15.zip (41,205,931 bytes). */
        const val MODEL_SHA256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498"
        const val EXPECTED_SIZE = 41_000_000L
        const val MAX_BYTES = 80_000_000L
        const val MAX_UNPACKED_BYTES = 200_000_000L
        const val READY_MARKER = ".vibe-ready"
    }
}
