package com.frontpagelinux

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    private var webView: WebView? = null
    var mySwipeRefreshLayout: SwipeRefreshLayout? = null
    var actionBar: ActionBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actionBar = supportActionBar
        actionBar!!.hide()
        mySwipeRefreshLayout = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        webView = findViewById<View>(R.id.webview) as WebView
        val webSettings = webView!!.settings
        /*All the swiperefreshlayout stuff was a feature request by Brian Snipes. It makes it so
        when you pull down on the screen, it refreshes the page. It's a great feature and definitely
        should have been added. */mySwipeRefreshLayout!!.setOnRefreshListener {
            webView!!.reload()
            Handler(Looper.getMainLooper()).postDelayed({
                if (mySwipeRefreshLayout!!.isRefreshing) {
                    mySwipeRefreshLayout!!.isRefreshing = false
                }
            }, 2000)
        }

        /*This is a feature request by Michael Tunnell. Press and hold on a link to be
        able to copy the URL or share it with other apps/friends. It works with text and
        image links.
         */
        webView!!.setOnLongClickListener(View.OnLongClickListener {
            val result = webView!!.hitTestResult
            var url :String? = null
            if (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val handler = Handler()
                val message = handler.obtainMessage()

                webView!!.requestFocusNodeHref(message)
                url = message.data.getString("url")
            }
            if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                url = result.extra
            }
            
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)

            true
        })

        /*Javascript is used in many modern websites, but you have to also
        enable DOM storage or the hamburger menu won't work */
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        //Hopefully speeds up webview
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = false
        webView!!.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY;
        webView!!.isScrollbarFadingEnabled = true

        loadWebSite()

        /*This is the second portion to restore state after orientation change.
        The first part is way below and an explanation is given there. I also had to put a
        configchanges line in AndroidManifest.xml. Without it, none of this will work.*/if (savedInstanceState == null) {
            webView!!.post { loadWebSite() }
        }

        /*The first is needed to keep each click from opening in your browser,
        but the second is needed to handle the embedded video correctly */webView!!.webViewClient = WebViewClient()
        webView!!.webChromeClient = ChromeClient()
    }

    //Code to make android back button act as browser back button
    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /*This whole class is just to allow you to fullscreen videos. Without this and
    changing setWebChromeClient above to new ChromeClient instead of WebChromeClient,
    you will not be able to fullscreen videos. */
    private inner class ChromeClient internal constructor() : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        protected var mFullscreenContainer: FrameLayout? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0
        override fun getDefaultVideoPoster(): Bitmap? {
            return if (mCustomView == null) {
                null
            } else BitmapFactory.decodeResource(applicationContext.resources, 2130837573)
        }

        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.setSystemUiVisibility(mOriginalSystemUiVisibility)
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(paramView: View, paramCustomViewCallback: CustomViewCallback) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
            mOriginalOrientation = requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
            window.decorView.setSystemUiVisibility(3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    /*But, if you just run the code as is, you'll find out that every time you switch from portrait
    to landscape, the video will reset and you will have to start over. That's obviously not what we
    want, so you're going to have to save and reload the website's state rather than a fresh new version
    of the website on each orientation change*/
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView!!.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView!!.restoreState(savedInstanceState)
    }

    private fun loadWebSite() {
        webView!!.loadUrl("https://frontpagelinux.com")
    }

    //This whole function is so when you can scroll up and down, but a pull down at the top refreshes.
    private var mOnScrollChangedListener: OnScrollChangedListener? = null
    public override fun onStart() {
        super.onStart()
        mySwipeRefreshLayout!!.viewTreeObserver.addOnScrollChangedListener(OnScrollChangedListener { if (webView!!.scrollY == 0) mySwipeRefreshLayout!!.isEnabled = true else mySwipeRefreshLayout!!.isEnabled = false }.also { mOnScrollChangedListener = it })
    }

    public override fun onStop() {
        mySwipeRefreshLayout!!.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
        super.onStop()
    }
}