package io.github.omeg4dev.greatuntis

object Fixtures {
    fun load(name: String): String {
        val path = if (name.startsWith("/")) name else "/fixtures/$name"
        val stream = javaClass.getResourceAsStream(path)
            ?: error("Fixture not found: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
