package com.example.logit.calendar

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.logit.R
import com.example.logit.Task
import com.example.logit.mainlog.CardColor

class RecyclerViewAdapter (
    private val tasks: List<Task>,
    private val mapSubjectColor: Map<String, Int>,
    private val cardColors: List<CardColor>
): RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivColorCode: ImageView = itemView.findViewById(R.id.colorCode)
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        val checkIcon: ImageView = itemView.findViewById(R.id.checkIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.calendar_task_card_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var checked = false
        val subject = tasks[position].subject
        val context = holder.tvSubject.context
        holder.tvSubject.text = subject
        holder.tvTaskName.text = tasks[position].task

        // set color coded tab
        val bgColorIndex: Int? = mapSubjectColor[subject]
        val bgColor: Int = if (bgColorIndex != null) {
            ContextCompat.getColor(context, cardColors[bgColorIndex].backgroundColor)
        } else {
            ContextCompat.getColor(context, R.color.gray)
        }
        holder.ivColorCode.imageTintList = ColorStateList.valueOf(bgColor)

        // check icon to mark as done
        holder.checkIcon.setOnClickListener {
            if (!checked) {
                checked = true
                holder.checkIcon.imageTintList = ColorStateList.valueOf(getColor(context, androidx.appcompat.R.attr.colorAccent))
                val completedTask: Task = tasks[position]
            } else {
                checked = false 
                holder.checkIcon.imageTintList = ColorStateList.valueOf(getColor(context, R.attr.calendarDialogCheckColor))
            }
        }
    }

    private fun getColor(context: Context, colorResId: Int): Int {
        val typedValue = TypedValue()
        val typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(colorResId))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }
}