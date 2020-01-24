package ru.lrmk.newttcalendar

import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.recyclerview.widget.RecyclerView

class SimpleRadioListAdapter<T>(var items: List<T>, val listener: ((T)->Unit)? = null) :
    RecyclerView.Adapter<SimpleRadioListAdapter<T>.PostHolder>() {
    var checked = ""
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PostHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_single_choice, parent, false))
    override fun onBindViewHolder(holder: PostHolder, position: Int) = holder.bind(items[position])
    inner class PostHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: T) = with(itemView) {
            listener?.let { view.setOnClickListener{ listener.invoke(item) } }
            with(findViewById<CheckedTextView>(android.R.id.text1)) {
                val istr = item.toString()
                text = istr
                isChecked = checked == istr
            }
        }
    }
}

class SimpleCheckListAdapter<T>(var items: List<T>, val listener: ((T)->Unit)? = null) :
    RecyclerView.Adapter<SimpleCheckListAdapter<T>.PostHolder>() {
    var checked = mutableSetOf<String>()
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PostHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false))
    override fun onBindViewHolder(holder: PostHolder, position: Int) = holder.bind(items[position])
    inner class PostHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: T) = with(itemView) {
            listener?.let { view.setOnClickListener{
                listener.invoke(item)
            } }
            with(findViewById<CheckedTextView>(android.R.id.text1)) {
                val istr = item.toString()
                text = istr
                isChecked = checked.contains(istr)
            }
        }
    }
}

class Calendar(
    var id: Long,
    var name: String
) {
    override fun toString(): String = name
}

fun fromHTML(str: String): String {
    @Suppress("DEPRECATION")
    return if (Build.VERSION.SDK_INT >= 24)
        Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
    else
        Html.fromHtml(str).toString()
}