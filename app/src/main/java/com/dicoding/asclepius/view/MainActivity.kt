package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import androidx.activity.viewModels
import org.tensorflow.lite.task.vision.classifier.Classifications

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    private var currentImageUri: Uri? = null
    private var isAnalyzing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        binding.analyzeButton.setOnClickListener {
            if (!isAnalyzing) {
                analyzeImage()
            } else {
                showToast("Analisis sedang berjalan, tunggu beberapa saat...")
            }
        }

        // Observe LiveData di ViewModel untuk menampilkan ulang gambar
        mainViewModel.imageUri.observe(this) { uri ->
            uri?.let {
                showImage(it)
            }
        }
    }

    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
//                currentImageUri = uri
                mainViewModel.setImageUri(uri)
                startCrop(uri)
            }
        } else {
            showToast("Gagal mengambil gambar")
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(cacheDir.resolve("cropped_image.jpg"))
        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(16f, 9f)
            .withMaxResultSize(1920, 1080)
            .getIntent(this)
        cropActivityLauncher.launch(cropIntent)
    }

    private val cropActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
//                currentImageUri = it
                mainViewModel.setImageUri(it)
                showImage(it)
            }
        } else {
            showToast("Gagal memotong gambar")
        }
    }

    private fun showImage(imageUri: Uri) {
        // TODO: Menampilkan gambar sesuai Gallery yang dipilih.
        Log.d("MainActivity", "Showing image: $imageUri")
        binding.previewImageView.setImageURI(imageUri)
//        Glide.with(this)
//            .load(imageUri)
//            .into(binding.previewImageView)
    }

    private fun analyzeImage() {
        // TODO: Menganalisa gambar yang berhasil ditampilkan.
        Log.d("MainActivity", "Current Image URI: $currentImageUri")

        if (isAnalyzing || mainViewModel.imageUri.value == null) {
            showToast("Tidak ada gambar untuk dianalisis")
            return
        }

        isAnalyzing = true
        mainViewModel.imageUri.value?.let { uri ->
            // Analisis gambar
            val imageClassifier = ImageClassifierHelper(context = this, classifierListener = object : ImageClassifierHelper.ClassifierListener{

                override fun onError(error: String) {
                    showToast(error)
                    isAnalyzing = false
                }

                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    val prediction = results?.firstOrNull()?.categories?.firstOrNull()?.label ?: "Unknown"
                    val confidenceScore = results?.firstOrNull()?.categories?.firstOrNull()?.score ?: 0f

                    moveToResult(uri, prediction, confidenceScore)
                    isAnalyzing = false
                }
            })
            imageClassifier.classifyStaticImage(uri)
        }
    }

    private fun moveToResult(imageUri: Uri, prediction: String, confidenceScore: Float) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            putExtra("prediction", prediction)
            putExtra("confidenceScore", confidenceScore)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}