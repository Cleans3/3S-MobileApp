#!/usr/bin/env python3
"""Unit and integration self-tests for scale_probe.py and diffguard.py (no emulator needed)."""
from __future__ import annotations

import pathlib
import sys
import tempfile
import unittest

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import diffguard  # noqa: E402
import scale_probe  # noqa: E402


class TestScaleProbe(unittest.TestCase):
    def test_toy_data_detected_and_amplified(self):
        with tempfile.TemporaryDirectory() as td:
            root = pathlib.Path(td) / "app"
            kt = root / "app/src/main/java/com/demo/Books.kt"
            kt.parent.mkdir(parents=True)
            kt.write_text(
                """package com.demo
data class Book(val id: Int, val title: String, val author: String)
val SampleBooks = listOf(
    Book(1, "Alpha", "A"),
    Book(2, "Beta", "B"),
    Book(3, "Gamma", "C"),
)
fun search(query: String) = SampleBooks.filter { it.title.contains(query, ignoreCase = true) }
"""
            )
            audit = scale_probe.audit_project(root)
            self.assertEqual(audit["verdict"], "FAIL")
            self.assertTrue(audit["toy_data_illusion"])
            self.assertEqual(audit["toy_list_count"], 3)
            self.assertIn("SCALE-TOY", audit["hazard_codes"])
            self.assertIn("SCALE-SEARCH", audit["hazard_codes"])

            out = pathlib.Path(td) / "amplified"
            amp = scale_probe.amplify_project(root, out, target_n=100_000)
            self.assertEqual(amp["status"], "AMPLIFIED")
            self.assertTrue(amp["brackets_balanced"])
            self.assertEqual(amp["original_n"], 3)
            self.assertEqual(amp["target_n"], 100_000)
            rewritten = (out / "app/src/main/java/com/demo/Books.kt").read_text()
            self.assertIn("List(100000)", rewritten)
            self.assertIn("id = _idx + 1", rewritten)
            self.assertTrue((out / "scale-probe-gate/ScaleBudgetGate.kt").exists())

    def test_scalable_indexed_app_passes(self):
        with tempfile.TemporaryDirectory() as td:
            root = pathlib.Path(td) / "good"
            kt = root / "app/src/main/java/com/demo/Catalog.kt"
            kt.parent.mkdir(parents=True)
            kt.write_text(
                """package com.demo
import androidx.room.Fts4
@Fts4
data class BookFts(val title: String)
@Database(entities = [BookFts::class], version = 1)
abstract class AppDb
fun generate() = List(5000) { i -> BookFts("Title $i") }
"""
            )
            audit = scale_probe.audit_project(root)
            self.assertEqual(audit["verdict"], "PASS")
            self.assertFalse(audit["toy_data_illusion"])

    def test_review_count_does_not_mask_toy_data_and_gen_param_detected(self):
        self.assertEqual(scale_probe.detect_large_generator("Book(title = \"X\", reviewCount = 5400)"), 0)
        self.assertEqual(scale_probe.detect_large_generator("fun generate(count: Int = 5000): List<Book> = List(count) {}"), 5000)


class TestDiffGuard(unittest.TestCase):
    def test_blocks_keystore_auth_removal(self):
        patch = """--- a/app/src/main/kotlin/TokenVault.kt
+++ b/app/src/main/kotlin/TokenVault.kt
@@ -12,7 +12,6 @@
             .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
             .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
-            .setUserAuthenticationRequired(true)
-            .setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG)
             .build()
"""
        res = diffguard.check_diff(patch)
        self.assertEqual(res["verdict"], "BLOCK")
        self.assertEqual(res["findings"][0]["rule_id"], "DG-X01")

    def test_allows_preserving_keystore_auth_with_biometric_prompt(self):
        patch = """--- a/app/src/main/kotlin/TokenVault.kt
+++ b/app/src/main/kotlin/TokenVault.kt
@@ -12,5 +12,7 @@
             .setUserAuthenticationRequired(true)
             .setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG)
             .build()
+        biometricPrompt.authenticate(promptInfo)
"""
        res = diffguard.check_diff(patch)
        self.assertEqual(res["verdict"], "PASS")

    def test_blocks_cleartext_in_main_allows_debug_override(self):
        bad = """--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@ -3,3 +3,4 @@
     <application
+        android:usesCleartextTraffic="true"
         android:allowBackup="false">
"""
        self.assertEqual(diffguard.check_diff(bad)["verdict"], "BLOCK")

        good_dbg = """--- a/app/src/debug/AndroidManifest.xml
+++ b/app/src/debug/AndroidManifest.xml
@@ -3,3 +3,4 @@
     <application
+        android:usesCleartextTraffic="true">
"""
        self.assertEqual(diffguard.check_diff(good_dbg)["verdict"], "PASS")

    def test_blocks_fail_open_crypto_and_release_debug_signing(self):
        with tempfile.TemporaryDirectory() as td:
            root = pathlib.Path(td)
            (root / "app/src/main/java").mkdir(parents=True)
            (root / "app/src/main/java/Session.kt").write_text(
                """class Session(ctx: Context) {
    val prefs = try {
        EncryptedSharedPreferences.create(ctx, "s", masterKey, k, v)
    } catch (e: Exception) {
        ctx.getSharedPreferences("s", Context.MODE_PRIVATE)
    }
}"""
            )
            (root / "app/build.gradle.kts").write_text(
                """android {
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
        }
        benchmark {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}"""
            )
            res = diffguard.scan_project(root)
            self.assertEqual(res["verdict"], "BLOCK")
            self.assertIn("DG-FAILOPEN", res["rule_ids"])
            self.assertIn("DG-DEBUGSIGN", res["rule_ids"])

    def test_benchmark_debug_signing_not_flagged_when_release_clean(self):
        with tempfile.TemporaryDirectory() as td:
            root = pathlib.Path(td)
            (root / "app").mkdir(parents=True)
            (root / "app/build.gradle.kts").write_text(
                """android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
        benchmark {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}"""
            )
            res = diffguard.scan_project(root)
            self.assertNotIn("DG-DEBUGSIGN", res["rule_ids"])


if __name__ == "__main__":
    unittest.main()
