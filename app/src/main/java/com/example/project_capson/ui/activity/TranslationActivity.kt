package com.example.project_capson.ui.activity

import android.content.*
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project_capson.R
import com.example.project_capson.utils.NepaliHindiMapping
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class TranslationActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private lateinit var topInputText: EditText
    private lateinit var bottomInputText: EditText
    private lateinit var topSpinner: Spinner
    private lateinit var bottomSpinner: Spinner
    private lateinit var topListenBtn: ImageButton
    private lateinit var bottomListenBtn: ImageButton
    private lateinit var mainSpinnerFrom: Spinner
    private lateinit var mainSpinnerTo: Spinner

    private var isEditingTop = false
    private var isEditingBottom = false

    private var topToBottomTranslator: Translator? = null
    private var bottomToTopTranslator: Translator? = null

    companion object {
        private const val NEPALI = "Nepali"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.ENGLISH
            }
        }

        val topPanel = findViewById<View>(R.id.topPanel)
        val bottomPanel = findViewById<View>(R.id.bottomPanel)
        mainSpinnerFrom = findViewById(R.id.spinner_from)
        mainSpinnerTo = findViewById(R.id.spinner_to)
        val exchangeBtn = findViewById<ImageButton>(R.id.btn_exchange)

        topInputText = topPanel.findViewById(R.id.inputText)
        bottomInputText = bottomPanel.findViewById(R.id.inputText)
        topSpinner = topPanel.findViewById(R.id.languageSpinner)
        bottomSpinner = bottomPanel.findViewById(R.id.languageSpinner)
        topListenBtn = topPanel.findViewById(R.id.listenBtn)
        bottomListenBtn = bottomPanel.findViewById(R.id.listenBtn)

        val languages = listOf("English", "Spanish", "French", "Hindi", NEPALI)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        mainSpinnerFrom.adapter = adapter
        mainSpinnerTo.adapter = adapter
        topSpinner.adapter = adapter
        bottomSpinner.adapter = adapter

        mainSpinnerFrom.setSelection(0)
        mainSpinnerTo.setSelection(0)
        topSpinner.setSelection(0)
        bottomSpinner.setSelection(0)

        intent?.getStringExtra("detectedText")?.let { text ->
            topInputText.setText(text)
            topInputText.setSelection(text.length)
        }

        intent?.getStringExtra("languageCode")?.let { langCode ->
            val lang = when (langCode) {
                "en" -> "English"
                "es" -> "Spanish"
                "fr" -> "French"
                "hi" -> "Hindi"
                "ne" -> NEPALI
                else -> "English"
            }
            val index = languages.indexOf(lang)
            if (index != -1) {
                mainSpinnerFrom.setSelection(index)
                topSpinner.setSelection(index)
                updateListenButtonVisibility(lang == NEPALI, topListenBtn)
            }
        }

        exchangeBtn.setOnClickListener {
            val fromPos = mainSpinnerFrom.selectedItemPosition
            val toPos = mainSpinnerTo.selectedItemPosition
            mainSpinnerFrom.setSelection(toPos)
            mainSpinnerTo.setSelection(fromPos)
            topSpinner.setSelection(toPos)
            bottomSpinner.setSelection(fromPos)

            val tempText = topInputText.text.toString()
            topInputText.setText(bottomInputText.text.toString())
            bottomInputText.setText(tempText)

            updateListenButtonVisibility(mainSpinnerFrom.selectedItem.toString() == NEPALI, topListenBtn)
            updateListenButtonVisibility(mainSpinnerTo.selectedItem.toString() == NEPALI, bottomListenBtn)
        }

        // Connect panel spinners to main spinners
        topSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mainSpinnerFrom.setSelection(position)
                updateListenButtonVisibility(parent.getItemAtPosition(position).toString() == NEPALI, topListenBtn)
                onLanguageChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        bottomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mainSpinnerTo.setSelection(position)
                updateListenButtonVisibility(parent.getItemAtPosition(position).toString() == NEPALI, bottomListenBtn)
                onLanguageChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Connect main spinners to panel spinners
        mainSpinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (topSpinner.selectedItemPosition != position) {
                    topSpinner.setSelection(position)
                }
                updateListenButtonVisibility(parent.getItemAtPosition(position).toString() == NEPALI, topListenBtn)
                onLanguageChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        mainSpinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (bottomSpinner.selectedItemPosition != position) {
                    bottomSpinner.setSelection(position)
                }
                updateListenButtonVisibility(parent.getItemAtPosition(position).toString() == NEPALI, bottomListenBtn)
                onLanguageChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        topInputText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isEditingTop) return
                isEditingBottom = true
                if (s.isNullOrBlank()) {
                    bottomInputText.setText("")
                    isEditingBottom = false
                    return
                }
                translateText(s.toString(), true)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        bottomInputText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isEditingBottom) return
                isEditingTop = true
                if (s.isNullOrBlank()) {
                    topInputText.setText("")
                    isEditingTop = false
                    return
                }
                translateText(s.toString(), false)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        createTranslators()
        setupPanelControls(topPanel)
        setupPanelControls(bottomPanel)
    }

    private fun onLanguageChanged() {
        createTranslators()
        if (!topInputText.text.isNullOrBlank()) {
            translateText(topInputText.text.toString())
        }
    }

    private fun updateListenButtonVisibility(isNepaliSelected: Boolean, listenBtn: ImageButton) {
        listenBtn.visibility = if (isNepaliSelected) View.INVISIBLE else View.VISIBLE
    }

    private fun translateText(text: String, isTopToBottom: Boolean = true) {
        val fromLang = if (isTopToBottom) topSpinner.selectedItem.toString() else bottomSpinner.selectedItem.toString()
        val toLang = if (isTopToBottom) bottomSpinner.selectedItem.toString() else topSpinner.selectedItem.toString()

        if (fromLang == NEPALI) {
            val hindi = NepaliHindiMapping.translateNepaliToHindi(text)
            if (toLang == "Hindi") {
                if (isTopToBottom) bottomInputText.setText(hindi) else topInputText.setText(hindi)
                isEditingBottom = !isTopToBottom
                isEditingTop = isTopToBottom
            } else {
                val target = getMlKitLanguageCode(toLang) ?: return
                val translator = Translation.getClient(
                    TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.HINDI).setTargetLanguage(target).build()
                )
                translator.downloadModelIfNeeded().addOnSuccessListener {
                    translator.translate(hindi).addOnSuccessListener { translated ->
                        if (isTopToBottom) bottomInputText.setText(translated) else topInputText.setText(translated)
                        isEditingBottom = !isTopToBottom
                        isEditingTop = isTopToBottom
                        translator.close()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Translation error", Toast.LENGTH_SHORT).show()
                        translator.close()
                    }
                }
            }
        } else if (toLang == NEPALI) {
            val source = getMlKitLanguageCode(fromLang) ?: return
            val translator = Translation.getClient(
                TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(TranslateLanguage.HINDI).build()
            )
            translator.downloadModelIfNeeded().addOnSuccessListener {
                translator.translate(text).addOnSuccessListener { hindi ->
                    val nepali = NepaliHindiMapping.translateHindiToNepali(hindi)
                    if (isTopToBottom) bottomInputText.setText(nepali) else topInputText.setText(nepali)
                    isEditingBottom = !isTopToBottom
                    isEditingTop = isTopToBottom
                    translator.close()
                }.addOnFailureListener {
                    Toast.makeText(this, "Translation error", Toast.LENGTH_SHORT).show()
                    translator.close()
                }
            }
        } else {
            val translator = if (isTopToBottom) topToBottomTranslator else bottomToTopTranslator
            translator?.translate(text)?.addOnSuccessListener {
                if (isTopToBottom) bottomInputText.setText(it) else topInputText.setText(it)
                isEditingBottom = !isTopToBottom
                isEditingTop = isTopToBottom
            }?.addOnFailureListener {
                Toast.makeText(this, "Translation error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTranslators() {
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()

        val source = getMlKitLanguageCode(topSpinner.selectedItem.toString())
        val target = getMlKitLanguageCode(bottomSpinner.selectedItem.toString())

        if (source != null && target != null) {
            topToBottomTranslator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build())
            bottomToTopTranslator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(target).setTargetLanguage(source).build())
            topToBottomTranslator?.downloadModelIfNeeded()
            bottomToTopTranslator?.downloadModelIfNeeded()
        } else {
            topToBottomTranslator = null
            bottomToTopTranslator = null
        }
    }

    private fun setupPanelControls(panel: View) {
        val inputText = panel.findViewById<EditText>(R.id.inputText)
        val spinner = panel.findViewById<Spinner>(R.id.languageSpinner)
        val speakBtn = panel.findViewById<ImageButton>(R.id.speakTextBtn)
        val copyBtn = panel.findViewById<ImageButton>(R.id.copyBtn)
        val clearBtn = panel.findViewById<ImageButton>(R.id.clearBtn)
        val listenBtn = panel.findViewById<ImageButton>(R.id.listenBtn)

        speakBtn.setOnClickListener {
            val text = inputText.text.toString()
            tts.language = getLocaleFromLanguage(spinner.selectedItem.toString())
            if (text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        copyBtn.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("translated", inputText.text))
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
        }

        clearBtn.setOnClickListener { inputText.setText("") }

        listenBtn.setOnClickListener {
            if (spinner.selectedItem.toString() == NEPALI) {
                Toast.makeText(this, "Voice input not supported for Nepali", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isListening) {
                speechRecognizer?.stopListening()
                listenBtn.setImageResource(R.drawable.speak)
                isListening = false
            } else {
                startSpeechToText(inputText, listenBtn, getLocaleFromLanguage(spinner.selectedItem.toString()))
                isListening = true
            }
        }
    }

    private fun startSpeechToText(editText: EditText, button: ImageButton, locale: Locale) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
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
                Toast.makeText(this@TranslationActivity, "Error: $error", Toast.LENGTH_SHORT).show()
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
    }

    private fun getLocaleFromLanguage(lang: String): Locale = when (lang) {
        "English" -> Locale.ENGLISH
        "Spanish" -> Locale("es", "ES")
        "French" -> Locale("fr", "FR")
        "Hindi" -> Locale("hi", "IN")
        NEPALI -> Locale("ne", "NP")
        else -> Locale.ENGLISH
    }

    private fun getMlKitLanguageCode(lang: String): String? = when (lang) {
        "English" -> TranslateLanguage.ENGLISH
        "Spanish" -> TranslateLanguage.SPANISH
        "French" -> TranslateLanguage.FRENCH
        "Hindi" -> TranslateLanguage.HINDI
        else -> null
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        speechRecognizer?.destroy()
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()
    }
}