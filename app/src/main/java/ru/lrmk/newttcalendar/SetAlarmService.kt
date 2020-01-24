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

class SetAlarmService : Service() {
    private lateinit var prefs: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val groups = prefs.getStringSet("groups", setOf())
        val teachers = prefs.getStringSet("teachers", setOf())
        val period = prefs.getInt("period", 0)
        val calendar = prefs.getLong("calendar", 0)

        Log.i("SERVICETT", "SET ALARMS ${flags} ${startId} $groups $teachers $period $calendar")

        if ( ( (groups!=null && groups.size>0) || (teachers!=null && teachers.size>0) ) && period>0)
        CoroutineScope(Dispatchers.Main).launch {
            val manager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val calendar = java.util.Calendar.getInstance()
            calendar.setTimeInMillis(System.currentTimeMillis())
            calendar.add(java.util.Calendar.SECOND, 1)
            val time = calendar.timeInMillis

            val intent = Intent(applicationContext, TimeTableService::class.java)
            val pending = PendingIntent.getService(applicationContext, 1, intent, 0)

            manager.set(AlarmManager.RTC_WAKEUP, time, pending)
            Log.i("SERVICETT", "SET $manager $intent $pending")

            stopSelf(startId)
        }
        else stopSelf(startId)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Log.i("SERVICETT", "ALARMS SET, STOPPED")
        super.onDestroy()
    }
}
