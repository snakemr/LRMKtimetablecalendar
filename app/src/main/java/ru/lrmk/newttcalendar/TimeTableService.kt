package ru.lrmk.newttcalendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset


class TimeTableService : Service() {
    val CHANNEL_ID = "LRMK_TIMETABLE"
    //val CHANNEL_NAME = "LRMK TimeTable"
    //val CHANNEL_DESCRIPTION = "Import timetable to calendar"
    val NOTIFY_ID = 101
    val grouplist = "grouplist"
    val teacherlist = "teacherlist"
    val pairlist = "pairlist"

    private lateinit var prefs: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("SERVICETT", "START! ${flags} ${startId}")
        CoroutineScope(Dispatchers.IO).launch process@{
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            var groups = prefs.getString(grouplist, "")!!
            var teachers = prefs.getString(teacherlist, "")!!
            var pairs = prefs.getString(pairlist, "")!!

            val g = if (groups=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/groups").readText(Charset.forName("cp1251"))
                    } catch (e: IOException) {""}
                } else null
            val t = if (teachers=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/teachers").readText(Charset.forName("cp1251"))
                    } catch (e: IOException) {""}
                } else null
            val p = if (pairs=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/pairs").readText()
                    } catch (e: IOException) {""}
                } else null
            g?.let { groups = g.await();    if(groups!="")   prefs.edit().putString(grouplist, groups).apply() }
            t?.let { teachers = t.await();  if(teachers!="") prefs.edit().putString(teacherlist, teachers).apply() }
            p?.let { pairs = p.await();     if(pairs!="")    prefs.edit().putString(pairlist, pairs).apply() }
            if (groups=="" || teachers=="" || pairs=="") {
                stopSelf(startId)
                return@process
            }
            val grps = getList(groups)
            val teas = getList(teachers)
            val prs = pairs.split("<br/>").filter { it!="" }.map {
                val t = it.split(',')
                Pair(t[0].toInt(), t[1])
            }
            val myGroups = prefs.getStringSet("groups", setOf())!!
            val myTeachers = prefs.getStringSet("teachers", setOf())!!
            val iGroups = myGroups.map {gr -> grps.firstOrNull { it.second==gr }?.first}
            val iTeachers = myTeachers.map {tea -> teas.firstOrNull { it.second==tea }?.first}
            val defs = mutableListOf<Deferred<String>>()
            iGroups.map {
                it?.let {
                    //defs.add(,)
                    Log.i("SERVICETT","https://www.lrmk.ru/tt/timetable?t=$it&w=2020-01-27")
                }
            }

            val content =
            try {
                URL("https://www.lrmk.ru/tt/groups").readText(Charset.forName("cp1251"))
            }
            catch (e: IOException) {
                "Нет связи с сервером 😕"
            }
            //Log.i("SERVICETT", content)

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
                //channel.description = CHANNEL_DESCRIPTION
                channel.enableLights(true)
                channel.lightColor = Color.RED
                channel.enableVibration(false)
                notificationManager.createNotificationChannel(channel)
            }
            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_school_black_24dp)
                .setContentTitle("Расписание")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            notificationManager.notify(NOTIFY_ID, builder.build())

            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun getList(breakedString: String): List<Pair<Int, String>> {
        val items = breakedString.split("<br/>")
        val result = mutableListOf<Pair<Int, String>>()
        for (i in 0 until items.size step 2)
            if (items[i].length>0 && i+1<items.size)
                result.add(Pair(items[i].toInt(), fromHTML(items[i+1])))
        return result
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        Log.i("SERVICETT", "STOP!")
        super.onDestroy()
    }
}
