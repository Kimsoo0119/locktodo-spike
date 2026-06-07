package com.example.locktodo

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** 잠금화면 위에 뜨는 화면 — 할 일 목록 + 체크박스(잠금 해제 없이 토글). */
class LockActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 120, 50, 50)
        }
        root.addView(TextView(this).apply { text = "할 일"; textSize = 28f })
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(container)
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        container.removeAllViews()
        val todos = TodoStore.load(this)
        if (todos.isEmpty()) {
            container.addView(TextView(this).apply { text = "할 일이 없습니다"; textSize = 18f })
            return
        }
        todos.forEach { todo ->
            container.addView(CheckBox(this).apply {
                text = todo.text
                textSize = 20f
                isChecked = todo.done
                setOnClickListener {
                    TodoStore.toggle(this@LockActivity, todo.id)
                    Log.i("LockTodo", "toggle id=${todo.id} done=${!todo.done}")
                }
            })
        }
    }
}
