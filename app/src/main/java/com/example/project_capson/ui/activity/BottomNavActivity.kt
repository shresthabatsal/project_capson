package com.example.project_capson.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.project_capson.R
import com.example.project_capson.databinding.ActivityBottomNavBinding
import com.example.project_capson.ui.fragment.ConversationFragment
import com.example.project_capson.ui.fragment.LiveFragment
import com.example.project_capson.ui.fragment.ScanFragment

class BottomNavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding

    enum class TabType {
        LIVE, CONVERSATION, SCAN
    }

    private var currentTab: TabType = TabType.CONVERSATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default selected bottom navigation item
        binding.bottomNav.selectedItemId = R.id.nav_conversation

        // Load default fragment
        loadFragment(ConversationFragment())
        updateTopBar(currentTab)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_live -> {
                    currentTab = TabType.LIVE
                    loadFragment(LiveFragment())
                }
                R.id.nav_conversation -> {
                    currentTab = TabType.CONVERSATION
                    loadFragment(ConversationFragment())
                }
                R.id.nav_scan -> {
                    currentTab = TabType.SCAN
                    loadFragment(ScanFragment())
                }
            }
            updateTopBar(currentTab)
            true
        }

        binding.helpButton.setOnClickListener {
            val intent = when (currentTab) {
                TabType.LIVE -> Intent(this, Help1Activity::class.java)
                TabType.CONVERSATION -> Intent(this, Help2Activity::class.java)
                TabType.SCAN -> Intent(this, Help3Activity::class.java)
            }
            startActivity(intent)
        }

        binding.profileButton.setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateTopBar(tab: TabType) {
        val title = when (tab) {
            TabType.LIVE -> "Live"
            TabType.CONVERSATION -> "Translate"
            TabType.SCAN -> "Scan"
        }
        binding.topBarTitle.text = title
    }
}