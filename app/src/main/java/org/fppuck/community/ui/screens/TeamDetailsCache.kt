package org.fppuck.community.data

/**
 * Short-lived in-memory cache for Team Details.
 *
 * Team pages are expensive because several sections depend on
 * rendered HockeyShift pages. This cache survives navigation
 * between Compose screens for the lifetime of the app process,
 * so reopening the same team can use data we already fetched.
 */
data class TeamDetailsSnapshot(
    val overviewStats: TeamOverviewStats? = null,
    val standing: LeagueStanding? = null,
    val roster: List<TeamPlayer> = emptyList(),
    val playerStats: List<TeamPlayerStat> = emptyList(),
    val goalieStats: List<TeamGoalieStat> = emptyList(),
    val schedule: List<ScheduledGame> = emptyList(),
    val cachedAtMillis: Long = System.currentTimeMillis()
)

object TeamDetailsCache {

    private const val CACHE_DURATION_MS =
        10 * 60 * 1000L

    private val cache =
        mutableMapOf<String, TeamDetailsSnapshot>()

    @Synchronized
    fun get(
        teamAbbreviation: String
    ): TeamDetailsSnapshot? {

        val snapshot =
            cache[teamAbbreviation]
                ?: return null

        val age =
            System.currentTimeMillis() -
                    snapshot.cachedAtMillis

        return if (
            age < CACHE_DURATION_MS
        ) {

            snapshot

        } else {

            cache.remove(
                teamAbbreviation
            )

            null
        }
    }

    @Synchronized
    fun updateOverview(
        teamAbbreviation: String,
        overviewStats: TeamOverviewStats,
        standing: LeagueStanding?
    ) {

        val current =
            cache[teamAbbreviation]
                ?: TeamDetailsSnapshot()

        cache[teamAbbreviation] =
            current.copy(
                overviewStats = overviewStats,
                standing = standing,
                cachedAtMillis =
                    System.currentTimeMillis()
            )
    }

    @Synchronized
    fun updateRoster(
        teamAbbreviation: String,
        roster: List<TeamPlayer>
    ) {

        val current =
            cache[teamAbbreviation]
                ?: TeamDetailsSnapshot()

        cache[teamAbbreviation] =
            current.copy(
                roster = roster,
                cachedAtMillis =
                    System.currentTimeMillis()
            )
    }

    @Synchronized
    fun updateStats(
        teamAbbreviation: String,
        playerStats: List<TeamPlayerStat>,
        goalieStats: List<TeamGoalieStat>
    ) {

        val current =
            cache[teamAbbreviation]
                ?: TeamDetailsSnapshot()

        cache[teamAbbreviation] =
            current.copy(
                playerStats = playerStats,
                goalieStats = goalieStats,
                cachedAtMillis =
                    System.currentTimeMillis()
            )
    }

    @Synchronized
    fun updateSchedule(
        teamAbbreviation: String,
        schedule: List<ScheduledGame>
    ) {

        val current =
            cache[teamAbbreviation]
                ?: TeamDetailsSnapshot()

        cache[teamAbbreviation] =
            current.copy(
                schedule = schedule,
                cachedAtMillis =
                    System.currentTimeMillis()
            )
    }
}
