package io.github.omeg4dev.greatuntis.watch

import android.content.Context
import io.github.omeg4dev.greatuntis.model.Change
import io.github.omeg4dev.greatuntis.model.ChangeKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@Serializable
data class ChangeCache(
    val kind: String,
    val date: String,
    val lessonKey: String?,
    val subject: String?,
    val title: String,
    val text: String,
    val fingerprint: String,
) {
    fun toModel(): Change = Change(
        kind = ChangeKind.valueOf(kind),
        date = LocalDate.parse(date),
        lessonKey = lessonKey,
        subject = subject,
        title = title,
        text = text,
        fingerprint = fingerprint,
    )

    companion object {
        fun fromModel(c: Change): ChangeCache = ChangeCache(
            kind = c.kind.name,
            date = c.date.toString(),
            lessonKey = c.lessonKey,
            subject = c.subject,
            title = c.title,
            text = c.text,
            fingerprint = c.fingerprint,
        )
    }
}

object WatchState {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): Map<String, Change> {
        val file = File(context.filesDir, "watch_state.json")
        if (!file.exists()) return emptyMap()
        return try {
            val list = json.decodeFromString<List<ChangeCache>>(file.readText())
            val today = LocalDate.now()
            list.map { it.toModel() }
                .filter { !it.date.isBefore(today) }
                .associateBy { it.fingerprint }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun save(context: Context, active: Map<String, Change>) {
        val file = File(context.filesDir, "watch_state.json")
        val dtos = active.values.map { ChangeCache.fromModel(it) }
        val temp = File(context.filesDir, "watch_state.json.tmp")
        try {
            FileOutputStream(temp).use { fos ->
                fos.write(json.encodeToString(dtos).toByteArray(Charsets.UTF_8))
                fos.fd.sync()
            }
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        } catch (e: Exception) {
            temp.delete()
        }
    }

    /** False until the first successful poll after login (no baseline yet). */
    fun exists(context: Context): Boolean = File(context.filesDir, "watch_state.json").exists()

    fun clear(context: Context) {
        File(context.filesDir, "watch_state.json").delete()
    }
}
