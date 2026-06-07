package com.example.locktodo

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings

/** 상주 포그라운드 서비스 — 잠금 상태에서 화면 ON 시 LockActivity 표시. 권한 없거나 BAL 차단 시 fullScreenIntent 노티로 fallback(D23). */
class ScreenService : Service() {

    private var receiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!km.isKeyguardLocked) return // D12: 잠금 상태에서만
            val open = Intent(context, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Settings.canDrawOverlays(context)) {
                runCatching { context.startActivity(open) }
                    .onFailure { postFullScreenNotification(context, open) } // BAL/OEM 차단 fallback
            } else {
                postFullScreenNotification(context, open)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // receiver 먼저 등록(startForeground throw 해도 onDestroy unregister 안전)
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        receiverRegistered = true
        startForeground(1, buildServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        if (receiverRegistered) runCatching { unregisterReceiver(screenReceiver) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun nm() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel(id: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= 26) nm().createNotificationChannel(NotificationChannel(id, name, importance))
    }

    private fun buildServiceNotification(): Notification {
        val ch = "locktodo_svc"
        ensureChannel(ch, "LockTodo 서비스", NotificationManager.IMPORTANCE_LOW)
        return Notification.Builder(this, ch)
            .setContentTitle("LockTodo 실행 중")
            .setContentText("화면을 켜면 잠금화면에 할 일을 표시합니다")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
    }

    /** 잠금화면 위 표시가 막힐 때 fullScreenIntent 노티로 사용자 동선 보장. */
    private fun postFullScreenNotification(context: Context, open: Intent) {
        val ch = "locktodo_lock"
        if (Build.VERSION.SDK_INT >= 26) {
            (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(ch, "할 일 표시", NotificationManager.IMPORTANCE_HIGH))
        }
        val pi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(context, ch)
            .setContentTitle("할 일")
            .setContentText("탭하면 할 일을 봅니다")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .build()
        runCatching { (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2, n) }
    }
}
