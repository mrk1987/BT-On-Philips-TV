# Bluetooth Audio

A remote-friendly Android TV audio panel that slides over the left 40% of the screen.

- **Connect Bluetooth Audio** connects the selected paired audio device, enabling Bluetooth if needed.
- **Choose audio device** selects from paired Bluetooth audio sinks, headphones, headsets and speakers.
- **Use TV speakers** disconnects connected A2DP devices without disabling the radio.
- **Turn Bluetooth on/off** switches the radio.
- **Pair another device** opens the stock TV accessory pairing screen.

Back closes the panel. Launching it again (for example pressing the mapped blue button) toggles it closed.
Volume keys adjust the active output. The panel saves volume per device;
Connect restores it once the Bluetooth route settles. New devices keep the current level, with no
high-volume default. Leave the panel open until connection and volume restoration finish.
Connections made outside the panel follow the firmware's own volume behavior.

## Compatibility

Built for Android 8 / API 26. Tested on a Philips TPM171E Android 8 TV with A2DP enabled separately.
The app does not enable missing firmware profiles. No root is required at runtime.
It uses reflected Android 8 connection APIs and targets API 26. Newer Android versions and their
Bluetooth permission restrictions are not yet supported or tested. The accessory screen depends
on firmware. Some underlying apps may pause; the panel is a translucent activity, not a system overlay.

No device address is embedded. Selection and volume preferences stay in private app data.
There is no network permission or analytics.

## Windows build

Install JDK 17, Android SDK platform 34 and build tools 34.0.0. Set `JAVA_HOME` and optionally
`ANDROID_SDK_ROOT` or `ANDROID_HOME`; otherwise the standard Windows SDK location is used.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

The output is `bt-control.apk`. The script creates a local debug key in `.local/` by default.
For release signing, pass `-Keystore <absolute-path> -KeyAlias <alias>` and set
`BT_KEYSTORE_PASSWORD` in the process environment. Keep keys private and use the same key for updates.

## Optional blue-button mapping

For a compatible cleanyourtv launcher with HomeKeyService enabled, add this to the existing theme's
`app_keys` list, preserving other mappings, then reload the key service:

```json
"KEYCODE_PROG_BLUE:com.gathin.btcontrol"
```

Activity: `com.gathin.btcontrol/.MainActivity`. Other remotes/launchers need their own mapping method.

## Sharing

Share only source, resources, manifest, build script, README and `.gitignore` from this folder.
Local backups, screenshots, device notes, Bluetooth captures, build outputs and signing keys do not
belong in the public repository. Original panel source is MIT licensed; see ../LICENSE. Root/profile enablement is documented in the repository README.
