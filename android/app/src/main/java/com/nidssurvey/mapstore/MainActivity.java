package com.nidssurvey.mapstore;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge().getWebView();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new BlobDownloadInterface(this), "AndroidDownload");

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (url.startsWith("blob:")) {
                String js = "var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '" + url + "', true);" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function() {" +
                        "    if (this.status === 200) {" +
                        "        var blob = this.response;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blob);" +
                        "        reader.onloadend = function() {" +
                        "            var base64data = reader.result;" +
                        "            AndroidDownload.getBase64FromBlobData(base64data, '" + mimeType + "');" +
                        "        };" +
                        "    }" +
                        "};" +
                        "xhr.send();";
                webView.loadUrl("javascript:" + js);
            } else if (url.startsWith("data:")) {
                saveDataUri(url, mimeType);
            } else {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.setDescription("Downloading document...");
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "កំពុងទាញយកឯកសារ...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "កំហុសទាញយក: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveDataUri(String dataUri, String mimeType) {
        try {
            String base64Data = dataUri.substring(dataUri.indexOf(",") + 1);
            byte[] pdfAsBytes = Base64.decode(base64Data, Base64.DEFAULT);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "commune_profile_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream os = new FileOutputStream(file);
            os.write(pdfAsBytes);
            os.flush();
            os.close();
            Toast.makeText(this, "រក្សាទុកឯកសារ PDF រួចរាល់ក្នុង Downloads!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "មិនអាចរក្សាទុកឯកសារ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class BlobDownloadInterface {
        private Context context;
        public BlobDownloadInterface(Context context) { this.context = context; }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data, String mimeType) {
            saveDataUri(base64Data, mimeType);
        }
    }
}