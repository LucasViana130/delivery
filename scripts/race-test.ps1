<#
 Race-condition test (PowerShell 5.1+ or 7+): fires N simultaneous POST /orders for the promotion dish.
 Expected on a freshly started order-service: exactly 10 x 201 and stock 0 at the end.
 Usage: .\scripts\race-test.ps1                      (defaults: 50 requests, dish 1)
        .\scripts\race-test.ps1 -Requests 80 -BaseUrl http://192.168.0.10:8080
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$DishId = 1,
    [int]$Requests = 50
)

Add-Type -AssemblyName System.Net.Http

function Get-Stock {
    (Invoke-RestMethod -Uri "$BaseUrl/dishes/$DishId").stock
}

Write-Host "Stock before: $(Get-Stock)"
Write-Host "Firing $Requests simultaneous orders (dishId=$DishId, quantity=1)..."

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(120)
$body = '{"dishId":' + $DishId + ',"quantity":1}'

$tasks = New-Object System.Collections.Generic.List[System.Threading.Tasks.Task]
for ($i = 0; $i -lt $Requests; $i++) {
    $content = New-Object System.Net.Http.StringContent($body, [System.Text.Encoding]::UTF8, "application/json")
    $tasks.Add($client.PostAsync("$BaseUrl/orders", $content))
}
[System.Threading.Tasks.Task]::WaitAll($tasks.ToArray())

$codes = $tasks | ForEach-Object { [int]$_.Result.StatusCode }
Write-Host ""
Write-Host "Status codes:"
$codes | Group-Object | Sort-Object Name | ForEach-Object { Write-Host ("  {0} x {1}" -f $_.Count, $_.Name) }

$confirmed = ($codes | Where-Object { $_ -eq 201 }).Count
$stockAfter = Get-Stock
Write-Host ""
Write-Host "Confirmed orders (201): $confirmed"
Write-Host "Stock after: $stockAfter"

if ($confirmed -eq 10 -and $stockAfter -eq 0) {
    Write-Host "RESULT: PASS - exactly 10 orders confirmed and stock ended at 0" -ForegroundColor Green
} else {
    Write-Host "RESULT: FAIL - expected 10 confirmed orders and stock 0 (restart order-service before re-running)" -ForegroundColor Red
    exit 1
}
