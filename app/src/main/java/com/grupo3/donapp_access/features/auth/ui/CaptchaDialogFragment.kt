package com.grupo3.donapp_access.features.auth.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment

class CaptchaDialogFragment(
    private val onCaptchaSuccess: (String) -> Unit
) : DialogFragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private val SITE_KEY = "0x4AAAAAADUakTYpLPM9cxyl"

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val root = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleSmall)
        webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
        }

        root.addView(progressBar)
        root.addView(webView)
        return root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onTokenGenerated(token: String) {
                activity?.runOnUiThread {
                    onCaptchaSuccess(token)
                    dismiss()
                }
            }
        }, "AndroidCaptcha")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }

        val htmlData = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
                <style>
                    body { display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #ffffff; }
                </style>
            </head>
            <body>
                <div class="cf-turnstile" data-sitekey="$SITE_KEY" data-callback="javascriptCallback"></div>
                <script>
                    function javascriptCallback(token) {
                        AndroidCaptcha.onTokenGenerated(token);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        // la URL de producción para validar de forma legítima
        webView.loadDataWithBaseURL("https://uomlyvsrlkvsroowlhqh.supabase.co", htmlData, "text/html", "UTF-8", null)
    }
}