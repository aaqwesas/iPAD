package com.main.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R

data class Alert(val symbol: String, val condition: String, val target: String)

class AlertsAdapter(private val alerts: List<Alert>) : RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alert = alerts[position]
        holder.symbolTextView.text = alert.symbol
        holder.conditionTextView.text = alert.condition
        holder.targetTextView.text = alert.target
    }

    override fun getItemCount() = alerts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val symbolTextView: TextView = itemView.findViewById(R.id.alert_symbol)
        val conditionTextView: TextView = itemView.findViewById(R.id.alert_condition)
        val targetTextView: TextView = itemView.findViewById(R.id.alert_target)
    }
}
