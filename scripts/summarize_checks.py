"""Summarize actual Gradle XML results and the APK; fail on empty or failed suites."""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

build = Path("app/build")
summary = {}
for kind, folder in (
    ("unit", build / "test-results/testDebugUnitTest"),
    ("instrumentation", build / "outputs/androidTest-results/connected"),
):
    totals = {name: 0 for name in ("tests", "failures", "errors", "skipped")}
    for report in folder.rglob("TEST*.xml"):
        root = ET.parse(report).getroot()
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            for case in suite.findall("testcase"):
                for failure in (*case.findall("failure"), *case.findall("error")):
                    print(f"{kind}: {case.get('classname')}.{case.get('name')}: {failure.text}")
            for name in totals:
                totals[name] += int(suite.get(name, "0"))
    if totals["tests"] == 0 or totals["failures"] or totals["errors"] or totals["skipped"]:
        raise SystemExit(f"{kind} gate incomplete: {totals}")
    summary[kind] = totals

apk = build / "outputs/apk/debug/app-debug.apk"
summary["apk"] = {"bytes": apk.stat().st_size, "sha256": hashlib.sha256(apk.read_bytes()).hexdigest()}
graph = (build / "reports/runtime-dependencies.txt").read_text()
summary["kotlin_runtime"] = sorted({line.strip() for line in graph.splitlines() if "org.jetbrains.kotlin:kotlin-stdlib:" in line})
result = json.dumps(summary, indent=2)
(build / "reports/verification-summary.json").write_text(result + "\n")
print(result)
