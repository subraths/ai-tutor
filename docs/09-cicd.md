# 09 — CI/CD

GitHub Actions in [`.github/workflows/`](../.github/workflows/). Path filters mean
backend changes only run backend jobs and vice-versa.

| Workflow | Trigger | What it does |
|---|---|---|
| `backend-ci` | PR / push to `main` (`backend/**`) | `pytest` + `docker build` sanity |
| `backend-deploy` | push to `main` (`backend/**`), manual | Build image → push to **GHCR** → copy compose+Caddyfile → `docker compose up` on **EC2** (backend + **Caddy/HTTPS**) |
| `android-ci` | PR / push to `main` (`android/**`) | `:app:assembleDebug`, uploads debug APK artifact |
| `android-release` | tag `v*`, manual | Build (signed) release APK → attach to a **GitHub Release** |

## Architecture

```mermaid
flowchart LR
    Dev[push to main] --> BCI[backend-ci: pytest]
    Dev --> BD[backend-deploy]
    BD -->|build+push| GHCR[(ghcr.io/subraths/ai-tutor-backend)]
    BD -->|scp + ssh: docker compose up| EC2[EC2: Caddy :443 HTTPS --> backend :8000]
    Phone[Android app] -->|https://BACKEND_DOMAIN| EC2
    Tag[git tag v*] --> AR[android-release]
    AR -->|assembleRelease| APK[app-release.apk]
    AR -->|attach| REL[GitHub Release]
```

The app reaches the backend over **HTTPS** through Caddy, which obtains and renews
a Let's Encrypt certificate for `BACKEND_DOMAIN` automatically — so the Android app
trusts it with no cleartext exception. If you only have an EC2 IP (no domain), set
`BACKEND_DOMAIN=<ec2-ip>.sslip.io`; `sslip.io` resolves the embedded IP and
Let's Encrypt still issues a valid cert for that name.

## Required GitHub secrets

Repo → Settings → Secrets and variables → Actions.

| Secret | Used by | Notes |
|---|---|---|
| `EC2_HOST` | backend-deploy | EC2 public IP / DNS |
| `EC2_USER` | backend-deploy | e.g. `ec2-user` (Amazon Linux) or `ubuntu` |
| `EC2_SSH_KEY` | backend-deploy | **private** key (PEM) for that user |
| `GEMINI_API_KEY` | backend-deploy | injected as `TUTOR_GEMINI_API_KEY` |
| `BACKEND_DOMAIN` | backend-deploy | public hostname Caddy gets a TLS cert for — a real domain pointed at the EC2 IP, or `<ec2-ip>.sslip.io` |
| `BACKEND_BASE_URL` | android-release | **`https://<BACKEND_DOMAIN>/`** (https, trailing slash) — baked into the release APK |
| `ANDROID_KEYSTORE_BASE64` | android-release | `base64 -w0 release.keystore` (optional*) |
| `ANDROID_KEYSTORE_PASSWORD` | android-release | optional* |
| `ANDROID_KEY_ALIAS` | android-release | optional* |
| `ANDROID_KEY_PASSWORD` | android-release | optional* |

`GITHUB_TOKEN` is automatic (used to push to GHCR and create Releases).

\* If the keystore secrets are absent, the release build falls back to the **debug**
signing key so the APK is still installable. Provide them for a properly-signed
release.

## One-time EC2 setup

```bash
# Amazon Linux 2023 — Docker + the Compose v2 plugin
sudo dnf -y install docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user        # re-login afterwards
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version                  # verify the plugin is picked up
```

- Open inbound **TCP 80 and 443** in the instance's security group (Caddy needs
  **80** for the ACME challenge and serves traffic on **443**). Port 8000 stays
  closed — the backend is only published on the host's loopback for debugging.
- Point `BACKEND_DOMAIN` at this host: either an `A` record for your domain → the
  EC2 IP, or just use `<ec2-ip>.sslip.io` (no DNS setup needed).
- The deploy job logs in to GHCR with `GITHUB_TOKEN`; if the package is **private**
  ensure it's linked to this repo (it is, by default). Otherwise make the GHCR
  package public (Packages → package → visibility).
- Data persists in the `tutor-data` volume; TLS certs persist in `caddy-data`
  (so redeploys don't re-hit Let's Encrypt rate limits).

## Generating a release keystore

```bash
keytool -genkeypair -v -keystore release.keystore -alias tutorai \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore           # -> ANDROID_KEYSTORE_BASE64
```

Then set `ANDROID_KEY_ALIAS=tutorai` and the two passwords as secrets.

## Cutting a release

```bash
git tag v1.0.0
git push origin v1.0.0     # triggers android-release -> GitHub Release with the APK
```

## Activation checklist

1. Commit and push these files to `main` (workflows only run from the default branch / PRs).
2. Add the secrets above — including `BACKEND_DOMAIN` and `BACKEND_BASE_URL=https://<BACKEND_DOMAIN>/`.
3. Prepare the EC2 host (Docker + Compose plugin, security group **80/443**).
4. Push a `backend/**` change (or run `backend-deploy` manually) → backend goes live.
   Verify: `curl https://<BACKEND_DOMAIN>/healthz` returns 200 (give Caddy ~30s on
   first run to obtain the certificate).
5. Tag `v*` → release APK (pointed at `https://<BACKEND_DOMAIN>/`) appears under **Releases**.

## Local verification already done

- `docker build` of [`backend/Dockerfile`](../backend/Dockerfile) succeeds; the
  container serves `/healthz` and `POST /api/v1/lessons` (mock providers, no key).
- `docker compose config` and `caddy validate` both pass for
  [`docker-compose.yml`](../backend/docker-compose.yml) + [`Caddyfile`](../backend/Caddyfile).
- `:app:assembleRelease` builds an installable APK (debug-key fallback path).
