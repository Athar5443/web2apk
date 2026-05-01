# ProGuard rules for the app
-keep class android.webkit.ValueCallback { *; }
-keep class android.webkit.WebView { *; }
-keep class android.webkit.WebChromeClient { *; }
-keep class android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void openFileChooser(...);
}
