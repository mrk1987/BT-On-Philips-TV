# Enabling A2DP: the two gates

Firmware: Philips TPM171E_R.107.001.143.000, Android 8.0, PH7M_EU_5596. A2DP is disabled out of the
box on the tested TV, but the firmware contains:

- Native source functions and SBC encoder in `/system/lib/hw/bluetooth.default.so`.
- `/system/lib/hw/audio.a2dp.default.so` and a vendor audio-policy A2DP include.
- Matching stack/HAL sockets `/data/misc/bluedroid/.a2dp_ctrl` and `.a2dp_data`.
- Java A2dpService/A2dpStateMachine. Available local codecs were SBC, AAC and LDAC.

## Gates

1. Bluetooth's `Config.init()` reads `bool/profile_supported_a2dp` (false, resource 0x7f010006).
2. Its manifest sets `.a2dp.A2dpService` enabled state from the same false resource. Root package-manager
   override is needed in addition to the runtime resource overlay.

`settings get global bluetooth_disabled_profiles` was 0, so it was not another blocker.
An RRO changes the resource without replacing/re-signing the system Bluetooth APK. This is lower
invasiveness than injecting hooks into zygote, but changes to system components still require care.

## Build overlay

Set JAVA_HOME to JDK 17 and optionally ANDROID_SDK_ROOT/ANDROID_HOME, with platform/build-tools 34:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\overlay\build.ps1
```

The script compiles aapt2 resources, links the code-free APK, aligns it, and signs with a locally
generated debug key in `overlay/.local/`. Keep the same key for updates. Build artifacts/keys are ignored.
The source is the same manifest/resource used on the TV; the standalone build script is an export of
the local aapt2 workflow. No original signing key is published.

## Install and verify

Install APK, enable overlay, root-enable service, reboot, in that order (main README has commands).
Plain shell `pm enable` was rejected by this firmware; `su -c` succeeded.
Only after the reboot did logs show `AdapterServiceConfig: Adding A2dpService`.

```sh
adb shell cmd overlay list --user 0 com.android.bluetooth
adb shell dumpsys bluetooth_manager
adb shell dumpsys media.audio_policy
```

Expected overlay `[x] com.gathin.bt_a2dp_overlay`, `Profile: A2dpService`, and audio module `a2dp`.
Registration proves the profile is running, not that a headset has connected. After pairing, I checked
`mCurrentDevice`, `curState=Connected`, `Current Codec: AAC`, PCM consumption, and the device output
in audio policy, then listened to confirm sound. Profile enablement survived its required reboot.

References:
[AOSP Android 8 Config.java](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/android-8.0.0_r1/src/com/android/bluetooth/btservice/Config.java),
[AOSP A2dpService.java](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/android-8.0.0_r1/src/com/android/bluetooth/a2dp/A2dpService.java).
