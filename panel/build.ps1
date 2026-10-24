param([switch]$SkipCompile, [string]$Keystore, [string]$KeyAlias = 'androiddebugkey')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to your JDK installation.' }
$sdk=$env:ANDROID_SDK_ROOT
if (!$sdk) { $sdk=$env:ANDROID_HOME }
if (!$sdk) { $sdk="$env:LOCALAPPDATA/Android/Sdk" }
New-Item -ItemType Directory -Force build | Out-Null
$btTools="$sdk/build-tools/34.0.0"
function Check { if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" } }
if (!$SkipCompile) {
  & "$env:JAVA_HOME/bin/javac.exe" -source 8 -target 8 -encoding UTF-8 -cp "$sdk/platforms/android-34/android.jar" -d build src/com/gathin/btcontrol/MainActivity.java
  Check
}
$btClasses=@(Get-ChildItem build/com/gathin/btcontrol/*.class | ForEach-Object FullName)
& "$btTools/d8.bat" --lib "$sdk/platforms/android-34/android.jar" --min-api 26 --output build @btClasses
Check
& "$btTools/aapt2.exe" compile --dir res -o build/res.zip
Check
& "$btTools/aapt2.exe" link -o build/unsigned.apk --manifest AndroidManifest.xml -I "$sdk/platforms/android-34/android.jar" build/res.zip
Check
& "$env:JAVA_HOME/bin/jar.exe" uf build/unsigned.apk -C build classes.dex
Check
& "$btTools/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk
Check
if (!$Keystore) {
  New-Item -ItemType Directory -Force .local | Out-Null
  $Keystore=Join-Path $PSScriptRoot '.local/debug.keystore'
  $KeyAlias='androiddebugkey'
  $env:BT_KEYSTORE_PASSWORD='android'
  if (!(Test-Path -LiteralPath $Keystore)) {
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $Keystore -storepass android -keypass android -alias $KeyAlias -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug'
    Check
  }
}
if (!$env:BT_KEYSTORE_PASSWORD) { throw 'Set BT_KEYSTORE_PASSWORD for your signing keystore.' }
& "$btTools/apksigner.bat" sign --ks $Keystore --ks-key-alias $KeyAlias --ks-pass env:BT_KEYSTORE_PASSWORD --key-pass env:BT_KEYSTORE_PASSWORD --out bt-control.apk build/aligned.apk
Check
& "$btTools/apksigner.bat" verify bt-control.apk
Check
