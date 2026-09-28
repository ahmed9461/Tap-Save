"""Deterministic device gate plus an explicitly requested, separately reported live Reel probe."""
import os
from pathlib import Path
import re
import shutil
import subprocess

# Do not replace an owner's Instagram installation. This package exists only on an empty emulator.
if subprocess.check_output(["adb", "shell", "getprop", "ro.kernel.qemu"], text=True).strip() != "1":
    raise SystemExit("Device tests require a disposable emulator")
# Keep a UI test device awake; owner devices are never targeted by this runner.
subprocess.run(["adb", "shell", "settings", "put", "global", "stay_on_while_plugged_in", "3"], check=True)
subprocess.run(["adb", "shell", "settings", "put", "system", "screen_off_timeout", "1800000"], check=True)
subprocess.run(["adb", "shell", "input", "keyevent", "KEYCODE_WAKEUP"], check=True)
subprocess.run(["adb", "shell", "wm", "dismiss-keyguard"], check=True)
existing = subprocess.run(["adb", "shell", "pm", "path", "com.instagram.android"], text=True, capture_output=True)
if existing.returncode not in (0, 1):
    raise SystemExit("Could not verify the emulator package inventory")
if "package:" in existing.stdout:
    raise SystemExit("Refusing to replace an existing Instagram package")
subprocess.run(["./gradlew", "--no-daemon", ":instagram-fixture:assembleDebug"], check=True)
subprocess.run(["adb", "install", "instagram-fixture/build/outputs/apk/debug/instagram-fixture-debug.apk"], check=True)
base = ["./gradlew", "--no-daemon", ":app:connectedDebugAndroidTest"]
gate = subprocess.run(base + ["-Pandroid.testInstrumentationRunnerArguments.notAnnotation=io.github.ahmed9461.tapsave.LiveNetwork"])
screens = subprocess.run(["adb", "pull", "/sdcard/Android/data/io.github.ahmed9461.tapsave/files/ui-checks", "app/build/reports/ui-checks"], capture_output=True, text=True)
gate.check_returncode()
screens.check_returncode()
subprocess.run(["adb", "uninstall", "com.instagram.android"], check=True)
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
