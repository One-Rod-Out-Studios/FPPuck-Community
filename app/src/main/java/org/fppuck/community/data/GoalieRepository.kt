package org.fppuck.community.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GoalieRepository {

    private val teamIds =
        mapOf(
            "BBB" to "684207",
            "BRB" to "684208",
            "DHT" to "684210",
            "TCT" to "684205",
            "IND" to "684211",
            "PHP" to "684213",
            "MCR" to "684212",
            "WTW" to "684215",
            "MRM" to "684200",
            "BKF" to "684206",
            "CRD" to "684203",
            "MSM" to "684284",
            "FRF" to "684285",
            "TOP" to "684214",
            "SCK" to "684286",
            "MNL" to "684281",
            "OCS" to "684287"
        )

    private val cache =
        mutableMapOf<String, List<TeamGoalieStat>>()

    suspend fun fetchGoalieStats(
        context: Context,
        teamAbbreviation: String
    ): List<TeamGoalieStat> {

        cache[teamAbbreviation]?.let {
            return it
        }

        val teamId =
            teamIds[teamAbbreviation]
                ?: error(
                    "No HockeyShift team ID for $teamAbbreviation"
                )

        val url =
            "https://www.federalhockey.com/stats" +
                    "?mobile-app=true&theme=false" +
                    "#/182/team/$teamId"

        val text =
            renderGoaliePage(
                context = context,
                url = url
            )

        val result =
            withContext(Dispatchers.Default) {
                parseGoalies(text)
            }

        cache[teamAbbreviation] = result

        return result
    }

    private fun parseGoalies(
        text: String
    ): List<TeamGoalieStat> {

        val lines =
            text.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        val headerIndex =
            lines.indexOfFirst {
                it.startsWith("GP") &&
                        it.contains("GAA") &&
                        it.contains("Sv%")
            }

        val namesIndex =
            lines.indexOfFirst {
                it.startsWith("#") &&
                        it.contains("Name")
            }

        if (
            headerIndex < 0 ||
            namesIndex <= headerIndex
        ) {
            return emptyList()
        }

        val statRows =
            lines
                .subList(
                    headerIndex + 1,
                    namesIndex
                )
                .map {
                    it.split("\t")
                        .map(String::trim)
                }
                .filter {
                    it.size >= 15
                }

        val nameRows =
            lines
                .drop(namesIndex + 1)
                .takeWhile {
                    !it.startsWith(
                        "Admin Login",
                        ignoreCase = true
                    )
                }

        val goalies =
            mutableListOf<TeamGoalieStat>()

        nameRows.forEachIndexed { index, line ->

            val stats =
                statRows.getOrNull(index)
                    ?: return@forEachIndexed

            val parts =
                line.split("\t")
                    .map(String::trim)

            val number =
                parts.firstOrNull()
                    ?.takeIf {
                        it.isNotBlank() &&
                                it.all(Char::isDigit)
                    }

            val rawName =
                parts.lastOrNull()
                    ?: return@forEachIndexed

            val rookie =
                rawName.contains(" - R")

            val cleaned =
                rawName
                    .replace(
                        Regex("#\\d+"),
                        ""
                    )
                    .replace(
                        " - R",
                        ""
                    )
                    .trim()

            val displayName =
                formatName(cleaned)

            goalies.add(
                TeamGoalieStat(
                    number = number,
                    name = displayName,
                    rookie = rookie,

                    gamesPlayed =
                        stats.getOrElse(0) { "" },

                    wins =
                        stats.getOrElse(1) { "" },

                    losses =
                        stats.getOrElse(2) { "" },

                    ties =
                        stats.getOrElse(3) { "" },

                    overtimeLosses =
                        stats.getOrElse(4) { "" },

                    shotsAgainst =
                        stats.getOrElse(5) { "" },

                    goalsAgainst =
                        stats.getOrElse(6) { "" },

                    goalsAgainstAverage =
                        stats.getOrElse(7) { "" },

                    saves =
                        stats.getOrElse(8) { "" },

                    savePercent =
                        stats.getOrElse(9) { "" },

                    shutouts =
                        stats.getOrElse(10) { "" },

                    minutesPlayed =
                        stats.getOrElse(11) { "" }
                )
            )
        }

        return goalies
    }

    private fun formatName(
        sourceName: String
    ): String {

        if (!sourceName.contains(",")) {
            return sourceName
        }

        val parts =
            sourceName.split(
                ",",
                limit = 2
            )

        return "${parts[1].trim()} ${parts[0].trim()}"
    }

    private suspend fun renderGoaliePage(
        context: Context,
        url: String
    ): String =
        withContext(Dispatchers.Main) {

            suspendCancellableCoroutine { continuation ->

                val webView =
                    WebView(context)

                val handler =
                    Handler(
                        Looper.getMainLooper()
                    )

                var started = false

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            if (started) {
                                return
                            }

                            started = true

                            handler.postDelayed(
                                {

                                    view.evaluateJavascript(
                                        """
                                        (function() {

                                            const goalie =
                                                document.querySelector(
                                                    'a[ng-click*="player_type = \'goalies\'"]'
                                                );

                                            if (!goalie) {
                                                return 'not found';
                                            }

                                            goalie.dispatchEvent(
                                                new MouseEvent(
                                                    'click',
                                                    {
                                                        bubbles: true,
                                                        cancelable: true,
                                                        view: window
                                                    }
                                                )
                                            );

                                            return 'clicked';

                                        })()
                                        """.trimIndent()
                                    ) {

                                        handler.postDelayed(
                                            {

                                                view.evaluateJavascript(
                                                    """
                                                    (function() {

                                                        if (!document.body) {
                                                            return "";
                                                        }

                                                        return document.body.innerText;

                                                    })()
                                                    """.trimIndent()
                                                ) { result ->

                                                    try {

                                                        val text =
                                                            if (
                                                                result == null ||
                                                                result == "null"
                                                            ) {
                                                                ""
                                                            } else {

                                                                JSONTokener(
                                                                    result
                                                                )
                                                                    .nextValue()
                                                                    .toString()
                                                            }

                                                        webView.destroy()

                                                        if (
                                                            continuation.isActive
                                                        ) {
                                                            continuation.resume(
                                                                text
                                                            )
                                                        }

                                                    } catch (
                                                        exception: Exception
                                                    ) {

                                                        webView.destroy()

                                                        if (
                                                            continuation.isActive
                                                        ) {
                                                            continuation
                                                                .resumeWithException(
                                                                    exception
                                                                )
                                                        }
                                                    }
                                                }

                                            },
                                            2500
                                        )
                                    }

                                },
                                5000
                            )
                        }
                    }

                continuation.invokeOnCancellation {

                    handler.removeCallbacksAndMessages(
                        null
                    )

                    webView.destroy()
                }

                webView.loadUrl(url)
            }
        }
}