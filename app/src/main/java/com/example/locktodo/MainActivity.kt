package com.example.locktodo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locktodo.ui.ALL_THEMES
import com.example.locktodo.ui.AppTheme
import com.example.locktodo.ui.LocalAppTheme
import com.example.locktodo.ui.ThemeId
import com.example.locktodo.ui.TodoCard
import com.example.locktodo.ui.TodoCheckbox
import com.example.locktodo.ui.itemTextStyle
import com.example.locktodo.ui.themeById
import kotlinx.coroutines.launch

/** 앱 본체 — 테마 선택, 권한/서비스, 할 일 추가/삭제(+Undo). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            var themeId by remember { mutableStateOf(runCatching { ThemeId.valueOf(TodoStore.loadTheme(ctx)) }.getOrDefault(ThemeId.A)) }
            CompositionLocalProvider(LocalAppTheme provides themeById(themeId)) {
                MainScreen(themeId) { picked ->
                    themeId = picked
                    TodoStore.saveTheme(ctx, picked.name)
                }
            }
        }
    }
}

@Composable
private fun MainScreen(themeId: ThemeId, onTheme: (ThemeId) -> Unit) {
    val t = LocalAppTheme.current
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    var todos by remember { mutableStateOf(TodoStore.load(ctx)) }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .background(t.bg)
                .padding(inner)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("LockTodo", color = t.text, style = TextStyle(fontSize = 22.sp, fontWeight = t.itemWeight))
            Spacer(Modifier.height(12.dp))

            // 테마 세그먼트
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ALL_THEMES.forEach { th ->
                    val selected = th.id == themeId
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) t.cardFill else t.text.copy(alpha = 0.06f))
                            .then(if (selected) Modifier.border(1.5.dp, t.accent, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onTheme(th.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(th.displayName, color = if (selected) t.accent else t.mutedStrong, style = TextStyle(fontSize = 14.sp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 권한/서비스 배너 (한 줄)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("화면 켜면 잠금화면 표시", color = t.mutedStrong, style = TextStyle(fontSize = 13.sp), modifier = Modifier.weight(1f))
                MiniButton("권한") { activity?.let(::requestPerms) }
                MiniButton("시작") { activity?.let(::startSvc) }
            }
            Spacer(Modifier.height(12.dp))

            // 입력 + 추가
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(t.fieldShape)
                        .background(t.cardFill, t.fieldShape).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (input.text.isEmpty()) Text("할 일 입력", color = t.mutedWeak, style = TextStyle(fontSize = 16.sp))
                    BasicTextField(
                        value = input, onValueChange = { input = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = t.text),
                        cursorBrush = SolidColor(t.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MiniButton("추가") {
                    val txt = input.text.trim()
                    if (txt.isNotEmpty()) {
                        TodoStore.add(ctx, txt); input = TextFieldValue(""); todos = TodoStore.load(ctx)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 목록 (완료 하단 sink, ✕ 삭제 + Undo)
            val sorted = todos.sortedBy { it.done }
            LazyColumn {
                items(sorted, key = { it.id }) { todo ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.clickable { TodoStore.toggle(ctx, todo.id); todos = TodoStore.load(ctx) }) {
                            TodoCheckbox(checked = todo.done)
                        }
                        Text(todo.text, style = itemTextStyle(todo.done), modifier = Modifier.weight(1f))
                        Box(
                            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                val idx = todos.indexOfFirst { it.id == todo.id }
                                val removed = todo
                                TodoStore.remove(ctx, todo.id); todos = TodoStore.load(ctx)
                                scope.launch {
                                    val r = snackbar.showSnackbar("삭제됨", "실행취소")
                                    if (r == SnackbarResult.ActionPerformed) {
                                        TodoStore.insert(ctx, removed, idx); todos = TodoStore.load(ctx)
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", color = t.destructive, style = TextStyle(fontSize = 18.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    val t = LocalAppTheme.current
    Box(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
            .background(t.accent, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = t.onAccent, style = TextStyle(fontSize = 15.sp))
    }
}

private fun requestPerms(activity: Activity) {
    if (!Settings.canDrawOverlays(activity)) {
        activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}")))
    }
    if (Build.VERSION.SDK_INT >= 33 &&
        activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
    }
}

private fun startSvc(activity: Activity) {
    androidx.core.content.ContextCompat.startForegroundService(activity, Intent(activity, ScreenService::class.java))
}
