package shelftap.android.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import shelftap.android.MainActivity
import shelftap.android.R
import shelftap.android.ShelfTapApp

/** Whether Android lets this app post notifications now: the runtime permission (Android 13 and up) and the app's switch in the phone's settings. */
fun notificationsAllowed(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

/** The real alarms: inexact ones that may fire while the phone sleeps, so the app needs no permission to schedule exact alarms. */
class AndroidReminderAlarms(private val context: Context) : ReminderAlarms {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    override fun set(id: Int, reminder: DueReminder) {
        val intent = intent(context, id)
            .putExtra(EXTRA_COUNT, reminder.count)
            .putExtra(EXTRA_DAY, reminder.dueDay.toEpochDay())
            .also { i -> reminder.title?.let { i.putExtra(EXTRA_TITLE, it) } }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt.toEpochMilli(), pending(context, id, intent))
    }

    override fun cancel(id: Int) {
        alarms.cancel(pending(context, id, intent(context, id)))
    }

    private fun intent(context: Context, id: Int) = Intent(context, DueReminderReceiver::class.java).setAction("shelftap.android.DUE_REMINDER.$id")

    private fun pending(context: Context, id: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

internal const val EXTRA_COUNT = "count"
internal const val EXTRA_TITLE = "title"
internal const val EXTRA_DAY = "day"

/**
 * Posts the notification when an alarm fires. Not exported: only this app's own alarms reach it. It reads the member's choice about titles
 * at that moment, so a change in the account page applies to reminders already set.
 */
class DueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ShelfTapApp).container
        val count = intent.getIntExtra(EXTRA_COUNT, 0)
        if (count <= 0 || !notificationsAllowed(context)) return
        val title = intent.getStringExtra(EXTRA_TITLE)
        val day = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DAY, 0L))
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                DueNotification.show(context, count, title, day, hideTitles = container.reminderPrefs.hideTitles.first())
            } finally {
                pending.finish()
            }
        }
    }
}

object DueNotification {
    const val CHANNEL_ID = "due_reminders"
    private val DAY = DateTimeFormatter.ofPattern("dd/MM")

    /**
     * The text for a reminder. With [hideTitles] on, the notification is private (the phone shows the full text only after it is unlocked)
     * and carries a public version that says only that a book is due; with it off the full text shows on the lock screen too.
     */
    fun show(context: Context, count: Int, title: String?, day: LocalDate, hideTitles: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_due), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val headline = if (count == 1 && title != null) context.getString(R.string.notif_due_one, title, DAY.format(day))
        else context.resources.getQuantityString(R.plurals.notif_due_many, count, count)
        val body = context.getString(R.string.notif_due_body)
        fun builder() = android.app.Notification.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentIntent(open).setAutoCancel(true)
        val full = builder().setContentTitle(headline).setContentText(body)
        if (hideTitles) {
            val generic = builder().setContentTitle(context.getString(R.string.notif_due_public_title)).setContentText(body).build()
            full.setVisibility(android.app.Notification.VISIBILITY_PRIVATE).setPublicVersion(generic)
        } else {
            full.setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
        }
        manager.notify(day.toEpochDay().toInt(), full.build())
    }
}
