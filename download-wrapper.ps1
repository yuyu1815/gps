$wrapperJarUrl = "https://github.com/gradle/gradle/raw/v8.11.1/gradle/wrapper/gradle-wrapper.jar"
$outputPath = "gradle\wrapper\gradle-wrapper.jar"

Write-Host "Downloading Gradle wrapper JAR from $wrapperJarUrl to $outputPath"
Invoke-WebRequest -Uri $wrapperJarUrl -OutFile $outputPath

if (Test-Path $outputPath) {
    Write-Host "Successfully downloaded gradle-wrapper.jar"
} else {
    Write-Host "Failed to download gradle-wrapper.jar"
    exit 1
}