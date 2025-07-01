package com.example.project_capson.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.project_capson.ui.fragment.ConversationFragment
import com.example.project_capson.ui.fragment.LiveFragment
import com.example.project_capson.R
import com.example.project_capson.ui.fragment.ScanFragment
import com.example.project_capson.databinding.ActivityBottomNavBinding

class BottomNavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(LiveFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_live -> loadFragment(LiveFragment())
                R.id.nav_conversation -> loadFragment(ConversationFragment())
                R.id.nav_scan -> loadFragment(ScanFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}