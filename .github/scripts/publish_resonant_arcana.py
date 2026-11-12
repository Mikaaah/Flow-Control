"""Publish checksum-verified release files without rebuilding them."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
import tomllib
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[2]
CF = "https://minecraft.curseforge.com"
GH = "https://api.github.com"
LOADERS = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


class PublishError(RuntimeError):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def request_json(url, *, token=None, github=False, method="GET", data=None, content_type=None):
    headers = {"User-Agent": "TNK-Release-Publisher/1.0", "Accept": "application/json"}
    if token:
        expected = "api.github.com" if github else "minecraft.curseforge.com"
        if urllib.parse.urlparse(url).hostname != expected:
            raise PublishError("Refusing to send credentials to a different host")
        headers["Authorization" if github else "X-Api-Token"] = "Bearer " + token if github else token
    if data is not None and not isinstance(data, bytes):
        data = json.dumps(data).encode()
        content_type = "application/json"
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, headers=headers, data=data, method=method)
    # Uploads are never automatically retried: a timeout can follow a successful upload.
    for attempt in range(3 if method == "GET" else 1):
        try:
            with urllib.request.build_opener(NoRedirect).open(req, timeout=90) as response:
                return json.load(response)
        except urllib.error.HTTPError as exc:
            detail = exc.read(1000).decode(errors="replace")
            if token:
                detail = detail.replace(token, "[redacted]")
            if method == "GET" and exc.code in (429, 500, 502, 503, 504) and attempt < 2:
                time.sleep(2)
                continue
            raise PublishError(f"{method} {urllib.parse.urlparse(url).path}: HTTP {exc.code}: {detail}") from None
        except (OSError, ValueError) as exc:
            if method == "GET" and attempt < 2:
                time.sleep(2)
                continue
            raise PublishError(f"{method} {urllib.parse.urlparse(url).path}: {type(exc).__name__}") from None


def version_id(versions, name):
    matches = [v for v in versions if v.get("name", "").casefold() == name.casefold()]
    ids = {v["id"] for v in matches}
    if len(ids) > 1:
        # CurseForge also returns Bukkit/API variants with the same display name.
        # Native Minecraft entries have a null apiVersion; server API aliases
        # carry a non-null value, including the empty string.
        native = [v for v in matches if "apiVersion" in v and v["apiVersion"] is None]
        if len({v["id"] for v in native}) == 1:
            return native[0]["id"]
    if len(ids) != 1:
        detail = matches or [v for v in versions if name.casefold() in str(v).casefold()][:8]
        raise PublishError(f"CurseForge version label must resolve uniquely: {name}; candidates={json.dumps(detail)}")
    return ids.pop()


def validate_artifact(artifact, version):
    path = (ROOT / artifact["path"]).resolve()
    if not path.is_relative_to(ROOT) or path.name != artifact["filename"]:
        raise PublishError("Invalid release file path")
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != artifact["sha256"]:
        raise PublishError(f"Checksum mismatch: {path.name}")
    expected_name = f"resonant-arcana-{version}-{artifact['loader']}-{artifact['minecraft']}.jar"
    if path.name != expected_name:
        raise PublishError(f"Unexpected release filename: {path.name}")
    with zipfile.ZipFile(path) as jar:
        marker = json.loads(jar.read("META-INF/tnk-release.json"))
        for key, expected in {"project": "resonant-arcana", "version": version, "target": artifact["target"], "loader": artifact["loader"],
                              "minecraft": artifact["minecraft"], "java": artifact["java"],
                              "source_commit": artifact["source_commit"], "source_dirty": False}.items():
            if marker.get(key) != expected:
                raise PublishError(f"Release identity mismatch: {path.name}: {key}")
        if artifact["loader"] == "fabric":
            metadata = json.loads(jar.read("fabric.mod.json"))
            if metadata.get("id") != "resonant_arcana" or metadata.get("version") != version:
                raise PublishError("Fabric metadata version mismatch")
        else:
            name = "META-INF/mods.toml" if artifact["loader"] == "forge" else "META-INF/neoforge.mods.toml"
            metadata = jar.read(name).decode()
            mods = tomllib.loads(metadata).get("mods", [])
            if len(mods) != 1 or mods[0].get("modId") != "resonant_arcana" or mods[0].get("version") != version:
                raise PublishError("FML mod identity mismatch")
            if not re.search(r'^version\s*=\s*"' + re.escape(version) + r'"\s*$', metadata, re.M):
                raise PublishError("FML metadata version mismatch")
    return path


def metadata_for(manifest, artifact, versions):
    labels = [artifact["minecraft"], LOADERS[artifact["loader"]], f"Java {artifact['java']}", "Client", "Server"]
    changelog_path = (ROOT / manifest["changelog"]).resolve()
    if not changelog_path.is_relative_to(ROOT):
        raise PublishError("Invalid changelog path")
    changelog = changelog_path.read_text().replace("{{minecraft}}", artifact["minecraft"]).replace("{{loader}}", LOADERS[artifact["loader"]])
    return {"displayName": artifact["filename"], "releaseType": "release", "changelogType": "html",
            "changelog": changelog, "gameVersions": [version_id(versions, label) for label in labels],
            "relations": {"projects": artifact["relations"]}}


def multipart(metadata, path):
    boundary = "tnk-" + uuid.uuid4().hex
    parts = [f'--{boundary}\r\nContent-Disposition: form-data; name="metadata"\r\nContent-Type: application/json\r\n\r\n'.encode(),
             json.dumps(metadata).encode(),
             f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{path.name}"\r\nContent-Type: application/java-archive\r\n\r\n'.encode(),
             path.read_bytes(), f"\r\n--{boundary}--\r\n".encode()]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def list_files(project_id):
    result = []
    for page in range(20):
        url = f"https://www.curseforge.com/api/v1/mods/{project_id}/files?pageIndex={page}&pageSize=50&sort=dateCreated&sortDescending=true"
        data = request_json(url)
        items = data.get("data", [])
        if not isinstance(items, list):
            raise PublishError("Unexpected public file-list response")
        result.extend(items)
        if len(items) < 50:
            return result
    raise PublishError("Public file list exceeded pagination limit")


def verified_existing(artifact, existing):
    matches = [f for f in existing if f.get("fileName") == artifact["filename"]]
    if not matches:
        return None
    if len(matches) != 1:
        raise PublishError(f"Multiple existing files named {artifact['filename']}")
    file_id = int(matches[0]["id"])
    prefix, suffix = divmod(file_id, 1000)
    url = f"https://mediafilez.forgecdn.net/files/{prefix}/{suffix}/{urllib.parse.quote(artifact['filename'])}"
    with urllib.request.urlopen(url, timeout=60) as response:
        digest = hashlib.sha256(response.read()).hexdigest()
    if digest != artifact["sha256"]:
        raise PublishError(f"Existing filename has different bytes: {artifact['filename']}")
    return file_id


class Ledger:
    def __init__(self, version, project_id):
        self.path = f".github/publish/resonant-arcana/receipts/{version}.json"
        self.repo = os.environ["GITHUB_REPOSITORY"]
        self.token = os.environ["GITHUB_TOKEN"]
        self.url = f"{GH}/repos/{self.repo}/contents/{self.path}"
        remote = request_json(self.url + "?ref=publish/resonant-arcana-1.1.3", token=self.token, github=True)
        self.sha = remote["sha"]
        self.data = json.loads(base64.b64decode(remote["content"]))
        if self.data["version"] != version or self.data["project_id"] != project_id:
            raise PublishError("Receipt ledger does not match the release")

    def save(self):
        content = base64.b64encode((json.dumps(self.data, indent=2) + "\n").encode()).decode()
        result = request_json(self.url, token=self.token, github=True, method="PUT",
                              data={"branch": "publish/resonant-arcana-1.1.3", "sha": self.sha, "content": content,
                                    "message": f"Record CurseForge publication progress for {self.data['version']}"})
        self.sha = result["content"]["sha"]

    def set(self, artifact, state, **extra):
        self.data["files"][artifact["filename"]] = {"status": state, "sha256": artifact["sha256"], **extra}
        self.save()


def pending_artifact(artifact, saved, existing_id):
    if saved and saved.get("sha256") != artifact["sha256"]:
        raise PublishError("A published version cannot acquire different bytes")
    if saved and saved.get("status") in ("published", "uploaded"):
        return False
    if existing_id:
        return False
    if saved and saved.get("status") in ("uploading", "uncertain"):
        raise PublishError(f"Previous upload outcome is uncertain for {artifact['filename']}; check CurseForge before retrying")
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", default="1.0.0")
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    if not re.fullmatch(r"\d+\.\d+\.\d+", args.version):
        raise PublishError("Invalid release version")
    token = os.environ.get("CURSEFORGE_TOKEN", "")
    if not token:
        raise PublishError("CURSEFORGE_TOKEN repository secret is missing")
    manifest = json.loads((ROOT / f".github/publish/resonant-arcana/releases/{args.version}.json").read_text())
    if manifest["version"] != args.version or manifest["project_id"] != 1750997:
        raise PublishError("Unexpected publishing project")
    if args.publish and os.environ.get("GITHUB_REF") != "refs/heads/publish/resonant-arcana-1.1.3":
        raise PublishError("Live publishing must run on the Resonant Arcana release branch")
    versions = request_json(CF + "/api/game/versions", token=token)
    if isinstance(versions, dict):
        versions = versions.get("data", [])
    if not isinstance(versions, list) or not versions:
        raise PublishError("CurseForge returned no game-version labels")
    existing = list_files(manifest["project_id"])
    ledger = Ledger(args.version, manifest["project_id"]) if args.publish else None
    state = ledger.data if ledger else json.loads((ROOT / f".github/publish/resonant-arcana/receipts/{args.version}.json").read_text())
    plans = []
    for artifact in manifest["artifacts"]:
        path = validate_artifact(artifact, args.version)
        metadata = metadata_for(manifest, artifact, versions)
        old = state["files"].get(artifact["filename"])
        existing_id = verified_existing(artifact, existing)
        pending = pending_artifact(artifact, old, existing_id)
        plans.append((artifact, path, metadata, pending, existing_id))
        print(("UPLOAD " if pending else "SKIP   ") + artifact["filename"] + f" | {artifact['loader']} | MC {artifact['minecraft']} | Java {artifact['java']}", flush=True)
    count = sum(plan[3] for plan in plans)
    print(f"Preflight passed: {count} uploads, {len(plans)-count} existing files.", flush=True)
    if not args.publish:
        return
    for artifact, path, metadata, pending, existing_id in plans:
        if not pending:
            if existing_id and artifact["filename"] not in state["files"]:
                ledger.set(artifact, "published", file_id=existing_id, origin="existing matching release")
            continue
        # Durable checkpoint before the external write prevents a blind retry.
        ledger.set(artifact, "uploading", run_id=os.environ.get("GITHUB_RUN_ID"))
        body, content_type = multipart(metadata, path)
        try:
            result = request_json(f"{CF}/api/projects/{manifest['project_id']}/upload-file", token=token,
                                  method="POST", data=body, content_type=content_type)
            file_id = int(result["id"])
        except Exception:
            ledger.set(artifact, "uncertain", run_id=os.environ.get("GITHUB_RUN_ID"))
            raise
        ledger.set(artifact, "uploaded", file_id=file_id, run_id=os.environ.get("GITHUB_RUN_ID"),
                   metadata_sha256=hashlib.sha256(json.dumps(metadata, sort_keys=True).encode()).hexdigest(),
                   game_versions=metadata["gameVersions"], changelog_type=metadata["changelogType"])
        print(f"UPLOADED {artifact['filename']} -> CurseForge file {file_id}", flush=True)
    print("Publication complete. CurseForge moderation may still be pending.", flush=True)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a") as summary:
            summary.write(f"## Resonant Arcana {args.version}\n\n")
            for name, entry in ledger.data["files"].items():
                if entry.get("file_id"):
                    summary.write(f"- [{name}](https://www.curseforge.com/minecraft/mc-mods/{manifest['project_slug']}/files/{entry['file_id']}) — {entry['status']}\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"::error::{type(exc).__name__}: {exc}", file=sys.stderr)
        sys.exit(1)


