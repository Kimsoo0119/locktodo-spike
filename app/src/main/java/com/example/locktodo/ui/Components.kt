package com.example.locktodo.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 화면 재개(ON_RESUME) 시 저장소에서 재로드 — 메인↔잠금 동기화(FIX1, D-SYNC). */
@Composable
fun rememberTodos(): androidx.compose.runtime.MutableState<List<com.example.locktodo.Todo>> {
    val ctx = LocalContext.current
    val state = remember { androidx.compose.runtime.mutableStateOf<List<com.example.locktodo.Todo>>(com.example.locktodo.TodoStore.load(ctx)) }
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) state.value = com.example.locktodo.TodoStore.load(ctx)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return state
}

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

/** 체크박스 — 미완 외곽선 / 완료 accent 채움 + ✓ 커스텀 path. box scale 0.9→1.0 + ✓ draw-in. */
@Composable
fun TodoCheckbox(checked: Boolean) {
    val t = LocalAppTheme.current
    val fill by animateColorAsState(if (checked) t.accent else Color.Transparent, tween(150), label = "fill")
    val borderColor by animateColorAsState(if (checked) t.accent else t.mutedWeak, tween(150), label = "border")
    val boxScale by animateFloatAsState(if (checked) 1f else 0.9f, tween(150, easing = FastOutSlowInEasing), label = "scale")
    Box(
        Modifier
            .size(24.dp)
            .graphicsLayer { scaleX = boxScale; scaleY = boxScale }
            .clip(t.checkboxShape)
            .background(fill, t.checkboxShape)
            .border(2.dp, borderColor, t.checkboxShape),
        contentAlignment = Alignment.Center,
    ) {
        // ✓ vector path (M6 12.5 L10.5 17 L18 7.5), 폰트 글리프 폐기(D16)
        AnimatedVisibility(checked, enter = scaleIn(tween(180)), exit = scaleOut(tween(120))) {
            val tint = t.onAccent
            Canvas(Modifier.size(24.dp)) {
                val w = size.width
                val p = Path().apply {
                    moveTo(w * 0.25f, w * 0.52f)
                    lineTo(w * 0.44f, w * 0.71f)
                    lineTo(w * 0.75f, w * 0.31f)
                }
                drawPath(p, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
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
