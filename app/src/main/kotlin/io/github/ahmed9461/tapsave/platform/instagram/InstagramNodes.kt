package io.github.ahmed9461.tapsave.platform.instagram

import android.view.accessibility.AccessibilityNodeInfo

/** Select a semantic control, then try only that control's actionable ancestors. No gestures. */
object InstagramNodes {
    data class Match(val node: AccessibilityNodeInfo, val score: Int, val path: String)
    data class Scan(val matches: List<Match>, val limited: Boolean) {
        fun unique(): Match? {
            if (limited) return null
            val top = matches.filter { it.score == matches.maxOfOrNull(Match::score) }
            // A repeated child label and its described parent can name the same control.
            return top.firstOrNull()?.takeIf { first -> top.all { sameControl(first.node, it.node) } }
        }
    }
    fun scan(root: AccessibilityNodeInfo?, action: InstagramControls.Action, trace: AcquisitionDiagnostics? = null): Scan {
        if (root?.packageName?.toString() != InstagramApp.PACKAGE_NAME) return Scan(emptyList(), false)
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, String>>()
        queue.add(root to "0")
        val matches = mutableListOf<Match>()
        var visited = 0
        while (queue.isNotEmpty()) {
            val (node, path) = queue.removeFirst()
            if (++visited > 400 || path.count { it == '/' } > 40) return Scan(matches, true)
            if (node.packageName?.toString() != InstagramApp.PACKAGE_NAME || !node.isVisibleToUser) continue
            val id = node.viewIdResourceName.orEmpty()
            val score = when {
                InstagramControls.idMatches(action, id) -> 100
                node.actionList.any { InstagramControls.matches(action, it.label?.toString().orEmpty(), id) } -> 90
                InstagramControls.matches(action, node.contentDescription?.toString().orEmpty(), id) -> 80
                InstagramControls.matches(action, node.text?.toString().orEmpty(), id) -> 60
                else -> 0
            }
            if (score > 0) {
                matches += Match(node, score, path)
                trace?.add("MATCH $action score=$score path=$path ${properties(node)}")
            }
            for (i in 0 until node.childCount.coerceAtMost(400)) node.getChild(i)?.let { queue.add(it to "$path/$i") }
        }
        trace?.add("SCAN $action nodes=$visited matches=${matches.size} window=${root.windowId}")
        return Scan(matches, false)
    }
    fun click(match: Match, action: InstagramControls.Action, trace: AcquisitionDiagnostics?): Boolean {
        var node: AccessibilityNodeInfo? = match.node
        repeat(8) { depth ->
            val current = node ?: return false
            if (current.packageName?.toString() != InstagramApp.PACKAGE_NAME) return false
            // Observed on Instagram 448 Arabic: a decorative ImageView is clickable
            // but advertises no click action. Its containing tile handles ACTION_CLICK.
            // Never climb beyond that tile into the whole share row or bottom sheet.
            if (action == InstagramControls.Action.COPY_LINK && current.viewIdResourceName in setOf(
                    "com.instagram.android:id/direct_external_reshare_row",
                    "com.instagram.android:id/direct_external_share_container_view",
                    "com.instagram.android:id/layout_container_bottom_sheet",
                )) return false
            val fresh = current.refresh()
            trace?.add("CANDIDATE $action parent=$depth fresh=$fresh ${properties(current)}")
            if (fresh && current.isVisibleToUser && current.isEnabled) {
                val actions = current.actionList
                val semantic = actions.firstOrNull { InstagramControls.matches(action, it.label?.toString().orEmpty(), current.viewIdResourceName.orEmpty()) }
                val click = actions.firstOrNull { it.id == AccessibilityNodeInfo.ACTION_CLICK }
                val selected = semantic ?: click
                if (selected != null) {
                    val accepted = current.performAction(selected.id)
                    trace?.add("ACTION $action parent=$depth id=${selected.id} accepted=$accepted")
                    if (accepted) return true
                }
            }
            // Some Instagram wrappers claim clickable but reject ACTION_CLICK. Do not
            // stop there: the containing action tile may be the real handler.
            node = current.parent
        }
        return false
    }
    private fun sameControl(a: AccessibilityNodeInfo, b: AccessibilityNodeInfo): Boolean {
        fun contains(start: AccessibilityNodeInfo, other: AccessibilityNodeInfo): Boolean {
            var node: AccessibilityNodeInfo? = start
            repeat(8) { if (node == other) return true; node = node?.parent }
            return false
        }
        return contains(a, b) || contains(b, a)
    }
    private fun properties(node: AccessibilityNodeInfo): String {
        val id = node.viewIdResourceName.orEmpty().take(120).replace(Regex("[^A-Za-z0-9_:. /-]"), "?")
        return "id=$id class=${node.className} visible=${node.isVisibleToUser} enabled=${node.isEnabled} clickable=${node.isClickable} actions=${node.actionList.map { it.id }} window=${node.windowId}"
    }
}
