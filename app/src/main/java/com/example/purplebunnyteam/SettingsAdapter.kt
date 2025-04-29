package com.example.purplebunnyteam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(
    private val settingsList: List<SettingsItem>,
    private val textColor: Int,
    private val onItemClick: (Int) -> Unit //This will pass the position or ID.
) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    class SettingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        val subTitleTextView: TextView = view.findViewById(R.id.subTitleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings, parent, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val item = settingsList[position]
        holder.titleTextView.text = item.title
        holder.subTitleTextView.text = item.subtitle

        //This will set text color dynamically based on theme
        holder.titleTextView.setTextColor(textColor)
        holder.subTitleTextView.setTextColor(textColor)




        //This will handle the item click.
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }






    }

    override fun getItemCount(): Int {
        return settingsList.size
    }
}