#!/usr/bin/env python3
"""Puts the Propwash FPV and Vivecraft jars for Minecraft 26.3 into libs/, where the build picks them up.

- Vivecraft: newest NeoForge build for 26.3 from Modrinth.
- Propwash FPV: from Modrinth if it is published there, otherwise built from its source repository at a release tag
  (needs git and Java 25, like the build itself).

Usage: python3 scripts/fetch-deps.py [--force]
Environment: PROPWASH_TAG (default v0.5.0), MC_VERSION (default 26.3).
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request

MC_VERSION = os.environ.get("MC_VERSION", "26.3")
PROPWASH_TAG = os.environ.get("PROPWASH_TAG", "v0.5.0")
PROPWASH_REPO = "https://github.com/LardTom/propwash-fpv"
LOADER = "neoforge"  # common code is the same in both loader jars; one variant is enough to compile against
USER_AGENT = "fullmetalex/Vivecraft-x-propwash-FPV build script"

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIBS = os.path.join(ROOT, "libs")


def get_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.load(response)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise


def download(url, target):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=300) as response, open(target, "wb") as out:
        shutil.copyfileobj(response, out)


def modrinth_jar(slugs, name):
    query = urllib.parse.urlencode({
        "game_versions": json.dumps([MC_VERSION]),
        "loaders": json.dumps([LOADER]),
    })
    for slug in slugs:
        versions = get_json(f"https://api.modrinth.com/v2/project/{slug}/version?{query}")
        if not versions:
            print(f"{name}: no {LOADER} build for {MC_VERSION} on Modrinth under '{slug}'")
            continue
        version = versions[0]
        files = version["files"]
        file = next((f for f in files if f.get("primary")), files[0])
        target = os.path.join(LIBS, file["filename"])
        print(f"{name}: Modrinth '{slug}' version {version['version_number']} -> libs/{file['filename']}")
        download(file["url"], target)
        return True
    return False


def build_propwash():
    work = tempfile.mkdtemp(prefix="propwash-src-")
    try:
        print(f"Propwash FPV: building {PROPWASH_REPO} at {PROPWASH_TAG}")
        subprocess.run(["git", "clone", "--depth", "1", "--branch", PROPWASH_TAG, PROPWASH_REPO, work], check=True)
        gradlew = os.path.join(work, "gradlew.bat" if os.name == "nt" else "gradlew")
        subprocess.run([gradlew, ":neoforge:jar", "--no-daemon"], cwd=work, check=True)
        out = os.path.join(work, "neoforge", "build", "libs")
        jars = [j for j in os.listdir(out) if j.endswith(".jar") and not j.endswith(("-sources.jar", "-javadoc.jar"))]
        if len(jars) != 1:
            raise SystemExit(f"Propwash FPV: expected one jar in {out}, found {jars}")
        shutil.copy(os.path.join(out, jars[0]), os.path.join(LIBS, jars[0]))
        print(f"Propwash FPV: built -> libs/{jars[0]}")
    finally:
        shutil.rmtree(work, ignore_errors=True)


def present(prefix):
    return any(j.lower().startswith(prefix) and j.endswith(".jar") for j in os.listdir(LIBS))


def main():
    os.makedirs(LIBS, exist_ok=True)
    force = "--force" in sys.argv[1:]
    if force:
        for jar in os.listdir(LIBS):
            if jar.endswith(".jar"):
                os.remove(os.path.join(LIBS, jar))

    if present("vivecraft"):
        print("Vivecraft: already in libs/")
    elif not modrinth_jar(["vivecraft"], "Vivecraft"):
        raise SystemExit(f"Vivecraft: no {LOADER} build for Minecraft {MC_VERSION} found; put the jar into libs/")

    if present("propwash"):
        print("Propwash FPV: already in libs/")
    elif not modrinth_jar(["propwash-fpv", "propwash"], "Propwash FPV"):
        build_propwash()


if __name__ == "__main__":
    main()
