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

object PlayerProbe {

    private const val TAG = "FPHL_PLAYER"

    // Aaron Shahin - Indiana Sentinels
    private const val PLAYER_ID = "2839802"

    private const val PLAYER_URL =
        "https://www.federalhockey.com/stats" +
                "?mobile-app=true&theme=false" +
                "#/player/$PLAYER_ID"

    suspend fun inspect(
        context: Context
    ) {

        Log.d(
            TAG,
            "Player probe started for $PLAYER_ID"
        )

        try {

            val text =
                renderPage(context)

            Log.d(
                TAG,
                "Player page length = ${text.length}"
            )

            text
                .chunked(3000)
                .forEachIndexed { index, chunk ->

                    Log.d(
                        TAG,
                        "PLAYER CHUNK ${index + 1}\n$chunk"
                    )
                }

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Player probe failed",
                exception
            )
        }
    }

    private suspend fun renderPage(
        context: Context
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
                                6000
                            )
                        }
                    }

                continuation.invokeOnCancellation {

                    handler.removeCallbacksAndMessages(
                        null
                    )

                    webView.destroy()
                }

                webView.loadUrl(
                    PLAYER_URL
                )
            }
        }
}