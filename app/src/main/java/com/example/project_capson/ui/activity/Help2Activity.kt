package com.example.project_capson.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project_capson.R
import com.example.project_capson.databinding.ActivityHelp1Binding

class Help2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityHelp1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelp1Binding.inflate(layoutInflater)
        setContentView(R.layout.activity_help2)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}