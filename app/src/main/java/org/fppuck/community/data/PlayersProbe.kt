package org.fppuck.community.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PlayersProbe {

    private const val TAG = "FPHL_PLAYERS"

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

    suspend fun inspect(
        context: Context,
        teamAbbreviation: String
    ) {

        val teamId =
            teamIds[teamAbbreviation]
                ?: run {
                    Log.e(
                        TAG,
                        "No team ID found for $teamAbbreviation"
                    )
                    return
                }

        val teamUrl =
            "https://www.federalhockey.com/stats" +
                    "?mobile-app=true&theme=false" +
                    "#/182/team/$teamId"

        Log.d(
            TAG,
            "Loading team $teamAbbreviation / ID $teamId"
        )

        try {

            val pageText =
                renderPage(
                    context = context,
                    url = teamUrl
                )

            Log.d(
                TAG,
                "Rendered team page received. Length = ${pageText.length}"
            )

            pageText
                .chunked(3000)
                .forEachIndexed { index, chunk ->

                    Log.d(
                        TAG,
                        "CHUNK ${index + 1}\n$chunk"
                    )
                }

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Team page probe failed",
                exception
            )
        }
    }

    private suspend fun renderPage(
        context: Context,
        url: String
    ): String =
        withContext(Dispatchers.Main) {

            suspendCancellableCoroutine { continuation ->

                val webView =
                    WebView(context)

                val handler =
                    Handler(Looper.getMainLooper())

                var captureScheduled = false

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            Log.d(
                                TAG,
                                "onPageFinished: $url"
                            )

                            if (captureScheduled) {
                                return
                            }

                            captureScheduled = true

                            handler.postDelayed(
                                {

                                    Log.d(
                                        TAG,
                                        "Reading rendered team page"
                                    )

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

                                                    JSONTokener(result)
                                                        .nextValue()
                                                        .toString()
                                                }

                                            webView.destroy()

                                            if (continuation.isActive) {
                                                continuation.resume(text)
                                            }

                                        } catch (exception: Exception) {

                                            webView.destroy()

                                            if (continuation.isActive) {
                                                continuation.resumeWithException(
                                                    exception
                                                )
                                            }
                                        }
                                    }

                                },
                                6000
                            )
                        }
                    }

                continuation.invokeOnCancellation {

                    handler.removeCallbacksAndMessages(null)
                    webView.destroy()
                }

                webView.loadUrl(url)
            }
        }
}