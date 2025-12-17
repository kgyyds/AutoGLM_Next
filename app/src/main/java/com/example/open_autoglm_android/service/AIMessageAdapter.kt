package com.example.open_autoglm_android.service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.open_autoglm_android.R

class AIMessageAdapter :
    ListAdapter<AIMessage, AIMessageAdapter.AIMessageViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AIMessage>() {
            override fun areItemsTheSame(oldItem: AIMessage, newItem: AIMessage) = oldItem === newItem
            override fun areContentsTheSame(oldItem: AIMessage, newItem: AIMessage) =
                oldItem.content == newItem.content
        }
    }

    inner class AIMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvMessageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AIMessageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_ai_message, parent, false)
        )

    override fun onBindViewHolder(holder: AIMessageViewHolder, position: Int) {
        holder.text.text = getItem(position).content
    }
}
