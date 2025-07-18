package com.example.project_capson.utils

import android.util.Log

object NepaliHindiMapping {
    private val nepaliToHindiMap = mapOf(
        // Common words
        "नमस्ते" to "नमस्ते",
        "धन्यवाद" to "धन्यवाद",
        "हजुर" to "आप",
        "माया" to "प्यार",
        "आमा" to "माँ",
        "बुबा" to "पिता",
        "दिदी" to "बहन",
        "दाई" to "भाई",
        "पानी" to "पानी",
        "खाना" to "खाना",
        "घर" to "घर",
        "स्कूल" to "स्कूल",
        "किताब" to "किताब",
        "लेखन" to "लेख",
        "पढ्न" to "पढ़ना",
        "बस" to "बस",
        "गाडी" to "गाड़ी",
        "हवाईअड्डा" to "हवाई अड्डा",
        "होटल" to "होटल",
        "डाक्टर" to "डॉक्टर",

        // Common phrases
        "तपाईंलाई कस्तो छ?" to "आपको कैसा है?",
        "म ठिक छु" to "मैं ठीक हूँ",
        "म ठिक छु।" to "मैं ठीक हूं",
        "तिमीलाई माया छ" to "तुमसे प्यार है",
        "के भयो?" to "क्या हुआ?",
        "मलाई मद्दत चाहियो" to "मुझे मदद चाहिए",
        "यो कति हो?" to "यह कितना है?",
        "कहाँ छ?" to "कहाँ है?",
        "कहिले आउँछ?" to "कब आएगा?",
        "म बुझ्दिन" to "मैं नहीं समझता",
        "फेरि भन्नुहोस्" to "फिर से बोलिए",
        "मेरो नाम हो" to "मेरा नाम है",
        "तपाईंको नाम के हो?" to "आपका नाम क्या है?",
        "म नेपाली हुँ" to "मैं नेपाली हूँ",
        "यहाँ फोटोकपी गरिन्छ" to "यहां फोटोकॉपी की जाती है।",
        "दही पनि पाइन्छ" to "दही भी उपलब्ध है",
        "दही पनि पाइन्छ।" to "दही भी उपलब्ध है",
        "यहाँ फोटोकपी गरिन्छ" to "फोटोकॉपी यहाँ किया जाता है"
    )

    // Create reverse mapping for Hindi to Nepali
    private val hindiToNepaliMap = nepaliToHindiMap.entries.associate { (k, v) -> v to k }

    fun translateNepaliToHindi(nepaliText: String): String {
        val normalizedText = nepaliText
            .replace(Regex("[\\n\\r]+"), " ")  // Replace line breaks with space
            .replace(Regex("\\s+"), " ")       // Replace multiple spaces with single space
            .trim()

        Log.d("NepaliHindiMapping", "Normalized Nepali text: '$normalizedText'")

        if (nepaliToHindiMap.containsKey(normalizedText)) {
            return nepaliToHindiMap[normalizedText]!!
        }

        val words = normalizedText.split(" ")
        return words.joinToString(" ") { word ->
            nepaliToHindiMap[word] ?: word
        }
    }

    fun translateHindiToNepali(hindiText: String): String {
        val normalizedText = hindiText
            .replace(Regex("[\\n\\r]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        Log.d("NepaliHindiMapping", "Normalized Hindi text: '$normalizedText'")

        if (hindiToNepaliMap.containsKey(normalizedText)) {
            return hindiToNepaliMap[normalizedText]!!
        }

        val words = normalizedText.split(" ")
        return words.joinToString(" ") { word ->
            hindiToNepaliMap[word] ?: word
        }
    }
}