# Levanta los 9 microservicios en ventanas separadas, en el orden correcto.
# Requisitos: PostgreSQL con las bases de init.sql, RabbitMQ corriendo y los .jar compilados (mvn clean package -DskipTests).
# Java 21: usa esta ruta si existe; si no, JAVA_HOME o el "java" del PATH (en otras computadoras)
$java21 = "C:\Program Files\Amazon Corretto\jdk21.0.12_8\bin\java.exe"
$java = if (Test-Path $java21) { $java21 } elseif ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$base = $PSScriptRoot

# Secretos locales (token de Telegram, etc.): copia secretos.local.ps1.ejemplo a secretos.local.ps1 y complétalo
$secretos = Join-Path $base 'secretos.local.ps1'
if (Test-Path $secretos) { . $secretos; Write-Host "Secretos locales cargados." } 

function Esperar-Puerto($puerto, $segundos = 120) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        $tcp = [System.Net.Sockets.TcpClient]::new()
        try { if ($tcp.ConnectAsync('localhost', $puerto).Wait(500)) { return $true } } catch {} finally { $tcp.Dispose() }
        Start-Sleep -Milliseconds 1000
    }
    return $false
}

function Iniciar($ms) {
    $jar = Join-Path $base "$ms\target\$ms-1.0.0.jar"
    if (-not (Test-Path $jar)) { throw "No existe $jar. Compila primero con: mvn clean package -DskipTests" }
    Start-Process powershell -ArgumentList '-NoExit', '-Command', "`$host.UI.RawUI.WindowTitle = '$ms'; & '$java' -jar '$jar'"
}

function Iniciar-Y-Esperar($ms, $puerto) {
    Write-Host "Iniciando $ms (puerto $puerto)..." -NoNewline
    Iniciar $ms
    if (Esperar-Puerto $puerto) { Write-Host " listo" -ForegroundColor Green }
    else { Write-Host " no respondió, revisa su ventana" -ForegroundColor Red }
}

Iniciar-Y-Esperar 'ms-config' 8888
Iniciar-Y-Esperar 'ms-eureka' 8761

$negocio = [ordered]@{ 'ms-auth' = 8081; 'ms-productos' = 8082; 'ms-lotes' = 8083
                       'ms-cotizaciones' = 8084; 'ms-alertas' = 8085; 'ms-reportes' = 8086 }
foreach ($ms in $negocio.Keys) { Iniciar-Y-Esperar $ms $negocio[$ms] }

Iniciar-Y-Esperar 'ms-gateway' 8080

Write-Host ""
Write-Host "Eureka (servicios registrados): http://localhost:8761"
Write-Host "API Gateway:                    http://localhost:8080"
