package ir.ramezani.expensenotebook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingOpenMode: String? = null
    private var pageLoaded = false

    companion object {
        const val EXTRA_OPEN_MODE = "open_mode"
        private const val FILE_CHOOSER_REQUEST_CODE = 1010
        private const val APP_URL = "file:///android_asset/www/index.html"
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpenMode = intent?.getStringExtra(EXTRA_OPEN_MODE)

        configureSystemBars()

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.rgb(236, 236, 236))
        }

        setContentView(webView)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        configureWebView()
        webView.loadUrl(APP_URL)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenMode = intent?.getStringExtra(EXTRA_OPEN_MODE)
        if (::webView.isInitialized) {
            pageLoaded = false
            webView.loadUrl(APP_URL)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized && pageLoaded) {
            webView.evaluateJavascript(
                "try{ if(window.__androidRefreshFromNative) window.__androidRefreshFromNative(); }catch(e){}",
                null
            )
        }
        ExpenseDataUtils.updateAllWidgets(this)
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.rgb(245, 245, 245)
        window.navigationBarColor = Color.WHITE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                pageLoaded = true
                injectNativeShare(view)
                syncWebStorageToWidget(view)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleExternalUrl(Uri.parse(url))
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = try {
                    fileChooserParams?.createIntent()
                } catch (_: Exception) {
                    null
                } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }

                return try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                    true
                } catch (_: Exception) {
                    this@MainActivity.filePathCallback = null
                    Toast.makeText(
                        this@MainActivity,
                        "انتخاب فایل در این دستگاه ممکن نیست",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
        }
    }

    private fun handleExternalUrl(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return false
        if (scheme == "file" || scheme == "about" || scheme == "data" || scheme == "javascript") {
            return false
        }

        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun injectNativeShare(view: WebView) {
        val script = """
            (function(){
              var nativeShare = function(data){
                data = data || {};
                AndroidBridge.share(String(data.title || 'دفترچه هزینه'), String(data.text || ''));
                return Promise.resolve();
              };
              try {
                Object.defineProperty(navigator, 'share', {
                  configurable: true,
                  enumerable: false,
                  value: nativeShare
                });
              } catch(e) {
                navigator.share = nativeShare;
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private fun syncWebStorageToWidget(view: WebView) {
        val script = """
            (function(){
              try {
                var data = localStorage.getItem('xp_v26') || '[]';
                if (window.AndroidBridge && AndroidBridge.syncExpenses) AndroidBridge.syncExpenses(data);
              } catch(e) {}
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val script = """
            (function(){
              var ids = ['cf','cv','shm','dmm','em','fp','bp'];
              var open = false;
              for (var i = 0; i < ids.length; i++) {
                var el = document.getElementById(ids[i]);
                if (el && el.classList.contains('show')) { open = true; break; }
              }
              if (open) {
                document.dispatchEvent(new KeyboardEvent('keydown', {key:'Escape'}));
                return true;
              }
              return false;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            if (result == "true") {
                return@evaluateJavascript
            }
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) return

        val callback = filePathCallback ?: return
        filePathCallback = null

        val result = if (resultCode == RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                ?: data?.data?.let { arrayOf(it) }
        } else {
            null
        }
        callback.onReceiveValue(result)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.apply {
                stopLoading()
                webChromeClient = null
                removeJavascriptInterface("AndroidBridge")
                destroy()
            }
        }
        super.onDestroy()
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun getExpensesJson(): String = ExpenseDataUtils.getExpensesJson(this@MainActivity)

        @JavascriptInterface
        fun getOpenMode(): String {
            val mode = pendingOpenMode ?: ""
            pendingOpenMode = null
            return mode
        }

        @JavascriptInterface
        fun syncExpenses(json: String?) {
            if (!json.isNullOrBlank()) {
                ExpenseDataUtils.syncExpensesJson(this@MainActivity, json)
            }
        }

        @JavascriptInterface
        fun share(title: String?, text: String?) {
            runOnUiThread {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title ?: "دفترچه هزینه")
                    putExtra(Intent.EXTRA_TEXT, text.orEmpty())
                }
                startActivity(Intent.createChooser(sendIntent, title ?: "دفترچه هزینه"))
            }
        }

        @JavascriptInterface
        fun saveFile(fileName: String?, mimeType: String?, content: String?) {
            val safeName = sanitizeFileName(fileName ?: "expenses.txt")
            val cleanMimeType = (mimeType ?: "text/plain").substringBefore(';')
            val bytes = content.orEmpty().toByteArray(Charsets.UTF_8)

            try {
                saveBytes(safeName, cleanMimeType, bytes)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "ذخیره شد: Downloads/DaftarcheHazine/$safeName",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "ذخیره فایل ناموفق بود",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return cleaned.ifEmpty { "expenses.txt" }
    }

    private fun saveBytes(fileName: String, mimeType: String, bytes: ByteArray): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DaftarcheHazine")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create MediaStore entry")

            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("Could not open output stream")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val baseDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
            val dir = File(baseDir, "DaftarcheHazine").apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file)
        }
    }
}
