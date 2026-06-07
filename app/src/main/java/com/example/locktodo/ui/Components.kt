package com.example.locktodo.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 카드 — 테마 fill/border/shape. */
@Composable
fun TodoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = LocalAppTheme.current
    val base = modifier
        .fillMaxWidth()
        .clip(t.cardShape)
        .background(t.cardFill, t.cardShape)
    val withBorder = if (t.cardBorder != null) base.border(1.dp, t.cardBorder, t.cardShape) else base
    Box(withBorder.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
}

/** 체크박스 — 미완 외곽선 / 완료 accent 채움 + ✓. 토글 애니(150/180/120ms). 표시 전용(클릭은 row). */
@Composable
fun TodoCheckbox(checked: Boolean) {
    val t = LocalAppTheme.current
    val fill by animateColorAsState(if (checked) t.accent else Color.Transparent, tween(150), label = "fill")
    val borderColor by animateColorAsState(if (checked) t.accent else t.mutedWeak, tween(150), label = "border")
    Box(
        Modifier.size(24.dp).clip(t.checkboxShape).background(fill, t.checkboxShape)
            .border(2.dp, borderColor, t.checkboxShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(checked, enter = scaleIn(tween(180)), exit = scaleOut(tween(120))) {
            Icon(Icons.Default.Check, contentDescription = null, tint = t.onAccent, modifier = Modifier.size(16.dp))
        }
    }
}

/** 시계 — 시스템 12/24h, OS 시각 일치, TIME_TICK 부분 갱신(전체 recompose 격리). */
@Composable
fun ClockHeader() {
    val t = LocalAppTheme.current
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { now = System.currentTimeMillis() }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(ctx, r, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(r) }
    }
    val time = remember(now) { DateFormat.getTimeFormat(ctx).format(Date(now)) }
    val date = remember(now) { SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(Date(now)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, color = t.text, style = TextStyle(fontSize = 60.sp, fontWeight = t.clockWeight, fontFeatureSettings = "tnum"))
        Text(date, color = t.mutedStrong, style = TextStyle(fontSize = 14.sp, fontWeight = t.dateWeight))
    }
}

/** 빈 상태. */
@Composable
fun EmptyState() {
    val t = LocalAppTheme.current
    Box(Modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
        Text("할 일이 없어요", color = t.mutedWeak, style = TextStyle(fontSize = 16.sp))
    }
}

/** 완료 텍스트 스타일 = 베이스색 × 0.6 + 취소선. */
@Composable
fun itemTextStyle(done: Boolean): TextStyle {
    val t = LocalAppTheme.current
    return TextStyle(
        fontSize = 16.sp,
        fontWeight = t.itemWeight,
        color = if (done) t.text.copy(alpha = 0.60f) else t.text,
        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
    )
}
