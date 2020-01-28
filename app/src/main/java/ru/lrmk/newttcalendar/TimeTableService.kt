package ru.lrmk.newttcalendar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.IBinder
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar


class TimeTableService : Service() {
    val CHANNEL_ID = "LRMK_TIMETABLE"
    //val CHANNEL_NAME = "LRMK TimeTable"
    //val CHANNEL_DESCRIPTION = "Import timetable to calendar"
    val NOTIFY_ID = 101
    val grouplist = "grouplist"
    val teacherlist = "teacherlist"
    val pairlist = "pairlist"
    val roomlist = "roomlist"
    val timetable = "timetable"
    val switchweek = "week"
    val br = "<br/>"
    val cp1251 = "cp1251"

    private lateinit var prefs: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("SERVICETT", "START! ${flags} ${startId}")
        CoroutineScope(Dispatchers.IO).launch process@{
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val calend = prefs.getLong("calendar", 0)
            var groups = prefs.getString(grouplist, "")!!
            var teachers = prefs.getString(teacherlist, "")!!
            var pairs = prefs.getString(pairlist, "")!!
            var rooms = prefs.getString(roomlist, "")!!
            var week = intent?.getIntExtra(switchweek, -1) ?: -1
            var reset = false

            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED
                || calend == 10000L) {
                stopSelf(startId)
                return@process
            }

            val time = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.setTimeInMillis(time)
            if (week<0) {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 17)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                if (calendar.timeInMillis < time) reset = true

                calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 13)
                week = if (calendar.timeInMillis < time) 1 else 0
            }
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (week>0) calendar.add(Calendar.WEEK_OF_MONTH, 1)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val monday = sdf.format(calendar.time)

            if (reset) {
                groups = ""; teachers=""; pairs=""; rooms=""
                Log.i("SERVICETT", "RESET LISTS")
            }

            val g = if (groups=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/groups").readText(Charset.forName(cp1251))
                    } catch (e: IOException) {""}
                } else null
            val t = if (teachers=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/teachers").readText(Charset.forName(cp1251))
                    } catch (e: IOException) {""}
                } else null
            val p = if (pairs=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/pairs").readText()
                    } catch (e: IOException) {""}
                } else null
            val r = if (rooms=="")
                async {
                    try {
                        URL("https://www.lrmk.ru/tt/rooms").readText(Charset.forName(cp1251))
                    } catch (e: IOException) {""}
                } else null
            g?.let { groups = g.await();    if(groups!="")   prefs.edit().putString(grouplist, groups).apply() }
            t?.let { teachers = t.await();  if(teachers!="") prefs.edit().putString(teacherlist, teachers).apply() }
            p?.let { pairs = p.await();     if(pairs!="")    prefs.edit().putString(pairlist, pairs).apply() }
            r?.let { rooms = r.await();     if(rooms!="")    prefs.edit().putString(roomlist, rooms).apply() }

            if (groups=="" || teachers=="" || pairs=="" || rooms=="") {
                stopSelf(startId)
                return@process
            }
            val grps = getList(groups)
            val teas = getList(teachers)
            val rms = getList(rooms)
            val prs = pairs.split(br).filter { it!="" }.map {
                val pair = it.split(',')
                Triple(pair[0].toInt(), pair[1], pair[2])
            }

            val myGroups = prefs.getStringSet("groups", setOf())!!.take(5)
            val myTeachers = prefs.getStringSet("teachers", setOf())!!.take(5-myGroups.size)
            val iGroups = myGroups.map {gr -> grps.firstOrNull { it.second==gr }?.first}
            val iTeachers = myTeachers.map {tea -> teas.firstOrNull { it.second==tea }?.first}
            Log.i("SERVICETT", "$myGroups g=$iGroups $myTeachers t=$iTeachers")
            val jobs = mutableListOf<Deferred<String>>()
            val disc = mutableListOf<Deferred<String>>()
            var dsc = ""
            iGroups.map {
                it?.let {
                    jobs.add(async {
                        try {
                            Log.i("SERVICETT", "https://www.lrmk.ru/tt/timetable?t=$it&w=$monday")
                            URL("https://www.lrmk.ru/tt/timetable?g=$it&w=$monday").readText()
                        } catch (e: IOException) {""}
                    })
                    val git = if (reset) "" else prefs.getString("g$it", "")!!
                    if (git == "")
                        disc.add(async {
                            try {
                                val d = URL("https://www.lrmk.ru/tt/discplines?g=$it").readText(Charset.forName(cp1251))
                                prefs.edit().putString("g$it", d).apply()
                                d
                            } catch (e: IOException) {""}
                        })
                    else dsc += git
                }
            }
            iTeachers.map {
                it?.let {
                    jobs.add(async {
                        try {
                            Log.i("SERVICETT", "https://www.lrmk.ru/tt/timetable?t=$it&w=$monday")
                            URL("https://www.lrmk.ru/tt/timetable?t=$it&w=$monday").readText()
                        } catch (e: IOException) {""}
                    })
                    val tit = if (reset) "" else prefs.getString("t$it", "")!!
                    if (tit == "")
                        disc.add(async {
                            try {
                                URL("https://www.lrmk.ru/tt/discplines?t=$it").readText(Charset.forName(cp1251))
                            } catch (e: IOException) {""}
                        })
                    else dsc += tit
                }
            }
            val tt = jobs.fold("") { start, next -> start + next.await() }
            //Log.i("SERVICETT", "$tt")
            dsc = disc.fold(dsc) { start, next -> start + next.await() }
            //Log.i("SERVICETT", "$dsc")

            if (tt == prefs.getString(timetable, "")) {
                Log.i("SERVICETT", "no changes, exiting")
                stopSelf(startId)
                return@process
            }
            prefs.edit().putString(timetable, tt).apply()

            val dscs = getList(dsc).map {
                val sec = it.second.split(' ')
                Triple(it.first, sec[0].toInt(), sec[1])
            }

            val ttt = tt.split(br).filter { it!="" }.map {
                val items = it.split(',').map { it.toIntOrNull() }
                val d =  dscs.firstOrNull{ it.first==items[3] }
                val gr = grps.firstOrNull { it.first == d?.second }?.second ?: ""

                val values = ContentValues()
                calendar.set(Calendar.DAY_OF_WEEK, (items[0] ?: 1) % 7 + 1)

                val begin = (prs.firstOrNull{ it.first==items[1] }?.second ?: "0:0").split(':').map { it.toIntOrNull() }
                begin[0]?.let { calendar.set(Calendar.HOUR_OF_DAY, it) }
                begin[1]?.let { calendar.set(Calendar.MINUTE, it) }
                values.put(CalendarContract.Events.DTSTART, calendar.timeInMillis)

                val end = (prs.firstOrNull{ it.first==items[1] }?.second ?: "0:0").split(':').map { it.toIntOrNull() }
                end[0]?.let { calendar.set(Calendar.HOUR_OF_DAY, it) }
                end[1]?.let { calendar.set(Calendar.MINUTE, it) }
                values.put(CalendarContract.Events.DTEND, calendar.timeInMillis)

                values.put(CalendarContract.Events.TITLE, gr)
                values.put(CalendarContract.Events.DESCRIPTION, d?.third ?: "")
                values.put(CalendarContract.Events.CALENDAR_ID, calend)
                values.put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Moscow")
                Log.i("SERVICETT", "$values")
                //if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
                contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

                listOf(
                    items[0],
                    prs.firstOrNull{ it.first==items[1] }?.second ?: "",
                    grps.firstOrNull { it.first == d?.second }?.second ?: "",
                    d?.third ?: "",
                    teas.firstOrNull{ it.first==items[4] }?.second ?: "",
                    if (items[4]!=items[5]) teas.firstOrNull{ it.first==items[5] }?.second ?: "" else "",
                    rms.firstOrNull{ it.first==items[6] }?.second ?: "",
                    if (items[6]!=items[7]) rms.firstOrNull{ it.first==items[7] }?.second ?: "" else ""
                )
            }
            Log.i("SERVICETT", "$ttt")

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
                .setContentText(ttt.toString())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            notificationManager.notify(NOTIFY_ID, builder.build())

            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun getList(breakedString: String): List<Pair<Int, String>> {
        val items = breakedString.split(br)
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
