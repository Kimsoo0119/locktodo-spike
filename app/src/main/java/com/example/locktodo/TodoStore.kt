package com.example.locktodo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Todo(val id: Long, val text: String, val done: Boolean)

/** 할 일을 SharedPreferences 에 JSON 으로 저장합니다. 모델은 id/text/done 뿐입니다. */
object TodoStore {
    private const val PREF = "locktodo"
    private const val KEY = "todos"

    fun load(ctx: Context): MutableList<Todo> {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Todo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Todo(o.getLong("id"), o.getString("text"), o.getBoolean("done")))
        }
        return list
    }

    private fun save(ctx: Context, list: List<Todo>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done)) }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, text: String) {
        val list = load(ctx)
        val id = (list.maxOfOrNull { it.id } ?: 0L) + 1L // 중복 없는 단조 증가 id
        list.add(Todo(id, text, false))
        save(ctx, list)
    }

    fun remove(ctx: Context, id: Long) = save(ctx, load(ctx).filterNot { it.id == id })

    fun toggle(ctx: Context, id: Long) =
        save(ctx, load(ctx).map { if (it.id == id) it.copy(done = !it.done) else it })

    fun insert(ctx: Context, todo: Todo, index: Int) {
        val list = load(ctx)
        list.add(index.coerceIn(0, list.size), todo)
        save(ctx, list)
    }

    private const val KEY_THEME = "theme"
    fun loadTheme(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_THEME, "A") ?: "A"

    fun saveTheme(ctx: Context, theme: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_THEME, theme).apply()
    }
}
