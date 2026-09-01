package com.hajiz.app.ui

/**
 * The destinations in the Hajiz product. Keeping route names in one typed
 * definition prevents deep links and bottom-bar actions from drifting apart.
 */
sealed interface HajizRoute {
    val path: String

    data object Splash : HajizRoute { override val path = "splash" }
    data object Onboarding : HajizRoute { override val path = "onboarding" }
    data object Home : HajizRoute { override val path = "home" }
    data object Protection : HajizRoute { override val path = "protection" }
    data object BlockedActivity : HajizRoute { override val path = "blocked-activity" }
    data object FocusMode : HajizRoute { override val path = "focus-mode" }
    data object Progress : HajizRoute { override val path = "progress" }
    data object DailyCheckIn : HajizRoute { override val path = "daily-check-in" }
    data object UrgeMode : HajizRoute { override val path = "urge-mode" }
    data object Settings : HajizRoute { override val path = "settings" }
    data object ProtectionLock : HajizRoute { override val path = "protection-lock" }
    data object Notifications : HajizRoute { override val path = "notifications" }
    data object WeeklyReport : HajizRoute { override val path = "weekly-report" }
    data object Privacy : HajizRoute { override val path = "privacy" }

    companion object {
        val bottomNavigation = listOf(Home, Protection, Progress, Settings)
        val all = listOf(
            Splash, Onboarding, Home, Protection, BlockedActivity, FocusMode,
            Progress, DailyCheckIn, UrgeMode, Settings, ProtectionLock,
            Notifications, WeeklyReport, Privacy,
        )

        fun fromPath(path: String): HajizRoute? = all.firstOrNull { it.path == path }
    }
}

/** A stable route string for Navigation Compose and deep-link tests. */
fun HajizRoute.route(): String = path