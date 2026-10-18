package com.nidssurvey.mapstore;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Base64;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge().getWebView();
        WebSettings settings = webView.getSettings();

        // ១. បើកសិទ្ធិទាំងអស់សម្រាប់ DOM, Window Open និង JavaScript
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // ២. ចាប់យក window.open() ទាំងអស់ដើម្បីទាញយក ឬបើកក្នុង Chrome ក្រៅ
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url.startsWith("blob:") || url.startsWith("data:")) {
                            triggerDownload(url, null, "application/pdf");
                        } else {
                            // បើកទៅកាន់ Browser ក្រៅ (Chrome) សម្រាប់ទាញយក
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        }
                        return true;
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        // ៣. ចាប់យក Download Listener ទូទៅ
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                triggerDownload(url, contentDisposition, mimeType);
            }
        });
    }

    private void triggerDownload(String url, String contentDisposition, String mimeType) {
        if (url == null) return;

        if (url.startsWith("blob:")) {
            // ចាប់បម្លែង Blob ទៅជា Base64 រួច Save
            String js = "var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + url + "', true);" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function() {" +
                    "    if (this.status === 200) {" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(this.response);" +
                    "        reader.onloadend = function() {" +
                    "            var link = document.createElement('a');" +
                    "            link.href = reader.result;" +
                    "            link.download = 'commune_profile.pdf';" +
                    "            document.body.appendChild(link);" +
                    "            link.click();" +
                    "        };" +
                    "    }" +
                    "};" +
                    "xhr.send();";
            getBridge().getWebView().evaluateJavascript(js, null);
            Toast.makeText(this, "កំពុងដំណើរការទាញយក PDF...", Toast.LENGTH_SHORT).show();
        } else if (url.startsWith("data:")) {
            saveDataUri(url);
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "កំហុស: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveDataUri(String dataUri) {
        try {
            String base64Data = dataUri.substring(dataUri.indexOf(",") + 1);
            byte[] pdfBytes = Base64.decode(base64Data, Base64.DEFAULT);
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloads, "commune_profile_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(pdfBytes);
            fos.flush();
            fos.close();
            Toast.makeText(this, "បានរក្សាទុកក្នុង Downloads: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "មិនអាចរក្សាទុក: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}