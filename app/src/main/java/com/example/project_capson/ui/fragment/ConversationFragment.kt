package com.example.project_capson.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.project_capson.R
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class ConversationFragment : Fragment() {

    companion object {
        private const val TAG = "ConversationFragment"
    }

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private var topToBottomTranslator: Translator? = null
    private var bottomToTopTranslator: Translator? = null

    private lateinit var topInputText: EditText
    private lateinit var bottomInputText: EditText
    private lateinit var topSpinner: Spinner
    private lateinit var bottomSpinner: Spinner

    private var isEditingTop = false
    private var isEditingBottom = false

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

        val topPanel = view.findViewById<View>(R.id.topPanel)
        val bottomPanel = view.findViewById<View>(R.id.bottomPanel)

        topInputText = topPanel.findViewById(R.id.inputText)
        bottomInputText = bottomPanel.findViewById(R.id.inputText)
        topSpinner = topPanel.findViewById(R.id.languageSpinner)
        bottomSpinner = bottomPanel.findViewById(R.id.languageSpinner)

        setupSpinner(topSpinner)
        setupSpinner(bottomSpinner)

        setupTranslation()

        setupPanelControls(topPanel)
        setupPanelControls(bottomPanel)

        return view
    }

    private fun setupSpinner(spinner: Spinner) {
        val languages = listOf("English", "Spanish", "French", "Hindi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        spinner.adapter = adapter
    }

    private fun setupTranslation() {
        val onLangChange = {
            createTranslators()
            topInputText.text.clear()
            bottomInputText.text.clear()
        }

        topSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                onLangChange()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        bottomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                onLangChange()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        topInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditingTop) return
                isEditingBottom = true
                val text = s.toString()
                if (text.isBlank()) {
                    bottomInputText.setText("")
                    isEditingBottom = false
                    return
                }
                topToBottomTranslator?.translate(text)
                    ?.addOnSuccessListener { translatedText ->
                        bottomInputText.setText(translatedText)
                        isEditingBottom = false
                    }
                    ?.addOnFailureListener { e ->
                        Log.e(TAG, "Translation top->bottom failed", e)
                        Toast.makeText(requireContext(),
                            "Translation top->bottom failed: ${e.localizedMessage ?: "Unknown error"}",
                            Toast.LENGTH_LONG).show()
                        isEditingBottom = false
                    }
            }
        })

        bottomInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditingBottom) return
                isEditingTop = true
                val text = s.toString()
                if (text.isBlank()) {
                    topInputText.setText("")
                    isEditingTop = false
                    return
                }
                bottomToTopTranslator?.translate(text)
                    ?.addOnSuccessListener { translatedText ->
                        topInputText.setText(translatedText)
                        isEditingTop = false
                    }
                    ?.addOnFailureListener { e ->
                        Log.e(TAG, "Translation bottom->top failed", e)
                        Toast.makeText(requireContext(),
                            "Translation bottom->top failed: ${e.localizedMessage ?: "Unknown error"}",
                            Toast.LENGTH_LONG).show()
                        isEditingTop = false
                    }
            }
        })

        createTranslators()
    }

    private fun createTranslators() {
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()

        val sourceLangCode = getMlKitLanguageCode(topSpinner.selectedItem.toString())
        val targetLangCode = getMlKitLanguageCode(bottomSpinner.selectedItem.toString())

        if (sourceLangCode == null || targetLangCode == null) {
            Toast.makeText(requireContext(),
                "Unsupported language selection. Please select different languages.",
                Toast.LENGTH_LONG).show()
            return
        }

        val options1 = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .build()
        val options2 = TranslatorOptions.Builder()
            .setSourceLanguage(targetLangCode)
            .setTargetLanguage(sourceLangCode)
            .build()

        topToBottomTranslator = com.google.mlkit.nl.translate.Translation.getClient(options1)
        bottomToTopTranslator = com.google.mlkit.nl.translate.Translation.getClient(options2)

        // Use relaxed network condition to allow model downloads on any network
        val conditions = DownloadConditions.Builder()
            //.requireWifi()  // Commented out to allow cellular
            .build()

        topToBottomTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Top->Bottom model ready", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to download top->bottom model", e)
                Toast.makeText(requireContext(),
                    "Failed to download Top->Bottom model: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG).show()
            }

        bottomToTopTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Bottom->Top model ready", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to download bottom->top model", e)
                Toast.makeText(requireContext(),
                    "Failed to download Bottom->Top model: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun setupPanelControls(panel: View) {
        val inputText = panel.findViewById<EditText>(R.id.inputText)
        val spinner = panel.findViewById<Spinner>(R.id.languageSpinner)
        val speakTextBtn = panel.findViewById<ImageButton>(R.id.speakTextBtn)
        val copyBtn = panel.findViewById<ImageButton>(R.id.copyBtn)
        val clearBtn = panel.findViewById<ImageButton>(R.id.clearBtn)
        val listenBtn = panel.findViewById<ImageButton>(R.id.listenBtn)

        speakTextBtn.setOnClickListener {
            val text = inputText.text.toString()
            val selectedLang = spinner.selectedItem.toString()
            val locale = getLocaleFromLanguage(selectedLang)
            tts.language = locale
            if (text.isNotBlank()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        copyBtn.setOnClickListener {
            val text = inputText.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("translated_text", text))
            Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
        }

        clearBtn.setOnClickListener {
            inputText.setText("")
        }

        listenBtn.setOnClickListener {
            val selectedLang = spinner.selectedItem.toString()
            val locale = getLocaleFromLanguage(selectedLang)

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
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            }

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
                    Toast.makeText(requireContext(), "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Speech recognition error code: $error")
                    isListening = false
                }

                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
            })

            speechRecognizer?.startListening(intent)
            button.setImageResource(R.drawable.baseline_stop_24)
        } else {
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.shutdown()
        speechRecognizer?.destroy()
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()
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

    private fun getMlKitLanguageCode(language: String): String? {
        return when (language) {
            "English" -> TranslateLanguage.ENGLISH
            "Spanish" -> TranslateLanguage.SPANISH
            "French" -> TranslateLanguage.FRENCH
            "Hindi" -> TranslateLanguage.HINDI
            else -> null
        }
    }
}