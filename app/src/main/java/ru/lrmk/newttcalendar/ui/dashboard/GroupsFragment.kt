package ru.lrmk.newttcalendar.ui.dashboard

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
import ru.lrmk.newttcalendar.fromHTML

class GroupsFragment : Fragment() {

    private lateinit var groupsViewModel: GroupsViewModel
    private lateinit var prefs: SharedPreferences
    private val adapter = SimpleCheckListAdapter(listOf(), ::onClick)
    val groups = "groups"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupsViewModel = ViewModelProviders.of(this).get(GroupsViewModel::class.java)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val root = inflater.inflate(R.layout.fragment_groups, container, false)
        val list = root.findViewById<RecyclerView>(R.id.list)
        list.adapter = adapter
        val set = prefs.getStringSet(groups, setOf())
        if (set != null) adapter.checked = set.toMutableSet()

        groupsViewModel.text.observe(this, Observer {
            val items = it.split("<br/>")
            val groups = mutableListOf<Group>()
            if (items.size==1) {
                groups.add(Group(0, it))
            }
            else if (items.size>1) {
                prefs.edit().putString("grouplist", it).apply()
                for (i in 0 until items.size step 2)
                    if (items[i].length>0 && i+1<items.size)
                        groups.add(Group(items[i].toLong(), fromHTML(items[i+1])))
            }
            groups.sort()
            adapter.items = groups
            adapter.notifyDataSetChanged()
        })
        return root
    }

    class Group (
        var id: Long,
        var name: String
    ) : Comparable<Group> {
        override fun compareTo(other: Group): Int{
            val a = this.name.takeLast(1).replace(")","9")
            val b = other.name.takeLast(1).replace(")","9")
            val c = a.compareTo(b)
            return if (c!=0) c else this.name.compareTo(other.name)
        }
        override fun toString(): String = name
    }

    fun onClick(item: Group) {
        if (adapter.checked.contains(item.name))
            adapter.checked.remove(item.name)
        else
            adapter.checked.add(item.name)
        prefs.edit().putStringSet(groups, adapter.checked).commit()
        adapter.notifyDataSetChanged()
    }
}