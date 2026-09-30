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
            info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            ui.setServiceInfo(info);
            if (arguments.getString("operation", "").startsWith("device:")) {
                inspectDevice(ui, report);
                result.putString("report", report.toString()); finish(0, result); return;
            }
            AccessibilityNodeInfo root = ui.getRootInActiveWindow();
            if (root == null || !"com.instagram.android".contentEquals(root.getPackageName())) {
                report.append("Instagram is not the active window; no tree read.");
            } else {
                if (arguments.getString("operation", "").endsWith("Copy") && controls(root, true).isEmpty()) {
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
                if ("compareCopy".equals(arguments.getString("operation"))) {
                    getContext().getSharedPreferences("copy-probe", 0).edit().putLong("after", System.currentTimeMillis()).commit();
                    List<AccessibilityNodeInfo> images = new ArrayList<>();
                    for (AccessibilityNodeInfo n : matches) if ("com.instagram.android:id/button".equals(n.getViewIdResourceName())) images.add(n);
                    if (images.size() == 1) {
                        AccessibilityNodeInfo n = images.get(0);
                        report.append("legacy_image_ACTION_CLICK=").append(n.performAction(AccessibilityNodeInfo.ACTION_CLICK)).append('\n');
                        for (int i = 0; n != null && i < 8; i++, n = n.getParent()) {
                            if ("com.instagram.android:id/direct_external_reshare_row".equals(n.getViewIdResourceName())) break;
                            boolean supported = false;
                            for (AccessibilityNodeInfo.AccessibilityAction a : n.getActionList()) if (a.getId() == AccessibilityNodeInfo.ACTION_CLICK) supported = true;
                            if (n.refresh() && n.isVisibleToUser() && n.isEnabled() && supported) {
                                report.append("supported_parent=").append(i).append(" ACTION_CLICK=").append(n.performAction(AccessibilityNodeInfo.ACTION_CLICK)).append('\n'); break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { report.append("inspection_failed=").append(e.getClass().getSimpleName()); }
        result.putString("report", report.toString()); finish(0, result);
    }

    // Explicit one-shot owner-device control only. Never reads login WebViews, captions,
    // contacts, clipboard or private app files; Instagram nodes expose structural metadata.
    private void inspectDevice(UiAutomation ui, StringBuilder report) {
        String operation = arguments.getString("operation", "device:inspect");
        String app = "io.github.ahmed9461.tapsave";
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        for (android.view.accessibility.AccessibilityWindowInfo window : ui.getWindows()) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            String pkg = String.valueOf(root.getPackageName());
            if (!pkg.equals(app) && !pkg.equals("com.instagram.android")) continue;
            report.append("window package=").append(pkg).append(" type=").append(window.getType())
                .append(" focused=").append(window.isFocused()).append('\n');
            ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
            int count = 0;
            while (!queue.isEmpty() && count++ < 500) {
                AccessibilityNodeInfo n = queue.removeFirst();
                if (!n.isVisibleToUser() || "android.webkit.WebView".contentEquals(n.getClassName())) continue;
                boolean own = app.contentEquals(n.getPackageName());
                String id = String.valueOf(n.getViewIdResourceName());
                String text = String.valueOf(n.getText());
                String desc = String.valueOf(n.getContentDescription());
                boolean click = n.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
                boolean scroll = n.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                if (operation.equals("device:overlay") && own && window.getType() == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM && click) matches.add(n);
                if (operation.equals("device:click") && own && (arguments.getString("label", "").equals(text) || arguments.getString("label", "").equals(desc))) matches.add(n);
                if (operation.equals("device:scroll") && arguments.getString("id", "").equals(id) && scroll) matches.add(n);
                if (operation.equals("device:scrollApp") && own && scroll) matches.add(n);
                if (operation.equals("device:toggleApp") && own && n.isCheckable()) matches.add(n);
                if (operation.equals("device:inspect")) {
                    if (own || click || scroll || id.contains("clips") || copy(n)) {
                        report.append(properties(n));
                        if (own) report.append(" text=").append(text).append(" description=").append(desc);
                        report.append('\n');
                    }
                }
                for (int i = 0; i < n.getChildCount() && i < 500; i++) {
                    AccessibilityNodeInfo child = n.getChild(i); if (child != null) queue.addLast(child);
                }
            }
        }
        if (!operation.equals("device:inspect")) {
            report.append("matches=").append(matches.size()).append('\n');
            if (matches.size() == 1) {
                AccessibilityNodeInfo n = matches.get(0);
                int action = operation.startsWith("device:scroll") ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_CLICK;
                if (operation.equals("device:scroll") && "page".equals(arguments.getString("mode"))) action = AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN.getId();
                final int selectedAction = action;
                for (int depth = 0; n != null && depth < 8; depth++, n = n.getParent()) {
                    if (!n.refresh() || !n.isVisibleToUser() || !n.isEnabled()) continue;
                    if (n.getActionList().stream().anyMatch(a -> a.getId() == selectedAction)) {
                        report.append("parent=").append(depth).append(" action=").append(action).append(" accepted=").append(n.performAction(action)).append('\n'); break;
                    }
                }
            }
        }
    }
}
