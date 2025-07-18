package com.example.project_capson.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.project_capson.databinding.ActivityWelcomeBinding


class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // “Get Started” -> send user to Sign‑Up or Login
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
//
//        // “Continue Without An Account” -> maybe MainActivity
        binding.tvSkip.setOnClickListener {
            startActivity(Intent(this, BottomNavActivity::class.java))
        }
        }
    }

