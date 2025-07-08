package com.example.project_capson.ui.fragment

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.project_capson.R
import java.util.*

class ConversationFragment : Fragment() {

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(requireContext()) {
            tts.language = Locale.ENGLISH
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_conversation, container, false)

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setupPanel(view.findViewById(R.id.topPanel))
        setupPanel(view.findViewById(R.id.bottomPanel))
        return view
    }

    private fun setupPanel(panel: View) {
        val inputText = panel.findViewById<EditText>(R.id.inputText)
        val spinner = panel.findViewById<Spinner>(R.id.languageSpinner)
        val speakTextBtn = panel.findViewById<ImageButton>(R.id.speakTextBtn)
        val copyBtn = panel.findViewById<ImageButton>(R.id.copyBtn)
        val clearBtn = panel.findViewById<ImageButton>(R.id.clearBtn)
        val listenBtn = panel.findViewById<ImageButton>(R.id.listenBtn)

        val languages = listOf("English", "Spanish", "French", "Hindi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        spinner.adapter = adapter

        // Text to Speech
        speakTextBtn.setOnClickListener {
            val text = inputText.text.toString()
            val selectedLang = spinner.selectedItem.toString()
            val locale = getLocaleFromLanguage(selectedLang)
            tts.language = locale
            if (text.isNotBlank()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        // Copy to clipboard
        copyBtn.setOnClickListener {
            val text = inputText.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("translated_text", text))
            Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
        }

        // Clear text
        clearBtn.setOnClickListener {
            inputText.setText("")
        }

        // Speech to text (toggle)
        listenBtn.setOnClickListener {
            Log.d("Speech", "Listen button clicked")

            val selectedLang = spinner.selectedItem.toString()
            val locale = getLocaleFromLanguage(selectedLang)

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Microphone permission not granted", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isListening) {
                speechRecognizer?.stopListening()
                listenBtn.setImageResource(R.drawable.baseline_stop_24)
                isListening = false
            } else {
                startSpeechToText(inputText, listenBtn, locale)
                isListening = true
            }
        }
    }

    private fun startSpeechToText(editText: EditText, button: ImageButton, locale: Locale) {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            if (speechRecognizer != null) {
                speechRecognizer?.destroy()
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            }

            speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                    editText.setText(result)
                    button.setImageResource(R.drawable.speak)
                    isListening = false
                }

                override fun onError(error: Int) {
                    button.setImageResource(R.drawable.speak)
                    isListening = false
                    Toast.makeText(requireContext(), "Speech error: $error", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
            })

            // Add a slight delay before listening
            Handler(Looper.getMainLooper()).postDelayed({
                speechRecognizer?.startListening(intent)
                button.setImageResource(R.drawable.baseline_stop_24)
            }, 200)
        } else {
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Microphone permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.shutdown()
        speechRecognizer?.destroy()
    }

    private fun getLocaleFromLanguage(language: String): Locale {
        return when (language) {
            "English" -> Locale("en", "US")
            "Spanish" -> Locale("es", "ES")
            "French" -> Locale("fr", "FR")
            "Hindi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }
    }
}