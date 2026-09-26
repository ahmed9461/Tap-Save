"""Deterministic device gate plus an explicitly requested, separately reported live Reel probe."""
import os
from pathlib import Path
import re
import shutil
import subprocess

base = ["./gradlew", "--no-daemon", ":app:connectedDebugAndroidTest"]
subprocess.run(base + ["-Pandroid.testInstrumentationRunnerArguments.notAnnotation=io.github.ahmed9461.tapsave.LiveNetwork"], check=True)
subprocess.run(["python3", "scripts/summarize_checks.py"], check=True)
live = os.environ.get("LIVE_REEL", "")
if live and os.environ.get("API_LEVEL") == "36":
    if not re.fullmatch(r"https://www\.instagram\.com/reel/[A-Za-z0-9_-]{1,64}/", live):
        raise SystemExit("Live probe requires a canonical public Instagram Reel URL")
    reports = Path("app/build/reports")
    shutil.copytree("app/build/outputs/androidTest-results", reports / "deterministic-device-results", dirs_exist_ok=True)
    try:
        subprocess.run(base + [
            "-Pandroid.testInstrumentationRunnerArguments.class=io.github.ahmed9461.tapsave.LiveReelSaveTest",
            f"-Pandroid.testInstrumentationRunnerArguments.liveReel={live}",
        ], check=True)
    finally:
        shutil.copytree("app/build/outputs/androidTest-results", reports / "live-device-results", dirs_exist_ok=True)
