"""Regression tests for unnumbered beta and browser version ordering.

Runs locally on Python stdlib only. Does not build or publish artifacts.
"""

import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location(
    "tempo_release_bundle", ROOT / "release-bundle.py"
)
assert SPEC and SPEC.loader
BUNDLE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUNDLE)


class ReleaseBundleMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.old_cwd = Path.cwd()
        os.chdir(self.temp.name)
        Path("app").mkdir()
        Path("docs/releases").mkdir(parents=True)
        Path("browser-extension").mkdir()

    def tearDown(self):
        os.chdir(self.old_cwd)
        self.temp.cleanup()

    def write_release(
        self, display="4.10.25-beta", internal="4.10.25.0",
        package=None, version_name=None,
    ):
        Path("app/build.gradle.kts").write_text(
            'val appVersionCode = 4821\n'
            f'val appVersionName = "{display}"\n'
        )
        manifest = {
            "manifest_version": 3,
            "version": internal,
            "version_name": version_name or display,
            "background": {"service_worker": "background/service-worker.js"},
            "action": {"default_popup": "popup/popup.html"},
            "icons": {"48": "icons/48.png"},
            "content_scripts": [{"js": ["content/probe.js"]}],
        }
        Path("browser-extension/manifest.json").write_text(json.dumps(manifest))
        pkg_ver = package or display
        Path("browser-extension/package.json").write_text(
            json.dumps({"version": pkg_ver})
        )
        Path("browser-extension/package-lock.json").write_text(
            json.dumps({"version": pkg_ver, "packages": {"": {"version": pkg_ver}}})
        )
        Path(f"docs/releases/v{display}.md").write_text("# Release notes\n")
        return manifest

    def test_unnumbered_beta_is_accepted(self):
        self.write_release()
        self.assertEqual(
            ("4.10.25-beta", "4821", "4.10.25.0"),
            BUNDLE.get_metadata("beta"),
        )

    def test_stable_follows_beta_in_browser_version_order(self):
        self.write_release(display="4.10.25", internal="4.10.25.1")
        self.assertEqual(
            ("4.10.25", "4821", "4.10.25.1"),
            BUNDLE.get_metadata("stable"),
        )
        self.assertGreater((4, 10, 25, 1), (4, 10, 25, 0))

    def test_numbered_beta_is_rejected_for_new_releases(self):
        self.write_release(display="4.10.25-beta.1", internal="4.10.25.0")
        with self.assertRaisesRegex(ValueError, "Invalid beta release version"):
            BUNDLE.get_metadata("beta")

    def test_mismatched_manifest_or_package_is_rejected(self):
        self.write_release(internal="4.10.25.1")
        with self.assertRaisesRegex(ValueError, "Manifest"):
            BUNDLE.get_metadata("beta")
        self.write_release(package="4.10.25.1")
        with self.assertRaisesRegex(ValueError, "package and lockfile"):
            BUNDLE.get_metadata("beta")

    def test_browser_manifest_display_channel(self):
        manifest = self.write_release()
        with tempfile.TemporaryDirectory() as directory:
            filename = Path(directory) / "chrome.zip"
            with ZipFile(filename, "w") as archive:
                for name in [
                    "background/service-worker.js", "popup/popup.html",
                    "icons/48.png", "content/probe.js",
                ]:
                    archive.writestr(name, "")
                archive.writestr("manifest.json", json.dumps(manifest))
            BUNDLE.validate_extension(
                filename, "chrome", "4.10.25.0", "4.10.25-beta"
            )
            firefox = dict(manifest)
            firefox.pop("version_name")
            firefox["background"] = {"scripts": ["background/service-worker.js"]}
            firefox["browser_specific_settings"] = {"gecko": {"id": "test@example.org"}}
            with ZipFile(filename, "w") as archive:
                for name in [
                    "background/service-worker.js", "popup/popup.html",
                    "icons/48.png", "content/probe.js",
                ]:
                    archive.writestr(name, "")
                archive.writestr("manifest.json", json.dumps(firefox))
            BUNDLE.validate_extension(
                filename, "firefox", "4.10.25.0", "4.10.25-beta"
            )


if __name__ == "__main__":
    unittest.main()
