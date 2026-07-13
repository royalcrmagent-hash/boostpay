package com.example.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.example.core.network.NetworkClient
import com.example.core.security.DeviceIntegrityDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val isMandatory: Boolean,
    val apkUrl: String,
    val sha256: String,
    val signature: String?,
    val minSupportedVersion: Int
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * Secure In-App Update Manager for Wallet App.
 * Incorporates:
 * - HTTPS & Certificate Pinning (via NetworkClient)
 * - Anti-Rollback (minSupportedVersion)
 * - SHA-256 Hash Verification
 * - Root/Frida Check before installing
 * - Mandatory vs Flexible updates
 */
class InAppUpdateManager(private val context: Context) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    // Secure HTTP client configured with Certificate Pinning for defense against MITM attacks.
    private val client = NetworkClient.secureOkHttpClient

    suspend fun checkForUpdates(updateApiUrl: String) {
        _updateState.value = UpdateState.Checking
        try {
            val request = Request.Builder().url(updateApiUrl).build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                
                val info = UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    isMandatory = json.getBoolean("mandatory"),
                    apkUrl = json.getString("apkUrl"),
                    sha256 = json.getString("sha256"),
                    signature = json.optString("signature"),
                    minSupportedVersion = json.getInt("minSupportedVersion")
                )

                val currentVersionCode = getAppVersionCode()

                // Anti-Rollback & Version matching
                if (currentVersionCode < info.minSupportedVersion) {
                    // Force mandatory update if we are below the minimum supported version
                    _updateState.value = UpdateState.UpdateAvailable(info.copy(isMandatory = true))
                } else if (currentVersionCode < info.versionCode) {
                    _updateState.value = UpdateState.UpdateAvailable(info)
                } else {
                    _updateState.value = UpdateState.Idle
                }
            } else {
                _updateState.value = UpdateState.Error("Failed to check for updates: ${response.code}")
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Unknown error while checking updates")
        }
    }

    suspend fun downloadAndInstallUpdate(info: UpdateInfo) {
        // Enforce Integrity before downloading/installing update (Prevent extraction of secure APK by attackers)
        if (DeviceIntegrityDetector.isDeviceRooted() || DeviceIntegrityDetector.isHookFrameworkDetected()) {
            _updateState.value = UpdateState.Error("Security Violation: Updates cannot be installed on a compromised device.")
            return
        }

        _updateState.value = UpdateState.Downloading(0)
        try {
            val request = Request.Builder().url(info.apkUrl).build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            
            if (!response.isSuccessful) throw Exception("Failed to download APK: ${response.code}")

            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val apkFile = File(updateDir, "wallet_update_v${info.versionCode}.apk")

            val body = response.body ?: throw Exception("Empty body received")
            val contentLength = body.contentLength()
            val source = body.source()
            val sink = FileOutputStream(apkFile)

            var bytesRead: Long = 0
            val buffer = ByteArray(8192)
            var read: Int

            withContext(Dispatchers.IO) {
                while (source.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    bytesRead += read
                    if (contentLength > 0) {
                        val progress = ((bytesRead * 100) / contentLength).toInt()
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                }
                sink.flush()
                sink.close()
                source.close()
            }

            // --- SECURITY ENFORCEMENT: SHA-256 Verification ---
            val fileHash = calculateSHA256(apkFile)
            if (!fileHash.equals(info.sha256, ignoreCase = true)) {
                apkFile.delete() // Destroy tampered/corrupted file immediately
                throw Exception("Security Verification Failed: SHA-256 hash mismatch. File has been tampered with or corrupted.")
            }

            _updateState.value = UpdateState.Downloaded(apkFile)
            installApk(apkFile)

        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Download failed")
        }
    }

    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = file.inputStream()
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } > 0) {
            digest.update(buffer, 0, read)
        }
        inputStream.close()
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun installApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to initiate installer: ${e.message}")
        }
    }
}
