param(
    [ValidateSet('local', 'prod')][string]$Mode = 'local',
    [string]$OutputDirectory = 'backups'
)

$ErrorActionPreference = 'Stop'
$compose = if ($Mode -eq 'prod') {
    @('--env-file', '.env.prod', '-f', 'compose.prod.yaml')
} else {
    @('-f', 'compose.yaml')
}
$container = (& docker compose @compose ps -q mysql).Trim()
if ($LASTEXITCODE -ne 0 -or -not $container) { throw 'MySQL is not running in the selected Compose stack.' }

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$backupFile = "/tmp/meos-backup-$stamp.sql"
$restoreDb = "meos_restore_$stamp"
$directory = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($directory) | Out-Null
$destination = Join-Path $directory "myenterpriseos-$stamp.sql"

try {
    & docker exec $container sh -c "set -e; MYSQL_PWD=`"`$MYSQL_PASSWORD`" mysqldump --single-transaction --no-tablespaces --routines --triggers -u`"`$MYSQL_USER`" `"`$MYSQL_DATABASE`" > $backupFile"
    if ($LASTEXITCODE -ne 0) { throw 'mysqldump failed.' }
    & docker cp "${container}:$backupFile" $destination
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the database backup.' }

    & docker exec $container sh -c "MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" mysql -uroot -e `"CREATE DATABASE $restoreDb`""
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the temporary restore database.' }
    & docker cp $destination "${container}:$backupFile"
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the backup for verification.' }
    & docker exec $container sh -c "MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" mysql -uroot $restoreDb < $backupFile"
    if ($LASTEXITCODE -ne 0) { throw 'Backup restore verification failed.' }
    $tables = & docker exec $container sh -c "MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" mysql -N -uroot -e `"SHOW TABLES FROM $restoreDb`""
    if ($LASTEXITCODE -ne 0 -or $tables -notcontains 'company' -or $tables -notcontains 'sales_order') {
        throw 'Restored database is missing expected tables.'
    }
    Write-Output "Backup verified: $destination"
} finally {
    & docker exec $container sh -c "MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" mysql -uroot -e `"DROP DATABASE IF EXISTS $restoreDb`"" | Out-Null
    & docker exec $container rm -f $backupFile | Out-Null
}
