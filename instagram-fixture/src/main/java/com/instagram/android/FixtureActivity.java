package com.instagram.android;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

public class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean arabic = getIntent().getBooleanExtra("arabic", true);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this); label.setText("TEST FIXTURE — not Instagram"); page.addView(label);
        Button share = new Button(this); share.setText(arabic ? "مشاركة" : "Share reel"); page.addView(share);
        share.setOnClickListener(v -> {
            Dialog sheet = new Dialog(this);
            LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
            TextView heading = new TextView(this); heading.setText("TEST SHARE SHEET"); content.addView(heading);
            Runnable copy = () -> {
                if (!getIntent().getBooleanExtra("stale", false)) getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("fixture", "https://www.instagram.com/reel/AdapterFixture/"));
                if (!getIntent().getBooleanExtra("keepSheet", false)) sheet.dismiss();
            };
            if (getIntent().getBooleanExtra("nested", false)) {
                LinearLayout tile = new LinearLayout(this);
                tile.setOnClickListener(c -> copy.run());
                if (getIntent().getBooleanExtra("fail", false)) tile.setAccessibilityDelegate(rejectClick());
                LinearLayout wrapper = new LinearLayout(this);
                wrapper.setClickable(true); wrapper.setAccessibilityDelegate(rejectClick());
                TextView text = new TextView(this); text.setText(arabic ? "نسخ الرابط" : "Copy link");
                wrapper.addView(text); tile.addView(wrapper); content.addView(tile);
                if (getIntent().getBooleanExtra("decorative", false)) {
                    ImageView image = new ImageView(this);
                    image.setImageResource(android.R.drawable.ic_menu_save);
                    image.setContentDescription(arabic ? "نسخ الرابط" : "Copy link"); image.setClickable(true);
                    image.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                        @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
                        }
                        @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
                            return action != AccessibilityNodeInfo.ACTION_CLICK && super.performAccessibilityAction(host, action, args);
                        }
                    });
                    LinearLayout imageContainer = new LinearLayout(this); imageContainer.addView(image); tile.addView(imageContainer);
                }
                if (getIntent().getBooleanExtra("delayed", false)) {
                    tile.setEnabled(false);
                    tile.postDelayed(() -> { tile.setEnabled(true); tile.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED); }, 650);
                }
            } else {
                Button button = new Button(this); button.setText(arabic ? "نسخ الرابط" : "Copy link");
                button.setOnClickListener(c -> copy.run()); content.addView(button);
            }
            sheet.setContentView(content); sheet.show();
        });
        if (getIntent().getBooleanExtra("ambiguous", false)) { Button second = new Button(this); second.setText(arabic ? "مشاركة" : "Share reel"); page.addView(second); }
        if (getIntent().getBooleanExtra("stale", false)) getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("old", "https://www.instagram.com/reel/OldReel/"));
        setContentView(page);
    }
    private View.AccessibilityDelegate rejectClick() {
        return new View.AccessibilityDelegate() {
            @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) return false;
                return super.performAccessibilityAction(host, action, args);
            }
        };
    }
}
