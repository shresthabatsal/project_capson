package com.example.project_capson.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project_capson.R
import com.google.firebase.auth.FirebaseAuth

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginOverlay: View
    private lateinit var overlayLoginButton: AppCompatButton
    private lateinit var accountContent: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        loginOverlay = findViewById(R.id.login_overlay_container)
        overlayLoginButton = findViewById(R.id.btn_overlay_login)
        accountContent = findViewById(R.id.main) // Your main content view

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up login overlay button
        overlayLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Set up other buttons
        val backBtn = findViewById<ImageView>(R.id.backButton)
        backBtn.setOnClickListener {
            finish()
        }

        val personalDetailsBtn = findViewById<LinearLayout>(R.id.btnPersonalDetails)
        personalDetailsBtn.setOnClickListener {
            startActivity(Intent(this, PersonalDetails::class.java))
        }

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

        // LOGOUT logic with confirmation dialog
        val logoutBtn = findViewById<AppCompatButton>(R.id.btnLogout)
        logoutBtn.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        checkLoginStatusAndToggleOverlay()
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatusAndToggleOverlay()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout Confirmation")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                auth.signOut()
                dialog.dismiss()
                // After logout, go to WelcomeActivity
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun checkLoginStatusAndToggleOverlay() {
        val loggedIn = auth.currentUser != null

        if (!loggedIn) {
            loginOverlay.visibility = View.VISIBLE
            disableAccountInteraction()
        } else {
            loginOverlay.visibility = View.GONE
            enableAccountInteraction()
        }
    }

    private fun disableAccountInteraction() {
        // Disable all interactive elements
        findViewById<LinearLayout>(R.id.btnPrivacyPolicy).isEnabled = false
        findViewById<LinearLayout>(R.id.btnTermsConditions).isEnabled = false
        findViewById<LinearLayout>(R.id.btnAboutApp).isEnabled = false
        findViewById<LinearLayout>(R.id.btnFAQs).isEnabled = false
        findViewById<AppCompatButton>(R.id.btnLogout).isEnabled = false
    }

    private fun enableAccountInteraction() {
        // Enable all interactive elements
        findViewById<LinearLayout>(R.id.btnPrivacyPolicy).isEnabled = true
        findViewById<LinearLayout>(R.id.btnTermsConditions).isEnabled = true
        findViewById<LinearLayout>(R.id.btnAboutApp).isEnabled = true
        findViewById<LinearLayout>(R.id.btnFAQs).isEnabled = true
        findViewById<AppCompatButton>(R.id.btnLogout).isEnabled = true
    }
}