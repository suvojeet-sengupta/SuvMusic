# SuvMusic Self-Hosted OTA — Implementation Plan

Goal: serve app updates (manifests + APKs) from our own Linux VPS instead of
GitHub raw + GitHub Releases, **robustly** — meaning: integrity-checked,
tamper-resistant, highly available (GitHub kept as automatic fallback), and
publishable with a single reproducible command.

---

## 1. Current state (what already exists)

Client module: `updater/` (already Media3-independent, DI-wired).

| Piece | File | Status |
|-------|------|--------|
| Manifest fetch | `UpdateChecker.kt` | Fetches `update.json` / `nightly.json` / `changelog.json` from `raw.githubusercontent.com/.../main/updater`, `FORCE_NETWORK`, cache-busted with `?t=`. |
| Manifest model | `UpdateInfo.kt` | Has `versionName`, `versionCode`, `downloadUrl`, `forceUpdate`, `size`, and a nullable **`sha256`**. |
| Download + install | `UpdateDownloader.kt` | `DownloadManager` → optional **SHA-256 verify** → `FileProvider` install. Receiver registered `NOT_EXPORTED`. |
| VM glue | `UpdateViewModel.kt` | `downloadAndInstall(url, versionName, info.sha256)` already forwards the hash. |

**The client is already solid.** The current `update.json` simply omits
`sha256`, so verification is a no-op today. The real work is **hosting + trust +
publish automation**, plus small client changes for failover and rollout.

### Gaps to close
1. `baseUrl` is hardcoded to GitHub raw — no way to point at the VPS, no failover.
2. Manifests carry no `sha256`, no mirror URL, no rollout %, no min-version floor.
3. No manifest **authenticity** — anyone who controls the host (or MITMs a
   non-pinned TLS connection) could point the app at a malicious APK. (Android's
   installer still blocks an APK signed with a different key, but a signed
   *downgrade* or a valid-but-unwanted build is not caught by that alone.)
4. Single point of failure — if the VPS is down, nobody can update.

---

## 2. Target architecture

```
          ┌──────────────────────────── Android client ───────────────────────────┐
          │ UpdateChecker: try PRIMARY (VPS) → on fail, FALLBACK (GitHub raw)       │
          │ Verify Ed25519 signature of manifest → verify APK SHA-256 → install     │
          └────────────────────────────────────────────────────────────────────────┘
                         │ https (TLS, optional cert-pin)          │ https
                         ▼                                         ▼
        ┌──────────────── VPS (nginx, static) ───────────┐   ┌──────── GitHub ───────┐
        │ ota.suvmusic.app/                               │   │ raw + Releases        │
        │   stable/update.json    stable/apks/*.apk       │   │ (mirror, always-on)   │
        │   nightly/nightly.json  nightly/apks/*.apk      │   └───────────────────────┘
        │   changelog.json        keys/ed25519.pub        │
        └─────────────────────────────────────────────────┘
                         ▲
                         │ rsync over SSH (publish, APK first, manifest last)
        ┌────────────────┴──────────────── release host / CI ─────────────────────┐
        │ build signed APK → sha256 + size → sign manifest (Ed25519) → rsync       │
        └──────────────────────────────────────────────────────────────────────────┘
```

**Recommendation: static nginx.** No app server, no DB, nothing to crash or
patch. A dynamic API is only needed if we later want *server-controlled* rollout
percentages or update analytics (see §10, optional).

---

## 3. Server setup (VPS)

Assumes Debian/Ubuntu with nginx. Replace `ota.suvmusic.app` with the real host.

### 3.1 Directory layout (web root)
```
/var/www/ota.suvmusic.app/
├── changelog.json
├── stable/
│   ├── update.json
│   └── apks/
│       └── SuvMusic-v2.5.8.0.apk
├── nightly/
│   ├── nightly.json
│   └── apks/
│       └── SuvMusic-nightly-<buildNo>.apk
└── keys/
    └── ed25519.pub          # public key only; private key NEVER on the VPS
```
APK filenames are **version-stamped and immutable** — once published, a given
filename's bytes never change. That lets us cache APKs forever and JSON briefly.

### 3.2 nginx config (`/etc/nginx/sites-available/ota.suvmusic.app`)
```nginx
server {
    listen 443 ssl http2;
    server_name ota.suvmusic.app;

    ssl_certificate     /etc/letsencrypt/live/ota.suvmusic.app/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ota.suvmusic.app/privkey.pem;
    add_header Strict-Transport-Security "max-age=63072000" always;

    root /var/www/ota.suvmusic.app;
    autoindex off;

    # APKs: immutable, versioned filename → cache hard. Range requests ON
    # (nginx default) so DownloadManager can resume partial downloads.
    location ~* \.apk$ {
        types { application/vnd.android.package-archive apk; }
        add_header Cache-Control "public, max-age=31536000, immutable";
        add_header Content-Disposition "attachment";
    }

    # Manifests: tiny, change often → short cache + revalidate via ETag.
    location ~* \.json$ {
        types { application/json json; }
        add_header Cache-Control "public, max-age=60, must-revalidate";
        gzip on; gzip_types application/json;
    }

    # Basic abuse protection for the download endpoint.
    limit_rate_after 20m;   # full speed for first 20MB (an APK), then coast
}

server {                      # force HTTPS
    listen 80;
    server_name ota.suvmusic.app;
    return 301 https://$host$request_uri;
}
```
TLS via `certbot --nginx -d ota.suvmusic.app`, auto-renew via the packaged timer.

---

## 4. Manifest schema (enhanced, backward-compatible)

`stable/update.json` — every existing field kept; new fields are additive so an
old client that ignores them still works.

```json
{
  "versionName": "2.5.9.0",
  "versionCode": 38,
  "minSupportedVersionCode": 30,
  "downloadUrl": "https://ota.suvmusic.app/stable/apks/SuvMusic-v2.5.9.0.apk",
  "mirrorUrl": "https://github.com/suvojeet-sengupta/SuvMusic/releases/download/v2.5.9.0/SuvMusic-v2.5.9.0.apk",
  "sha256": "<hex sha-256 of the apk>",
  "size": 17301504,
  "forceUpdate": false,
  "rolloutPercent": 100,
  "changelog": "• …",
  "releasedAt": "2026-07-06T00:00:00Z",
  "sig": "<base64 ed25519 signature over the canonical manifest, sig field excluded>"
}
```

Field roles:
- **`sha256`** — makes the already-present client verification real. Mandatory going forward.
- **`mirrorUrl`** — GitHub Releases fallback if the VPS download fails.
- **`minSupportedVersionCode`** — anything below this gets a *forced* update (kill-switch for broken old builds).
- **`rolloutPercent`** — client-side staged rollout (§10).
- **`sig`** — Ed25519 signature so a compromised host still can't push a malicious manifest (§8).

---

## 5. Publish pipeline (one command)

`scripts/publish-ota.sh` — run on the release machine (private key lives here,
**never** on the VPS). Pseudocode:

```bash
#!/usr/bin/env bash
set -euo pipefail
CHANNEL="${1:-stable}"          # stable | nightly
VPS="deploy@ota.suvmusic.app"
WEBROOT="/var/www/ota.suvmusic.app"

# 1. Build the signed release APK (uses the app release keystore).
./gradlew :app:assembleRelease
APK=$(ls -t app/build/outputs/apk/release/*.apk | head -1)

VNAME=$(grep versionName app/build.gradle.kts | ... )   # or read from AGP output
VCODE=$(grep versionCode app/build.gradle.kts | ... )
STAMPED="SuvMusic-v${VNAME}.apk"
cp "$APK" "dist/${STAMPED}"

# 2. Integrity + size.
SHA=$(sha256sum "dist/${STAMPED}" | cut -d' ' -f1)
SIZE=$(stat -c%s "dist/${STAMPED}")

# 3. Emit manifest, then Ed25519-sign it (private key from a local file / GH secret).
build_manifest.py --channel "$CHANNEL" --vname "$VNAME" --vcode "$VCODE" \
  --sha "$SHA" --size "$SIZE" --out dist/update.json
sign_manifest.py --key "$OTA_ED25519_PRIVATE" --in dist/update.json   # fills "sig"

# 4. Prepend the changelog entry.
update_changelog.py --vname "$VNAME" --vcode "$VCODE"

# 5. Upload APK FIRST, manifest LAST (atomicity: manifest never references a
#    file that isn't there yet). rsync is resumable and idempotent.
rsync dist/${STAMPED}   "$VPS:$WEBROOT/${CHANNEL}/apks/"
rsync dist/changelog.json "$VPS:$WEBROOT/"
rsync dist/update.json  "$VPS:$WEBROOT/${CHANNEL}/update.json"

# 6. Mirror APK to GitHub Releases (fallback source).
gh release create "v${VNAME}" "dist/${STAMPED}" --notes-file dist/notes.md || \
  gh release upload "v${VNAME}" "dist/${STAMPED}" --clobber
```

Key ordering rule: **APK before manifest**. If the process dies mid-publish, the
live manifest still points at the previous, fully-present APK.

Can also run as a GitHub Actions job on tag push, with the SSH key and the
Ed25519 private key stored as encrypted Actions secrets.

---

## 6. Client changes (`updater/`)

Small, targeted, backward-compatible.

### 6.1 `UpdateInfo.kt` — add fields
```kotlin
val minSupportedVersionCode: Int = 0,
val mirrorUrl: String? = null,
val rolloutPercent: Int = 100,
val sig: String? = null,
```

### 6.2 `UpdateChecker.kt` — configurable base + failover
- Replace the hardcoded `baseUrl` with an ordered list:
  `["https://ota.suvmusic.app", "https://raw.githubusercontent.com/suvojeet-sengupta/SuvMusic/main/updater"]`.
- `fetchJson` tries each host in order; first success wins. VPS down → GitHub serves.
- (Channel path differs: VPS uses `stable/update.json`; GitHub keeps flat
  `update.json`. Encode both in the host list as full path templates.)

### 6.3 Manifest signature verify (the important trust upgrade)
- Bundle the Ed25519 **public** key in the app (`res/raw/ota_ed25519.pub` or a constant).
- After fetching a manifest, canonicalize (drop `sig`, stable key order) and
  verify `sig` with `java.security.Signature` (`Ed25519`, API 33+) or Tink/
  BouncyCastle for older APIs. Reject the manifest on failure → fall through to
  the next host.
- Net effect: even a fully compromised VPS cannot push an update the app trusts.

### 6.4 `UpdateViewModel.kt` — rollout + force floor + mirror
- **Force floor:** if `currentVersionCode < info.minSupportedVersionCode`, treat as `forceUpdate = true` regardless of the manifest's own flag.
- **Staged rollout:** compute a stable per-install bucket
  `bucket = abs(installId.hashCode()) % 100`; only surface the update if
  `bucket < info.rolloutPercent`. (Force updates ignore rollout.)
- **Mirror fallback on download:** if the primary `downloadUrl` fails in
  `UpdateDownloader`, retry once with `mirrorUrl` before erroring.

### 6.5 `UpdateDownloader.kt` — make hash mandatory
- Today `sha256` is optional. Going forward, if `expectedSha256 == null` for a
  *stable* update, refuse to install (defensive: a manifest without a hash is
  treated as untrustworthy). Keep nightly lenient if desired.

---

## 7. Security model (summary)

| Threat | Mitigation |
|--------|-----------|
| MITM swaps the APK bytes | HTTPS **+** mandatory SHA-256 in manifest, verified pre-install (already coded). |
| Host compromised, pushes malicious manifest | **Ed25519 manifest signature**; app ships the public key, rejects unsigned/forged manifests. |
| Malicious APK signed with attacker key | Android package installer enforces same signing certificate on update — blocks it. |
| Forced downgrade to a vulnerable build | `versionCode` monotonic check on client: never "update" to a lower code. |
| VPS down → users stuck | GitHub raw + Releases as automatic fallback host + mirror. |
| Replay of an old signed manifest | Include `versionCode`/`releasedAt` in the signed payload; client already refuses lower `versionCode`. |
| DNS/TLS interception | Optional OkHttp **certificate pinning** to the VPS leaf/intermediate. |

Private-key handling: the **Ed25519 signing key** and the **APK release keystore**
live only on the release host / in CI secrets, never on the VPS. Back both up
offline; losing the Ed25519 key means shipping a client update to rotate it.

---

## 8. Reliability & correctness rules
- **Atomic publish:** APK uploaded before manifest (see §5).
- **Immutable APK URLs:** version-stamped filenames, cached forever; no in-place edits.
- **Monotonic versions:** client ignores any manifest whose `versionCode` ≤ installed.
- **Idempotent deploys:** rsync — re-running publish is safe.
- **Two independent sources:** VPS primary, GitHub fallback; either alone suffices.

---

## 9. Phased migration (low-risk rollout)

| Phase | Action | Risk |
|-------|--------|------|
| 0 | Stand up nginx + TLS on VPS; mirror current `update.json`/`nightly.json`/`changelog.json` + APK. Point nothing at it yet. | none |
| 1 | Ship client build: multi-host `UpdateChecker` (VPS primary, GitHub fallback) + wire `sha256` as mandatory. Start publishing `sha256` in manifests. | low (fallback covers VPS issues) |
| 2 | Move canonical publishing to `publish-ota.sh`; VPS becomes source of truth, GitHub the mirror. Monitor nginx logs / update success. | low |
| 3 | Add Ed25519 manifest signing + client verification; add `rolloutPercent` + `minSupportedVersionCode`. | medium — test signing end-to-end on nightly first |

Each phase is independently shippable and reversible (flip host order / drop a field).

---

## 10. Optional: dynamic API (only if needed later)
A ~100-line service (Go/Node/FastAPI behind nginx) enables:
- **Server-controlled rollout** (change % without a new manifest push).
- **Update analytics** (`GET /check?vc=37&id=…` logs adoption, active versions).
- **Targeted holds** (pause rollout for a specific device/ABI/OS if a build misbehaves).

Not required for a robust launch — static nginx covers everything above. Add it
only when we actually want live rollout control or metrics.

---

## 11. Testing checklist
- [ ] Fresh install → check → download → SHA-256 pass → install succeeds.
- [ ] Corrupt one byte of the hosted APK → client aborts with "signature check failed".
- [ ] Tamper with a manifest field → Ed25519 verify fails → client falls back to GitHub.
- [ ] Stop nginx → client transparently updates via GitHub fallback.
- [ ] `rolloutPercent: 10` → only ~10% of installs see the update; force update ignores it.
- [ ] `minSupportedVersionCode` above an old build → that build gets a mandatory prompt.
- [ ] DownloadManager resume: kill network mid-download, resume completes (Range requests).
- [ ] Publish script killed between APK and manifest upload → live manifest still valid.

---

## 12. Deliverables to build
1. `scripts/publish-ota.sh` + `build_manifest.py` / `sign_manifest.py` / `update_changelog.py`.
2. nginx site config + certbot setup (one-time, documented in `docs/OTA_SERVER_SETUP.md`).
3. Ed25519 keypair; public key committed to the app, private key in CI secrets.
4. `updater/` client changes (§6).
5. Migration executed per §9.
