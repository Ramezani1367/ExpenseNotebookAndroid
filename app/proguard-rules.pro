# JavaScript bridge methods are referenced from WebView JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
