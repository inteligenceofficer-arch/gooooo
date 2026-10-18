package com.stampcam.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Thin shell around the hosted StampCam web app: grants camera/GPS, saves photos to the gallery, checks for APK updates. */
public class MainActivity extends Activity {
    private static final String APP_URL = "https://inteligenceofficer-arch.github.io/gooooo/camera/";
    private static final String RELEASE_API = "https://api.github.com/repos/inteligenceofficer-arch/gooooo/releases/latest";
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (web == null) startWeb();
    }

    private void startWeb() {
        web = new WebView(this);
        setContentView(web);
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(() -> r.grant(r.getResources()));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED, false);
            }
        });
        web.loadUrl(APP_URL);
        checkUpdate();
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    /** Called from the web page: stores a JPEG in Pictures/StampCam so it shows up in the gallery app. */
    private class Bridge {
        @JavascriptInterface
        public boolean saveImage(String base64, String name) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StampCam");
                v.put(MediaStore.Images.Media.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                try (OutputStream o = getContentResolver().openOutputStream(uri)) {
                    o.write(Base64.decode(base64, Base64.DEFAULT));
                }
                v.clear();
                v.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, v, null, null);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /** Compares the newest GitHub release (tag v<versionCode>) with this build and offers the APK download. */
    private void checkUpdate() {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(RELEASE_API).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                InputStream in = c.getInputStream();
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                byte[] tmp = new byte[4096];
                int n;
                while ((n = in.read(tmp)) > 0) buf.write(tmp, 0, n);
                JSONObject j = new JSONObject(buf.toString("UTF-8"));
                long latest = Long.parseLong(j.getString("tag_name").replaceAll("[^0-9]", ""));
                long mine = getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
                JSONArray assets = j.getJSONArray("assets");
                String apk = null;
                for (int i = 0; i < assets.length(); i++) {
                    String u = assets.getJSONObject(i).getString("browser_download_url");
                    if (u.endsWith(".apk")) apk = u;
                }
                if (latest > mine && apk != null) {
                    final String url = apk;
                    runOnUiThread(() -> new AlertDialog.Builder(this)
                            .setTitle("새 버전이 있습니다")
                            .setMessage("업데이트를 다운로드할까요? 다운로드 후 파일을 열어 설치하세요.")
                            .setPositiveButton("업데이트", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
                            .setNegativeButton("나중에", null)
                            .show());
                }
            } catch (Exception ignored) {
                // offline or rate-limited: skip silently
            }
        }).start();
    }
}
