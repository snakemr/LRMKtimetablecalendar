package ru.lrmk.newttcalendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class SetAlarmService : Service() {
    private lateinit var prefs: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val groups = prefs.getStringSet("groups", setOf())
        val teachers = prefs.getStringSet("teachers", setOf())
        val period = prefs.getInt("period", 0)
        val calend = prefs.getLong("calendar", 0)

        Log.i("SERVICETT", "SET ALARMS ${flags} ${startId} $groups $teachers $period $calend")

        if ( ( (groups!=null && groups.size>0) || (teachers!=null && teachers.size>0) ) && period>0)
        CoroutineScope(Dispatchers.Main).launch {
            val manager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val calendar = Calendar.getInstance()
            calendar.setTimeInMillis(System.currentTimeMillis())
            calendar.set(Calendar.DAY_OF_WEEK, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 14)
            calendar.set(Calendar.MINUTE, (0..59).random())
            //calendar.add(java.util.Calendar.WEEK_OF_MONTH, 1)
            //calendar.add(java.util.Calendar.SECOND, 1)
            val time = calendar.timeInMillis
            Log.i("SERVICETT", "TIME $calendar ${(time-System.currentTimeMillis())/3600000}")

            val intentt = Intent(applicationContext, TimeTableService::class.java)
            val pending = PendingIntent.getService(applicationContext, 1, intentt, 0)

            //manager.set(AlarmManager.RTC_WAKEUP, time, pending)
            manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, pending)
            Log.i("SERVICETT", "SET $manager $intent $pending")

            stopSelf(startId)
        }
        else stopSelf(startId)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        Log.i("SERVICETT", "ALARMS SET, STOPPED")
        super.onDestroy()
    }
}
