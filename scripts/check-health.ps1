param(
    [string]$HealthUrl = 'http://127.0.0.1:8080/actuator/health',
    [string]$StateFile = '.local/health-alert.state'
)

$ErrorActionPreference = 'Stop'
$previous = if (Test-Path -LiteralPath $StateFile) {
    (Get-Content -LiteralPath $StateFile -Raw).Trim()
} else { 'UP' }
$current = 'DOWN'
$reason = ''
try {
    $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 10
    if ($response.status -eq 'UP') { $current = 'UP' }
    else { $reason = "Health status: $($response.status)" }
} catch {
    $reason = $_.Exception.Message
}

if ($current -ne $previous) {
    $webhook = $env:APP_ALERT_WEBHOOK_URL
    if ($webhook) {
        if (-not $webhook.StartsWith('https://', [System.StringComparison]::OrdinalIgnoreCase)) {
            throw 'APP_ALERT_WEBHOOK_URL must use HTTPS.'
        }
        $text = if ($current -eq 'UP') {
            "MyEnterpriseOpenSource recovered: $HealthUrl"
        } else {
            "MyEnterpriseOpenSource health check failed: $HealthUrl — $reason"
        }
        $body = @{ text = $text } | ConvertTo-Json -Compress
        Invoke-RestMethod -Uri $webhook -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 10 | Out-Null
    } elseif ($current -eq 'DOWN') {
        throw "Health check failed; set APP_ALERT_WEBHOOK_URL to deliver alerts. $reason"
    }
    $absolute = [System.IO.Path]::GetFullPath($StateFile)
    [System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($absolute)) | Out-Null
    [System.IO.File]::WriteAllText($absolute, $current)
}

if ($current -eq 'DOWN') { throw "Health check failed: $reason" }
