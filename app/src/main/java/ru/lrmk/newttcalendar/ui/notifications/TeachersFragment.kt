package ru.lrmk.newttcalendar.ui.notifications

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import ru.lrmk.newttcalendar.R
import ru.lrmk.newttcalendar.SimpleCheckListAdapter

class TeachersFragment : Fragment() {

    private lateinit var teachersViewModel: TeachersViewModel
    private lateinit var prefs: SharedPreferences
    private val adapter = SimpleCheckListAdapter(listOf(), ::onClick)
    val teachers = "teachers"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        teachersViewModel = ViewModelProviders.of(this).get(TeachersViewModel::class.java)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val root = inflater.inflate(R.layout.fragment_teachers, container, false)
        val list = root.findViewById<RecyclerView>(R.id.list)
        list.adapter = adapter
        val set = prefs.getStringSet(teachers, setOf())
        if (set != null) adapter.checked = set.toMutableSet()

        teachersViewModel.text.observe(this, Observer {
            val items = it.split("<br/>")
            val teachers = mutableListOf<Teacher>()
            if (items.size==1) {
                teachers.add(Teacher(0, it))
            }
            else if (items.size>1) {
                prefs.edit().putString("teacherlist", it).apply()
                for (i in 0 until items.size step 2)
                    if (items[i].length>0 && i+1<items.size)
                        teachers.add(Teacher(items[i].toLong(), items[i + 1]))
            }
            teachers.sort()
            adapter.items = teachers
            adapter.notifyDataSetChanged()
        })
        return root
    }

    class Teacher (
        var id: Long,
        var name: String
    ) : Comparable<Teacher> {
        override fun compareTo(other: Teacher) = this.name.compareTo(other.name)
        override fun toString(): String = name
    }

    fun onClick(item: Teacher) {
        if (adapter.checked.contains(item.name))
            adapter.checked.remove(item.name)
        else
            adapter.checked.add(item.name)
        prefs.edit().putStringSet(teachers, adapter.checked).commit()
        adapter.notifyDataSetChanged()
    }
}