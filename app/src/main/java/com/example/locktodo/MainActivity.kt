package com.example.locktodo

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** 앱 본체 — 권한 요청, 서비스 시작, 할 일 추가/삭제. */
class MainActivity : AppCompatActivity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }
        root.addView(TextView(this).apply { text = "LockTodo"; textSize = 24f })

        root.addView(Button(this).apply {
            text = "권한 허용 (오버레이/알림)"
            setOnClickListener { requestPerms() }
        })
        root.addView(Button(this).apply {
            text = "서비스 시작 (화면 켜면 잠금화면 표시)"
            setOnClickListener { startSvc() }
        })
        root.addView(Button(this).apply {
            text = "잠금화면 화면 미리보기"
            setOnClickListener { startActivity(Intent(this@MainActivity, LockActivity::class.java)) }
        })

        val addRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        input = EditText(this).apply {
            hint = "할 일 입력"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val addBtn = Button(this).apply {
            text = "추가"
            setOnClickListener {
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) {
                    TodoStore.add(this@MainActivity, t)
                    input.setText("")
                    refresh()
                }
            }
        }
        addRow.addView(input)
        addRow.addView(addBtn)
        root.addView(addRow)

        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listContainer)

        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        listContainer.removeAllViews()
        TodoStore.load(this).forEach { todo ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(this).apply {
                text = (if (todo.done) "✓ " else "○ ") + todo.text
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                text = "삭제"
                setOnClickListener { TodoStore.remove(this@MainActivity, todo.id); refresh() }
            })
            listContainer.addView(row)
        }
    }

    private fun requestPerms() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun startSvc() {
        ContextCompat.startForegroundService(this, Intent(this, ScreenService::class.java))
        Toast.makeText(this, "서비스 시작됨", Toast.LENGTH_SHORT).show()
    }
}
