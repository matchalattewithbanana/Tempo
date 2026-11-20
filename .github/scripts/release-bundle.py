"""Validate synchronized release metadata and stage the three distributable assets."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile
from tempo_versioning import normalize_version

EXTENSION = Path("browser-extension")
BUNDLE = Path("release-bundle")


def get_metadata(channel):
    gradle = Path("app/build.gradle.kts").read_text()
    version_match = re.search(r'^val appVersionName = "([^"]+)"$', gradle, re.M)
    code_match = re.search(r"^val appVersionCode = ([1-9][0-9]*)$", gradle, re.M)
    if not version_match or not code_match:
        raise ValueError("Missing Android version name or code")
    version = version_match[1]
    component = r"(0|[1-9][0-9]*)"
    pattern = rf"{component}\.{component}\.{component}"
    if channel == "beta":
        pattern += r"-beta"
    matched = re.fullmatch(pattern, version)
    if not matched or any(int(part) > 65535 for part in matched.groups()):
        raise ValueError(f"Invalid {channel} release version: {version}")
    major, minor, patch = (int(x) for x in matched.groups()[:3])
    if normalize_version(major, minor, patch) != (major, minor, patch):
        correct = ".".join(map(str, normalize_version(major, minor, patch)))
        raise ValueError(
            f"Unnormalized {channel} release version {version}: use {correct} "
            "(patch 0..99, minor 0..10)"
        )

    # Browser manifest version must be numeric; .0 is beta and .1 stable.
    # This internal suffix is not a numbered beta release.
    browser_version = f"{major}.{minor}.{patch}." + ("0" if channel == "beta" else "1")
    manifest = json.loads((EXTENSION / "manifest.json").read_text())
    package = json.loads((EXTENSION / "package.json").read_text())
    lock = json.loads((EXTENSION / "package-lock.json").read_text())
    if manifest.get("version") != browser_version or manifest.get("version_name") != version:
        raise ValueError(f"Manifest must use version={browser_version}, version_name={version}")
    if [package.get("version"), lock.get("version"), lock.get("packages", {}).get("", {}).get("version")] != [version] * 3:
        raise ValueError(f"Browser package and lockfile must use {version}")
    if not Path(f"docs/releases/v{version}.md").is_file():
        raise ValueError(f"Missing release notes for {version}")
    return version, code_match[1], browser_version


def validate_extension(path, target, version, display_version):
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError(f"Corrupt ZIP: {path}")
        manifest = json.loads(archive.read("manifest.json"))
        if manifest["version"] != version or manifest["manifest_version"] != 3:
            raise ValueError(f"Incorrect manifest version in {path}")
        if target == "chrome" and manifest.get("version_name") != display_version:
            raise ValueError(f"Incorrect Chromium display version in {path}")
        if target == "firefox" and "version_name" in manifest:
            raise ValueError(f"Unexpected version_name in Firefox manifest: {path}")
        background = manifest["background"]
        if target == "chrome":
            scripts = [background["service_worker"]]
            if "scripts" in background:
                raise ValueError("Chromium bundle contains Firefox background configuration")
        else:
            scripts = list(background["scripts"])
            if "service_worker" in background or "gecko" not in manifest["browser_specific_settings"]:
                raise ValueError("Firefox bundle contains the wrong background configuration")
        files = scripts + [manifest["action"]["default_popup"]]
        files += list(manifest["icons"].values())
        for script in manifest["content_scripts"]:
            files += script["js"]
        for name in files:
            archive.getinfo(name)


def stage(channel, apk):
    version, _, browser_version = get_metadata(channel)
    files = [(apk, f"Tempo-{version}.apk")]
    for target, browser in (("chrome", "Chromium"), ("firefox", "Firefox")):
        path = EXTENSION / "dist" / f"tempo-stats-{target}.zip"
        validate_extension(path, target, browser_version, version)
        files.append((path, f"Tempo-Stats-{browser}-{version}.zip"))
    BUNDLE.mkdir(exist_ok=False)
    checksums = []
    for source, filename in files:
        target = BUNDLE / filename
        shutil.copyfile(source, target)
        with target.open("rb") as payload:
            checksums.append(f"{hashlib.file_digest(payload, 'sha256').hexdigest()}  {filename}\n")
    (BUNDLE / "SHA256SUMS.txt").write_text("".join(checksums))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("channel", choices=("stable", "beta"))
    parser.add_argument("--stage-apk", type=Path)
    args = parser.parse_args()
    if args.stage_apk:
        stage(args.channel, args.stage_apk)
    else:
        version, version_code, browser_version = get_metadata(args.channel)
        print(f"version={version}\nversion_code={version_code}\nbrowser_version={browser_version}")


if __name__ == "__main__":
    main()
