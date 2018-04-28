package com.khangnt.testocr

import android.content.Context
import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Created by Khang NT on 4/26/18.
 * Email: khang.neon.1997@gmail.com
 */

class TessaractApi(private val context: Context) {

    private val isBusy = AtomicBoolean(false)
    private var isInitialized = false

    @get:WorkerThread
    private val tessBaseAPI: TessBaseAPI by lazy {
        isInitialized = true
        val dataPath = context.getDir("tesseract", Context.MODE_PRIVATE)
        val lang = "vie"
        val vieTrainedDataPath = File(dataPath, "tessdata/$lang.traineddata")
        if (!vieTrainedDataPath.exists()) {
            vieTrainedDataPath.parentFile.mkdirs()
            BufferedOutputStream(FileOutputStream(vieTrainedDataPath)).use { outputStream ->
                context.assets.open("$lang.traineddata").copyTo(outputStream)
            }
        }
        TessBaseAPI().apply {
            init(dataPath.absolutePath, lang)
        }
    }


    fun findText(bitmap: Bitmap, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
        thread {
            if (!isBusy.compareAndSet(false, true)) {
                throw IllegalStateException("TessaractApi is busy")
            }
            try {
                tessBaseAPI.clear()
                tessBaseAPI.setImage(bitmap)
                onSuccess(tessBaseAPI.utF8Text)
            } catch (error: Throwable) {
                onError(error)
            } finally {
                isBusy.set(false)
            }
        }
    }

    fun release() {
        if (isInitialized) {
            tessBaseAPI.end()
        }
    }
}