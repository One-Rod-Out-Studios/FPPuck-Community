package org.fppuck.community.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume

object SporfieRepository {

    private const val FPHL_SPORFIE_ROOT =
        "https://fphl.sporfie.com"

    private val eventUrlCache =
        mutableMapOf<String, String>()

    suspend fun resolveEventUrl(
        context: Context,
        game: ScoreboardGame,
        watchUrl: String
    ): String? {

        if (
            watchUrl.contains(
                "/event/",
                ignoreCase = true
            )
        ) {
            return watchUrl
        }

        val cacheKey =
            "${game.date}|" +
                    "${game.awayAbbreviation}|" +
                    game.homeAbbreviation

        eventUrlCache[
            cacheKey
        ]?.let {
            return it
        }

        val homeSlug =
            teamSlug(
                game.homeAbbreviation
            )
                ?: return null

        val awayTeam =
            sampleTeams.firstOrNull {
                it.abbreviation ==
                        game.awayAbbreviation
            }

        val homeTeam =
            sampleTeams.firstOrNull {
                it.abbreviation ==
                        game.homeAbbreviation
            }

        val awayFullName =
            awayTeam
                ?.let {
                    "${it.city} ${it.name}"
                }
                ?: game.awayAbbreviation

        val homeFullName =
            homeTeam
                ?.let {
                    "${it.city} ${it.name}"
                }
                ?: game.homeAbbreviation

        val awayShortName =
            awayTeam?.name
                ?: game.awayAbbreviation

        val homeShortName =
            homeTeam?.name
                ?: game.homeAbbreviation

        val dateLabel =
            game.date.format(
                DateTimeFormatter.ofPattern(
                    "MMM d",
                    Locale.US
                )
            )

        val searchPages =
            listOf(
                "$FPHL_SPORFIE_ROOT/$homeSlug",
                FPHL_SPORFIE_ROOT
            )

        searchPages.forEach { pageUrl ->

            val resolved =
                runCatching {

                    resolveFromPage(
                        context = context,
                        pageUrl = pageUrl,
                        awayAbbreviation =
                            game.awayAbbreviation,
                        homeAbbreviation =
                            game.homeAbbreviation,
                        awayFullName =
                            awayFullName,
                        homeFullName =
                            homeFullName,
                        awayShortName =
                            awayShortName,
                        homeShortName =
                            homeShortName,
                        dateLabel =
                            dateLabel
                    )

                }.getOrNull()

            if (
                resolved != null
            ) {

                eventUrlCache[
                    cacheKey
                ] =
                    resolved

                return resolved
            }
        }

        return null
    }

    private fun teamSlug(
        abbreviation: String
    ): String? {

        return when (
            abbreviation.uppercase(
                Locale.US
            )
        ) {

            "BBB" ->
                "binghamtonblackbears"

            "BRB" ->
                "blueridgebobcats"

            "DHT" ->
                "danburyhattricks"

            "TCT" ->
                "twincitythunderbirds"

            "IND" ->
                "indianasentinels"

            "MCR" ->
                "motorcityrockers"

            "PHP" ->
                "porthuronprowlers"

            "WTW" ->
                "watertownwolves"

            "MSM" ->
                "midsouthmonarchs"

            "MRM" ->
                "monroemoccasins"

            "BKF" ->
                "batonrougekingfish"

            "CRD" ->
                "columbusriverdragons"

            "MNL" ->
                "minnesotanorthernlights"

            "TOP" ->
                "topekascarecrows"

            "SCK" ->
                "stocktonthunder"

            "FRF" ->
                "fresnofalcons"

            "OCS" ->
                "oceansideshock"

            else ->
                null
        }
    }

    @SuppressLint(
        "SetJavaScriptEnabled"
    )
    private suspend fun resolveFromPage(
        context: Context,
        pageUrl: String,
        awayAbbreviation: String,
        homeAbbreviation: String,
        awayFullName: String,
        homeFullName: String,
        awayShortName: String,
        homeShortName: String,
        dateLabel: String
    ): String? =
        withContext(
            Dispatchers.Main
        ) {

            suspendCancellableCoroutine { continuation ->

                val webView =
                    WebView(
                        context
                    )

                val handler =
                    Handler(
                        Looper.getMainLooper()
                    )

                var started =
                    false

                var finished =
                    false

                fun finish(
                    value: String?
                ) {

                    if (finished) {
                        return
                    }

                    finished =
                        true

                    handler
                        .removeCallbacksAndMessages(
                            null
                        )

                    webView.stopLoading()
                    webView.destroy()

                    if (
                        continuation.isActive
                    ) {

                        continuation.resume(
                            value
                        )
                    }
                }

                val awayAbbreviationJs =
                    JSONObject.quote(
                        awayAbbreviation
                    )

                val homeAbbreviationJs =
                    JSONObject.quote(
                        homeAbbreviation
                    )

                val awayFullNameJs =
                    JSONObject.quote(
                        awayFullName
                    )

                val homeFullNameJs =
                    JSONObject.quote(
                        homeFullName
                    )

                val awayShortNameJs =
                    JSONObject.quote(
                        awayShortName
                    )

                val homeShortNameJs =
                    JSONObject.quote(
                        homeShortName
                    )

                val dateLabelJs =
                    JSONObject.quote(
                        dateLabel
                    )

                fun inspect(
                    attempt: Int
                ) {

                    webView.evaluateJavascript(
                        """
                        (function() {

                            const normalize =
                                function(value) {

                                    return (
                                        value || ''
                                    )
                                        .replace(
                                            /\s+/g,
                                            ' '
                                        )
                                        .trim()
                                        .toUpperCase();
                                };

                            const awayAbbreviation =
                                normalize(
                                    $awayAbbreviationJs
                                );

                            const homeAbbreviation =
                                normalize(
                                    $homeAbbreviationJs
                                );

                            const awayFullName =
                                normalize(
                                    $awayFullNameJs
                                );

                            const homeFullName =
                                normalize(
                                    $homeFullNameJs
                                );

                            const awayShortName =
                                normalize(
                                    $awayShortNameJs
                                );

                            const homeShortName =
                                normalize(
                                    $homeShortNameJs
                                );

                            const dateLabel =
                                normalize(
                                    $dateLabelJs
                                );

                            const links =
                                Array.from(
                                    document.querySelectorAll(
                                        'a[href*="/event/"]'
                                    )
                                );

                            let bestUrl = '';
                            let bestScore = -1;

                            links.forEach(
                                function(link) {

                                    const href =
                                        link.href ||
                                        link.getAttribute(
                                            'href'
                                        ) ||
                                        '';

                                    if (!href) {
                                        return;
                                    }

                                    let node =
                                        link;

                                    let contextText = '';

                                    for (
                                        let depth = 0;
                                        depth < 8 && node;
                                        depth++
                                    ) {

                                        const text =
                                            normalize(
                                                node.innerText ||
                                                node.textContent ||
                                                ''
                                            );

                                        if (
                                            text.length >
                                            contextText.length
                                        ) {

                                            contextText =
                                                text;
                                        }

                                        node =
                                            node.parentElement;
                                    }

                                    let score = 0;

                                    if (
                                        contextText.includes(
                                            awayFullName
                                        )
                                    ) {
                                        score += 30;
                                    }

                                    if (
                                        contextText.includes(
                                            homeFullName
                                        )
                                    ) {
                                        score += 30;
                                    }

                                    if (
                                        contextText.includes(
                                            awayShortName
                                        )
                                    ) {
                                        score += 18;
                                    }

                                    if (
                                        contextText.includes(
                                            homeShortName
                                        )
                                    ) {
                                        score += 18;
                                    }

                                    if (
                                        contextText.includes(
                                            awayAbbreviation
                                        )
                                    ) {
                                        score += 8;
                                    }

                                    if (
                                        contextText.includes(
                                            homeAbbreviation
                                        )
                                    ) {
                                        score += 8;
                                    }

                                    if (
                                        contextText.includes(
                                            dateLabel
                                        )
                                    ) {
                                        score += 12;
                                    }

                                    if (
                                        contextText.includes(
                                            'LIVE'
                                        )
                                    ) {
                                        score += 20;
                                    }

                                    if (
                                        contextText.includes(
                                            'CURRENT EVENT'
                                        ) ||
                                        contextText.includes(
                                            'CURRENT EVENTS'
                                        )
                                    ) {
                                        score += 10;
                                    }

                                    /*
                                     * Both teams appearing in the same event
                                     * card is the strongest signal. This also
                                     * prevents us from selecting an unrelated
                                     * live event on the league landing page.
                                     */
                                    const hasAway =
                                        contextText.includes(
                                            awayFullName
                                        ) ||
                                        contextText.includes(
                                            awayShortName
                                        ) ||
                                        contextText.includes(
                                            awayAbbreviation
                                        );

                                    const hasHome =
                                        contextText.includes(
                                            homeFullName
                                        ) ||
                                        contextText.includes(
                                            homeShortName
                                        ) ||
                                        contextText.includes(
                                            homeAbbreviation
                                        );

                                    if (
                                        hasAway &&
                                        hasHome &&
                                        score > bestScore
                                    ) {

                                        bestScore =
                                            score;

                                        bestUrl =
                                            href;
                                    }
                                }
                            );

                            return bestUrl;
                        })()
                        """.trimIndent()
                    ) { rawResult ->

                        val resolved =
                            runCatching {

                                if (
                                    rawResult == null ||
                                    rawResult ==
                                    "null"
                                ) {

                                    ""

                                } else {

                                    JSONTokener(
                                        rawResult
                                    )
                                        .nextValue()
                                        .toString()
                                }

                            }.getOrDefault(
                                ""
                            )
                                .trim()

                        if (
                            resolved.contains(
                                "/event/",
                                ignoreCase = true
                            )
                        ) {

                            finish(
                                resolved
                            )

                        } else if (
                            attempt >= 20
                        ) {

                            finish(
                                null
                            )

                        } else {

                            handler.postDelayed(
                                {
                                    inspect(
                                        attempt + 1
                                    )
                                },
                                250
                            )
                        }
                    }
                }

                webView.settings
                    .javaScriptEnabled =
                    true

                webView.settings
                    .domStorageEnabled =
                    true

                webView.webViewClient =
                    object :
                        WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            if (started) {
                                return
                            }

                            started =
                                true

                            handler.postDelayed(
                                {
                                    inspect(
                                        0
                                    )
                                },
                                250
                            )
                        }
                    }

                continuation
                    .invokeOnCancellation {

                        handler
                            .removeCallbacksAndMessages(
                                null
                            )

                        webView.destroy()
                    }

                webView.loadUrl(
                    pageUrl
                )
            }
        }
}
