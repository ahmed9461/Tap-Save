package io.github.ahmed9461.tapsave.probe;

import android.app.Activity;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import java.net.URI;

/** Manual ADB gate after compareCopy only. Reads only while focused, no arbitrary clip export. */
public class ClipboardProbeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int attempts;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        TextView label = new TextView(this); label.setText("Tap Save: checking fresh Reel link"); setContentView(label);
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        handler.removeCallbacksAndMessages(null);
        if (focused) inspect();
    }
    private void inspect() {
        long after = getSharedPreferences("copy-probe", 0).getLong("after", 0);
        if (!hasWindowFocus() || after == 0 || System.currentTimeMillis() - after > 60000) { finish(); return; }
        android.content.ClipData clip = getSystemService(ClipboardManager.class).getPrimaryClip();
        long timestamp = clip == null ? 0 : clip.getDescription().getTimestamp();
        boolean fresh = timestamp >= after;
        String code = "none";
        if (fresh && clip.getItemCount() == 1) {
            try {
                URI uri = URI.create(String.valueOf(clip.getItemAt(0).getText()));
                if ("https".equals(uri.getScheme()) && ("www.instagram.com".equals(uri.getHost()) || "instagram.com".equals(uri.getHost())) && uri.getPath().matches("/reels?/[A-Za-z0-9_-]{1,64}/?")) code = uri.getPath().replaceFirst("^/reels?/", "").replace("/", "");
            } catch (RuntimeException ignored) { }
        }
        if (fresh || ++attempts >= 25) {
            Log.i("TapSaveCopyProbe", "focused=true fresh=" + fresh + " timestamp_delta_ms=" + (timestamp - after) + " reel=" + code);
            getSharedPreferences("copy-probe", 0).edit().clear().apply(); finish();
        } else handler.postDelayed(this::inspect, 200);
    }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
