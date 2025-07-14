package com.example.project_capson.ui.activity

import android.content.*
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project_capson.R
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
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

    private var isEditingTop = false
    private var isEditingBottom = false

    private var topToBottomTranslator: Translator? = null
    private var bottomToTopTranslator: Translator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation)

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Initialize Text-to-Speech
        tts = TextToSpeech(this) { tts.language = Locale.ENGLISH }

        // Bind views
        val topPanel = findViewById<View>(R.id.topPanel)
        val bottomPanel = findViewById<View>(R.id.bottomPanel)
        val spinnerFrom = findViewById<Spinner>(R.id.spinner_from)
        val spinnerTo = findViewById<Spinner>(R.id.spinner_to)
        val exchangeBtn = findViewById<ImageButton>(R.id.btn_exchange)

        topInputText = topPanel.findViewById(R.id.inputText)
        bottomInputText = bottomPanel.findViewById(R.id.inputText)
        topSpinner = topPanel.findViewById(R.id.languageSpinner)
        bottomSpinner = bottomPanel.findViewById(R.id.languageSpinner)

        // Setup spinners
        val languages = listOf("English", "Spanish", "French", "Hindi")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter
        topSpinner.adapter = adapter
        bottomSpinner.adapter = adapter

        // Set initial selections
        spinnerFrom.setSelection(0)
        spinnerTo.setSelection(1)
        topSpinner.setSelection(0)
        bottomSpinner.setSelection(1)

        // Exchange button logic
        exchangeBtn.setOnClickListener {
            val fromPos = spinnerFrom.selectedItemPosition
            val toPos = spinnerTo.selectedItemPosition
            spinnerFrom.setSelection(toPos)
            spinnerTo.setSelection(fromPos)
        }

        // Language change listener
        val onLangChange = {
            val fromPos = spinnerFrom.selectedItemPosition
            val toPos = spinnerTo.selectedItemPosition

            topSpinner.setSelection(fromPos)
            bottomSpinner.setSelection(toPos)

            createTranslators()
            topInputText.text.clear()
            bottomInputText.text.clear()
        }

        spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) = onLangChange()
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) = onLangChange()
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // Sync panel spinners -> main spinners
        topSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                spinnerFrom.setSelection(pos)
            }

            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        bottomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                spinnerTo.setSelection(pos)
            }

            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // Text change watchers
        topInputText.addTextChangedListener(object : TextWatcher {
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
                    ?.addOnSuccessListener {
                        bottomInputText.setText(it)
                        isEditingBottom = false
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this@TranslationActivity, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                        isEditingBottom = false
                    }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        bottomInputText.addTextChangedListener(object : TextWatcher {
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
                    ?.addOnSuccessListener {
                        topInputText.setText(it)
                        isEditingTop = false
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this@TranslationActivity, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                        isEditingTop = false
                    }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        createTranslators()
        setupPanelControls(topPanel)
        setupPanelControls(bottomPanel)
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
            val lang = spinner.selectedItem.toString()
            tts.language = getLocaleFromLanguage(lang)
            if (text.isNotBlank()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        copyBtn.setOnClickListener {
            val text = inputText.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("translated", text))
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
        }

        clearBtn.setOnClickListener {
            inputText.setText("")
        }

        listenBtn.setOnClickListener {
            val lang = spinner.selectedItem.toString()
            val locale = getLocaleFromLanguage(lang)

            if (isListening) {
                speechRecognizer?.stopListening()
                listenBtn.setImageResource(R.drawable.speak)
                isListening = false
            } else {
                startSpeechToText(inputText, listenBtn, locale)
                isListening = true
            }
        }
    }

    private fun startSpeechToText(editText: EditText, button: ImageButton, locale: Locale) {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
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
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTranslators() {
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()

        val sourceLang = getMlKitLanguageCode(findViewById<Spinner>(R.id.spinner_from).selectedItem.toString())
        val targetLang = getMlKitLanguageCode(findViewById<Spinner>(R.id.spinner_to).selectedItem.toString())

        if (sourceLang == null || targetLang == null) {
            Toast.makeText(this, "Unsupported language selected", Toast.LENGTH_SHORT).show()
            return
        }

        topToBottomTranslator = com.google.mlkit.nl.translate.Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(sourceLang).setTargetLanguage(targetLang).build()
        )
        bottomToTopTranslator = com.google.mlkit.nl.translate.Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(targetLang).setTargetLanguage(sourceLang).build()
        )

        val conditions = DownloadConditions.Builder().build()

        topToBottomTranslator?.downloadModelIfNeeded(conditions)
        bottomToTopTranslator?.downloadModelIfNeeded(conditions)
    }

    private fun getLocaleFromLanguage(lang: String): Locale {
        return when (lang) {
            "English" -> Locale.ENGLISH
            "Spanish" -> Locale("es", "ES")
            "French" -> Locale("fr", "FR")
            "Hindi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }
    }

    private fun getMlKitLanguageCode(lang: String): String? {
        return when (lang) {
            "English" -> TranslateLanguage.ENGLISH
            "Spanish" -> TranslateLanguage.SPANISH
            "French" -> TranslateLanguage.FRENCH
            "Hindi" -> TranslateLanguage.HINDI
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        speechRecognizer?.destroy()
        topToBottomTranslator?.close()
        bottomToTopTranslator?.close()
    }
}