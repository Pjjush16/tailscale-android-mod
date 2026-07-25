// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

/**
 * Built-in browser that automatically routes traffic through the Tailscale tunnel.
 * This allows users to access HTTP/FTP services on the Tailnet without needing
 * to configure a proxy in their browser.
 */
class BuiltInBrowserActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "BuiltInBrowser"
        private const val PROXY_HOST = "127.0.0.1"
        private const val PROXY_PORT = 8080
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create layout programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // URL bar
        val urlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }

        urlEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "http://100.x.x.x/"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        val goButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setOnClickListener {
                val url = urlEditText.text.toString()
                if (url.isNotBlank()) {
                    loadUrl(url)
                }
            }
        }

        val backButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_rew)
            setOnClickListener {
                if (webView.canGoBack()) {
                    webView.goBack()
                }
            }
        }

        val forwardButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_ff)
            setOnClickListener {
                if (webView.canGoForward()) {
                    webView.goForward()
                }
            }
        }

        urlBar.addView(backButton)
        urlBar.addView(forwardButton)
        urlBar.addView(urlEditText)
        urlBar.addView(goButton)

        // Progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4
            )
            max = 100
        }

        // WebView
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
        }

        // Configure proxy
        configureProxy(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                urlEditText.setText(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = android.view.View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    progressBar.visibility = android.view.View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }

        layout.addView(urlBar)
        layout.addView(progressBar)
        layout.addView(webView)

        setContentView(layout)

        // Load initial URL if provided
        val initialUrl = intent.getStringExtra("url")
        if (initialUrl != null) {
            urlEditText.setText(initialUrl)
            loadUrl(initialUrl)
        } else {
            // Show a welcome page
            webView.loadData(
                """
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: sans-serif; padding: 20px; background: #f0f0f0; }
                        h1 { color: #333; }
                        .info { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; }
                    </style>
                </head>
                <body>
                    <h1>🌐 Tailscale 内置浏览器</h1>
                    <div class="info">
                        <p>此浏览器已自动配置代理，可以直接访问 Tailnet 内的 HTTP/FTP 服务。</p>
                        <p>代理地址: $PROXY_HOST:$PROXY_PORT</p>
                    </div>
                    <div class="info">
                        <h2>使用方法</h2>
                        <ol>
                            <li>在地址栏输入 URL（如 http://100.x.x.x/）</li>
                            <li>点击播放按钮或按回车</li>
                            <li>浏览器会通过 Tailscale 隧道访问目标</li>
                        </ol>
                    </div>
                    <div class="info">
                        <h2>示例</h2>
                        <p>http://100.64.0.1/ - 访问 Tailnet 内的 HTTP 服务器</p>
                        <p>http://100.64.0.2:8080/ - 访问带端口的服务</p>
                    </div>
                </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8"
            )
        }
    }

    /**
     * Configure WebView to use proxy.
     * Note: On modern Android versions (API 21+), WebView proxy configuration
     * is done via ProxyController. For older versions, we use the deprecated method.
     */
    @Suppress("DEPRECATION")
    private fun configureProxy(webView: WebView) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // Use ProxyController for API 21+
                val proxyController = android.webkit.ProxyController.getInstance()
                proxyController.setProxyOverride(
                    android.webkit.ProxyConfig.Builder()
                        .addProxyRule("$PROXY_HOST:$PROXY_PORT")
                        .build(),
                    { it.run() },
                    { it.run() }
                )
                Log.d(TAG, "Proxy configured using ProxyController: $PROXY_HOST:$PROXY_PORT")
            } else {
                // Fallback for older Android versions
                val webViewClass = webView.javaClass
                try {
                    val method = webViewClass.getMethod("setHttpProxy", java.lang.Boolean.TYPE)
                    method.invoke(webView, true)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set proxy via reflection: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure proxy: ${e.message}")
        }
    }

    private fun loadUrl(url: String) {
        var targetUrl = url.trim()
        // Add http:// if no protocol specified
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "http://$targetUrl"
        }
        webView.loadUrl(targetUrl)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
