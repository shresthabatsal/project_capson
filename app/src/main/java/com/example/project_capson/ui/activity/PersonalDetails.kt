

package com.example.project_capson.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project_capson.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class PersonalDetails : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var editSaveButton: Button
    private lateinit var changePasswordTextView: TextView
    private lateinit var backButton: ImageView
    private lateinit var logoutButton: Button


    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        fullNameEditText = findViewById(R.id.editTextFullName)
        emailEditText = findViewById(R.id.editTextEmail)
        editSaveButton = findViewById(R.id.buttonEditSave)
        changePasswordTextView = findViewById(R.id.textViewChangePassword)
        backButton = findViewById(R.id.buttonBack)

        setEditingEnabled(false)
        loadUserInfo()

        editSaveButton.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                setEditingEnabled(true)
                editSaveButton.text = getString(R.string.save)
            } else {
                val newName = fullNameEditText.text.toString().trim()
                val newEmail = emailEditText.text.toString().trim()

                if (newName.isEmpty() || newEmail.isEmpty()) {
                    Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val user = auth.currentUser
                user?.let {
                    if (newName != user.displayName) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build()
                        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show()
                                // Update UI immediately
                                fullNameEditText.setText(newName)
                            } else {
                                Toast.makeText(this, "Name update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    if (newEmail != user.email) {
                        showReAuthDialog { currentPassword ->
                            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                                if (reauthTask.isSuccessful) {
                                    user.verifyBeforeUpdateEmail(newEmail)
                                        .addOnCompleteListener { verifyTask ->
                                            if (verifyTask.isSuccessful) {
                                                Toast.makeText(
                                                    this,
                                                    "Verification email sent to $newEmail. Verify to complete update.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                // Don't update email field until verified
                                            } else {
                                                Toast.makeText(
                                                    this,
                                                    "Email update failed: ${verifyTask.exception?.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Re-authentication failed: ${reauthTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }

                isEditing = false
                setEditingEnabled(false)
                editSaveButton.text = getString(R.string.Edit)
            }
        }

        changePasswordTextView.setOnClickListener {
            startActivity(Intent(this, ChangePw::class.java))
        }

//        backButton.setOnClickListener {
//            finish()
//        }
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }


    }

    override fun onResume() {
        super.onResume()
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Check if email has been verified and updated
                loadUserInfo()
            } else {
                Toast.makeText(this, "Failed to refresh user info", Toast.LENGTH_SHORT).show()
                Log.e("USER_RELOAD", "Error reloading user", task.exception)
            }
        }
    }

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            fullNameEditText.setText(user.displayName ?: "")
            emailEditText.setText(user.email ?: "")
        }
    }

    private fun setEditingEnabled(enabled: Boolean) {
        fullNameEditText.isEnabled = enabled
        emailEditText.isEnabled = enabled
    }

    private fun showReAuthDialog(onPasswordEntered: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Re-enter Password")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Current Password"
        builder.setView(input)

        builder.setPositiveButton("Confirm") { dialog, _ ->
            val password = input.text.toString()
            if (password.isNotEmpty()) {
                onPasswordEntered(password)
            } else {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}
