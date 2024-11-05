package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.

        // Mengambil data dari Intent
        val imageUriString = intent.getStringExtra("imageUri")
        val prediction = intent.getStringExtra("prediction")
        val confidenceScore = intent.getFloatExtra("confidenceScore", 0.0f)



        // Konversi imageUriString ke Uri
        val imageUri: Uri? = imageUriString?.let { Uri.parse(it) }
//        val imageUri = Uri.parse(imageUriString)

        // Menampilkan hasil
        imageUri?.let {
            binding.resultImage.setImageURI(it)
        } ?: run {
            // Menangani kasus di mana gambar tidak ditemukan
            binding.resultImage.setImageResource(R.drawable.ic_place_holder)
        }

        binding.resultText.text = "Prediction: $prediction\nConfidence: ${confidenceScore * 100}%"
    }
}