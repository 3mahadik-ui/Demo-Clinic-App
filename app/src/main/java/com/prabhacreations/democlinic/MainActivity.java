package com.prabhacreations.democlinic;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int FILE_REQ = 11;
    private WebView web;
    private WebView printView;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "AyushNative");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                // tel:, https://wa.me, sms: etc. open in the phone's own apps
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = params.createIntent();
                i.setType("*/*");
                try {
                    startActivityForResult(i, FILE_REQ);
                } catch (ActivityNotFoundException e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });

        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (saved != null) web.restoreState(saved);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onActivityResult(int req, int code, Intent data) {
        super.onActivityResult(req, code, data);
        if (req == FILE_REQ && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(code, data));
            fileCb = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private class Bridge {
        /** Same value after uninstall + reinstall on the same phone (Android 8+), as long as the APK is signed with the same key. */
        @JavascriptInterface
        public String getDeviceId() {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "" : id;
        }

        /** Saves a file in Downloads/DemoClinic. */
        @JavascriptInterface
        public String saveFile(String name, String mime, String b64) {
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DemoClinic");
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u == null) return "error";
                    try (OutputStream o = getContentResolver().openOutputStream(u)) {
                        if (o == null) return "error";
                        o.write(data);
                    }
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DemoClinic");
                    if (!dir.exists() && !dir.mkdirs()) return "error";
                    try (FileOutputStream o = new FileOutputStream(new File(dir, name))) {
                        o.write(data);
                    }
                }
                return "ok";
            } catch (Exception e) {
                return "error";
            }
        }

        /** Shares a file straight to WhatsApp ("wa") or Google Drive ("drive"). */
        @JavascriptInterface
        public String shareTo(String name, String mime, String b64, String target) {
            String pkg = "wa".equals(target) ? "com.whatsapp" : "com.google.android.apps.docs";
            try {
                getPackageManager().getPackageInfo(pkg, 0);
            } catch (PackageManager.NameNotFoundException e) {
                return "missing";
            }
            try {
                File dir = new File(getCacheDir(), "share");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, name);
                try (FileOutputStream o = new FileOutputStream(f)) {
                    o.write(Base64.decode(b64, Base64.DEFAULT));
                }
                Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(mime);
                i.putExtra(Intent.EXTRA_STREAM, uri);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.setPackage(pkg);
                runOnUiThread(() -> {
                    try { startActivity(i); } catch (ActivityNotFoundException ignored) { }
                });
                return "ok";
            } catch (Exception e) {
                return "error";
            }
        }

        /** Prints a report or prescription with the phone's print / Save as PDF screen. */
        @JavascriptInterface
        public void printHtmlSz(String title, String html, String code) {
            runOnUiThread(() -> {
                printView = new WebView(MainActivity.this);
                printView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView v, String url) {
                        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                        boolean land = "A4L".equals(code) || "A5".equals(code);
                        PrintAttributes.MediaSize size = land
                                ? PrintAttributes.MediaSize.ISO_A4.asLandscape()
                                : PrintAttributes.MediaSize.ISO_A4;
                        PrintAttributes attrs = new PrintAttributes.Builder().setMediaSize(size).build();
                        pm.print(title, v.createPrintDocumentAdapter(title), attrs);
                    }
                });
                printView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
            });
        }
    }
}
