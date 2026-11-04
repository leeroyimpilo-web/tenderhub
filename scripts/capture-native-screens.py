#!/usr/bin/env python3
"""Capture REAL native Android emulator screenshots; no mockups or synthetic data."""
import gzip
import io
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

apk = Path(os.environ.get("TENDERBASE_APK", "native-offline.apk"))
output = Path(os.environ.get("SCREENSHOT_DIR", "native-screenshots"))
output.mkdir(parents=True, exist_ok=True)

def adb(*args, check=True):
    return subprocess.run(["adb", *args], capture_output=True, text=True,
                          check=check, timeout=45)

def ui_dump():
    for _ in range(5):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/window.xml")
            result = adb("shell", "cat", "/sdcard/window.xml")
            start = result.stdout.find("<?xml")
            if start != -1:
                root = ET.fromstring(result.stdout[start:])
                return list(root.iter("node"))
        except Exception as exc:
            print("UiAutomator retry:", exc)
        time.sleep(2)
    raise RuntimeError("Could not read native accessibility hierarchy")

def bounds_center(node):
    b = node.attrib.get("bounds", "")
    v = [int(x) for x in re.findall(r"\d+", b)]
    if len(v) != 4:
        raise ValueError("Missing bounds " + b)
    return (v[0] + v[2]) // 2, (v[1] + v[3]) // 2

def label(node):
    a = node.attrib
    return " ".join((a.get("text", ""), a.get("content-desc", ""),
                     a.get("resource-id", "")))

def wait_for(needle, timeout=110):
    start = time.time()
    while time.time() - start < timeout:
        nodes = ui_dump()
        if any(needle.lower() in label(n).lower() for n in nodes):
            return nodes
        time.sleep(2)
    raise RuntimeError("Screen did not expose: " + needle)

def tap_label(needle, timeout=25):
    start = time.time()
    while time.time() - start < timeout:
        nodes = ui_dump()
        matching = [n for n in nodes if needle.lower() in label(n).lower()]
        if matching:
            n = matching[0]
            x, y = bounds_center(n)
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(2)
            return
        time.sleep(1)
    raise RuntimeError("No accessible UI action matching: " + needle)

def assert_app_alive(stage):
    process = adb("shell", "pidof", "za.co.tenderbase.nativeui", check=False)
    if not process.stdout.strip():
        crash = adb("logcat", "-d", "-t", "300", "-s", "AndroidRuntime:E", check=False)
        raise RuntimeError(f"TenderBase process stopped during {stage}:\\n{crash.stdout[-7000:]}")
    crash = adb("logcat", "-d", "-t", "250", "-s", "AndroidRuntime:E", check=False)
    if "FATAL EXCEPTION" in crash.stdout and "za.co.tenderbase.nativeui" in crash.stdout:
        raise RuntimeError(f"AndroidRuntime crash during {stage}:\\n{crash.stdout[-7000:]}")

def capture(filename):
    assert_app_alive(filename)
    target = output / filename
    with target.open("wb") as f:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=f, check=True, timeout=30)
    data = target.read_bytes()
    if not data.startswith(b"\x89PNG") or len(data) < 10000:
        raise RuntimeError("Invalid real emulator screenshot " + filename)
    print("Captured", filename, len(data), "bytes")

def title_from_real_snapshot(nodes):
    with zipfile.ZipFile(apk) as z:
        lines = gzip.decompress(z.read("assets/bootstrap.tenderpack")).decode("utf-8").splitlines()
        rows = [json.loads(s) for s in lines[1:] if s.strip()]
    if len(rows) < 1000:
        raise RuntimeError("Offline APK is missing verified official data")
    visible = [n for n in nodes if n.attrib.get("text")]
    for node in visible:
        value = node.attrib.get("text", "")
        if len(value) < 15:
            continue
        if any(r["title"].lower().startswith(value[:18].lower()) for r in rows):
            return node
    raise RuntimeError("Could not locate an actual rendered tender title")

print("Installing", apk)
subprocess.run(["adb", "install", "-r", str(apk)], check=True, timeout=180)
adb("shell", "svc", "wifi", "disable")
adb("shell", "svc", "data", "disable")
adb("shell", "am", "force-stop", "za.co.tenderbase.nativeui")
adb("shell", "am", "start", "-n", "za.co.tenderbase.nativeui/.MainActivity")
nodes = wait_for("opportunities")
# The APK has no INTERNET permission; record both data count and offline first launch.
capture("01-discover.png")
tap_label("Refine search")
wait_for("Choose what matters")
capture("05-refine-search.png")
try:
    tap_label("Close filters")
except RuntimeError:
    adb("shell", "input", "keyevent", "4")
time.sleep(2)
nodes = ui_dump()
tender_title = title_from_real_snapshot(nodes)
x, y = bounds_center(tender_title)
adb("shell", "input", "tap", str(x), str(y))
wait_for("Overview")
capture("02-review.png")
tap_label("Track bid")
wait_for("From shortlist to submission")
try:
    tap_label("Edit bid", timeout=6)
except RuntimeError:
    print("Bid editor expansion not located; capturing collapsed tracking list")
capture("03-my-bids.png")
tap_label("Deadlines")
wait_for("Keep the next date")
capture("04-deadlines.png")
tap_label("Insights")
wait_for("Market overview")
capture("06-insights.png")
print("All six native Android screens captured without network access.")
