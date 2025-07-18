package com.example.project_capson.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project_capson.R

class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button logic
        val backBtn = findViewById<ImageView>(R.id.backButton)
        backBtn.setOnClickListener {
            finish()
        }

        // Privacy Policy click listener
        val privacyPolicyBtn = findViewById<LinearLayout>(R.id.btnPrivacyPolicy)
        privacyPolicyBtn.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
        val termsBtn = findViewById<LinearLayout>(R.id.btnTermsConditions)
        termsBtn.setOnClickListener {
            startActivity(Intent(this, TermsAndCondition::class.java))
        }
        val aboutAppBtn = findViewById<LinearLayout>(R.id.btnAboutApp)
        aboutAppBtn.setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }
        val faqsBtn = findViewById<LinearLayout>(R.id.btnFAQs)
        faqsBtn.setOnClickListener {
            startActivity(Intent(this, FAQActivity::class.java))
        }
    }
}
