package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

object ApkExporter {
    private const val TAG = "ApkExporter"

    fun exportAndShareApk(context: Context) {
        try {
            var sourceApkPath = context.applicationInfo.publicSourceDir
            var sourceFile = File(sourceApkPath)
            if (!sourceFile.exists()) {
                sourceApkPath = context.applicationInfo.sourceDir
                sourceFile = File(sourceApkPath)
            }
            
            if (!sourceFile.exists()) {
                // Secondary fallback: search package resource path manually
                val packageCodePath = context.packageCodePath
                sourceFile = File(packageCodePath)
            }

            if (!sourceFile.exists()) {
                Toast.makeText(context, "Jarvis source APK not found! Check app compile status.", Toast.LENGTH_SHORT).show()
                return
            }

            // Copy to Cache for secure FileProvider sharing
            val cacheApk = File(context.cacheDir, "Jarvis.apk")
            if (cacheApk.exists()) {
                cacheApk.delete()
            }
            sourceFile.copyTo(cacheApk)

            // Try saving to public Download Folder
            saveToPublicDownloadFolder(context, sourceFile)

            // Share via Intent using FileProvider so users can directly send manually to WhatsApp or install it
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share & Install Jarvis").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting APK", e)
            Toast.makeText(context, "Export Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveToPublicDownloadFolder(context: Context, sourceFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Save using MediaStore Downloads for Android 10+
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "Jarvis.apk")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "Jarvis.apk successfully saved to Downloads using MediaStore!")
                    Toast.makeText(context, "Jarvis.apk downloaded to your 'Download' folder!", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Fallback for legacy storage
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && (downloadDir.exists() || downloadDir.mkdirs())) {
                val destFile = File(downloadDir, "Jarvis.apk")
                if (destFile.exists()) {
                    destFile.delete()
                }
                sourceFile.copyTo(destFile)
                Log.d(TAG, "Jarvis.apk saved to Legacy Downloads: ${destFile.absolutePath}")
                Toast.makeText(context, "Jarvis.apk saved in Download Folder!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving to public Download Folder: ${e.message}", e)
        }
    }
}
