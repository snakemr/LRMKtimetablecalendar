package ru.lrmk.newttcalendar.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import ru.lrmk.newttcalendar.*


class HomeFragment : Fragment(), SeekBar.OnSeekBarChangeListener {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: SimpleCheckListAdapter<String>
    private lateinit var calendars: SimpleRadioListAdapter<Calendar>
    private lateinit var every: TextView
    val choose = "Выберите группу или преподавателя"
    val group = "Группа "
    val groups = "groups"
    val teachers = "teachers"
    val calendar = "calendar"
    val period = "period"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        homeViewModel.context = context!!
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val list = root.findViewById<RecyclerView>(R.id.list)
        val set1 = prefs.getStringSet(groups, setOf())
        val set2 = prefs.getStringSet(teachers, setOf())
        val set = mutableSetOf<String>()
        if (set1 != null) set.addAll(set1.map { group+it } )
        if (set2 != null) set.addAll(set2)
        if (set.size == 0) set.add(choose)
        adapter = SimpleCheckListAdapter(set.take(5).toList(), ::onClick)
        adapter.checked = set
        list.adapter = adapter

        val cals = root.findViewById<RecyclerView>(R.id.calendars)
        calendars = SimpleRadioListAdapter(listOf(), ::onClick)
        cals.adapter = calendars

        every = root.findViewById(R.id.every)
        val seek = root.findViewById<SeekBar>(R.id.seekBar)
        seek.setOnSeekBarChangeListener(this)
        seek.progress = prefs.getInt(period, 2)

        root.findViewById<Button>(R.id.button_now).setOnClickListener(::updateNow)

        homeViewModel.text.observe(this, Observer {
            calendars.items = it
            val current = it.find { it.id==prefs.getLong(calendar, -1) }
            current?.let { calendars.checked = current.name }
            calendars.notifyDataSetChanged()

        })
        return root
    }

    fun onClick(item: String) {
        if (item == choose) {
            val navController = activity!!.findNavController(R.id.nav_host_fragment)
            navController.navigate(R.id.navigation_dashboard)
            return
        }
        var pref = teachers
        var name = item
        if (item.startsWith(group)) {
            pref = groups
            name = item.replace(group,"")
        }
        val set = prefs.getStringSet(pref, setOf())!!.toMutableSet()
        if (adapter.checked.contains(item)) {
            adapter.checked.remove(item)
            set.remove(name)
        } else {
            adapter.checked.add(item)
            set.add(name)
        }
        prefs.edit().putStringSet(pref, set).apply()
        adapter.notifyDataSetChanged()
        setAlarms()
    }

    override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
        every.setText(when(progress){
            1 -> R.string.when_weekly
            2 -> R.string.when_daily
            3 -> R.string.when_often
            else -> R.string.when_manually
        })
        if (fromUser) {
            prefs.edit().putInt(period, progress).apply()
            setAlarms()
        }
    }
    override fun onStartTrackingTouch(p0: SeekBar?) {}
    override fun onStopTrackingTouch(p0: SeekBar?) {}

    fun onClick(item: Calendar) {
        if (item.id == 0L) return
        calendars.checked = item.name
        prefs.edit().putLong(calendar, item.id).apply()
        calendars.notifyDataSetChanged()
        setAlarms()
    }

    fun setAlarms() {
        activity!!.startService(Intent(activity, SetAlarmService::class.java))
    }

    fun updateNow(v: View){
        activity!!.startService(Intent(activity, TimeTableService::class.java))

        /*
        val CHANNEL_ID = "MyChan"
        val NOTIFY_ID = 101
        val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "My channel", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "My channel description"
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(activity as AppCompatActivity, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_school_black_24dp)
            .setContentTitle("Напоминание")
            .setContentText("Пора покормить кота")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(NOTIFY_ID, builder.build())
        */
        /*
        val manager = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = java.util.Calendar.getInstance()
        calendar.setTimeInMillis(System.currentTimeMillis())
        calendar.add(java.util.Calendar.SECOND, 1)
        val time = calendar.timeInMillis

        val intent = Intent(activity, TimeTableService::class.java)
        //activity!!.startService(intent)
        val pending = PendingIntent.getService(context, 1, intent, 0)
        manager.set(AlarmManager.RTC_WAKEUP, time, pending)
        Log.i("SERVICETT", "SET $manager $intent $pending")
        */
    }
}