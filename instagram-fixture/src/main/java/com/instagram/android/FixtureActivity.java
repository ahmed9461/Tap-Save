package com.instagram.android;
import android.app.Activity;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.*;
public class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean arabic = getIntent().getBooleanExtra("arabic", true);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this); label.setText("TEST FIXTURE — not Instagram"); page.addView(label);
        Button share = new Button(this); share.setText(arabic ? "مشاركة" : "Share reel"); page.addView(share);
        share.setOnClickListener(v -> {
            LinearLayout sheet = new LinearLayout(this);
            Button copy = new Button(this); copy.setText(arabic ? "نسخ الرابط" : "Copy link"); sheet.addView(copy);
            copy.setOnClickListener(c -> {
                if (!getIntent().getBooleanExtra("stale", false)) getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("fixture", "https://www.instagram.com/reel/AdapterFixture/"));
                setContentView(page);
            });
            setContentView(sheet);
        });
        if (getIntent().getBooleanExtra("ambiguous", false)) { Button second = new Button(this); second.setText(arabic ? "مشاركة" : "Share reel"); page.addView(second); }
        if (getIntent().getBooleanExtra("stale", false)) getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("old", "https://www.instagram.com/reel/OldReel/"));
        setContentView(page);
    }
}
