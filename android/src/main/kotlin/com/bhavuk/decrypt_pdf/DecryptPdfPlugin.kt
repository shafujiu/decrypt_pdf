package com.bhavuk.decrypt_pdf

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File
import java.io.IOException
import android.util.Log
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

import java.io.RandomAccessFile
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy

/** DecryptPdfPlugin */
class DecryptPdfPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel: MethodChannel
  private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null

  companion object {
    private const val TAG = "DecryptPdfPlugin"

    // Error codes
    private const val ERROR_FILE_NOT_FOUND = "FILE_NOT_FOUND"
    private const val ERROR_INVALID_PASSWORD = "INVALID_PASSWORD"
    private const val ERROR_PDF_CORRUPTED = "PDF_CORRUPTED"
    private const val ERROR_UNSUPPORTED_ENCRYPTION = "UNSUPPORTED_ENCRYPTION"
    private const val ERROR_INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS"
    private const val ERROR_OUT_OF_MEMORY = "OUT_OF_MEMORY"
    private const val ERROR_IO = "IO_ERROR"
    private const val ERROR_UNKNOWN = "UNKNOWN_ERROR"
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    this.flutterPluginBinding = flutterPluginBinding
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "decrypt_pdf")
    channel.setMethodCallHandler(this)
    Log.d(TAG, "DecryptPdfPlugin attached to engine.")

    // Initialize PDFBox - corrected package path
    PDFBoxResourceLoader.init(flutterPluginBinding.applicationContext)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    Log.d(TAG, "onMethodCall: ${call.method}")
    when (call.method) {
      "getPlatformVersion" -> {
        result.success("Android ${android.os.Build.VERSION.RELEASE}")
      }
      "openPdf" -> {
        val filePath = call.argument<String>("filePath")
        val password = call.argument<String>("password")

        if (filePath == null || password == null) {
          Log.e(TAG, "openPdf: Invalid arguments - filePath or password is null")
          result.error("INVALID_ARGUMENTS", "File path or password is null", null)
          return
        }
        Log.d(TAG, "openPdf: filePath=$filePath")

        Thread {
          var document: PDDocument? = null
          try {
            val file = File(filePath)
            if (!file.exists()) {
              Log.e(TAG, "openPdf: File not found at path: $filePath")
              result.error("FILE_NOT_FOUND", "PDF file not found at path: $filePath", null)
              return@Thread
            }

            // Load document with password
            document = PDDocument.load(file, password)

            // Remove encryption (fixed null safety issue)
            document.isAllSecurityToBeRemoved = true

            // Create temp file
            val cacheDir = flutterPluginBinding?.applicationContext?.cacheDir
            if (cacheDir == null) {
              Log.e(TAG, "openPdf: Could not get cache directory")
              result.error("CACHE_DIR_ERROR", "Could not get cache directory", null)
              return@Thread
            }
            val tempFile = File.createTempFile("decrypted_pdf_", ".pdf", cacheDir)

            // Save decrypted document
            document.save(tempFile)
            Log.d(TAG, "openPdf: Document saved successfully to ${tempFile.absolutePath}")

            // Return path to decrypted file
            result.success(tempFile.absolutePath)

          } catch (e: InvalidPasswordException) {
            Log.e(TAG, "openPdf: Invalid password", e)
            result.error(ERROR_INVALID_PASSWORD, "Incorrect password for PDF", null)
          } catch (e: IOException) {
            Log.e(TAG, "openPdf: IO error", e)
            result.error(ERROR_IO, "Failed to process PDF: ${e.message}", null)
          } catch (e: OutOfMemoryError) {
            Log.e(TAG, "openPdf: Out of memory", e)
            result.error(ERROR_OUT_OF_MEMORY, "PDF file too large to process", null)
          } catch (e: Exception) {
            Log.e(TAG, "openPdf: Unexpected error", e)
            result.error(ERROR_UNKNOWN, "Failed to process PDF: ${e.message}", null)
          } finally {
            document?.close()
          }
        }.start()
      }
      "getPdfAsBase64" -> {
        val filePath = call.argument<String>("filePath")
        val password = call.argument<String>("password")

        if (filePath == null || password == null) {
          result.error("INVALID_ARGUMENTS", "File path or password is null", null)
          return
        }

        Thread {
          try {
            val file = File(filePath)
            if (!file.exists()) {
              result.error("FILE_NOT_FOUND", "PDF file not found", null)
              return@Thread
            }

            // Load and decrypt document
            val document = PDDocument.load(file, password)
            document.isAllSecurityToBeRemoved = true

            val byteArrayOutputStream = ByteArrayOutputStream()
            document.save(byteArrayOutputStream)
            document.close()

            val base64 = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
            result.success(base64)

          } catch (e: InvalidPasswordException) {
            Log.e(TAG, "getPdfAsBase64: Invalid password", e)
            result.error(ERROR_INVALID_PASSWORD, "Incorrect password for PDF", null)
          } catch (e: IOException) {
            Log.e(TAG, "getPdfAsBase64: IO error", e)
            result.error(ERROR_IO, "IO error: ${e.message}", null)
          } catch (e: OutOfMemoryError) {
            Log.e(TAG, "getPdfAsBase64: Out of memory", e)
            result.error(ERROR_OUT_OF_MEMORY, "PDF too large to convert to Base64", null)
          } catch (e: Exception) {
            Log.e(TAG, "getPdfAsBase64: Unexpected error", e)
            result.error(ERROR_UNKNOWN, "Unexpected error: ${e.message}", null)
          }
        }.start()
      }


      "isPdfProtected" -> {
        val filePath = call.argument<String>("filePath")
        if (filePath == null) {
          result.error("INVALID_ARGUMENTS", "File path is null", null)
          return
        }

        Thread {
          var document: PDDocument? = null
          try {
            val file = File(filePath)
            if (!file.exists()) {
              result.error("FILE_NOT_FOUND", "PDF file not found", null)
              return@Thread
            }

            // Try loading without password
            try {
              document = PDDocument.load(file, "")
              // Fixed null safety issue by using safe call
              val isProtected = document?.isEncrypted ?: false
              result.success(isProtected)
            } catch (e: InvalidPasswordException) {
              // Needs password
              result.success(true)
            }

          } catch (e: Exception) {
            Log.e(TAG, "isPdfProtected: Error", e)
            result.error("ERROR", "Failed to check PDF protection: ${e.message}", null)
          } finally {
            document?.close()
          }
        }.start()
      }
      "encryptPdf" -> {
          val filePath = call.argument<String>("filePath")
          val ownerPassword = call.argument<String>("ownerPassword")
          val userPassword = call.argument<String>("userPassword")

          if (filePath == null || ownerPassword == null || userPassword == null) {
              result.error("INVALID_ARGUMENTS", "File path or password is null", null)
              return
          }

          Thread {
              encryptPdf(filePath, ownerPassword, userPassword, result)
          }.start()
      }
      "decryptPdf" -> {
          val filePath = call.argument<String>("filePath")
          val password = call.argument<String>("password")

          if (filePath == null || password == null) {
              result.error("INVALID_ARGUMENTS", "File path or password is null", null)
              return
          }

          Thread {
              decryptPdf(filePath, password, result)
          }.start()
      }
      "isEncrypted" -> {
          val filePath = call.argument<String>("filePath")
          if (filePath == null) {
              result.error("INVALID_ARGUMENTS", "File path is null", null)
              return
          }

          Thread {
            isPdfEncrypted(filePath, result)
          }.start()
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    flutterPluginBinding = null
  }

  private fun encryptPdf(filePath: String, ownerPassword: String, userPassword: String, result: Result) {
    try {
      val file = File(filePath)
      if (!file.exists()) {
        result.error(ERROR_FILE_NOT_FOUND, "PDF file not found at path: $filePath", null)
        return
      }
      
      PDDocument.load(file).use { document ->
        if (document.isEncrypted) {
          result.error(ERROR_PDF_CORRUPTED, "PDF file is already encrypted", null)
          return@use
        }
        val accessPermission = AccessPermission()
        val protectionPolicy = StandardProtectionPolicy(ownerPassword, userPassword, accessPermission).apply {
          encryptionKeyLength = 128
          permissions = accessPermission
        }
        document.protect(protectionPolicy)
        document.save(file)
        result.success(true)
      }
    } catch (e: InvalidPasswordException) {
      result.error(ERROR_INVALID_PASSWORD, "Incorrect password for PDF", null)
    } catch (e: IOException) {
      result.error(ERROR_IO, "Failed to process PDF: ${e.message}", null)
    } catch (e: OutOfMemoryError) {
      result.error(ERROR_OUT_OF_MEMORY, "PDF file too large to process", null)
    } catch (e: Exception) {
      result.error(ERROR_UNKNOWN, "Failed to process PDF: ${e.message}", null)
    }
  }

  private fun decryptPdf(filePath: String, password: String, result: Result) {
    try {
      val file = File(filePath)
      if (!file.exists()) {
        result.error(ERROR_FILE_NOT_FOUND, "PDF file not found at path: $filePath", null)
        return
      }
      
      PDDocument.load(file, password).use { document ->
        if (document.isEncrypted) {
          document.isAllSecurityToBeRemoved = true
          document.save(file)
        }
        result.success(true)
      }
    } catch (e: InvalidPasswordException) {
      result.error(ERROR_INVALID_PASSWORD, "Incorrect password for PDF", null)
    } catch (e: IOException) {
      result.error(ERROR_IO, "Failed to process PDF: ${e.message}", null)
    } catch (e: OutOfMemoryError) {
      result.error(ERROR_OUT_OF_MEMORY, "PDF file too large to process", null)
    } catch (e: Exception) {
      result.error(ERROR_UNKNOWN, "Failed to process PDF: ${e.message}", null)
    }
  }

  private fun isPdfEncrypted(filePath: String, result: Result) {
    val file = File(filePath)
    if (!file.exists()) {
        result.error("FILE_NOT_FOUND", "File does not exist: $filePath", null)
        return
    }

    RandomAccessFile(file, "r").use { raf ->
        val fileLength = raf.length()
        // PDF trailer 通常在文件结尾，所以只读最后 4KB 足够
        val readSize = if (fileLength > 4096) 4096 else fileLength.toInt()
        raf.seek(fileLength - readSize)

        val buffer = ByteArray(readSize)
        raf.readFully(buffer)

        // 允许解码失败的情况下最大限度恢复字符串
        val tailString = buffer.toString(Charsets.UTF_8)

        // 查找 trailer
        val trailerIndex = tailString.lastIndexOf("trailer")
        if (trailerIndex != -1) {
            val dictStart = tailString.indexOf("<<", trailerIndex)
            val dictEnd = tailString.indexOf(">>", dictStart)
            if (dictStart != -1 && dictEnd != -1) {
                val trailerDict = tailString.substring(dictStart, dictEnd + 2)
                if (trailerDict.contains("/Encrypt")) {
                    result.success(true)
                    return
                }
            }
        }

        // fallback：直接扫描尾部字符串是否包含 /Encrypt
        if (tailString.contains("/Encrypt")) {
            result.success(true)
            return
        }

        result.success(false)
    }
  } 

}
