package io.github.ahmed9461.tapsave.probe;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Explicit ADB-only inspection. No Activity, idle monitoring, networking, files or raw screen text. */
public class Inspector extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments = args; start(); }
    private boolean copy(AccessibilityNodeInfo n) {
        return label(n.getText()) || label(n.getContentDescription());
    }
    private boolean label(CharSequence v) {
        String s = v == null ? "" : v.toString().replaceAll("[\\u200e\\u200f\\u202a-\\u202e\\u2066-\\u2069]", "").trim();
        return s.equals("نسخ الرابط") || s.equalsIgnoreCase("Copy link");
    }
    private List<AccessibilityNodeInfo> controls(AccessibilityNodeInfo root, boolean isCopy) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
        List<AccessibilityNodeInfo> found = new ArrayList<>(); int count = 0;
        while (!queue.isEmpty() && count++ < 400) {
            AccessibilityNodeInfo n = queue.removeFirst();
            if (!"com.instagram.android".contentEquals(n.getPackageName()) || !n.isVisibleToUser()) continue;
            String text = String.valueOf(n.getText()); String desc = String.valueOf(n.getContentDescription());
            String id = String.valueOf(n.getViewIdResourceName());
            boolean share = text.equals("مشاركة") || desc.equals("مشاركة") || text.equalsIgnoreCase("Share") || desc.equalsIgnoreCase("Share") ||
                ((text.equals("إرسال") || desc.equals("إرسال")) && (id.contains("share") || id.contains("clips")));
            if (isCopy ? copy(n) : share) found.add(n);
            for (int i = 0; i < Math.min(n.getChildCount(), 400); i++) { AccessibilityNodeInfo c = n.getChild(i); if (c != null) queue.add(c); }
        }
        return found;
    }
    private String properties(AccessibilityNodeInfo n) {
        List<Integer> actions = new ArrayList<>();
        for (AccessibilityNodeInfo.AccessibilityAction a : n.getActionList()) actions.add(a.getId());
        String id = n.getViewIdResourceName();
        return "id=" + id + " class=" + n.getClassName() + " visible=" + n.isVisibleToUser() + " clickable=" + n.isClickable() + " enabled=" + n.isEnabled() + " actions=" + actions + " copyLabel=" + copy(n);
    }
    @Override public void onStart() {
        Bundle result = new Bundle(); StringBuilder report = new StringBuilder();
        try {
            UiAutomation ui = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            android.accessibilityservice.AccessibilityServiceInfo info = ui.getServiceInfo();
            info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            ui.setServiceInfo(info);
            AccessibilityNodeInfo root = ui.getRootInActiveWindow();
            if (root == null || !"com.instagram.android".contentEquals(root.getPackageName())) {
                report.append("Instagram is not the active window; no tree read.");
            } else {
                if ("legacyCopy".equals(arguments.getString("operation")) && controls(root, true).isEmpty()) {
                    List<AccessibilityNodeInfo> shares = controls(root, false);
                    report.append("share_matches=").append(shares.size()).append('\n');
                    if (shares.size() == 1) {
                        AccessibilityNodeInfo n = shares.get(0);
                        for (int i = 0; n != null && i < 4; i++, n = n.getParent()) {
                            if (n.isClickable()) { report.append("share_click=").append(n.performAction(AccessibilityNodeInfo.ACTION_CLICK)).append('\n'); break; }
                        }
                        long end = android.os.SystemClock.uptimeMillis() + 5000;
                        do { android.os.SystemClock.sleep(100); root = ui.getRootInActiveWindow(); }
                        while (root != null && controls(root, true).isEmpty() && android.os.SystemClock.uptimeMillis() < end);
                    }
                }
                if (root == null || !"com.instagram.android".contentEquals(root.getPackageName())) throw new IllegalStateException();
                ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
                List<AccessibilityNodeInfo> matches = new ArrayList<>(); int count = 0;
                while (!queue.isEmpty() && count++ < 400) {
                    AccessibilityNodeInfo n = queue.removeFirst();
                    if (!"com.instagram.android".contentEquals(n.getPackageName()) || !n.isVisibleToUser()) continue;
                    if (copy(n)) matches.add(n);
                    for (int i = 0; i < Math.min(n.getChildCount(), 400); i++) { AccessibilityNodeInfo c = n.getChild(i); if (c != null) queue.add(c); }
                }
                report.append("nodes=").append(count).append(" copy_matches=").append(matches.size()).append('\n');
                for (AccessibilityNodeInfo match : matches) {
                    AccessibilityNodeInfo n = match;
                    for (int depth = 0; n != null && depth < 8; depth++, n = n.getParent()) report.append("parent=").append(depth).append(' ').append(properties(n)).append('\n');
                }
                if ("legacyCopy".equals(arguments.getString("operation")) && matches.size() == 1) {
                    AccessibilityNodeInfo n = matches.get(0);
                    for (int i = 0; n != null && i < 4; i++, n = n.getParent()) {
                        if (n.isVisibleToUser() && n.isEnabled() && n.isClickable()) {
                            report.append("legacy_ACTION_CLICK=").append(n.performAction(AccessibilityNodeInfo.ACTION_CLICK)).append('\n'); break;
                        }
                    }
                }
            }
        } catch (Exception e) { report.append("inspection_failed=").append(e.getClass().getSimpleName()); }
        result.putString("report", report.toString()); finish(0, result);
    }
}
