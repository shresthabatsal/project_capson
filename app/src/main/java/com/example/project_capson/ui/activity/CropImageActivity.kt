package com.example.project_capson.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import java.io.File

class CropImageActivity : AppCompatActivity() {
    private val TAG = "CropImageActivity"

    // Create recognizers
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceUri = intent.data ?: run {
            finishWithError("No image provided")
            return
        }

        val destinationUri = Uri.fromFile(
            File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        UCrop.of(sourceUri, destinationUri)
            .withOptions(UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setToolbarTitle("Crop Text Area")
                withAspectRatio(16F, 9F)
                withMaxResultSize(2000, 2000)
            })
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP -> {
                UCrop.getOutput(data!!)?.let { uri ->
                    processCroppedImage(uri)
                } ?: finishWithError("Cropping failed")
            }
            resultCode == UCrop.RESULT_ERROR -> {
                finishWithError(UCrop.getError(data!!)?.message ?: "Crop error")
            }
            else -> finish()
        }
    }

    private fun processCroppedImage(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)

        // First try with Devanagari script
        devanagariRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                if (text.isNotBlank()) {
                    Log.d(TAG, "Devanagari text detected: ${text.take(50)}...")
                    detectLanguage(text)
                } else {
                    // If Devanagari found nothing, try Latin script
                    tryLatinRecognition(image)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Devanagari recognition failed, trying Latin", e)
                tryLatinRecognition(image)
            }
    }

    private fun tryLatinRecognition(image: InputImage) {
        latinRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                if (text.isNotBlank()) {
                    Log.d(TAG, "Latin text detected: ${text.take(50)}...")
                    detectLanguage(text)
                } else {
                    finishWithError("No text found in image")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Latin recognition failed", e)
                finishWithError("Text detection failed")
            }
    }

    private fun detectLanguage(text: String) {
        Log.d(TAG, "Starting language detection for text: ${text.take(50)}...")

        val languageIdentifier = LanguageIdentification.getClient()

        languageIdentifier.identifyPossibleLanguages(text)
            .addOnSuccessListener { identifiedLanguages ->
                if (identifiedLanguages.isEmpty()) {
                    Log.w(TAG, "No languages identified")
                    returnToTranslationActivity(text, "hi") // Default to Hindi if no detection
                    return@addOnSuccessListener
                }

                // Filter for our supported languages only
                val supportedLanguages = identifiedLanguages.filter { lang ->
                    when (lang.languageTag) {
                        "en", "es", "fr", "hi" -> true
                        else -> false
                    }
                }

                if (supportedLanguages.isNotEmpty()) {
                    // Get the highest confidence supported language
                    val bestLanguage = supportedLanguages.maxByOrNull { it.confidence }!!
                    Log.d(TAG, "Detected language: ${bestLanguage.languageTag} (confidence: ${bestLanguage.confidence})")
                    returnToTranslationActivity(text, bestLanguage.languageTag)
                } else {
                    // If text was detected by Devanagari but language not identified, assume Hindi
                    val assumedLanguage = if (text.contains(Regex("[\\u0900-\\u097F]"))) "hi" else "en"
                    Log.w(TAG, "No supported language detected, assuming $assumedLanguage")
                    returnToTranslationActivity(text, assumedLanguage)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Language detection failed", e)
                // If text contains Devanagari characters, assume Hindi
                val assumedLanguage = if (text.contains(Regex("[\\u0900-\\u097F]"))) "hi" else "en"
                returnToTranslationActivity(text, assumedLanguage)
            }
    }

    private fun returnToTranslationActivity(text: String, langCode: String) {
        Log.d(TAG, "Returning to TranslationActivity with language: $langCode")
        setResult(RESULT_OK, Intent().apply {
            putExtra("detectedText", text)
            putExtra("languageCode", langCode)
        })
        finish()
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}