package org.fppuck.community.data

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object StandingsRepository {

    private const val STANDINGS_URL =
        "https://www.federalhockey.com/?lang=en"

    /*
     * Standings are used both on the Standings screen and
     * inside Team Overview. They do not need a brand-new
     * WebView render every time the user moves between those
     * screens.
     */
    private const val CACHE_DURATION_MS =
        5 * 60 * 1000L

    private val fetchMutex =
        Mutex()

    @Volatile
    private var cachedStandings:
            List<LeagueStanding> =
        emptyList()

    @Volatile
    private var cachedAtMillis:
            Long =
        0L

    suspend fun fetchStandings(
        context: Context
    ): List<LeagueStanding> {

        val now =
            System.currentTimeMillis()

        val cached =
            cachedStandings

        if (
            cached.isNotEmpty() &&
            now - cachedAtMillis <
            CACHE_DURATION_MS
        ) {
            return cached
        }

        return fetchMutex.withLock {

            val lockedNow =
                System.currentTimeMillis()

            val lockedCached =
                cachedStandings

            if (
                lockedCached.isNotEmpty() &&
                lockedNow - cachedAtMillis <
                CACHE_DURATION_MS
            ) {
                return@withLock lockedCached
            }

            val renderedHtml =
                renderPage(
                    context
                )

            val parsed =
                withContext(
                    Dispatchers.Default
                ) {
                    parseStandings(
                        renderedHtml
                    )
                }

            if (parsed.isNotEmpty()) {

                cachedStandings =
                    parsed

                cachedAtMillis =
                    System.currentTimeMillis()

                parsed

            } else {

                cachedStandings
            }
        }
    }

    private suspend fun renderPage(
        context: Context
    ): String =
        withContext(Dispatchers.Main) {

            suspendCancellableCoroutine { continuation ->

                val webView = WebView(context)

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            fun waitForStandings(
                                attempt: Int = 0
                            ) {

                                view.evaluateJavascript(
                                    """
                                    document.body &&
                                    document.body.innerText.includes(
                                        'ATLANTIC DIVISION'
                                    )
                                    """.trimIndent()
                                ) { result ->

                                    if (result == "true") {

                                        view.evaluateJavascript(
                                            """
                                            (function() {
                                                return document.documentElement.outerHTML;
                                            })()
                                            """.trimIndent()
                                        ) { htmlResult ->

                                            try {

                                                val html =
                                                    JSONTokener(htmlResult)
                                                        .nextValue() as String

                                                view.destroy()

                                                if (continuation.isActive) {
                                                    continuation.resume(html)
                                                }

                                            } catch (exception: Exception) {

                                                view.destroy()

                                                if (continuation.isActive) {
                                                    continuation.resumeWithException(
                                                        exception
                                                    )
                                                }
                                            }
                                        }

                                    } else if (attempt < 12) {

                                        view.postDelayed(
                                            {
                                                waitForStandings(
                                                    attempt + 1
                                                )
                                            },
                                            500
                                        )

                                    } else {

                                        view.destroy()

                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                Exception(
                                                    "Standings did not finish loading."
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            waitForStandings()
                        }
                    }

                continuation.invokeOnCancellation {
                    webView.destroy()
                }

                webView.loadUrl(STANDINGS_URL)
            }
        }

    private fun parseStandings(
        html: String
    ): List<LeagueStanding> {

        val document =
            Jsoup.parse(html)

        val standings =
            mutableListOf<LeagueStanding>()

        var currentDivision = ""

        document.select("tr").forEach { row ->

            val rowText =
                row.text().trim()

            when {

                rowText.contains(
                    "ATLANTIC DIVISION",
                    ignoreCase = true
                ) -> {
                    currentDivision = "Atlantic"
                }

                rowText.contains(
                    "GREAT LAKES DIVISION",
                    ignoreCase = true
                ) -> {
                    currentDivision = "Great Lakes"
                }

                rowText.contains(
                    "DELTA DIVISION",
                    ignoreCase = true
                ) -> {
                    currentDivision = "Delta"
                }

                rowText.contains(
                    "FRONTIER DIVISION",
                    ignoreCase = true
                ) -> {
                    currentDivision = "Frontier"
                }
            }

            val cells =
                row.select("td")

            if (cells.size < 9) {
                return@forEach
            }

            val rank =
                cells[0]
                    .text()
                    .trim()
                    .toIntOrNull()
                    ?: return@forEach

            val team =
                sampleTeams.firstOrNull { team ->

                    val fullName =
                        "${team.city} ${team.name}"

                    rowText.contains(
                        fullName,
                        ignoreCase = true
                    )
                }
                    ?: return@forEach

            val conference =
                when (currentDivision) {

                    "Atlantic",
                    "Great Lakes" ->
                        "Eastern"

                    "Delta",
                    "Frontier" ->
                        "Western"

                    else ->
                        ""
                }

            standings.add(
                LeagueStanding(
                    rank = rank,
                    teamName =
                        "${team.city} ${team.name}",
                    division =
                        currentDivision,
                    conference =
                        conference,
                    gamesPlayed =
                        cells[2]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0,
                    regulationWins =
                        cells[3]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0,
                    losses =
                        cells[4]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0,
                    overtimeLosses =
                        cells[5]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0,
                    overtimeWins =
                        cells[6]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0,
                    shootoutWins =
                        cells[7]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0,
                    points =
                        cells[8]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: 0
                )
            )
        }

        return standings
            .distinctBy {
                it.teamName
            }
            .sortedWith(
                compareBy<LeagueStanding> {
                    divisionOrder(it.division)
                }.thenBy {
                    it.rank
                }
            )
    }

    private fun divisionOrder(
        division: String
    ): Int {

        return when (division) {
            "Atlantic" -> 1
            "Great Lakes" -> 2
            "Delta" -> 3
            "Frontier" -> 4
            else -> 5
        }
    }
}