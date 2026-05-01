package athars.template;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private WebView popupWebView; // Tracks active popup window
    private View customView; // Fullscreen video view
    private WebChromeClient.CustomViewCallback customViewCallback;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout errorLayout;
    private Button btnRetry;
    private String TARGET_URL = "{{TARGET_URL}}";

    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;
    
    // JIT Permission Variables
    private final static int GEO_PERMISSION_REQUEST_CODE = 1001;
    private final static int WEBRTC_PERMISSION_REQUEST_CODE = 1002;
    private final static int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;
    private GeolocationPermissions.Callback mGeoLocationCallback;
    private String mGeoLocationOrigin;
    private PermissionRequest mPermissionRequest;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        errorLayout = findViewById(R.id.errorLayout);
        btnRetry = findViewById(R.id.btnRetry);

        // Apply Window Insets for Edge-to-Edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupWebView();

        // Sleek Minimalist Spinner Color
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        );
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        btnRetry.setOnClickListener(v -> {
            crossfade(webView, errorLayout);
            webView.reload();
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(TARGET_URL);
        }
    }

    private void crossfade(View viewToShow, View viewToHide) {
        viewToShow.setAlpha(0f);
        viewToShow.setVisibility(View.VISIBLE);
        viewToShow.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null);

        if (viewToHide != null && viewToHide.getVisibility() == View.VISIBLE) {
            viewToHide.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            viewToHide.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setGeolocationEnabled(true);
        
        // Smart Offline Caching
        if (isNetworkAvailable()) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        
        // Custom User Agent to bypass Google OAuth 403 Error
        String userAgent = webSettings.getUserAgentString().replace("; wv", "");
        webSettings.setUserAgentString(userAgent + " Web2APK-Android");

        // Dark Mode Support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
            }
        }

        // Swipe-to-Refresh Scroll Bug Fix
        webView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (webView.getScrollY() == 0) {
                swipeRefreshLayout.setEnabled(true);
            } else {
                swipeRefreshLayout.setEnabled(false);
            }
        });

        // Handle target="_blank"
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false; 
                }
                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            PackageManager packageManager = getPackageManager();
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent);
                                return true;
                            }
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                webView.loadUrl(fallbackUrl);
                                return true;
                            }
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + intent.getPackage()));
                            if (marketIntent.resolveActivity(packageManager) != null) {
                                startActivity(marketIntent);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    // Deep Link Fallback
                    Toast.makeText(MainActivity.this, "Aplikasi untuk membuka tautan ini tidak terinstal.", Toast.LENGTH_SHORT).show();
                    return true; 
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                crossfade(webView, errorLayout);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showErrorLayout();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
                decorView.addView(customView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
                decorView.removeView(customView);
                customView = null;
                customViewCallback.onCustomViewHidden();
                webView.setVisibility(View.VISIBLE);
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                popupWebView = new WebView(MainActivity.this);
                view.addView(popupWebView);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();
                
                popupWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        MainActivity.this.webView.loadUrl(request.getUrl().toString());
                        return true;
                    }
                });
                
                // Handle javascript window.close() on popup
                popupWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        if (popupWebView != null) {
                            ((ViewGroup) popupWebView.getParent()).removeView(popupWebView);
                            popupWebView.destroy();
                            popupWebView = null;
                        }
                    }
                });
                return true;
            }

            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            // For File Upload
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILECHOOSER_RESULTCODE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }

            // For Geolocation (JIT)
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    mGeoLocationCallback = callback;
                    mGeoLocationOrigin = origin;
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GEO_PERMISSION_REQUEST_CODE);
                } else {
                    callback.invoke(origin, true, false);
                }
            }

            // For WebRTC Camera/Mic (JIT)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                mPermissionRequest = request;
                List<String> requestedPermissions = new ArrayList<>();
                
                for (String resource : request.getResources()) {
                    if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestedPermissions.add(Manifest.permission.CAMERA);
                        }
                    } else if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestedPermissions.add(Manifest.permission.RECORD_AUDIO);
                        }
                    }
                }

                if (!requestedPermissions.isEmpty()) {
                    ActivityCompat.requestPermissions(MainActivity.this, requestedPermissions.toArray(new String[0]), WEBRTC_PERMISSION_REQUEST_CODE);
                } else {
                    // All needed Android permissions are already granted, grant the web request immediately
                    request.grant(request.getResources());
                }
            }
        });

        // Handle File Downloads
        webView.setDownloadListener((url, userAgent1, contentDisposition, mimetype, contentLength) -> {
            // Android 13+ JIT Notification Permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                }
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent1);
            request.setDescription(getString(R.string.downloading));
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                fileName = fileName.substring(1, fileName.length() - 1);
            }
            request.setTitle(fileName);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), R.string.downloading, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Handle JIT Permission Results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == GEO_PERMISSION_REQUEST_CODE) {
            if (mGeoLocationCallback != null) {
                boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                mGeoLocationCallback.invoke(mGeoLocationOrigin, granted, false);
                mGeoLocationCallback = null;
                mGeoLocationOrigin = null;
            }
        } else if (requestCode == WEBRTC_PERMISSION_REQUEST_CODE) {
            if (mPermissionRequest != null) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    mPermissionRequest.grant(mPermissionRequest.getResources());
                } else {
                    mPermissionRequest.deny();
                }
                mPermissionRequest = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void showErrorLayout() {
        crossfade(errorLayout, webView);
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers(); // Pause JS timers and media
        }
        // Force sync cookies to disk to prevent session loss
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState); // Save back history
        }
    }

    @Override
    public void onBackPressed() {
        // Hide fullscreen video if active
        if (customView != null) {
            customViewCallback.onCustomViewHidden();
            return;
        }
        
        // Fix Popup Back-Button Trap
        if (popupWebView != null) {
            ((ViewGroup) popupWebView.getParent()).removeView(popupWebView);
            popupWebView.destroy();
            popupWebView = null;
        } else if (errorLayout.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}