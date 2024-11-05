package com.dicoding.asclepius.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.dicoding.asclepius.R
import com.dicoding.asclepius.view.ResultActivity
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException


class ImageClassifierHelper(
    var threshold: Float = 0.1f,
    var maxResults: Int = 3,
    val modelName: String = "cancer_classification.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(results: List<Classifications>?, inferenceTime: Long)
    }

    private fun setupImageClassifier() {
        // TODO: Menyiapkan Image Classifier untuk memproses gambar.
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(4)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }

    fun classifyStaticImage(imageUri: Uri) {
        // TODO: mengklasifikasikan imageUri dari gambar statis.
        try {
            val bitmap = uriToBitmap(imageUri)
            bitmap?.let {
                classifyImage(it, imageUri)
            } ?: classifierListener?.onError("Failed to convert Uri to Bitmap.")
        } catch (e: IOException) {
            classifierListener?.onError("Error processing image: ${e.message}")
            Log.e(TAG, e.message.toString())
        }
    }

    private fun classifyImage(bitmap: Bitmap, imageUri: Uri) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Konversi Bitmap ke TensorImage
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(org.tensorflow.lite.DataType.UINT8))
            .build()

        // Konversi Bitmap ke TensorImage
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // Proses gambar menggunakan ImageProcessor
        val processedImage = imageProcessor.process(tensorImage)

        // Klasifikasi gambar
        val inferenceTime = System.currentTimeMillis()
        val results = imageClassifier?.classify(processedImage)
        val timeTaken = System.currentTimeMillis() - inferenceTime

        // Kirim hasil klasifikasi melalui listener
        classifierListener?.onResults(results, timeTaken)

        // Mengirim data ke ResultActivity
        val prediction = results?.get(0)?.categories?.get(0)?.label ?: "Unknown"
        val confidenceScore = results?.get(0)?.categories?.get(0)?.score ?: 0f

        // Buat Intent untuk ResultActivity
        val intent = Intent(context, ResultActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            putExtra("prediction", prediction)
            putExtra("confidenceScore", confidenceScore)
        }

        // Mulai ResultActivity
//        context.startActivity(intent)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API level 28 (P) dan lebih tinggi
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            } else {
                // API level di bawah 28
                val inputStream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)?.copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error converting Uri to Bitmap: ${e.message}")
            null
        }
    }
}