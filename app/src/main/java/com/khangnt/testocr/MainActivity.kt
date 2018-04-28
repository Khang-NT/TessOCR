package com.khangnt.testocr

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val tessaractApi by lazy { TessaractApi(this) }
    private var loadingDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
        } else {
            cameraView.addCameraListener(object : CameraListener() {
                override fun onPictureTaken(jpeg: ByteArray) {
                    showLoading()
                    CameraUtils.decodeBitmap(jpeg, 500, Int.MAX_VALUE) { bitmap ->
                        tessaractApi.findText(
                                bitmap,
                                onSuccess = {
                                    showResult(it)
                                },
                                onError = {
                                    showError(it)
                                }
                        )
                    }
                }
            })
            captureButton.setOnClickListener {
                cameraView.capturePicture()
            }
        }
    }

    private fun showLoading() {
        runOnUiThread {
            loadingDialog = ProgressDialog.show(this@MainActivity, "", "Processing", true, false)
        }
    }

    private fun hideLoading() {
        runOnUiThread {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    private fun showResult(text: String) {
        hideLoading()
        runOnUiThread {
            AlertDialog.Builder(this)
                    .setTitle("Result")
                    .setMessage(text)
                    .setPositiveButton("OK", null)
                    .show()
        }
    }

    private fun showError(throwable: Throwable) {
        hideLoading()
        runOnUiThread {
            throwable.printStackTrace()
            Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun hasPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission()) {
            cameraView.start()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
        tessaractApi.release()
    }
}
