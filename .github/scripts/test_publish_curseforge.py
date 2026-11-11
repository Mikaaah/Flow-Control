import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("publisher", Path(__file__).with_name("publish_curseforge.py"))
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)


class PublishingChecks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads((p.ROOT / ".github/publish/releases/1.0.0.json").read_text())

    def test_exact_delivered_files_and_embedded_identities(self):
        self.assertEqual(len(self.manifest["artifacts"]), 9)
        for artifact in self.manifest["artifacts"]:
            with self.subTest(target=artifact["target"]):
                p.validate_artifact(artifact, "1.0.0")

    def test_existing_and_uncertain_uploads_are_not_repeated(self):
        a = self.manifest["artifacts"][0]
        for state in ("published", "uploaded"):
            self.assertFalse(p.pending_artifact(a, {"status": state, "sha256": a["sha256"]}, None))
        self.assertFalse(p.pending_artifact(a, None, 123))
        self.assertTrue(p.pending_artifact(a, None, None))
        for state in ("uploading", "uncertain"):
            with self.assertRaises(p.PublishError):
                p.pending_artifact(a, {"status": state, "sha256": a["sha256"]}, None)
        with self.assertRaises(p.PublishError):
            p.pending_artifact(a, {"status": "uploaded", "sha256": "different"}, None)

    def test_metadata_sets_only_each_files_own_target(self):
        labels = sorted({label for a in self.manifest["artifacts"] for label in
                         [a["minecraft"], p.LOADERS[a["loader"]], f"Java {a['java']}", "Client", "Server"]})
        versions = [{"name": name, "id": i} for i, name in enumerate(labels)]
        for artifact in self.manifest["artifacts"]:
            metadata = p.metadata_for(self.manifest, artifact, versions)
            self.assertEqual(metadata["displayName"], artifact["filename"])
            self.assertEqual(len(metadata["gameVersions"]), 5)
            self.assertEqual(metadata["changelogType"], "html")
            self.assertNotIn("{{", metadata["changelog"])
            self.assertTrue(any(x["projectID"] == 1581408 and x["type"] == "requiredDependency"
                                for x in metadata["relations"]["projects"]))

    def test_version_labels_fail_if_missing_or_ambiguous(self):
        with self.assertRaises(p.PublishError):
            p.version_id([], "Java 25")
        with self.assertRaises(p.PublishError):
            p.version_id([{"name": "Fabric", "id": 1}, {"name": "Fabric", "id": 2}], "Fabric")

    def test_minecraft_label_uses_native_entry_not_server_api_alias(self):
        versions = [{"name": "1.20.1", "id": 9990, "apiVersion": None},
                    {"name": "1.20.1", "id": 9993, "apiVersion": ""},
                    {"name": "1.20.1", "id": 9994, "apiVersion": ""}]
        self.assertEqual(p.version_id(versions, "1.20.1"), 9990)


if __name__ == "__main__":
    unittest.main()
