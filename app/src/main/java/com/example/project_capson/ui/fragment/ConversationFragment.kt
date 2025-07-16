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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.project_capson.R
import com.example.project_capson.utils.NepaliHindiMapping
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class ConversationFragment : Fragment() {

    companion object {
        private const val TAG = "ConversationFragment"
        private const val NEPALI = "Nepali"
    }

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private var topToBottomTranslator: Translator? = null
    private var bottomToTopTranslator: Translator? = null
    private var englishToHindiTranslator: Translator? = null

    private lateinit var topInputText: EditText
    private lateinit var bottomInputText: EditText
    private lateinit var topSpinner: Spinner
    private lateinit var bottomSpinner: Spinner
    private lateinit var topListenBtn: ImageButton
    private lateinit var bottomListenBtn: ImageButton

    private var isEditingTop = false
    private var isEditingBottom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.ENGLISH
            } else {
                Log.e(TAG, "TextToSpeech initialization failed")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_conversation, container, false)

        val topPanel = view.findViewById<View>(R.id.topPanel)
        val bottomPanel = view.findViewById<View>(R.id.bottomPanel)

        topInputText = topPanel.findViewById(R.id.inputText)
        bottomInputText = bottomPanel.findViewById(R.id.inputText)
        topSpinner = topPanel.findViewById(R.id.languageSpinner)
        bottomSpinner = bottomPanel.findViewById(R.id.languageSpinner)
        topListenBtn = topPanel.findViewById(R.id.listenBtn)
        bottomListenBtn = bottomPanel.findViewById(R.id.listenBtn)

        setupSpinner(topSpinner)
        setupSpinner(bottomSpinner)
        setupTranslation()
        setupPanelControls(topPanel)
        setupPanelControls(bottomPanel)
        initializeEnglishHindiTranslator()

        return view
    }

    private fun setupSpinner(spinner: Spinner) {
        val languages = listOf("English", "Spanish", "French", "Hindi", NEPALI)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLang = parent.getItemAtPosition(position).toString()
                updateListenButtonVisibility(
                    selectedLang == NEPALI,
                    if (spinner == topSpinner) topListenBtn else bottomListenBtn
                )
                createTranslators()
                topInputText.text.clear()
                bottomInputText.text.clear()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun initializeEnglishHindiTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HINDI)
            .build()

        englishToHindiTranslator = Translation.getClient(options)
        englishToHindiTranslator?.downloadModelIfNeeded(DownloadConditions.Builder().build())
            ?.addOnSuccessListener {
                Log.d(TAG, "English to Hindi translator ready")
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to download English to Hindi model", e)
            }
    }

    private fun updateListenButtonVisibility(isNepaliSelected: Boolean, listenBtn: ImageButton) {
        listenBtn.visibility = if (isNepaliSelected) View.INVISIBLE else View.VISIBLE
    }

    private fun setupTranslation() {
        topInputText.addTextChangedListener(createTextWatcher(true))
        bottomInputText.addTextChangedListener(createTextWatcher(false))
        createTranslators()
    }

    private fun createTextWatcher(isTop: Boolean): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (if (isTop) isEditingTop else isEditingBottom) return

                val text = s.toString()
                if (text.isBlank()) {
                    if (isTop) {
                        isEditingBottom = true
                        bottomInputText.setText("")
                        isEditingBottom = false
                    } else {
                        isEditingTop = true
                        topInputText.setText("")
                        isEditingTop = false
                    }
                    return
                }

                if (isTop) {
                    handleTopTextChange(text)
                } else {
                    handleBottomTextChange(text)
                }
            }
        }
    }

    private fun handleTopTextChange(text: String) {
        isEditingBottom = true
        when (topSpinner.selectedItem.toString()) {
            NEPALI -> {
                val hindiText = NepaliHindiMapping.translateNepaliToHindi(text)
                translateHindiToTarget(hindiText, bottomSpinner.selectedItem.toString()) { translatedText ->
                    bottomInputText.setText(translatedText)
                    isEditingBottom = false
                }
            }
            else -> {
                if (bottomSpinner.selectedItem.toString() == NEPALI) {
                    translateToHindiThenNepali(text, topSpinner.selectedItem.toString()) { nepaliText ->
                        bottomInputText.setText(nepaliText)
                        isEditingBottom = false
                    }
                } else {
                    topToBottomTranslator?.translate(text)
                        ?.addOnSuccessListener { translatedText ->
                            bottomInputText.setText(translatedText)
                            isEditingBottom = false
                        }
                        ?.addOnFailureListener { e ->
                            handleTranslationError(e, "top->bottom")
                            isEditingBottom = false
                        }
                }
            }
        }
    }

    private fun handleBottomTextChange(text: String) {
        isEditingTop = true
        when (bottomSpinner.selectedItem.toString()) {
            NEPALI -> {
                val hindiText = NepaliHindiMapping.translateNepaliToHindi(text)
                translateHindiToTarget(hindiText, topSpinner.selectedItem.toString()) { translatedText ->
                    topInputText.setText(translatedText)
                    isEditingTop = false
                }
            }
            else -> {
                if (topSpinner.selectedItem.toString() == NEPALI) {
                    translateToHindiThenNepali(text, bottomSpinner.selectedItem.toString()) { nepaliText ->
                        topInputText.setText(nepaliText)
                        isEditingTop = false
                    }
                } else {
                    bottomToTopTranslator?.translate(text)
                        ?.addOnSuccessListener { translatedText ->
                            topInputText.setText(translatedText)
                            isEditingTop = false
                        }
                        ?.addOnFailureListener { e ->
                            handleTranslationError(e, "bottom->top")
                            isEditingTop = false
                        }
                }
            }
        }
    }

    private fun translateToHindiThenNepali(text: String, sourceLanguage: String, callback: (String) -> Unit) {
        when (sourceLanguage) {
            "English" -> {
                englishToHindiTranslator?.translate(text)
                    ?.addOnSuccessListener { hindiText ->
                        val nepaliText = NepaliHindiMapping.translateHindiToNepali(hindiText)
                        callback(nepaliText)
                    }
                    ?.addOnFailureListener { e ->
                        Log.e(TAG, "English to Hindi translation failed", e)
                        callback("Translation failed")
                    }
            }
            "Hindi" -> {
                val nepaliText = NepaliHindiMapping.translateHindiToNepali(text)
                callback(nepaliText)
            }
            else -> {
                val sourceLangCode = getMlKitLanguageCode(sourceLanguage) ?: run {
                    callback("Language not supported")
                    return
                }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLangCode)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()

                val tempTranslator = Translation.getClient(options)
                tempTranslator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                    ?.addOnSuccessListener {
                        tempTranslator.translate(text)
                            ?.addOnSuccessListener { englishText ->
                                englishToHindiTranslator?.translate(englishText)
                                    ?.addOnSuccessListener { hindiText ->
                                        val nepaliText = NepaliHindiMapping.translateHindiToNepali(hindiText)
                                        callback(nepaliText)
                                        tempTranslator.close()
                                    }
                                    ?.addOnFailureListener { e ->
                                        callback("Translation failed")
                                        tempTranslator.close()
                                    }
                            }
                            ?.addOnFailureListener { e ->
                                callback("Translation failed")
                                tempTranslator.close()
                            }
                    }
                    ?.addOnFailureListener { e ->
                        callback("Model download failed")
                        tempTranslator.close()
                    }
            }
        }
    }

    private fun translateHindiToTarget(hindiText: String, targetLanguage: String, callback: (String) -> Unit) {
        when (targetLanguage) {
            "Hindi" -> callback(hindiText)
            else -> {
                val targetLangCode = getMlKitLanguageCode(targetLanguage) ?: run {
                    callback("Language not supported")
                    return
                }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.HINDI)
                    .setTargetLanguage(targetLangCode)
                    .build()

                val tempTranslator = Translation.getClient(options)
                tempTranslator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                    ?.addOnSuccessListener {
                        tempTranslator.translate(hindiText)
                            ?.addOnSuccessListener { translatedText ->
                                callback(translatedText)
                                tempTranslator.close()
                            }
                            ?.addOnFailureListener { e ->
                                callback("Translation failed")
                                tempTranslator.close()
                            }
                    }
                    ?.addOnFailureListener { e ->
                        callback("Model download failed")
                        tempTranslator.close()
                    }
            }
        }
    }

    private fun handleTranslationError(e: Exception, direction: String) {
        Log.e(TAG, "Translation $direction failed", e)
        Toast.makeText(
            requireContext(),
            "Translation $direction failed: ${e.localizedMessage ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun createTranslators() {
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()

        val topLang = topSpinner.selectedItem.toString()
        val bottomLang = bottomSpinner.selectedItem.toString()

        if (topLang == NEPALI || bottomLang == NEPALI) {
            return
        }

        val sourceLangCode = getMlKitLanguageCode(topLang)
        val targetLangCode = getMlKitLanguageCode(bottomLang)

        if (sourceLangCode == null || targetLangCode == null) {
            Toast.makeText(
                requireContext(),
                "Unsupported language selection. Please select different languages.",
                Toast.LENGTH_LONG
            ).show()
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

        topToBottomTranslator = Translation.getClient(options1)
        bottomToTopTranslator = Translation.getClient(options2)

        val conditions = DownloadConditions.Builder().build()

        topToBottomTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "Top->Bottom model ready")
            }
            ?.addOnFailureListener { e ->
                handleTranslationError(e, "top->bottom model download")
            }

        bottomToTopTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "Bottom->Top model ready")
            }
            ?.addOnFailureListener { e ->
                handleTranslationError(e, "bottom->top model download")
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
            if (text.isNotBlank()) {
                val selectedLang = spinner.selectedItem.toString()
                val locale = getLocaleFromLanguage(selectedLang)
                tts.language = locale
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        copyBtn.setOnClickListener {
            val text = inputText.text.toString()
            if (text.isNotBlank()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("translated_text", text))
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        clearBtn.setOnClickListener {
            inputText.setText("")
        }

        listenBtn.setOnClickListener {
            val selectedLang = spinner.selectedItem.toString()
            if (selectedLang == NEPALI) {
                Toast.makeText(requireContext(), "Voice input not supported for Nepali", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isListening) {
                speechRecognizer?.stopListening()
                listenBtn.setImageResource(R.drawable.speak)
                isListening = false
            } else {
                startSpeechToText(inputText, listenBtn, getLocaleFromLanguage(selectedLang))
                isListening = true
            }
        }
    }

    private fun startSpeechToText(editText: EditText, button: ImageButton, locale: Locale) {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
                setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        button.setImageResource(R.drawable.speak)
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                            else -> "Unknown error"
                        }
                        Toast.makeText(requireContext(), "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            editText.setText(matches[0])
                        }
                        button.setImageResource(R.drawable.speak)
                        isListening = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            }

            speechRecognizer?.startListening(intent)
            button.setImageResource(R.drawable.baseline_stop_24)
        } else {
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show()
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

    private fun getLocaleFromLanguage(language: String): Locale {
        return when (language) {
            "English" -> Locale("en", "US")
            "Spanish" -> Locale("es", "ES")
            "French" -> Locale("fr", "FR")
            "Hindi" -> Locale("hi", "IN")
            NEPALI -> Locale("ne", "NP")
            else -> Locale.ENGLISH
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.shutdown()
        speechRecognizer?.destroy()
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()
        englishToHindiTranslator?.close()
    }
}