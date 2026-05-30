# Oracle Manual Backend Deploy Draft

This folder is a deferred draft for moving LaundryHub backend to Oracle Cloud later. The project is using Render as the temporary low-effort backend host while Oracle A1 capacity is unavailable in the current region.

Keep this folder because the VM, Docker, Supabase, Caddy HTTPS, and Android Remote Config path is still useful once Oracle capacity is available.

## VM Checklist

- Ubuntu 24.04 on Oracle Cloud Always Free.
- Oracle ingress allows `22`, `80`, and `443`.
- Docker Engine and Docker Compose plugin are installed.
- The backend deploy lives under `/opt/laundryhub/backend`.

## First Deploy

From the VM:

```bash
cd /opt/laundryhub/backend
bash deploy/oracle/bootstrap-ubuntu.sh
```

Log out and back in after the bootstrap so the Docker group change takes effect. If you prefer manual setup, the equivalent directory preparation is:

```bash
sudo mkdir -p /opt/laundryhub/backend
sudo chown -R "$USER":"$USER" /opt/laundryhub
```

Copy the repository into `/opt/laundryhub/backend`, then create the runtime env:

```bash
cd /opt/laundryhub/backend/deploy/oracle
cp env.example .env
nano .env
```

For the temporary host, use:

```text
ORACLE_PUBLIC_HOST=<PUBLIC_IP>.sslip.io
```

Keep real secrets only in `.env` on the VM. `GOOGLE_SERVICE_ACCOUNT_JSON` must be a single-line JSON value.

Start the backend:

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=120 backend
```

Verify HTTPS through Caddy:

```bash
curl https://<PUBLIC_IP>.sslip.io/api/health
```

## Android Cutover

Only after `/api/health` passes, set Firebase Remote Config:

```text
api_base_url=https://<PUBLIC_IP>.sslip.io/api
```

Raw `http://<PUBLIC_IP>` is useful for server-only debugging, but Android remote backend config requires HTTPS.

## Rollback

Do not remove Railway until Oracle passes smoke tests. Rollback is changing Firebase Remote Config `api_base_url` back to the Railway URL.

## Notes

- Supabase remains the production database.
- Google Sheets remains a mirror/sync surface, not the Android client's direct data source.
- Replace `sslip.io` with a real domain/subdomain before treating Oracle as the final production endpoint.
