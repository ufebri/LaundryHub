# Deployment Brief

## Goal

LaundryHub backend needs a temporary Railway replacement with the lowest practical setup cost. Render is the current short-term path. Oracle Cloud remains documented as a draft fallback for later, once Always Free capacity is available.

## Current Decision

- Backend stays as the source-facing API for Android.
- Supabase Postgres remains the production database.
- Google Sheets remains a mirror/reporting and manual sync surface.
- Railway stays available until Render passes backend and Android smoke tests.
- Render is the current temporary deploy path because it can use the existing root `Dockerfile` with little setup.
- Oracle Cloud is deferred because `VM.Standard.A1.Flex` returned host-capacity errors in `ap-batam-1`.
- The Oracle draft assets stay under `deploy/oracle` for a later manual-then-GitHub-Actions migration.

## Render Temporary Runtime

- Render Web Service runs the existing root `Dockerfile`.
- Health check path is `/api/health`.
- Render provides HTTPS, so Android Remote Config can use `https://<render-service>.onrender.com/api`.
- Free Render services can sleep, so first request after inactivity may be slow and background Sheets sync may pause while inactive.
- Backend sync defaults to App Database as master so queued writes push to Sheets after Render restarts. Summary reads still prefer live Sheets data when configured. Near-real-time push is debounced by `SHEETS_PUSH_DEBOUNCE_MILLIS`.

## Oracle Draft Runtime

- Public traffic terminates at Caddy on `80/443`.
- Caddy proxies `/api` and `/api/*` to the backend container on internal port `8080`.
- Temporary Android URL is `https://<PUBLIC_IP>.sslip.io/api`.
- A real domain/subdomain should replace `sslip.io` before final production cutover.

## Required Runtime Variables

- Supabase: `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_SSL_MODE`
- Sheets/FCM: `SPREADSHEET_ID`, `GOOGLE_SERVICE_ACCOUNT_JSON`
- Safety: `ENABLE_MIGRATION_ROUTES=false`
- Near-real-time Sheets push: `SHEETS_PUSH_DEBOUNCE_MILLIS=3000`
- Oracle HTTPS draft only: `ORACLE_PUBLIC_HOST`, `ACME_EMAIL`

## Verification

- Render: `curl https://<render-service>.onrender.com/api/health`
- Render data path: `curl https://<render-service>.onrender.com/api/summary`
- Render sync status: `curl https://<render-service>.onrender.com/api/sync/status`
- Android startup after Firebase Remote Config points `api_base_url` to the Render URL.
- Set `api_fallback_base_urls` to every standby HTTPS API URL while Render is temporary, for example Railway first and Oracle later if the VM becomes available.
- Keep Railway as rollback until a guarded order/outcome smoke test and Sheets mirror behavior are confirmed.

Oracle draft verification:

- `docker compose ps`
- `docker compose logs --tail=120 backend`
- `curl https://<PUBLIC_IP>.sslip.io/api/health`

Latest local verification for deploy asset creation:

- `./gradlew :backend:test --no-daemon`
- `bash -n deploy/oracle/bootstrap-ubuntu.sh`
- `ruby -e "require 'yaml'; YAML.load_file('deploy/oracle/docker-compose.yml'); puts 'docker-compose.yml YAML OK'"`
- `git diff --check`

Skipped locally:

- `docker compose -f deploy/oracle/docker-compose.yml --env-file deploy/oracle/env.example config` because Docker is not installed on this machine.
