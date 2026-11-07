#!/usr/bin/env python3
"""
MotoLog Emulator Ride Simulator
Replays recorded GPX fixtures into the active Android emulator via `adb emu geo fix`.
"""

import argparse
import os
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

DEFAULT_FIXTURES_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "test", "resources", "fixtures"
)

def find_gpx_file(name_or_path):
    if os.path.isfile(name_or_path):
        return name_or_path
    candidate = os.path.join(DEFAULT_FIXTURES_DIR, name_or_path)
    if os.path.isfile(candidate):
        return candidate
    candidate_gpx = os.path.join(DEFAULT_FIXTURES_DIR, f"{name_or_path}.gpx")
    if os.path.isfile(candidate_gpx):
        return candidate_gpx
    return None

def parse_gpx(file_path):
    tree = ET.parse(file_path)
    root = tree.getroot()
    # Handle optional XML namespaces
    ns = {"gpx": root.tag.split("}")[0].strip("{")} if "}" in root.tag else {}
    prefix = "gpx:" if ns else ""

    points = []
    for trkpt in root.findall(f".//{prefix}trkpt", ns):
        lat = float(trkpt.attrib["lat"])
        lon = float(trkpt.attrib["lon"])
        ele_elem = trkpt.find(f"{prefix}ele", ns)
        ele = float(ele_elem.text) if ele_elem is not None and ele_elem.text else 0.0

        # Optional speed extension
        speed = None
        for ext in trkpt.findall(f".//{prefix}speed", ns):
            if ext.text:
                try:
                    speed = float(ext.text) * 3.6  # m/s to km/h
                except ValueError:
                    pass

        points.append({"lat": lat, "lon": lon, "ele": ele, "speed_kmh": speed})
    return points

def send_geo_fix(serial, lon, lat, ele):
    cmd = ["adb"]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(["emu", "geo", "fix", str(lon), str(lat), str(ele)])
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return res.returncode == 0

def main():
    parser = argparse.ArgumentParser(description="Simulate GPS movement in Android emulator from a GPX file.")
    parser.add_argument(
        "fixture",
        nargs="?",
        default="city_ride",
        help="GPX file name or fixture name (e.g. city_ride, highway_ride, stop_and_go)"
    )
    parser.add_argument(
        "--speed",
        type=float,
        default=1.0,
        help="Playback speed multiplier (default: 1.0 = real-time, 2.0 = 2x speed, 5.0 = 5x speed)"
    )
    parser.add_argument(
        "--device",
        default=None,
        help="ADB device serial (e.g. emulator-5554)"
    )
    parser.add_argument(
        "--loop",
        action="store_true",
        help="Loop replay indefinitely"
    )

    args = parser.parse_args()

    gpx_path = find_gpx_file(args.fixture)
    if not gpx_path:
        print(f"Error: Fixture '{args.fixture}' not found.")
        print(f"Available fixtures in {DEFAULT_FIXTURES_DIR}:")
        if os.path.isdir(DEFAULT_FIXTURES_DIR):
            for f in sorted(os.listdir(DEFAULT_FIXTURES_DIR)):
                if f.endswith(".gpx"):
                    print(f"  - {f[:-4]}")
        sys.exit(1)

    points = parse_gpx(gpx_path)
    if not points:
        print(f"Error: No track points found in {gpx_path}")
        sys.exit(1)

    interval = 1.0 / max(0.1, args.speed)
    print(f"Replaying: {os.path.basename(gpx_path)} ({len(points)} points)")
    print(f"Speed: {args.speed}x (Interval: {interval:.2f}s per point)")
    print("Press Ctrl+C to stop simulation.\n")

    iteration = 0
    try:
        while True:
            iteration += 1
            if iteration > 1:
                print(f"\n--- Looping (Iteration {iteration}) ---")

            for i, pt in enumerate(points):
                success = send_geo_fix(args.device, pt["lon"], pt["lat"], pt["ele"])
                if not success:
                    print("\nWarning: adb emu geo fix failed. Ensure emulator is running.")
                spd_str = f" | {pt['speed_kmh']:.1f} km/h" if pt["speed_kmh"] is not None else ""
                sys.stdout.write(
                    f"\r[{i+1}/{len(points)}] Lat: {pt['lat']:.5f}, Lon: {pt['lon']:.5f}, Alt: {pt['ele']:.0f}m{spd_str}  "
                )
                sys.stdout.flush()
                time.sleep(interval)

            if not args.loop:
                break
        print("\n\nSimulation completed.")
    except KeyboardInterrupt:
        print("\n\nSimulation stopped by user.")

if __name__ == "__main__":
    main()
