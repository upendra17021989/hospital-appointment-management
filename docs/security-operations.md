# Security Operations

## Daily Database Backup

Set `DATABASE_URL` in the scheduler environment and run:

```powershell
.\scripts\backup-postgres.ps1 -OutputDir D:\hospital-backups
```

Run this from Windows Task Scheduler once per day. Keep at least 7 daily backups and store a copy outside the application server.

Never store database backups in the source repository. Backups can contain patient records,
password hashes, and credentials. Encrypt backup storage, restrict access to operators who
need it, and periodically test restoration into an isolated environment.

For managed databases, enable provider backups too:

- Supabase: enable daily backups on the project plan that supports them.
- Render Postgres: use a paid database plan with automatic backups.

## Production Secrets

Keep real values out of committed config. Set these as hosting environment variables:

- `APP_JWT_SECRET`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `WHATSAPP_ACCOUNT_SID`
- `WHATSAPP_AUTH_TOKEN`
- `WHATSAPP_FROM_NUMBER`
- `SMS_ACCOUNT_SID`
- `SMS_AUTH_TOKEN`
- `SMS_FROM_NUMBER`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_SECURITY_REQUIRE_HTTPS`
