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
        String s = v == null ? "" : v.toString().trim();
        return s.equals("نسخ الرابط") || s.equalsIgnoreCase("Copy link");
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
