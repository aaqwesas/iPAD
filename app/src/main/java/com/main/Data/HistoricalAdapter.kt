package com.main.Data

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.main.models.HistoricalBar

class HistoricalAdapter :
    ListAdapter<HistoricalBar, HistoricalAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoricalBar>() {
            override fun areItemsTheSame(old: HistoricalBar, new: HistoricalBar) =
                old.date == new.date

            override fun areContentsTheSame(old: HistoricalBar, new: HistoricalBar) =
                old == new
        }
    }

    inner class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
    ) {
        private val tv1 = itemView.findViewById<TextView>(android.R.id.text1)
        private val tv2 = itemView.findViewById<TextView>(android.R.id.text2)

        fun bind(item: HistoricalBar) {
            tv1.text = item.date
            tv2.text = "O:${item.open} H:${item.high} L:${item.low} C:${item.close} V:${item.volume}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent)

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}