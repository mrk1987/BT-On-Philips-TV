$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK installation.' }
$sdk=$env:ANDROID_SDK_ROOT
if (!$sdk) { $sdk=$env:ANDROID_HOME }
if (!$sdk) { $sdk="$env:LOCALAPPDATA/Android/Sdk" }
$tools="$sdk/build-tools/34.0.0"
function Check { if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force build,.local | Out-Null
& "$tools/aapt2.exe" compile --dir res -o build/res.zip
Check
& "$tools/aapt2.exe" link -o build/unsigned.apk --manifest AndroidManifest.xml -I "$sdk/platforms/android-34/android.jar" build/res.zip
Check
& "$tools/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk
Check
$key=Join-Path $PSScriptRoot '.local/debug.keystore'
if (!(Test-Path -LiteralPath $key)) {
  & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug'
  Check
}
& "$tools/apksigner.bat" sign --ks $key --ks-pass pass:android --key-pass pass:android --out bt-a2dp-overlay.apk build/aligned.apk
Check
& "$tools/apksigner.bat" verify bt-a2dp-overlay.apk
Check
