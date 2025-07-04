package com.example.project_capson.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_capson.OnboardingItem
import com.example.project_capson.R

class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.textTitle)
        private val description = view.findViewById<TextView>(R.id.textDescription)
        private val image = view.findViewById<ImageView>(R.id.slideImage)

        fun bind(item: OnboardingItem) {
            title.text = item.title
            description.text = item.description
            image.setImageResource(item.image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.slider_layout, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
