package app.hushwood.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.hushwood.MainActivity
import app.hushwood.R
import app.hushwood.container
import app.hushwood.domain.Keeper
import app.hushwood.localized
import app.hushwood.ru
import app.hushwood.ui.Words
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Once a day, at the hour you pick, and only if you haven't sat yet: "The heron is waiting by the water." */
object Remind {
    private const val CHANNEL = "remind_v1"
    private const val ID = 7
    internal const val FIRE = "app.hushwood.notify.REMIND"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Also on every start: creating it again renames it after a language change. */
    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val l = context.localized()
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, l.getString(R.string.channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = l.getString(R.string.channel_desc)
                setSound(null, null)
            }
        )
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, RemindReceiver::class.java).setAction(FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        if (!s.onboarded || !s.remind) {
            am.cancel(p)
            return
        }
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var at = now.toLocalDate().atTime(s.remindAt / 60, s.remindAt % 60)
        if (!at.isAfter(now)) at = at.plusDays(1)
        // A gentle reminder doesn't need to-the-second precision; this avoids the exact-alarm permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(zone).toInstant().toEpochMilli(), p)
    }

    suspend fun fire(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (!s.onboarded || !s.remind || !canNotify(context)) return
        val sessions = c.db.dao().sessionsNow()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        if (sessions.any { it.day(zone) == today }) return
        val l = context.localized()
        val g = Keeper.guardian(sessions, zone)
        val title = if (g != null) l.getString(R.string.remind_guardian, Words.name(g).of(l.ru)) else l.getString(R.string.remind_title)
        val open = PendingIntent.getActivity(
            context, 100,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_PLACE)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.moss))
            .setContentTitle(title)
            .setContentText(l.getString(R.string.remind_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID, n)
        } catch (_: SecurityException) {
        }
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class RemindReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Remind.FIRE) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Remind.fire(context)
                Remind.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Remind.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
