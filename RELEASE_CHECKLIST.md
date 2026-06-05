# Release Checklist

Use GitHub Releases for downloadable jars. Do not create a separate jar-only repository.

## Prepare

1. Update `mod_version` in `gradle.properties`.
2. Update `CHANGELOG.md`.
3. Run:

```sh
./gradlew build --no-daemon
```

4. Verify the jar exists:

```text
build/libs/dreadfall-VERSION.jar
```

5. Start a local dev server:

```sh
./gradlew runServer --no-daemon
```

6. Confirm the log shows Dreadfall loading and config generation.

## Commit

```sh
git status
git add README.md CHANGELOG.md RELEASE_CHECKLIST.md gradle.properties
git commit -m "Prepare VERSION release"
git push
```

## Tag

```sh
git tag vVERSION
git push origin vVERSION
```

Example:

```sh
git tag v0.1.0
git push origin v0.1.0
```

## GitHub Release

1. Open the repository on GitHub.
2. Go to Releases.
3. Draft a new release from the tag.
4. Attach:

```text
build/libs/dreadfall-VERSION.jar
```

5. Do not attach the `-sources.jar` for normal players.
6. Paste the matching `CHANGELOG.md` section into the release notes.
7. Publish the release.

## Smoke Test The Release Jar

1. Download the jar from GitHub Releases.
2. Install it in a clean Fabric 26.1.2 client or server with Fabric API.
3. Confirm config generation.
4. Summon a zombie, skeleton, creeper, and ghast.
5. Confirm no startup errors in logs.

