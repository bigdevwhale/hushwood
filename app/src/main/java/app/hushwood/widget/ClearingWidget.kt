package app.hushwood.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import app.hushwood.MainActivity
import app.hushwood.R
import app.hushwood.container
import app.hushwood.domain.Daypart
import app.hushwood.domain.Keeper
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.localized
import app.hushwood.ru
import app.hushwood.ui.Words
import app.hushwood.ui.pix.Clearing
import app.hushwood.ui.pix.Layout
import app.hushwood.ui.pix.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The place on the home screen as it is this hour, with the guardian where it stands; a tap opens it. */
class ClearingWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClearingWidget::class.java))
            if (ids.isEmpty()) return
            val c = context.container
            val s = c.settings.now()
            val sessions = c.db.dao().sessionsNow()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val part = Daypart.of(LocalDateTime.now(zone))
            val season = Season.of(today)
            val place = s.place.takeIf { it in Place.open(sessions.sumOf { it.minutes }) } ?: Place.FOREST
            val guardian = Keeper.guardian(sessions, zone)
            val trust = guardian?.let { Keeper.trust(sessions, today, zone) }
            // A wide crop of the picture's lower part, where the animals are.
            val w = 270
            val h = 200
            val lay = Layout(w, h, place)
            val bg = Clearing.paint(w, h, part, season, place)
            val picture = Stage.still(lay, bg, Stage.cast(lay, emptyList(), guardian, trust, null, false), season, part, null, false)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(picture.c, 0, w, 0, 0, w, h) }
            val big = Bitmap.createScaledBitmap(bmp, w * 3, h * 3, false)
            val l = context.localized()
            val line = if (guardian != null && trust != null) Words.name(guardian).of(l.ru) + " " + Words.trust(trust).of(l.ru)
            else Words.place(place).of(l.ru)
            val views = RemoteViews(context.packageName, R.layout.widget_clearing)
            views.setImageViewBitmap(R.id.scene, big)
            views.setTextViewText(R.id.line, line)
            val open = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_PLACE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            views.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            manager.updateAppWidget(ids, views)
        }
    }
}
