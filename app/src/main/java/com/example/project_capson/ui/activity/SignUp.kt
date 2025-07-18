//
//package com.example.project_capson.ui.activity
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.*
//import androidx.appcompat.app.AppCompatActivity
//import com.example.project_capson.R
//import com.google.android.material.textfield.TextInputEditText
//import com.google.firebase.auth.FirebaseAuth
//
//class SignUp : AppCompatActivity() {
//
//    private lateinit var auth: FirebaseAuth
//
//    private lateinit var fullNameEditText: TextInputEditText
//    private lateinit var emailEditText: TextInputEditText
//    private lateinit var passwordEditText: TextInputEditText
//    private lateinit var checkboxTerms: CheckBox
//    private lateinit var signUpButton: Button
//    private lateinit var loginRedirect: TextView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_sign_up)
//
//        // Firebase instance
//        auth = FirebaseAuth.getInstance()
//
//        // Initialize views
//        fullNameEditText = findViewById(R.id.editTextFullName)
//        emailEditText = findViewById(R.id.editTextSignupEmail)
//        passwordEditText = findViewById(R.id.editTextSignupPassword)
//        checkboxTerms = findViewById(R.id.checkboxTerms)
//        signUpButton = findViewById(R.id.buttonSignup)
//        loginRedirect = findViewById(R.id.textViewLoginRe)
//
//        signUpButton.setOnClickListener {
//            val fullName = fullNameEditText.text.toString().trim()
//            val email = emailEditText.text.toString().trim()
//            val password = passwordEditText.text.toString().trim()
//
//            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            if (!checkboxTerms.isChecked) {
//                Toast.makeText(this, "You must agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Register user with Firebase
//            auth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
//
//                        // Optional: Save user's full name to Firebase Database here if needed
//                        // Navigate to Login or Home
//                        val intent = Intent(this, LoginActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    } else {
//                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//                    }
//                }
//        }
//
//        // Login redirect text click
//        loginRedirect.setOnClickListener {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//        }
//    }
//}

package com.example.project_capson.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project_capson.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var checkboxTerms: CheckBox
    private lateinit var signUpButton: Button
    private lateinit var loginRedirect: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        fullNameEditText = findViewById(R.id.editTextFullName)
        emailEditText = findViewById(R.id.editTextSignupEmail)
        passwordEditText = findViewById(R.id.editTextSignupPassword)
        checkboxTerms = findViewById(R.id.checkboxTerms)
        signUpButton = findViewById(R.id.buttonSignup)
        loginRedirect = findViewById(R.id.textViewLoginRe)

        signUpButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkboxTerms.isChecked) {
                Toast.makeText(this, "You must agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser

                        // ✅ Update display name (full name)
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    // Navigate to login screen
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to save name: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
