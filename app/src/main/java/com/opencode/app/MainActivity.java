package com.opencode.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.webkit.*;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;
    private ValueCallback<Uri[]> fileCb;
    private String host = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("oc", MODE_PRIVATE);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 1);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (host.equals(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
            // OpenCode server can be protected with HTTP basic auth (OPENCODE_SERVER_PASSWORD)
            @Override public void onReceivedHttpAuthRequest(WebView v, HttpAuthHandler h, String hst, String realm) {
                String u = prefs.getString("user", "opencode"), p = prefs.getString("pass", "");
                if (!p.isEmpty() && !prefs.getBoolean("authTried", false)) {
                    prefs.edit().putBoolean("authTried", true).apply();
                    h.proceed(u, p);
                } else {
                    prefs.edit().putBoolean("authTried", false).apply();
                    h.cancel();
                    askServer(true);
                }
            }
            @Override public void onPageFinished(WebView v, String url) { prefs.edit().putBoolean("authTried", false).apply(); }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest r) { runOnUiThread(() -> r.grant(r.getResources())); }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try { startActivityForResult(p.createIntent(), 2); } catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });

        String url = prefs.getString("url", "");
        if (url.isEmpty()) askServer(false); else load(url);
    }

    private void load(String url) {
        if (!url.startsWith("http")) url = "http://" + url;
        host = Uri.parse(url).getHost();
        prefs.edit().putString("url", url).apply();
        web.loadUrl(url);
    }

    private void askServer(boolean needAuth) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(48, 24, 48, 0);
        final EditText url = new EditText(this);
        url.setHint("Server URL, e.g. http://192.168.1.10:4096");
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(prefs.getString("url", ""));
        final EditText user = new EditText(this);
        user.setHint("Username (default: opencode)");
        user.setText(prefs.getString("user", "opencode"));
        final EditText pass = new EditText(this);
        pass.setHint("Password (leave blank if none)");
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setText(prefs.getString("pass", ""));
        l.addView(url); l.addView(user); l.addView(pass);
        new AlertDialog.Builder(this)
            .setTitle(needAuth ? "Server needs a login" : "OpenCode server")
            .setMessage("Run `opencode web --hostname 0.0.0.0` on your computer, then enter its address.")
            .setView(l).setCancelable(false)
            .setPositiveButton("Connect", (d, w) -> {
                prefs.edit().putString("user", user.getText().toString().trim())
                    .putString("pass", pass.getText().toString()).apply();
                load(url.getText().toString().trim());
            }).show();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == 2 && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        }
    }

    // Back navigates the web history; at the first page, a second Back opens server settings.
    private boolean backOnce = false;
    @Override public void onBackPressed() {
        if (web.canGoBack()) { web.goBack(); backOnce = false; }
        else if (!backOnce) { backOnce = true; }
        else { backOnce = false; askServer(false); }
    }
}
