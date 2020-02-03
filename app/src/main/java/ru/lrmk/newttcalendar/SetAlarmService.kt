package ru.lrmk.newttcalendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class SetAlarmService : Service() {
    private lateinit var prefs: SharedPreferences
    private val times = arrayOf( intArrayOf( 8,  9, 11, 12, 14, 16, 17),
                                 intArrayOf( 0, 30, 20, 50, 30,  0, 25) )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val groups = prefs.getStringSet("groups", setOf())
        val teachers = prefs.getStringSet("teachers", setOf())
        val period = prefs.getInt("period", 0)
        val calend = prefs.getLong("calendar", 0)

        //Log.i("SERVICETT", "SET ALARMS ${flags} ${startId} $groups $teachers $period $calend")

        if ( ( (groups!=null && groups.size>0) || (teachers!=null && teachers.size>0) ) /*&& calend>0*/ || period==0)
        CoroutineScope(Dispatchers.Main).launch {
            val manager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intentt = Intent(applicationContext, TimeTableService::class.java)

            for (alarm in 1..7) {
                val pending = PendingIntent.getService(applicationContext, alarm, intentt, 0)
                manager.cancel(pending)
            }

            var time = System.currentTimeMillis()
            val time1 = time
            var interval = AlarmManager.INTERVAL_DAY
            val calendar = Calendar.getInstance()

            //Log.i("SERVICETT","TIME1 $time1 $calendar ${interval / 3600000}")
            val alarms = if (period==3) 7 else if (period>0) 1 else 0
            for (alarm in 1..alarms) {
                val pending = PendingIntent.getService(applicationContext, alarm, intentt, 0)

                calendar.setTimeInMillis(time1)
                if (period == 1) {
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    interval = 7 * AlarmManager.INTERVAL_DAY
                }

                if (period==3) {
                    calendar.set(Calendar.HOUR_OF_DAY, times[0][alarm-1])
                    calendar.set(Calendar.MINUTE, times[1][alarm-1])
                    calendar.add(Calendar.MINUTE, (0..9).random())
                }  else {
                    calendar.set(Calendar.HOUR_OF_DAY, 17)
                    calendar.set(Calendar.MINUTE, (0..59).random())
                }

                time = calendar.timeInMillis
                if (time < time1) {
                    calendar.add( if (period == 1) Calendar.WEEK_OF_MONTH else Calendar.DAY_OF_WEEK, 1 )
                    time = calendar.timeInMillis
                }
                //interval = 5000L + alarm*5000
                //Log.i("SERVICETT","TIME2 $time $calendar ${(time - time1) / 3600000}")

                manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, interval, pending)
                //Log.i("SERVICETT", "SET $manager $intent $pending")
            }
            stopSelf(startId)
        }
        else stopSelf(startId)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? = null

    /*override fun onDestroy() {
        Log.i("SERVICETT", "ALARMS SET, STOPPED")
        super.onDestroy()
    }*/
}
