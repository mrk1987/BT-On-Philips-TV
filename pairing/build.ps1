$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK installation.' }
$sdk=$env:ANDROID_SDK_ROOT
if (!$sdk) { $sdk=$env:ANDROID_HOME }
if (!$sdk) { $sdk="$env:LOCALAPPDATA/Android/Sdk" }
function Check { if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force build | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -source 8 -target 8 -encoding UTF-8 -cp "$sdk/platforms/android-34/android.jar" -d build BtPair.java
Check
& "$sdk/build-tools/34.0.0/d8.bat" --lib "$sdk/platforms/android-34/android.jar" --min-api 26 --output build build/BtPair.class
Check
