$deploymentName = "discord-bot"

$isCleaningUp = $false

function Stop-Services
{
    if ($isCleaningUp)
    {
        return
    }
    $isCleaningUp = $true

    Write-Host "Shutdown detected! Cleaning up resources..." -ForegroundColor Yellow

    Write-Host " # Scaling deployment '$deploymentName' back up to 1..."
    oc scale --replicas=1 deployment/$deploymentName

    if ($null -ne $portForwardProcess)
    {
        Write-Host " # Stopping port-forward process (PID: $( $portForwardProcess.Id ))..."
        Stop-Process -Id $portForwardProcess.Id -Force
    }

    Write-Host "Cleanup complete." -ForegroundColor Green
}

trap
{
    Stop-Services
    exit 0
}

try
{
    Write-Host "Starting local development environment..." -ForegroundColor Cyan

    Write-Host " # Scaling deployment '$deploymentName' down to 0..."
    oc scale --replicas=0 deployment/$deploymentName

    Write-Host " # Starting port-forward to PostgreSQL..."
    $portForwardProcess = Start-Process oc -ArgumentList "port-forward svc/postgresql 5432:5432" -PassThru -NoNewWindow
    Write-Host " # Port-forward started in background (PID: $( $portForwardProcess.Id ))"

    Start-Sleep -Seconds 3

    Write-Host "Running the Kotlin application via Gradle..." -ForegroundColor Green

    if ($IsWindows)
    {
        ./gradlew.bat run
    }
    else
    {
        ./gradlew run
    }

}
finally
{
    Stop-Services
}