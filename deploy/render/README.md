# Render Temporary Backend Deploy

Render is the temporary low-effort backend host while Oracle Cloud A1 capacity is unavailable. Supabase remains the database, and Railway should stay available until the Render smoke test passes.

## Service Setup

- Create a Render Web Service from the GitHub repository.
- Runtime: Docker.
- Dockerfile path: `Dockerfile`.
- Root directory: repository root.
- Branch: start with the deployment branch you want to verify manually, then switch to the production branch after smoke testing.
- Health check path: `/api/health`.
- Free instance is acceptable only as a temporary bridge because it can sleep after inactivity.

## Runtime Variables

Set these in the Render dashboard. Do not commit real values.

```text
DATABASE_HOST=<SUPABASE_DB_HOST>
DATABASE_PORT=5432
DATABASE_NAME=postgres
DATABASE_USER=<SUPABASE_DB_USER>
DATABASE_PASSWORD=<SUPABASE_DB_PASSWORD>
DATABASE_SSL_MODE=require
SPREADSHEET_ID=<SPREADSHEET_ID>
GOOGLE_SERVICE_ACCOUNT_JSON=<SERVICE_ACCOUNT_JSON_ON_ONE_LINE>
ENABLE_MIGRATION_ROUTES=false
SHEETS_PUSH_DEBOUNCE_MILLIS=3000
```

Render provides HTTPS automatically. After deploy, the Android Remote Config value should be:

```text
api_base_url=https://<render-service>.onrender.com/api
api_fallback_base_urls=
https://<railway-service>/api
https://<oracle-domain-or-ip-host>/api
```

`api_fallback_base_urls` can contain multiple HTTPS URLs separated by newlines or commas. Keep Render as the primary temporary URL, put Railway next while it remains available, and add Oracle later if the Always Free VM becomes available.

## Verification

```bash
curl https://<render-service>.onrender.com/api/health
```

Then launch Android and confirm startup resolves the Render URL. Keep Railway as rollback until an order/outcome smoke test and Sheets mirror behavior are confirmed.

## Known Tradeoffs

- Free services can sleep, so the first request after inactivity may be slow.
- Background Sheets sync may not run predictably while the service is sleeping, but writes should push to Sheets a few seconds after the service is awake.
- If cold start or background sync becomes disruptive, move to a paid Render instance, Cloud Run, VPS, or return to the Oracle draft plan.
