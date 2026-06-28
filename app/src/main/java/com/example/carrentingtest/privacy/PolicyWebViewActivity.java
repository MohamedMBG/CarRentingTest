package com.example.carrentingtest.privacy;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;

/**
 * Generic WebView host for remote legal HTML (Privacy Policy, ToS).
 *
 * <p>The text lives on a server so legal can update wording without an app release.
 * JavaScript is disabled — the pages must render as static HTML to keep the attack
 * surface small.</p>
 */
public class PolicyWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    public static void start(@NonNull Context context, @NonNull String url, @NonNull String title) {
        Intent intent = new Intent(context, PolicyWebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy_web);

        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        WebView webView = findViewById(R.id.policyWebView);
        ProgressBar progress = findViewById(R.id.policyProgress);
        TextView error = findViewById(R.id.policyError);

        // JS off: legal text is static HTML; reduces XSS surface from a compromised CDN.
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setDomStorageEnabled(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
                error.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError err) {
                if (request.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    error.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            }
        });

        if (url == null || url.isEmpty()) {
            progress.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            return;
        }
        webView.loadUrl(url);
    }
}
