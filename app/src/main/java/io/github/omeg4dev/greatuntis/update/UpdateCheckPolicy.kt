package io.github.omeg4dev.greatuntis.update

/**
 * Policies governing update checks, snoozing prompts, settings indicators, and network constraints.
 *
 * GreatUntis drops mobile-data ask prompts and Wi-Fi restrictions because
 * GreatUntis APK builds are lightweight (~10-15 MB) and can be downloaded over any active connection.
 */
object UpdateCheckPolicy {
    /** 3-day snooze duration when the user taps "Later". */
    const val LATER_MS = 3L * 24 * 60 * 60 * 1000

    /** Standard automatic check interval: at most once per 24 hours. */
    const val STARTUP_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    fun intervalMs(hours: Int = 24): Long = hours * 3_600_000L

    /**
     * Determines whether an automatic update check should run on app startup.
     */
    fun shouldCheckOnStartup(
        autoCheck: Boolean = true,
        lastCheckMs: Long,
        nowMs: Long,
        intervalMs: Long = STARTUP_CHECK_INTERVAL_MS,
        knownAvailable: String?,
        installed: String,
        skipped: String?,
        laterUntilMs: Long,
    ): Boolean {
        if (!autoCheck) return false
        if (lastCheckMs <= 0L || lastCheckMs > nowMs) return true
        if (nowMs - lastCheckMs >= intervalMs) return true
        return knownAvailable != null && shouldShowPrompt(knownAvailable, installed, skipped, laterUntilMs, nowMs)
    }

    fun shouldNotify(
        available: String,
        installed: String,
        skipped: String?,
        laterUntilMs: Long,
        notifiedVersion: String?,
        nowMs: Long,
    ): Boolean = shouldShowPrompt(available, installed, skipped, laterUntilMs, nowMs) && available != notifiedVersion

    /**
     * Calculates the timestamp until which update prompts are snoozed after tapping "Later" at [nowMs].
     */
    fun laterUntil(nowMs: Long): Long = nowMs + LATER_MS

    /**
     * Evaluates whether an update prompt dialog should be presented to the user.
     *
     * Shown when [available] is newer than [installed], not marked as [skipped],
     * and the snooze period [laterUntilMs] has expired ([nowMs] >= [laterUntilMs]).
     */
    fun shouldShowPrompt(
        available: String,
        installed: String,
        skipped: String?,
        laterUntilMs: Long,
        nowMs: Long,
    ): Boolean {
        if (nowMs < laterUntilMs) return false
        return ReleaseParser.isNewer(available, installed, skipped)
    }

    /**
     * Determines whether the settings screen should display an update badge/marker.
     *
     * Returns true if [available] is strictly newer than [installed].
     * Unlike the dialog prompt, this marker ignores the "Later" snooze and persists even if [available] was skipped.
     */
    fun showSettingsMarker(available: String?, installed: String): Boolean {
        if (available == null) return false
        return ReleaseParser.compareVersions(available, installed) > 0
    }
}
