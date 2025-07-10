package com.example.project_capson.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.example.project_capson.R
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class LiveFragment : Fragment() {

    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var outputBox: TextView
    private lateinit var btnListenStop: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnExchange: ImageButton
    private lateinit var timerText: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isPaused = false
    private var currentLanguageCode = "en-US"
    private var elapsedSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "LiveFragment"

    private var translator: Translator? = null
    private var sourceLangCode: String = "en"
    private var targetLangCode: String = "hi"

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isListening && !isPaused) {
                elapsedSeconds++
                updateTimerText()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val languages = listOf(
        Triple("en-US", "en", "English"),
        Triple("hi-IN", "hi", "Hindi"),
        Triple("es-ES", "es", "Spanish"),
        Triple("fr-FR", "fr", "French"),
        Triple("de-DE", "de", "German")
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) initSpeechRecognizer()
        else if (isAdded) Toast.makeText(requireContext(), "Mic permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_live, container, false)

        // Initialize views
        spinnerFrom = view.findViewById(R.id.spinner_from)
        spinnerTo = view.findViewById(R.id.spinner_to)
        outputBox = view.findViewById(R.id.output_box)
        btnListenStop = view.findViewById(R.id.btn_listen_stop)
        btnPause = view.findViewById(R.id.btn_pause)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnExchange = view.findViewById(R.id.btn_exchange)
        timerText = view.findViewById(R.id.timer_text)

        btnPause.setImageResource(R.drawable.pause)

        setupSpinners()
        setupListeners()
        checkMicPermission()

        return view
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages.map { it.third })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter
        spinnerFrom.setSelection(0)
        spinnerTo.setSelection(1)

        spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentLanguageCode = languages[position].first
                sourceLangCode = languages[position].second
                createTranslator()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                targetLangCode = languages[position].second
                createTranslator()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        btnListenStop.setOnClickListener {
            if (isListening) stopListening()
            else startListening()
        }

        btnPause.setOnClickListener {
            if (!isListening) return@setOnClickListener
            if (!isPaused) pauseListening()
            else resumeListening()
        }

        btnRefresh.setOnClickListener {
            stopListening()
            outputBox.text = ""
        }

        btnExchange.setOnClickListener {
            val fromPos = spinnerFrom.selectedItemPosition
            val toPos = spinnerTo.selectedItemPosition
            spinnerFrom.setSelection(toPos)
            spinnerTo.setSelection(fromPos)
        }
    }

    private fun checkMicPermission() {
        if (!isAdded) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            initSpeechRecognizer()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun initSpeechRecognizer() {
        destroyRecognizer()

        if (!isAdded || !SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show()
                btnListenStop.isEnabled = false
            }
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    Log.e(TAG, "SpeechRecognizer error: $error")
                    if (isListening && !isPaused) {
                        handler.postDelayed({ startListening() }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spokenText.isNullOrEmpty()) {
                        translator?.translate(spokenText)
                            ?.addOnSuccessListener { translatedText ->
                                if (isAdded) {
                                    val needsSpace = outputBox.text.isNotEmpty() &&
                                            !outputBox.text.trim().endsWith(" ")
                                    outputBox.append((if (needsSpace) " " else "") + translatedText)
                                }
                            }
                            ?.addOnFailureListener {
                                if (isAdded) {
                                    outputBox.append(" Translation failed: $spokenText")
                                }
                            }
                    }
                    if (isListening && !isPaused) {
                        handler.postDelayed({ startListening() }, 300)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (speechRecognizer == null) initSpeechRecognizer()
        if (!isAdded) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            return
        }

        if (!isListening) {
            elapsedSeconds = 0
            updateTimerText()
            handler.post(timerRunnable)
        }

        isListening = true
        isPaused = false

        btnListenStop.setImageResource(R.drawable.baseline_stop_24)
        btnPause.setImageResource(R.drawable.pause)
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        isListening = false
        isPaused = false
        handler.removeCallbacks(timerRunnable)
        elapsedSeconds = 0
        updateTimerText()

        btnListenStop.setImageResource(R.drawable.speak)
        btnPause.setImageResource(R.drawable.pause)
    }

    private fun pauseListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isPaused = true
        handler.removeCallbacks(timerRunnable)
        btnPause.setImageResource(R.drawable.resume)
    }

    private fun resumeListening() {
        if (speechRecognizer == null) initSpeechRecognizer()
        if (!isAdded) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume listening: ${e.message}")
        }

        isPaused = false
        handler.post(timerRunnable)
        btnPause.setImageResource(R.drawable.pause)
    }

    private fun createTranslator() {
        translator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .build()

        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "ML Kit model downloaded")
            }
            ?.addOnFailureListener {
                Toast.makeText(requireContext(), "Translation model download failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTimerText() {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        timerText.text = String.format("%02d:%02d", min, sec)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    override fun onPause() {
        stopListening()
        super.onPause()
    }

    override fun onStop() {
        stopListening()
        super.onStop()
    }

    override fun onDestroyView() {
        destroyRecognizer()
        handler.removeCallbacksAndMessages(null)
        translator?.close()
        super.onDestroyView()
    }
}