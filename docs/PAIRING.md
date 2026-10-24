# Missing pairing consent, not a controller passkey limitation

The TV discovered the device and started bonding, then failed with authentication status 9.
Its log showed `pairingVariant 2`, but the stock TV pairing dialog never supplied consent.
The manifest's pairing-request receiver had an empty intent filter in the inspected TV settings APK.

An early parser used incorrect HCI numbers and led to a wrong conclusion. The corrected mapping is:

| HCI event | Meaning |
|---|---|
| 0x31 | IO Capability Request |
| 0x32 | IO Capability Response |
| 0x33 | User Confirmation Request |
| 0x34 | User Passkey Request |
| 0x36 | Simple Pairing Complete |
| 0x3B | User Passkey Notification |

The IO Capability Response has six address bytes, then IO, OOB, auth. It has no leading status byte.
In both original and modified-stack captures the peer response was present, reporting IO=3
(NoInputNoOutput), followed by 0x33 and no confirmation reply. Command 0x042C accepts confirmation;
0x042D rejects it; 0x042E supplies a passkey. The corrected parser is in `tools/parse_btsnoop.py`.
Raw captures are intentionally not published because they contain nearby device identifiers.

Reference: [Bluetooth SIG Core HCI specification](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Core-61/out/en/host-controller-interface/host-controller-interface-functional-specification.html),
sections 7.7.41-7.7.48 and 7.1.30.

## One-shot confirmation

`pairing/BtPair.java` calls the existing `BluetoothDevice.setPairingConfirmation(true)` only for an
address supplied explicitly by the operator and only while it is bonding. It does not weaken the
global pairing policy, replace Bluetooth libraries, or auto-approve all nearby devices.

Build from Windows with JDK 17/Android SDK 34, then push `pairing/build/classes.dex`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\pairing\build.ps1
adb push pairing/build/classes.dex /data/local/tmp/bt-pair.dex
```

With your owned device in pairing mode, replace `AA:BB:CC:DD:EE:FF` in this command:

```sh
adb shell "su -c 'CLASSPATH=/data/local/tmp/bt-pair.dex app_process /system/bin BtPair pair AA:BB:CC:DD:EE:FF; sleep 1; CLASSPATH=/data/local/tmp/bt-pair.dex app_process /system/bin BtPair confirm AA:BB:CC:DD:EE:FF; sleep 2; CLASSPATH=/data/local/tmp/bt-pair.dex app_process /system/bin BtPair status AA:BB:CC:DD:EE:FF'"
```

The local fixed-address prototype returned `createBond=true`, `confirmation=true`, then `bond=12`
(BONDED). A slower first attempt failed; prompt confirmation was necessary. A2DP connected shortly
after bonding, with AAC, and the user heard TV audio. The parameterized public variant changes only
address selection/argument validation; its live behavior has not been retested on the TV.
Do not re-pair a device already bonded. In normal use the panel connects an existing bond.

Native Just Works library patches and an EdXposed approach were explored before this was understood.
The library patches were disabled. Stock library SHA-256 after restoration:
`6f2c24a63a2edf5d118e1b7014c2b425f2d570ade77e40d55d79719c6f6b718e`.
No Magisk pairing module remains necessary. There is no need to distribute or install those patches.
