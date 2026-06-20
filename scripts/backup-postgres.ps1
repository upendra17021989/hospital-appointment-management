param(
    [string]$OutputDir = ".\backups"
)

$ErrorActionPreference = "Stop"

if (-not $env:DATABASE_URL) {
    throw "DATABASE_URL is required. Example: postgresql://user:password@host:5432/postgres"
}

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump was not found. Install PostgreSQL client tools and add them to PATH."
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupFile = Join-Path $OutputDir "hospital-db-$timestamp.dump"

pg_dump $env:DATABASE_URL --format=custom --no-owner --no-acl --file=$backupFile

Write-Host "Backup created: $backupFile"
