package com.shirsh.flutter_doc_scanner

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener

class FlutterDocScannerPlugin : MethodCallHandler, ActivityResultListener, FlutterPlugin,
    ActivityAware {
    private var channel: MethodChannel? = null
    private var pluginBinding: FlutterPluginBinding? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var applicationContext: Application? = null
    private val CHANNEL = "flutter_doc_scanner"
    private var activity: Activity? = null
    private val TAG = FlutterDocScannerPlugin::class.java.simpleName

    private val REQUEST_CODE_SCAN_IMAGES = 215512
    private val REQUEST_CODE_SCAN_PDF = 216612
    private lateinit var resultChannel: Result


    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "getScannedDocumentAsImages" -> {
                val arguments = call.arguments as? Map<*, *>
                val page = (arguments?.get("page") as? Int)?.coerceAtLeast(1) ?: 4
                resultChannel = result
                startDocumentScanImages(page)
            }

            "getScannedDocumentAsPdf" -> {
                val arguments = call.arguments as? Map<*, *>
                val page = (arguments?.get("page") as? Int)?.coerceAtLeast(1) ?: 4
                resultChannel = result
                startDocumentScanPDF(page)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    private fun startDocumentScanImages(page: Int = 4) {
        val options =
            GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(page)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                ).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        val scanner = GmsDocumentScanning.getClient(options)
        val task: Task<IntentSender>? = activity?.let { scanner.getStartScanIntent(it) }
        task?.addOnSuccessListener { intentSender ->
            val intent = IntentSenderRequest.Builder(intentSender).build().intentSender
            try {

                startIntentSenderForResult(
                    activity!!, intent, REQUEST_CODE_SCAN_IMAGES, null, 0, 0, 0, null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting document scan for Images", e)
            }
        }?.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get start scan intent for Images", e)
        }
    }

    private fun startDocumentScanPDF(page: Int = 4) {
        val options =
            GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(page)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                ).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        val scanner = GmsDocumentScanning.getClient(options)
        val task: Task<IntentSender>? = activity?.let { scanner.getStartScanIntent(it) }
        task?.addOnSuccessListener { intentSender ->
            val intent = IntentSenderRequest.Builder(intentSender).build().intentSender
            try {

                startIntentSenderForResult(
                    activity!!, intent, REQUEST_CODE_SCAN_PDF, null, 0, 0, 0, null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting document scan for PDF", e)
            }
        }?.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get start scan intent for PDF", e)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            REQUEST_CODE_SCAN_IMAGES -> {
                if (resultCode == Activity.RESULT_OK) {
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    scanningResult?.pages?.let { pages ->
                        val uriList = pages.map { page ->
                            Uri.parse(page.imageUri.toString()).path ?: page.imageUri.toString()
                        }
                        resultChannel.success(
                            mapOf(
                                "imagesUri" to uriList,
                                "pageCount" to pages.size,
                            )
                        )
                    } ?: resultChannel.error("SCAN_FAILED", "No image results returned", null)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(null)
                }
            }

            REQUEST_CODE_SCAN_PDF -> {
                if (resultCode == Activity.RESULT_OK) {
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    scanningResult?.pdf?.let { pdf ->
                        val pdfUriPath = Uri.parse(pdf.uri.toString()).path ?: pdf.uri.toString()
                        val pageCount = pdf.pageCount
                        resultChannel.success(
                            mapOf(
                                "pdfUri" to pdfUriPath,
                                "pageCount" to pageCount,
                            )
                        )
                    } ?: resultChannel.error("SCAN_FAILED", "No PDF result returned", null)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(null)
                }
            }
        }
        return false
    }

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        pluginBinding = binding
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        pluginBinding = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    private fun createPluginSetup(
        messenger: BinaryMessenger,
        applicationContext: Application?,
        activityBinding: ActivityPluginBinding?
    ) {
        this.activity = activityBinding!!.activity
        this.applicationContext = applicationContext
        channel = MethodChannel(messenger, CHANNEL)
        channel!!.setMethodCallHandler(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        activityBinding?.addActivityResultListener(this) // Register the plugin as an ActivityResultListener
        createPluginSetup(
            pluginBinding!!.binaryMessenger,
            pluginBinding!!.applicationContext as Application,
            activityBinding
        )
    }

    override fun onDetachedFromActivity() {
        activityBinding?.removeActivityResultListener(this) // Unregister the plugin as an ActivityResultListener
        activityBinding = null

    }
}