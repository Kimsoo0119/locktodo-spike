package com.example.locktodo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locktodo.ui.ClockHeader
import com.example.locktodo.ui.EmptyState
import com.example.locktodo.ui.LocalAppTheme
import com.example.locktodo.ui.ThemeId
import com.example.locktodo.ui.TodoCard
import com.example.locktodo.ui.TodoCheckbox
import com.example.locktodo.ui.itemTextStyle
import com.example.locktodo.ui.themeById
import androidx.compose.material3.Text

/** 잠금화면 위 화면 — 시계 + 할 일 카드. 잠금 해제 없이 체크. 카드 밖 탭/back 으로 dismiss. */
class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val theme = remember { themeById(runCatching { ThemeId.valueOf(TodoStore.loadTheme(ctx)) }.getOrDefault(ThemeId.A)) }
            CompositionLocalProvider(LocalAppTheme provides theme) {
                LockScreen(onDismiss = { finish() })
            }
        }
    }
}

@Composable
private fun LockScreen(onDismiss: () -> Unit) {
    val t = LocalAppTheme.current
    val ctx = LocalContext.current
    var todos by com.example.locktodo.ui.rememberTodos()
    // 카드 밖 빈 영역 탭 = dismiss (D20). row toggle 이 이벤트를 우선 소비.
    Column(
        Modifier
            .fillMaxSize()
            .background(t.bgLock)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        ClockHeader()
        Spacer(Modifier.height(24.dp))
        TodoCard(Modifier.padding(horizontal = 16.dp)) {
            Column {
                Text(
                    "할 일",
                    color = t.mutedStrong,
                    style = TextStyle(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing = 1.sp),
                )
                Spacer(Modifier.height(8.dp))
                if (todos.isEmpty()) {
                    EmptyState()
                } else {
                    val sorted = todos.sortedBy { it.done } // 미완 먼저, 완료 하단 sink
                    LazyColumn(Modifier.heightIn(max = 480.dp)) {
                        items(sorted, key = { it.id }) { todo ->
                            LockRow(done = todo.done, text = todo.text, modifier = Modifier.animateItem()) {
                                TodoStore.toggle(ctx, todo.id)
                                todos = TodoStore.load(ctx)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockRow(done: Boolean, text: String, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TodoCheckbox(checked = done)
        Text(text, style = itemTextStyle(done), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
