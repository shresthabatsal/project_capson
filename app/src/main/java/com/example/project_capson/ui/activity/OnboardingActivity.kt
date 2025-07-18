package com.example.project_capson.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.project_capson.OnboardingItem
import com.example.project_capson.R
import com.example.project_capson.adapter.OnboardingAdapter

class OnboardingActivity : AppCompatActivity() {

    private lateinit var indicators: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Initialize slides
        val items = listOf(
            OnboardingItem("Understand Instantly", "Get live translated captions\n as people speak.", R.drawable.onboarding1),
            OnboardingItem("Talk Without Barriers", "Speak or type in your language and\n see instant translations\n in conversations.", R.drawable.onboarding2),
            OnboardingItem("Scan & Go", "Scan printed text to listen or\ntranslate instantly, anytime,\n anywhere", R.drawable.onboarding3)
        )

        // Setup ViewPager
        val adapter = OnboardingAdapter(items)
        val viewPager = findViewById<ViewPager2>(R.id.slideViewPager)
        viewPager.adapter = adapter

        // Setup indicators
        indicators = findViewById(R.id.indicatorContainer)
        setupIndicators(items.size)
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setCurrentIndicator(position)
            }
        })

        // Setup button actions
        findViewById<Button>(R.id.skipButton).setOnClickListener {
            val intent= Intent(
                this@OnboardingActivity,
                WelcomeActivity::class.java
            )
            startActivity(intent)
        }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            if (viewPager.currentItem + 1 < adapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                val intent= Intent(
                    this@OnboardingActivity,
                    WelcomeActivity::class.java
                )
                startActivity(intent)
            }
        }
    }

    private fun setupIndicators(count: Int) {
        indicators.removeAllViews() // Ensure the container is cleared first
        val layoutParams = LinearLayout.LayoutParams(
            24, // Width in pixels (e.g., 24px or any desired size)
            24  // Height in pixels
        )
        layoutParams.setMargins(16, 0, 16, 0) // Adjust spacing between indicators

        repeat(count) {
            val imageView = ImageView(this)
            imageView.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.indicator_inactive)
            )
            imageView.layoutParams = layoutParams
            indicators.addView(imageView)
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = indicators.childCount
        for (i in 0 until childCount) {
            val imageView = indicators.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.indicator_active)
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.indicator_inactive)
                )
            }
        }
    }
}