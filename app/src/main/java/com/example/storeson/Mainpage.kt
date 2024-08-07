package com.example.storeson

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.nio.ByteBuffer


class Mainpage : AppCompatActivity() {

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainpage)
        val buttonScan = findViewById<Button>(R.id.buttonScan)
        buttonScan.setOnClickListener {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    // kamerayı başlatma
    private fun startCamera() {
        val previewView = findViewById<PreviewView>(R.id.previewView)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrResult ->
                        runOnUiThread {
                            // QR kod tarama sonucu burada işlenecek
                            // Örneğin, bir Toast gösterebilirsiniz
                            // Toast.makeText(this, "QR Kod: $qrResult", Toast.LENGTH_SHORT).show()
                            //Toast.makeText(applicationContext,"${qrResult}",Toast.LENGTH_SHORT).show()
                            val db = FirebaseFirestore.getInstance()
                            if (qrResult.isNotEmpty()){
                                val docRef = db.collection("cihaz").document(qrResult)
                                docRef.get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            //Toast.makeText(this,"${document.data}",Toast.LENGTH_SHORT).show()
                                            val token = document.data.toString()
                                            sendDataToServer(token)
                                        } else {
                                            Toast.makeText(this,"no such document",Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this,"hata",Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                // Handle exception
            }

        }, ContextCompat.getMainExecutor(this))
    }
    //İzin kontrolü
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private class QRCodeAnalyzer(private val onQRCodeScanned: (String) -> Unit) :
        ImageAnalysis.Analyzer {

        private val reader = MultiFormatReader()

        override fun analyze(image: ImageProxy) {
            val imageData = image.planes[0].buffer
            val data = imageData.toByteArray()

            // YUV formatındaki veriyi ByteArray'e dönüştürme
            val source = PlanarYUVLuminanceSource(
                data,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result: Result = reader.decode(bitmap)
                onQRCodeScanned(result.text)
            } catch (e: Exception) {
                // QR kod bulunamadı veya hata oluştu
            } finally {
                image.close()
            }
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val byteArray = ByteArray(remaining())
            get(byteArray)
            return byteArray
        }
    }
    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }
    private fun sendDataToServer(data: String) {
        val apiService = RetrofitClient.apiService
        apiService.sendData(data).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext,"token gönderildi",Toast.LENGTH_SHORT).show()
                } else {
                    println("Hata: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                println("Veri gönderme başarısız: ${t.message}")
            }
        })
    }
}