package com.example.locktodo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/** 상주 포그라운드 서비스 — 화면 ON 을 받아 LockActivity 를 띄웁니다. */
class ScreenService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                // D12: 잠금 상태일 때만 표시(잠금 아니면 홈/앱 위로 튀어나오지 않게).
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                if (!km.isKeyguardLocked) return
                context.startActivity(
                    Intent(context, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        // ACTION_SCREEN_ON 은 정적 등록이 안 되므로 런타임 동적 등록합니다.
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "locktodo_svc"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "LockTodo 서비스", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("LockTodo 실행 중")
            .setContentText("화면을 켜면 잠금화면에 할 일을 표시합니다")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
    }
}
