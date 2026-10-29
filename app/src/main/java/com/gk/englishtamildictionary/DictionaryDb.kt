package com.gk.englishtamildictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.pm.PackageInfoCompat

data class Sense(val pos: String, val definition: String, val example: String?)
data class Entry(val word: String, val senses: List<Sense>, val collocations: String?)

/** Read-only access to the bundled assets/dictionary.db (built by tools/build_dictionary_db.py). */
class DictionaryDb(private val ctx: Context) {

    private val db: SQLiteDatabase by lazy { open() }

    private fun open(): SQLiteDatabase {
        val file = ctx.getDatabasePath("dictionary.db")
        val pkg = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val appVersion = PackageInfoCompat.getLongVersionCode(pkg)
        val prefs = ctx.getSharedPreferences("db", Context.MODE_PRIVATE)
        if (!file.exists() || prefs.getLong("copied_version", -1) != appVersion) {
            file.parentFile?.mkdirs()
            ctx.assets.open("dictionary.db").use { input ->
                file.outputStream().use { out -> input.copyTo(out) }
            }
            prefs.edit().putLong("copied_version", appVersion).apply()
        }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    /** Up to [limit] headwords starting with [prefix]. */
    fun suggestions(prefix: String, limit: Int = 15): List<String> {
        val p = prefix.trim().lowercase()
        if (p.isEmpty()) return emptyList()
        val upper = p.dropLast(1) + (p.last() + 1)
        val out = ArrayList<String>()
        db.rawQuery(
            "SELECT DISTINCT word FROM entries WHERE word_lc >= ? AND word_lc < ? ORDER BY word_lc LIMIT ?",
            arrayOf(p, upper, limit.toString())
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    fun lookup(raw: String): Entry? {
        val q = raw.trim().lowercase()
        if (q.isEmpty()) return null
        for (cand in candidates(q)) {
            val senses = senses(cand)
            if (senses.isNotEmpty()) return Entry(cand, senses, collocations(cand))
        }
        return null
    }

    /** Exact word first, then simple inflection stripping (cats, walked, running ...). */
    private fun candidates(w: String): List<String> {
        val c = linkedSetOf(w)
        if (w.endsWith("ies") && w.length > 4) c += w.dropLast(3) + "y"
        if (w.endsWith("es") && w.length > 3) c += w.dropLast(2)
        if (w.endsWith("s") && w.length > 3) c += w.dropLast(1)
        if (w.endsWith("ed") && w.length > 4) { c += w.dropLast(2); c += w.dropLast(1) }
        if (w.endsWith("ing") && w.length > 5) {
            val s = w.dropLast(3); c += s; c += s + "e"
            if (s.length > 2 && s[s.length - 1] == s[s.length - 2]) c += s.dropLast(1)
        }
        if (w.endsWith("er") && w.length > 4) c += w.dropLast(2)
        if (w.endsWith("est") && w.length > 5) c += w.dropLast(3)
        return c.toList()
    }

    private fun senses(word: String): List<Sense> {
        val out = ArrayList<Sense>()
        db.rawQuery(
            "SELECT pos, definition, example FROM entries WHERE word_lc = ? ORDER BY id",
            arrayOf(word)
        ).use { c ->
            while (c.moveToNext()) out.add(Sense(c.getString(0), c.getString(1), c.getString(2)))
        }
        return out
    }

    private fun collocations(word: String): String? {
        db.rawQuery("SELECT body FROM collocations WHERE word_lc = ?", arrayOf(word)).use { c ->
            val parts = ArrayList<String>()
            while (c.moveToNext()) parts.add(c.getString(0))
            return parts.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
        }
    }
}
