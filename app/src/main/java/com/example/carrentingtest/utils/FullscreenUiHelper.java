package com.example.carrentingtest.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.IdRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class FullscreenUiHelper {

    private FullscreenUiHelper() {
    }

    public static void apply(Activity activity, @IdRes int rootViewId) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            controller.hide(WindowInsetsCompat.Type.systemBars());
        }

        View root = activity.findViewById(rootViewId);
        if (root == null) {
            View content = activity.findViewById(android.R.id.content);
            if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
                root = ((ViewGroup) content).getChildAt(0);
            }
        }
        if (root == null) {
            return;
        }

        final int start = root.getPaddingStart();
        final int top = root.getPaddingTop();
        final int end = root.getPaddingEnd();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(bars.bottom, ime.bottom);
            view.setPaddingRelative(start, top + bars.top, end, bottom + bottomInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
