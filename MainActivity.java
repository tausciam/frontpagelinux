package com.frontpagelinux;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    SwipeRefreshLayout mySwipeRefreshLayout;
    ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();
        actionBar.hide();
        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeContainer);
        webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings= webView.getSettings();
        /*All the swiperefreshlayout stuff was a feature request by Brian Snipes. It makes it so
        when you pull down on the screen, it refreshes the page. It's a great feature and definitely
        should have been added. */

        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (mySwipeRefreshLayout.isRefreshing()) {
                            mySwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                },2000);
            }
        });

        /*Javascript is used in many modern websites, but you have to also
        enable DOM storage or the hamburger menu won't work */
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        loadWebSite();

        /*This is the second portion to restore state after orientation change.
        The first part is way below and an explanation is given there. I also had to put a
        configchanges line in AndroidManifest.xml. Without it, none of this will work.*/

        if(savedInstanceState==null){
            webView.post(new Runnable() {
                @Override
                public void run() {
                    loadWebSite();
                }
            });
        }

        /*The first is needed to keep each click from opening in your browser,
        but the second is needed to handle the embedded video correctly */
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new ChromeClient());

    }

    //Code to make android back button act as browser back button
    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /*This whole class is just to allow you to fullscreen videos. Without this and
    changing setWebChromeClient above to new ChromeClient instead of WebChromeClient,
    you will not be able to fullscreen videos. */
    private class ChromeClient extends WebChromeClient {
        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        ChromeClient() {}

        public Bitmap getDefaultVideoPoster()
        {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView()
        {
            ((FrameLayout)getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback)
        {
            if (this.mCustomView != null)
            {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout)getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /*But, if you just run the code as is, you'll find out that every time you switch from portrait
    to landscape, the video will reset and you will have to start over. That's obviously not what we
    want, so you're going to have to save and reload the website's state rather than a fresh new version
    of the website on each orientation change*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState )
    {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    private void loadWebSite() {
        webView.loadUrl("https://frontpagelinux.com");
    }

    //This whole function is so when you can scroll up and down, but a pull down at the top refreshes.
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    @Override
    public void onStart() {
        super.onStart();

        mySwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener =
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (webView.getScrollY() == 0)
                            mySwipeRefreshLayout.setEnabled(true);
                        else
                            mySwipeRefreshLayout.setEnabled(false);

                    }
                });
    }

    @Override
    public void onStop() {
        mySwipeRefreshLayout.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        super.onStop();
    }

}