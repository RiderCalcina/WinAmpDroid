package com.winampdroid.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.winampdroid.MainActivity
import com.winampdroid.R
import com.winampdroid.core.audio.WinampPlaybackService

class WinampAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, lastTitle, lastIsPlaying)
        }
    }

    companion object {
        private var lastTitle: String = "DJ Mike Llama - Llama Whippin' Intro"
        private var lastIsPlaying: Boolean = false

        fun updateAllWidgets(context: Context, trackTitle: String, isPlaying: Boolean) {
            lastTitle = trackTitle
            lastIsPlaying = isPlaying
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, WinampAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, trackTitle, isPlaying)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_winamp_classic)

            // Track title & status
            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(
                R.id.widget_btn_play_pause,
                if (isPlaying) "❚❚ PAUSE" else "▶ PLAY"
            )
            views.setTextViewText(
                R.id.widget_status_badge,
                if (isPlaying) "▶ 192k STEREO" else "■ STOPPED"
            )

            // Open MainActivity on clicking header or root background
            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_header, openAppIntent)

            // Transport Actions to WinampPlaybackService
            val prevIntent = PendingIntent.getService(
                context,
                10,
                Intent(context, WinampPlaybackService::class.java).apply {
                    action = WinampPlaybackService.ACTION_PREV
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevIntent)

            val playPauseIntent = PendingIntent.getService(
                context,
                11,
                Intent(context, WinampPlaybackService::class.java).apply {
                    action = WinampPlaybackService.ACTION_PLAY_PAUSE
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPauseIntent)

            val stopIntent = PendingIntent.getService(
                context,
                12,
                Intent(context, WinampPlaybackService::class.java).apply {
                    action = WinampPlaybackService.ACTION_STOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_stop, stopIntent)

            val nextIntent = PendingIntent.getService(
                context,
                13,
                Intent(context, WinampPlaybackService::class.java).apply {
                    action = WinampPlaybackService.ACTION_NEXT
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

